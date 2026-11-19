package com.alvand.player.ui.components

/**
 * قالب زمان — همان `formatTime` نسخهٔ اندروید: `mm:ss` و اگر ساعت شد `h:mm:ss`.
 *
 * یک جا تعریف شده تا همهٔ صفحه‌ها (پنل در حال پخش، کتابخانه، اکتشاف، ویژوالایزر،
 * شناسنامهٔ آهنگ و پرش به زمان) دقیقاً یک شکل زمان نشان دهند.
 */
fun formatTime(ms: Long): String {
    if (ms <= 0L) return "0:00"
    val totalSeconds = ms / 1000
    val seconds = totalSeconds % 60
    val minutes = (totalSeconds / 60) % 60
    val hours = totalSeconds / 3600
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}