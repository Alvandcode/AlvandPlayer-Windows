package com.alvand.player.lyrics

/**
 * یک خط متن سنک‌شده — زمان به میلی‌ثانیه از ابتدای آهنگ.
 */
data class LrcLine(val timeMs: Long, val text: String)

/**
 * پارسر LRC — خالص و بدون وابستگی.
 *
 * قرارداد (همان LRC رایج):
 *  - `[mm:ss.xx] متن` که `xx` دو رقم (صدم‌ثانیه) یا سه رقم (میلی‌ثانیه) است.
 *  - چند تایم‌استمپ در یک خط مجاز است: `[00:10.00][00:12.00]متن` → دو خط.
 *  - تگ‌های متا (`[ar:]`, `[ti:]`, `[al:]`, `[length:]`, `[by:]`) نادیده گرفته می‌شوند.
 *  - خط خراب بی‌صدا رد می‌شود، نه خطا — فایل لیریکس اینترنت همیشه تمیز نیست.
 *  - خروجی همیشه مرتب زمانی است؛ متن خالی حذف می‌شود.
 */
object LrcParser {

    private val TIMESTAMP = Regex("""\[(\d{1,3}):(\d{1,2})[.:](\d{2,3})\]""")
    private val META = setOf("ar", "ti", "al", "au", "length", "by", "offset", "re", "ve")

    fun parse(lrc: String): List<LrcLine> {
        if (lrc.isBlank()) return emptyList()
        val out = ArrayList<LrcLine>()
        for (raw in lrc.lineSequence()) {
            val line = raw.trim()
            if (line.isEmpty()) continue
            // تگ متا: [xx:...] که xx حرفی است و تایم‌استمپ نیست
            if (isMeta(line)) continue
            val stamps = TIMESTAMP.findAll(line).toList()
            if (stamps.isEmpty()) continue
            val text = TIMESTAMP.replace(line, "").trim()
            if (text.isEmpty()) continue
            for (m in stamps) {
                val ms = toMs(m.groupValues[1], m.groupValues[2], m.groupValues[3]) ?: continue
                out.add(LrcLine(ms, text))
            }
        }
        out.sortBy { it.timeMs }
        return out
    }

    /** آخرین خطی که زمانش رسیده؛ ‎-1‎ یعنی هنوز هیچ‌چیز خوانده نشده */
    fun activeIndex(lines: List<LrcLine>, positionMs: Long): Int {
        var idx = -1
        for (i in lines.indices) {
            if (lines[i].timeMs <= positionMs) idx = i else break
        }
        return idx
    }

    private fun isMeta(line: String): Boolean {
        if (!line.startsWith('[')) return false
        val head = line.drop(1).takeWhile { it != ']' && it != ':' }.lowercase()
        if (head in META) return true
        // [ar:xxx] شکل کلی متا — حرفیِ خالصِ کوتاهِ بدون الگوی زمانی
        if (head.all { it.isLetter() } && head.length <= 6 && !line.contains(TIMESTAMP)) return true
        return false
    }

    private fun toMs(min: String, sec: String, frac: String): Long? {
        val m = min.toLongOrNull() ?: return null
        val s = sec.toLongOrNull() ?: return null
        if (s >= 60) return null
        val f = frac.toLongOrNull() ?: return null
        val ms = if (frac.length == 2) f * 10 else f
        return m * 60_000L + s * 1000L + ms
    }
}
