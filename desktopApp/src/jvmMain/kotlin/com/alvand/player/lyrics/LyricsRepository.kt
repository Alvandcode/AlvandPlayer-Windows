package com.alvand.player.lyrics

import com.alvand.player.model.Song
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Path
import java.util.Collections

/**
 * نتیجهٔ خام LRCLIB — بدون UI، بدون Compose.
 *
 * هرگز Exception پرتاب نمی‌کند: تایم‌اوت/قطعی/JSON خراب همه به [Failed] تبدیل
 * می‌شوند تا UI بتواند پیام صادقانه نشان بدهد (اینترنت قطع است؟ سرویس خوابیده؟
 * برای این آهنگ متنی نیست؟).
 */
sealed interface LrclibResult {
    data class Found(val syncedLrc: String?, val plainText: String?) : LrclibResult
    data object NotFound : LrclibResult
    data class Failed(val reason: String) : LrclibResult
}

object LrclibClient {

    const val CONNECT_TIMEOUT_MS = 8_000
    const val READ_TIMEOUT_MS = 8_000
    const val MAX_BODY_CHARS = 200_000

    fun fetch(song: Song): LrclibResult = runCatching {
        val url = LrclibQuery.buildGetUrl(
            artist = song.artist,
            title = song.title,
            album = song.album,
            durationSec = (song.durationMs / 1000L).toInt()
        )
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            setRequestProperty("User-Agent", "AlvandPlayer/1.1.0")
            setRequestProperty("Accept", "application/json")
            instanceFollowRedirects = true
        }
        try {
            when (conn.responseCode) {
                200 -> parseBody(readBody(conn))
                404 -> LrclibResult.NotFound
                429 -> LrclibResult.Failed("rate-limited")
                in 500..599 -> LrclibResult.Failed("server-error")
                else -> LrclibResult.Failed("http-${conn.responseCode}")
            }
        } finally {
            conn.disconnect()
        }
    }.getOrElse { t ->
        val reason = when (t) {
            is java.net.SocketTimeoutException -> "timeout"
            is java.net.UnknownHostException, is java.net.ConnectException -> "offline"
            else -> "error"
        }
        LrclibResult.Failed(reason)
    }

    private fun parseBody(body: String): LrclibResult {
        if (body.isBlank()) return LrclibResult.Failed("empty-body")
        val synced = LrclibQuery.extractJsonString(body, "syncedLyrics")
        val plain = LrclibQuery.extractJsonString(body, "plainLyrics")
        if (synced.isNullOrBlank() && plain.isNullOrBlank()) return LrclibResult.NotFound
        return LrclibResult.Found(synced?.takeIf { it.isNotBlank() }, plain?.takeIf { it.isNotBlank() })
    }

    private fun readBody(conn: HttpURLConnection): String {
        val stream = try {
            conn.inputStream
        } catch (_: Throwable) {
            return ""
        }
        return stream.bufferedReader(Charsets.UTF_8).use { reader ->
            val sb = StringBuilder()
            val buf = CharArray(8_192)
            while (sb.length < MAX_BODY_CHARS) {
                val n = reader.read(buf)
                if (n < 0) break
                sb.append(buf, 0, n)
            }
            if (sb.length > MAX_BODY_CHARS) sb.substring(0, MAX_BODY_CHARS) else sb.toString()
        }
    }
}

/**
 * مخزن متن آهنگ: حافظه → دیسک → شبکه (فقط با رضایت).
 *
 *  - کلید کش همان `Song.id` پایدار است؛ فایل `<id>.lrc` در `lyricsDir`.
 *  - فرمت فایل: اگر سنک‌شده داریم همان LRC، وگرنه `PLAIN\n` + متن ساده.
 *  - فایل خراب → نادیده گرفته می‌شود و دوباره از شبکه گرفته می‌شود.
 */
