package com.alvand.player.player

/**
 * تایمر خواب — قطع خودکار پخش بعد از مدت انتخابی.
 *
 * پیاده‌سازی اوریجینال الوند، عمداً بدون نخ و بدون ساعت واقعی:
 *  - فقط ریاضی خالص روی `nowMs` است تا بدون نمایشگر و بدون flaky تست شود.
 *  - مالک واقعی زمان در لایهٔ دسکتاپ است (یک چک ۱ ثانیه‌ای)، اینجا فقط
 *    «کی منقضی شده؟ چقدر مانده؟» جواب می‌دهد.
 *  - وقتی کاربر دستی pause کند، تایمر نمی‌سوزد — فقط وقتی `isPlaying=true`
 *    بوده منقضی شود، موتور pause می‌شود (تصمیم در Main).
 */
object SleepTimer {

    /** دقیقه‌های استاندارد؛ 0 یعنی خاموش */
    val OPTIONS_MINUTES = listOf(0, 15, 30, 45, 60)

    /** چرخهٔ دکمه: خاموش → ۱۵ → ۳۰ → ۴۵ → ۶۰ → خاموش */
    fun nextOption(currentMinutes: Int): Int {
        val idx = OPTIONS_MINUTES.indexOf(currentMinutes)
        if (idx < 0) return OPTIONS_MINUTES[0]
        return OPTIONS_MINUTES[(idx + 1) % OPTIONS_MINUTES.size]
    }

    /** deadline مطلق بر حسب ms. minutes<=0 یعنی خاموش (null). */
    fun deadlineFromNow(nowMs: Long, minutes: Int): Long? {
        if (minutes <= 0) return null
        return nowMs + minutes * 60_000L
    }

    fun remainingMs(deadlineMs: Long?, nowMs: Long): Long {
        if (deadlineMs == null) return 0L
        return (deadlineMs - nowMs).coerceAtLeast(0L)
    }

    fun isExpired(deadlineMs: Long?, nowMs: Long): Boolean {
        if (deadlineMs == null) return false
        return nowMs >= deadlineMs
    }

    /** قالب `mm:ss` همیشه چپ‌به‌راست — مثل formatTime صفحهٔ پلیر */
    fun formatRemaining(remainingMs: Long): String {
        if (remainingMs <= 0) return "0:00"
        val s = remainingMs / 1000
        return "%d:%02d".format(s / 60, s % 60)
    }
}
