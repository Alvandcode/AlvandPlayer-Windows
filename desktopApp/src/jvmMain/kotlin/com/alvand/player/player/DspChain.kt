package com.alvand.player.player

import com.alvand.player.audio.BassBoost
import com.alvand.player.audio.DspSpec
import com.alvand.player.audio.EqPresets
import com.alvand.player.audio.NoiseReduction
import com.alvand.player.audio.decibelsToLinear
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * زنجیرهٔ DSP روی جریان PCM.
 *
 * ترتیب بلوک‌ها همان ترتیب زنجیرهٔ افکت‌ها در نسخهٔ اندروید است:
 * تقویت باس → اکولایزر → حذف نویز → اکو → بهرهٔ نهایی.
 *
 * **یک تفاوت که عمدی و ضروری است:** روی اندروید `LoudnessEnhancer` خودش یک
 * محدودکننده (limiter) بود. اینجا تقویت صدا فقط یک بهرهٔ ساده است؛ بدون
 * محدودکننده، بالا بردن صدا حتماً **کلیپ** می‌شود (صدای ترش و خراب). پس یک
 * محدودکننده اضافه شده. این اشکال‌زدایی است، نه قابلیت جدید.
 */
class DspChain(
    spec: DspSpec,
    private val format: AudioFormat,
    volume: Float = 1f
) {

    private var spec = spec
    private var volume = volume

    /** فیلترها با تغییر تنظیمات از نو ساخته می‌شوند، پس lazy هستند */
    private var eqFilters: List<Biquad>? = null
    private var bassFilter: Biquad? = null
    private var reverbFilter: CombFilterChain? = null

    init { rebuild() }

    fun updateSpec(newSpec: DspSpec, newVolume: Float = volume) {
        spec = newSpec
        volume = newVolume
        rebuild()
    }

    /**
     * به‌روزرسانی فقط-ولوم، بدون بازسازی فیلترها.
     *
     * نخ پخش این را در هر تکرار صدا می‌زند، پس باید ارزان باشد: فقط یک فیلد
     * عوض می‌شود. (بازسازی فیلترها فقط وقتی لازم است که خودِ spec عوض شده باشد.)
     */
    fun updateVolume(v: Float) {
        volume = v
    }

    private fun rebuild() {
        eqFilters = if (!spec.eqEnabled) emptyList() else buildEq()
        bassFilter = if (spec.isBassActive) {
            Biquad.highShelf(BASS_SHELF_HZ, format.sampleRate, BassBoost.strengthToDb(spec.bassStrength))
        } else null
        reverbFilter = if (spec.isReverbActive) {
            CombFilterChain(format.sampleRate, spec.reverbPreset)
        } else null
    }

    private fun buildEq(): List<Biquad> {
        val levels = if (spec.noiseReduction) {
            NoiseReduction.apply(spec.bandLevels, spec.noiseLevel)
        } else spec.bandLevels

        val freqs = EqPresets.defaultBandFrequencies()
        val out = ArrayList<Biquad>(freqs.size)
        freqs.forEachIndexed { i, f ->
            val db = levels.getOrElse(i) { 0 } / 100f
            if (db != 0f) {
                out.add(Biquad.peaking(f.toFloat(), format.sampleRate, EQ_Q, db))
            }
        }
        return out
    }

    /** پردازش یک بلوک بایت و برگرداندن بایت */
    fun process(bytes: ByteArray): ByteArray {
        val sampleCount = bytes.size / 2
        if (sampleCount == 0) return bytes

        val samples = FloatArray(sampleCount) { i ->
            val lo = bytes[i * 2].toInt() and 0xFF
            val hi = bytes[i * 2 + 1].toInt()
            (((hi shl 8) or lo).toShort()) / 32768f
        }

        applyBass(samples)
        applyEq(samples)
        applyNoiseCut(samples)
        reverbFilter?.process(samples, format.channels)
        applyGain(samples)

        val out = ByteArray(sampleCount * 2)
        for (i in 0 until sampleCount) {
            val v = (samples[i] * 32767f).toInt().coerceIn(-32768, 32767).toShort()
            out[i * 2] = (v.toInt() and 0xFF).toByte()
            out[i * 2 + 1] = ((v.toInt() shr 8) and 0xFF).toByte()
        }
        return out
    }

    private fun applyBass(buf: FloatArray) {
        bassFilter?.process(buf, format.channels)
    }

    private fun applyEq(buf: FloatArray) {
        val filters = eqFilters ?: return
        filters.forEach { it.process(buf, format.channels) }
    }

    /**
     * کاهش نویز.
     *
     * صادق باشیم: [NoiseReduction] فقط مشخص می‌کند کدام باندها کم شوند، پس اینجا
     * یک فیلتر پایین‌گذر ساده اعمال می‌شود تا هیسِ فراگیر کم شود. این **کاهش
     * نویز واقعی نیست** — همان وضعیت نسخهٔ اندروید.
     */
    private fun applyNoiseCut(buf: FloatArray) {
        if (!spec.noiseReduction || spec.noiseLevel <= 0) return
        val k = (spec.noiseLevel / 100f).coerceIn(0f, 1f)
        val channels = format.channels
        if (channels <= 0) return
        val frames = buf.size / channels
        val alpha = 1f - 0.3f * k
        val mix = 0.2f * k

        for (ch in 0 until min(channels, 2)) {
            var prev = 0f
            for (f in 0 until frames) {
                val i = f * channels + ch
                val s = buf[i]
                val filtered = prev + alpha * (s - prev)
                prev = filtered
                buf[i] = s * (1f - mix) + filtered * mix
            }
        }
    }

    /** بهرهٔ نهایی + محدودکنندهٔ ضد-کلیپ */
    private fun applyGain(buf: FloatArray) {
        val gain = decibelsToLinear(spec.volumeBoostDb.toFloat()) * volume.coerceIn(0f, 1f)
        if (gain == 0f) {
            buf.fill(0f)
            return
        }
        if (abs(gain - 1f) > 0.0001f) {
            for (i in buf.indices) buf[i] *= gain
        }
        // محدودکننده: هر چیزی بیرون از ‎-1..1‎ بریده می‌شود
        for (i in buf.indices) {
            if (buf[i] > 1f) buf[i] = 1f
            else if (buf[i] < -1f) buf[i] = -1f
        }
    }

    private companion object {
        const val EQ_Q = 1.41f          // Q ≈ 1.41 (حدود ۱ باند اکتاو)
        const val BASS_SHELF_HZ = 125f  // فرکانس شِلف تقویت باس
    }
}

