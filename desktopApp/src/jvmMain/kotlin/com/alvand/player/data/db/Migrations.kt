package com.alvand.player.data.db

/**
 * لیست مهاجرت‌ها — فقط‌رو‌به‌جلو، هرگز ویرایش فایل قدیمی.
 *
 * هر مهاجرت یک فایل `.sql` روی classpath است با نام `NNN_name.sql`.
 * `PRAGMA user_version` عدد نسخهٔ فعلی را نگه می‌دارد؛ وقتی نسخهٔ فایل با دیتابیس
 * نخواند، مهاجرت‌های جاافتاده اجرا می‌شوند.
 */
data class Migration(val version: Int, val name: String, val sql: String) {
    val fileName: String get() = "%03d_%s.sql".format(version, name)

    /**
     * شکستن فایل به دستورهای جدا.
     *
     * نکتهٔ مهم: جدول‌ها **چندخطی** هستند، پس تقسیم خط‌به‌خط کار نمی‌کند. اول
     * کامنت‌های `--` حذف می‌شوند، بعد روی `;` شکسته می‌شود.
     */
    fun statements(): List<String> = sql
        .lines()
        .map { it.substringBefore("--") }
        .joinToString("\n")
        .split(";")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
}

object Migrations {

    private val DEFINITIONS = listOf(
        1 to ("initial" to "/db/migrations/001_initial.sql")
    )

    val LATEST_VERSION: Int = DEFINITIONS.maxOf { it.first }

    fun all(): List<Migration> = DEFINITIONS.map { (version, spec) ->
        val (name, resource) = spec
        val sql = Migration::class.java.getResourceAsStream(resource)?.use {
            it.readBytes().toString(Charsets.UTF_8)
        } ?: error("Migration file not found on classpath: $resource")
        Migration(version, name, sql)
    }
}
