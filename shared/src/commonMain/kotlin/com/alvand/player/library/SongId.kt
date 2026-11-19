package com.alvand.player.library

/**
 * آیدی پایدار آهنگ‌های روی دیسک.
 *
 * ویندوز مثل اندروید جدول مرکزی رسانه ندارد، پس «هویت» یک فایل را از روی مسیر
 * خودش می‌سازیم: SHA-256 مسیرِ نرمال‌شده، ۵۶ بیت اول، با علامت مثبت.
 *
 * سه چیز تضمین می‌شود و هر سه تست دارند:
 *  ۱. بین دو بار اسکن، آیدی عوض نمی‌شود (کاربر پلی‌لیست و علاقه‌مندی‌اش حفظ می‌شود)
 *  ۲. آیدی هیچ‌وقت منفی نمی‌شود، پس با آیدی لینک مستقیم (که بین ‎-999_999‎ و ‎-1‎ است)
 *     و آیدی فایلِ کشیده‌شده روی پنجره (کمتر از ‎-2_000_000‎) قاطی نمی‌شود
 *  ۳. «نرمال‌سازی» یعنی حروف کوچک و `\` به `/`، تا همان فایل روی درایوهای مختلف
 *     آیدی یکسان بگیرد
 */
object SongId {

    fun forPath(path: String): Long {
        val normalized = normalize(path)
        val hash = Sha256.digest(normalized.encodeToByteArray())
        // ۷ بایت اول = ۵۶ بیت؛ Long مثبت ۶۳ بیت است ولی ما عمداً ۵۶ بیت برمی‌داریم
        var v = 0L
        for (i in 0 until 7) {
            v = (v shl 8) or (hash[i].toLong() and 0xFF)
        }
        return v and Long.MAX_VALUE
    }

    fun normalize(path: String): String = path.replace('\\', '/').lowercase()
}