/**
 * زنجیرهٔ فیلتر comb برای اکوی ساده — جایگزین `PresetReverb`.
 *
 * نسخهٔ اندروید از `PresetReverb` با سه اندازهٔ اتاق استفاده می‌کرد ولی **هیچ
 * UI نداشت** و مقدارش همیشه صفر می‌ماند. اینجا همان سه اندازه پیاده شده و در
 * milestone 5 به پنجرهٔ اکولایزر اضافه می‌شود.
 */
class CombFilterChain(
    private val sampleRate: Int,
    roomSize: Int
) {
    private val delayMs = when (roomSize) {
        1 -> 29.7    // اتاق کوچک
        2 -> 37.1    // اتاق متوسط
        else -> 41.1 // اتاق بزرگ
    }
    private val feedback = 0.35f
    private val wet = 0.25f
    private val delaySamples = max(1, (sampleRate * delayMs / 1000f).toInt())
    private val buffer = FloatArray(delaySamples)
    private var index = 0

    fun process(data: FloatArray, channels: Int) {
        if (channels <= 0) return
        val frames = data.size / channels
        for (f in 0 until frames) {
            for (ch in 0 until channels) {
                val i = f * channels + ch
                val delayed = buffer[index]
                buffer[index] = data[i] + delayed * feedback
                index = (index + 1) % delaySamples
                data[i] = data[i] * (1f - wet) + delayed * wet
            }
        }
    }
}
