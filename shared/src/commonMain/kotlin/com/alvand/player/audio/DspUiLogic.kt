package com.alvand.player.audio

/**
 * منطق خالص UI اکولایزر — بدون Compose، بدون وابستگی جدید.
 *
 * موتور (`DspChain`) و ماندگاری (`DspSpecCodec`) از قبل آماده‌اند؛ اینجا فقط
 * «انتخاب پریست یعنی چه باندهایی؟» و «چطور نمایش بدهیم؟» جواب داده می‌شود تا
 * هم UI نازک بماند هم بدون نمایشگر تست شود.
 */
object DspUiLogic {

    /** باندهای یک پریست، یا null اگر دستی/نامعتبر (یعنی باندهای فعلی بمانند) */
    fun bandsForPreset(preset: Int): List<Int>? {
        if (preset < 0) return null
        val name = EqPresets.DISPLAY_ORDER.getOrNull(preset) ?: return null
        val curve = EqPresets.CURVES[name] ?: return null
        return EqPresets.interpolate(EqPresets.defaultBandFrequencies(), curve).toList()
    }

    /** نام نمایشی پریست؛ ‎-1‎ یعنی دستی */
    fun presetDisplayName(preset: Int): String {
        if (preset < 0) return "manual"
        return EqPresets.DISPLAY_ORDER.getOrNull(preset) ?: "manual"
    }

    /** ‎+3.0 dB‎ / ‎-1.5 dB‎ — همیشه با علامت تا بالا/پایین بودن معلوم باشد */
    fun formatBandDb(millibel: Int): String {
        val db = millibel / 100.0
        val sign = if (db >= 0) "+" else ""
        return "$sign${"%.1f".format(db)} dB"
    }

    fun formatBassDb(strength: Int): String {
        val db = BassBoost.strengthToDb(strength)
        return "${"%.1f".format(db)} dB"
    }

    fun formatBoostDb(db: Int): String = "+${db} dB".let {
        if (db <= 0) "0 dB" else it
    }

    /** برچسب فرکانس باند: ‎20Hz‎ … ‎14.0kHz‎ */
    fun formatBandFreq(hz: Double): String = when {
        hz < 1000 -> "${hz.toInt()}Hz"
        else -> "${"%.1f".format(hz / 1000.0)}kHz"
    }

    fun bandFrequencies(): DoubleArray = EqPresets.defaultBandFrequencies()

    /** اعمال انتخاب پریست روی spec — باندها از منحنی، بقیه دست‌نخورده */
    fun applyPreset(spec: DspSpec, preset: Int): DspSpec {
        val bands = bandsForPreset(preset) ?: return spec.copy(preset = -1)
        return spec.copy(preset = preset, bandLevels = bands, eqEnabled = true)
    }

    /** تغییر یک باند یعنی حالت دستی */
    fun withBand(spec: DspSpec, index: Int, millibel: Int): DspSpec {
        if (index !in 0 until DspSpec.EQ_BANDS) return spec
        val next = spec.bandLevels.toMutableList()
        next[index] = millibel.coerceIn(DspSpec.MIN_MB, DspSpec.MAX_MB)
        return spec.copy(bandLevels = next, preset = -1, eqEnabled = true)
    }

    fun reset(): DspSpec = DspSpec()
}
