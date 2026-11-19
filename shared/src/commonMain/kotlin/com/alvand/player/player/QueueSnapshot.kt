package com.alvand.player.player

import com.alvand.player.model.Song

/**
 * اسنپ‌شات صف پخش — ماندگاری صف بین اجراها.
 *
 * ایدهٔ کلی (صف نباید با بستن برنامه بپرد) از پلیرهای مدرن گرفته شده،
 * ولی پیاده‌سازی کاملاً اوریجینال الوند است:
 *  - فقط `uri` + `index` ذخیره می‌شود، نه آبجکت کامل — پس با اسکن دوباره
 *    که آیدی‌ها پایدارند (SHA-256 مسیر) بازسازی دقیق است.
 *  - رشته فشرده و امن برای فایل properties است (بدون newline واقعی).
 *  - هر رشتهٔ خراب → null، نه کرش — تنظیمات دستکاری‌شده فقط صف را خالی می‌کند.
 *  - بازسازی هرگز autoplay نمی‌کند؛ مالکیت پخش همیشه با کاربر است.
 *
 * فرمت: `v1|<index>|<count>|<uri1>\u001F<uri2>...`
 * که در آن `\` و `\u001F` داخل uriها escape می‌شوند (`\\` و `\u001E`).
 */
object QueueSnapshot {

    private const val PREFIX = "v1"
    private const val SEP = '|'
    private const val UNIT = ''
    private const val ESC = '\\'
    private const val ESCAPED_UNIT = ''
    const val MAX_ITEMS = 8000
    const val MAX_SNAPSHOT_CHARS = 300_000

    fun encode(uris: List<String>, index: Int): String {
        val safeIndex = if (uris.isEmpty()) -1 else index.coerceIn(0, uris.lastIndex)
        val limited = uris.take(MAX_ITEMS)
        val body = limited.joinToString(UNIT.toString()) { escape(it) }
        val raw = "$PREFIX$SEP$safeIndex$SEP${limited.size}$SEP$body"
        return if (raw.length > MAX_SNAPSHOT_CHARS) "" else raw
    }

    fun encodeQueue(queue: List<PlayableItem>, index: Int): String =
        encode(queue.map { it.uri }, index)

    /** @return Pair(uris, index) یا null اگر خراب باشد */
    fun decode(snapshot: String): Pair<List<String>, Int>? {
        if (snapshot.isBlank()) return null
        val head = snapshot.split(SEP, limit = 4)
        if (head.size != 4) return null
        if (head[0] != PREFIX) return null
        val index = head[1].toIntOrNull() ?: return null
        val count = head[2].toIntOrNull() ?: return null
        if (count < 0 || count > MAX_ITEMS) return null
        if (count == 0) return emptyList<String>() to -1
        val uris = splitUnescaped(head[3])
        if (uris.size != count) return null
        if (uris.any { it.isBlank() }) return null
        val safeIndex = index.coerceIn(0, uris.lastIndex)
        return uris to safeIndex
    }

    /**
     * بازسازی صف از روی کتابخانهٔ تازه اسکن‌شده.
     *
     * آهنگ‌هایی که دیگر روی دیسک نیستند بی‌صدا حذف می‌شوند (مثل رفتار اسکن
     * دوباره)، اندیس به نزدیک‌ترین آیتم موجود clamp می‌شود.
     */
    fun rebuild(
        uris: List<String>,
        index: Int,
        library: List<Song>
    ): Pair<List<PlayableItem>, Int> {
        if (uris.isEmpty() || library.isEmpty()) return emptyList<PlayableItem>() to -1
        val byUri = library.associateBy { it.uri }
        val items = uris.mapNotNull { byUri[it] }.map { PlayableItem.fromSong(it) }
        if (items.isEmpty()) return emptyList<PlayableItem>() to -1
        val wantedUri = uris.getOrNull(index)
        val newIndex = if (wantedUri != null) {
            val pos = items.indexOfFirst { it.uri == wantedUri }
            if (pos >= 0) pos else 0
        } else 0
        return items to newIndex.coerceIn(0, items.lastIndex)
    }

    private fun escape(raw: String): String {
        val sb = StringBuilder(raw.length)
        for (c in raw) {
            when (c) {
                ESC -> sb.append(ESC).append(ESC)
                UNIT -> sb.append(ESC).append(ESCAPED_UNIT)
                '\n' -> sb.append(ESC).append('n')
                '\r' -> sb.append(ESC).append('r')
                else -> sb.append(c)
            }
        }
        return sb.toString()
    }

    private fun splitUnescaped(body: String): List<String> {
        val out = ArrayList<String>()
        val cur = StringBuilder()
        var i = 0
        while (i < body.length) {
            val c = body[i]
            if (c == ESC && i + 1 < body.length) {
                when (body[i + 1]) {
                    ESC -> cur.append(ESC)
                    ESCAPED_UNIT -> cur.append(UNIT)
                    'n' -> cur.append('\n')
                    'r' -> cur.append('\r')
                    else -> { cur.append(c); cur.append(body[i + 1]) }
                }
                i += 2
            } else {
                if (c == UNIT) {
                    out.add(unescapeDone(cur))
                    cur.clear()
                } else {
                    cur.append(c)
                }
                i++
            }
        }
        out.add(unescapeDone(cur))
        return out
    }

    private fun unescapeDone(sb: StringBuilder): String = sb.toString()
}
