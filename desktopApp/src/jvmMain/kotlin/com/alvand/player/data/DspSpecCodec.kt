package com.alvand.player.data

import com.alvand.player.audio.DspSpec

/**
 * تبدیل `DspSpec` به رشته و برگشت، برای ذخیره در فایل تنظیمات.
 *
 * **چرا دستی، نه JSON:** تنظیمات در یک فایل `properties` ساده ذخیره می‌شوند (بدون
 * وابستگی)، پس رشته‌ای فشرده و قابل خواندن کافی است. مثلاً:
 * `1;-1;0,600,-300,0,-300;500;6;1;50;0`
 *
 * **نکتهٔ مهم — ورودی نامعتبر:** هر مقداری که از فایل بخواند ممکن است خراب یا
 * دستکاری‌شده باشد. `decode` هر جزء را جداگانه بررسی می‌کند و در صورت خرابی به
 * مقدار پیش‌فرض برمی‌گردد؛ یعنی یک خطا در فایل تنظیمات **هیچ‌وقت** باعث
 * کرش نمی‌شود، برخلاف `DspSpec`'s `require` که روی تعداد باند سخت‌گیر است.
 */
object DspSpecCodec {

    /** جداکنندهٔ بخش‌ها؛ کاما داخل بخش باندها استفاده می‌شود پس سمی‌کالن امن است */
    private const val SEP = ';'

    fun encode(spec: DspSpec): String = listOf(
        if (spec.eqEnabled) 1 else 0,
        spec.preset,
        spec.bandLevels.joinToString(","),
        spec.bassStrength,
        spec.volumeBoostDb,
        if (spec.noiseReduction) 1 else 0,
        spec.noiseLevel,
        spec.reverbPreset
    ).joinToString(SEP.toString())

    fun decode(raw: String?): DspSpec? {
        val text = raw?.trim().orEmpty()
        if (text.isEmpty()) return null

        val parts = text.split(SEP)
        if (parts.size < 8) return null

        val fallback = DspSpec()
        val eqEnabled = parts[0].trim().toIntOrNull()?.let { it != 0 } ?: fallback.eqEnabled
        val preset = parts[1].trim().toIntOrNull()?.coerceIn(-1, MAX_PRESET)
            ?: fallback.preset

        val bands = parts[2].split(',')
            .map { it.trim().toIntOrNull() }
            .let { list ->
                // دقیقاً باید EQ_BANDS تا باشد وگرنه پیش‌فرض
                if (list.size != DspSpec.EQ_BANDS || list.any { it == null }) {
                    fallback.bandLevels
                } else {
                    @Suppress("UNCHECKED_CAST")
                    list.map { (it as Int).coerceIn(DspSpec.MIN_MB, DspSpec.MAX_MB) }
                }
            }

        return DspSpec(
            eqEnabled = eqEnabled,
            preset = preset,
            bandLevels = bands,
            bassStrength = parts[3].trim().toIntOrNull()?.coerceIn(0, 1000)
                ?: fallback.bassStrength,
            volumeBoostDb = parts[4].trim().toIntOrNull()?.coerceIn(0, 10)
                ?: fallback.volumeBoostDb,
            noiseReduction = parts[5].trim().toIntOrNull()?.let { it != 0 }
                ?: fallback.noiseReduction,
            noiseLevel = parts[6].trim().toIntOrNull()?.coerceIn(0, 100)
                ?: fallback.noiseLevel,
            reverbPreset = parts[7].trim().toIntOrNull()?.coerceIn(0, MAX_REVERB)
                ?: fallback.reverbPreset
        )
    }

    private const val MAX_PRESET = 16
    private const val MAX_REVERB = 16
}