package com.alvand.player.library

import com.alvand.player.model.Song
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.TagTextField
import java.nio.file.Path
import kotlin.io.path.name

/** متادیتای خوانده‌شده از تگ فایل صوتی */
data class TagInfo(
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val durationMs: Long? = null,
    /** بهرهٔ ReplayGain بر حسب دسی‌بل (تگ REPLAYGAIN_TRACK_GAIN) */
    val replayGainDb: Float? = null,
    /** ژانر (تگ GENRE) */
    val genre: String? = null
)

/**
 * خواندن تگ با jaudiotagger (کتابخانهٔ خالص JVM).
 *
 * روی اندروید این کار را MediaStore انجام می‌داد؛ اینجا خودمان فایل را باز می‌کنیم.
 *
 * نکتهٔ API: در jaudiotagger 3.x، `TagField` رابط خام است و `getValue()` ندارد؛
 * مقدار متنی از `TagTextField.getContent()` می‌آید.
 *
 * نکتهٔ اجرایی: باز کردن فایل کار سنگینی است و باید روی نخ پس‌زمینه انجام شود.
 * مسئولیت نخ با لایهٔ بالاست (`LibraryStore`).
 */
object TagReader {

    /** هرگز Exception پرتاب نمی‌کند؛ فایل خراب یعنی «تگ نداریم». */
    fun read(path: Path): TagInfo? = runCatching {
        val file = AudioFileIO.read(path.toFile())
        val tag = file.tag

        fun text(key: FieldKey): String? = runCatching {
            (tag.getFirstField(key) as? TagTextField)?.content?.trim()?.takeIf { it.isNotEmpty() }
        }.getOrNull()

        val durationSec = runCatching { file.audioHeader.trackLength }.getOrNull()?.toLong()

        TagInfo(
            title = text(FieldKey.TITLE),
            artist = text(FieldKey.ARTIST)
                ?: text(FieldKey.ALBUM_ARTIST)
                ?: text(FieldKey.PERFORMER),
            album = text(FieldKey.ALBUM),
            durationMs = durationSec?.let { it * 1000L },
            replayGainDb = replayGainDbOf(tag),
            genre = text(FieldKey.GENRE)
        )
    }.getOrNull()

    /**
     * بهرهٔ ReplayGain از یک تگِ ازپیش‌خوانده‌شده.
     *
     * جت‌آدیو و VLC هر دو ReplayGain را رعایت می‌کنند تا بلندی آهنگ‌ها یکدست شود.
     * مقدار معمولاً به شکل `-7.25 dB` است؛ نبود تگ یعنی null (بدون تغییر صدا).
     */
    fun replayGainDbOf(tag: org.jaudiotagger.tag.Tag?): Float? {
        if (tag == null) return null
        val raw = runCatching { tag.getFirst(REPLAYGAIN_TRACK_GAIN_KEY) }.getOrNull()
            ?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        return REPLAYGAIN_DB.find(raw)?.groupValues?.get(1)?.toFloatOrNull()
    }

    /**
     * خواندن بهرهٔ ReplayGain مستقیم از فایل (برای تست و موارد تکی).
     * در مسیر اسکن از [replayGainDbOf] روی همان تگِ خوانده‌شده استفاده می‌شود تا
     * فایل دوبار باز نشود.
     */
    fun readReplayGainDb(path: Path): Float? = runCatching {
        val tag = AudioFileIO.read(path.toFile()).tag ?: return null
        replayGainDbOf(tag)
    }.getOrNull()

    /**
     * ساخت مدل [Song] از روی فایل.
     *
     * قواعد نام‌گذاری، عیناً مثل SongRepository نسخهٔ اندروید:
     *  - تگ خالی → حدس از نام فایل با قالب `01 - Artist - Title.mp3`
     *  - باز هم خالی → `Unknown` / `Unknown Artist`
     */
    fun toSong(path: Path, id: Long = SongId.forPath(path.toString())): Song {
        val tags = read(path)
        val guessed = guessFromFileName(path.name)

        return Song(
            id = id,
            title = tags?.title ?: guessed?.second ?: UNKNOWN,
            artist = tags?.artist ?: guessed?.first ?: UNKNOWN_ARTIST,
            album = tags?.album.orEmpty(),
            uri = path.toAbsolutePath().toString(),
            durationMs = tags?.durationMs ?: 0L,
            isRemote = false,
            mimeHint = Song.mimeForUrl(path.toString()),
            replayGainDb = tags?.replayGainDb,
            genre = tags?.genre.orEmpty()
        )
    }

    /** `01 - Artist - Title.mp3` → (artist, title). اگر الگو نبود null. */
    fun guessFromFileName(fileName: String): Pair<String, String>? {
        val base = fileName.substringBeforeLast('.', fileName)
        val parts = base.split(" - ", limit = 3).map { it.trim() }
        return when {
            parts.size >= 3 && parts[0].all { it.isDigit() } ->
                parts[1] to parts[2]
            parts.size == 2 && parts[0].all { it.isDigit() } ->
                parts[0] to parts[1]
            else -> null
        }
    }

    const val UNKNOWN = "Unknown"
    const val UNKNOWN_ARTIST = "Unknown Artist"

    private const val REPLAYGAIN_TRACK_GAIN_KEY = "REPLAYGAIN_TRACK_GAIN"
    private val REPLAYGAIN_DB = Regex("""([+-]?\d+(?:\.\d+)?)\s*dB""", RegexOption.IGNORE_CASE)
}
