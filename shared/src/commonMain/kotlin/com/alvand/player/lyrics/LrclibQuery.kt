package com.alvand.player.lyrics

import java.net.URLEncoder

/**
 * کوئری LRCLIB — API رایگان و بدون کلید متن آهنگ.
 *
 * فقط وقتی صدا زده می‌شود که کاربر صریحاً «جستجوی آنلاین» را روشن کرده باشد
 * (پیش‌فرض خاموش). تنها چیزی که ارسال می‌شود «نام خواننده + عنوان + آلبوم +
 * مدت» است — هیچ فایلی آپلود نمی‌شود و هیچ شناسه‌ای فرستاده نمی‌شود.
 *
 * JSON با دستِ خودمان و بدون کتابخانه خوانده می‌شود (پاسخ فقط دو فیلد لازم
 * دارد) تا وابستگی جدید نیاید.
 */
object LrclibQuery {

    const val HOST = "https://lrclib.net"
    const val GET_PATH = "/api/get"

    fun buildGetUrl(artist: String, title: String, album: String, durationSec: Int): String {
        val params = listOf(
            "artist_name" to artist,
            "track_name" to title,
            "album_name" to album,
            "duration" to durationSec.coerceAtLeast(0).toString()
        ).joinToString("&") { (k, v) -> "$k=${encode(v)}" }
        return "$HOST$GET_PATH?$params"
    }

    /**
     * استخراج رشتهٔ یک کلید از JSON تخت — با پشتیبانی از escapeهای استاندارد.
     * برای پاسخ LRCLIB (آبجکت تخت با چند فیلد) کافی است؛ اگر ساختار عوض شود،
     * null برمی‌گردد و لایهٔ بالا آن را «خطا» گزارش می‌کند، نه کرش.
     */
    fun extractJsonString(json: String, key: String): String? {
        val needle = "\"$key\""
        var i = json.indexOf(needle)
        if (i < 0) return null
        i += needle.length
        while (i < json.length && json[i].isWhitespace()) i++
        if (i >= json.length || json[i] != ':') return null
        i++
        while (i < json.length && json[i].isWhitespace()) i++
        if (i >= json.length) return null
        if (json.startsWith("null", i)) return null
        if (json[i] != '"') return null
        i++
        val sb = StringBuilder()
        while (i < json.length) {
            val c = json[i]
            if (c == '"') return sb.toString()
            if (c == '\\' && i + 1 < json.length) {
                when (val e = json[i + 1]) {
                    'n' -> sb.append('\n')
                    'r' -> sb.append('\r')
                    't' -> sb.append('\t')
                    '"', '\\', '/' -> sb.append(e)
                    'u' -> {
                        val hex = json.substring(i + 2, (i + 6).coerceAtMost(json.length))
                        sb.append(hex.toIntOrNull(16)?.toChar() ?: '?')
                        i += 4
                    }
                    else -> sb.append(e)
                }
                i += 2
            } else {
                sb.append(c)
                i++
            }
        }
        return null // کوتیشن بسته نشد — JSON ناقص
    }

    private fun encode(value: String): String =
        runCatching { URLEncoder.encode(value, "UTF-8") }.getOrDefault(value)
}
