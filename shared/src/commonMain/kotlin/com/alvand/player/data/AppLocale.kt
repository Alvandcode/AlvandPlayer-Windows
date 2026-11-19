package com.alvand.player.data

/**
 * زبان‌های پشتیبانی‌شده + منطق نرمال‌سازی تگ زبان.
 *
 * دقیقاً همان دو زبان نسخهٔ اندروید: en و fa. هیچ زبان دیگری اضافه نمی‌شود.
 *
 * بخش خالص این فایل در `shared` است تا بدون وابستگی به سیستم‌عامل تست شود؛
 * اعمال واقعی زبان در لایهٔ دسکتاپ (`AppLocale.desktop`) انجام می‌شود.
 */
object AppLocale {

    data class Lang(val code: String, val nativeName: String)

    val all = listOf(
        Lang("en", "English"),
        Lang("fa", "فارسی")
    )

    private val codeSet = all.map { it.code }.toSet()

    /** نرمالایز تگ زبان؛ زبان‌های بدون ترجمه به انگلیسی برمی‌گردند */
    fun normalize(code: String): String {
        val c = code.lowercase()
        val base = c.substringBefore("-").substringBefore("_")
        return if (base in codeSet) base else "en"
    }

    /**
     * تگ زبان فعلی اپ، مثل fa یا en — چندلوکیله و منطقه را درست هندل می‌کند.
     * [rawTags] همان خروجی `Locale.toLanguageTags()` است.
     */
    fun currentTag(rawTags: String): String {
        val tags = rawTags.ifBlank { return "en" }
        // اولویت اول کاربر، بعد بقیه — فقط زبان‌های واقعاً پشتیبانی‌شده پذیرفته می‌شوند
        for (raw in tags.split(",")) {
            val base = raw.trim().lowercase().substringBefore("-").substringBefore("_")
            if (base in codeSet) return base
        }
        return "en"
    }
}