class LyricsRepository(
    private val lyricsDir: Path,
    private val fetch: (Song) -> LrclibResult = LrclibClient::fetch
) {

    private val memory = Collections.synchronizedMap(
        object : LinkedHashMap<Long, Entry>(16, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Long, Entry>?): Boolean =
                size > MAX_MEMORY
        }
    )

    sealed interface Entry {
        data class Synced(val lines: List<LrcLine>) : Entry
        data class Plain(val text: String) : Entry
    }

    /** نتیجهٔ آمادهٔ نمایش — حالت شبکه/خطا جدا گزارش می‌شود */
    sealed interface Outcome {
        data class Ready(val entry: Entry) : Outcome
        data object NotFound : Outcome
        data class Failed(val reason: String) : Outcome
    }

    fun get(song: Song, onlineEnabled: Boolean): Outcome {
        memory[song.id]?.let { return Outcome.Ready(it) }
        // اولویت با فایل کنار خود آهنگ است: کاربر معمولاً .lrc را کنار فایل می‌گذارد
        // و این فقط منبع صادقانه‌تر و بدون اینترنت است. بعد کش دیسک، بعد شبکه.
        readSidecar(song)?.let {
            memory[song.id] = it
            return Outcome.Ready(it)
        }
        readDisk(song.id)?.let {
            memory[song.id] = it
            return Outcome.Ready(it)
        }
        if (!onlineEnabled) return Outcome.NotFound
        return when (val res = fetch(song)) {
            is LrclibResult.Found -> {
                val entry = entryOf(res) ?: return Outcome.NotFound
                memory[song.id] = entry
                writeDisk(song.id, res)
                Outcome.Ready(entry)
            }
            LrclibResult.NotFound -> Outcome.NotFound
            is LrclibResult.Failed -> Outcome.Failed(res.reason)
        }
    }

    fun clearMemory() = memory.clear()

    private fun entryOf(res: LrclibResult.Found): Entry? {
        val lines = res.syncedLrc?.let { LrcParser.parse(it) }.orEmpty()
        if (lines.isNotEmpty()) return Entry.Synced(lines)
        val plain = res.plainText?.trim()
        if (!plain.isNullOrEmpty()) return Entry.Plain(plain)
        return null
    }

    private fun diskFile(id: Long): File = lyricsDir.toFile().resolve("$id.lrc")

    /**
     * فایل .lrc/.txt کنار خود فایل صوتی — قرارداد رایج همهٔ پلیرها.
     * فقط برای فایل لوکال؛ برای فایل‌های ریموت هیچ. اگر LRC معتبر نبود،
     * همان متن ساده پذیرفته می‌شود (مثل .txt).
     */
    private fun readSidecar(song: Song): Entry? = runCatching {
        if (song.isRemote) return null
        val audio = File(song.uri)
        if (!audio.isFile) return null
        val dir = audio.parentFile ?: return null
        val base = audio.nameWithoutExtension
        for (cand in arrayOf(File(dir, "$base.lrc"), File(dir, "$base.txt"))) {
            if (!cand.isFile || cand.length() <= 0L || cand.length() > 300_000L) continue
            val raw = cand.readText(Charsets.UTF_8)
            val lines = LrcParser.parse(raw)
            if (lines.isNotEmpty()) return Entry.Synced(lines)
            val plain = raw.trim()
            if (plain.isNotEmpty()) return Entry.Plain(plain)
        }
        null
    }.getOrNull()

    private fun readDisk(id: Long): Entry? = runCatching {
        val file = diskFile(id)
        if (!file.isFile || file.length() <= 0L || file.length() > 300_000L) return null
        val raw = file.readText(Charsets.UTF_8)
        if (raw.startsWith(PLAIN_PREFIX)) {
            val text = raw.removePrefix(PLAIN_PREFIX).trim()
            if (text.isEmpty()) null else Entry.Plain(text)
        } else {
            val lines = LrcParser.parse(raw)
            if (lines.isEmpty()) null else Entry.Synced(lines)
        }
    }.getOrNull()

    private fun writeDisk(id: Long, res: LrclibResult.Found) {
        runCatching {
            val file = diskFile(id)
            file.parentFile?.mkdirs()
            val content = if (!res.syncedLrc.isNullOrBlank()) res.syncedLrc else PLAIN_PREFIX + (res.plainText ?: "")
            // نوشتن اتمیک مثل تنظیمات: موقت + جابه‌جایی
            val tmp = File(file.parent, file.name + ".tmp")
            tmp.writeText(content, Charsets.UTF_8)
            if (!tmp.renameTo(file)) {
                file.delete()
                tmp.renameTo(file)
            }
        }
    }

    companion object {
        const val PLAIN_PREFIX = "PLAIN\n"
        const val MAX_MEMORY = 20
    }
}
