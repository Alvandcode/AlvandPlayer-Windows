package com.alvand.player.audio

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.sin

/**
 * تحلیل‌گر طیف برای ویژوالایزر — خالص کاتلین، بدون وابستگی (حتی در shared).
 *
 * ورودی: دست‌کم ۱۰۲۴ نمونهٔ مونو (میانگین کانال‌ها).
 * خروجی: ۲۴ باند لگاریتمی ۶۰ هرتز تا ۱۶ کیلوهرتز، نرمال‌شدهٔ ۰..۱.
 *
 * مراحل: پنجرهٔ Hann (کاهش نشت طیفی) ← FFT رادیکس-۲ ← بزرگی ← باند لگاریتمی ←
 * نرمال‌سازی نسبت به قوی‌ترین باند. سکوت → همه صفر.
 */
object SpectrumAnalyzer {

    const val FFT_SIZE = 1024
    const val BANDS = 24
    const val MIN_HZ = 60.0
    const val MAX_HZ = 16000.0

    fun analyze(mono: FloatArray, sampleRate: Int): List<Float> {
        require(mono.size >= FFT_SIZE) { "need at least $FFT_SIZE samples" }
        require(sampleRate > 0) { "sample rate must be positive" }

        val re = DoubleArray(FFT_SIZE)
        val im = DoubleArray(FFT_SIZE)
        for (i in 0 until FFT_SIZE) {
            val w = 0.5 * (1.0 - cos(2.0 * PI * i / (FFT_SIZE - 1)))
            re[i] = mono[i] * w
        }
        fft(re, im)

        val half = FFT_SIZE / 2
        val mags = DoubleArray(half) { i -> hypot(re[i], im[i]) / half }

        val out = FloatArray(BANDS)
        val lo = ln(MIN_HZ)
        val hi = ln(MAX_HZ)
        for (b in 0 until BANDS) {
            val f0 = exp(lo + (hi - lo) * b / BANDS)
            val f1 = exp(lo + (hi - lo) * (b + 1) / BANDS)
            val bin0 = (f0 * FFT_SIZE / sampleRate).toInt().coerceIn(0, half - 1)
            val bin1 = (f1 * FFT_SIZE / sampleRate).toInt().coerceIn(0, half - 1)
            var m = 0.0
            for (k in bin0..bin1) if (mags[k] > m) m = mags[k]
            out[b] = m.toFloat()
        }
        val peak = out.maxOrNull() ?: 0f
        if (peak <= 0f) return out.toList()
        return out.map { (it / peak).coerceIn(0f, 1f) }
    }

    /**
     * ترکیب بایت‌های PCM (۱۶ بیت little-endian چندکاناله) به ۱۰۲۴ نمونهٔ مونو.
     * از انتهای بافر برمی‌دارد (تازه‌ترین صدا) و اگر کم بود با سکوت پر می‌کند.
     */
    fun mixMonoTail(bytes: ByteArray, channels: Int, frames: Int = FFT_SIZE): FloatArray {
        val ch = channels.coerceAtLeast(1)
        val frameSize = ch * 2
        val take = minOf(frames, bytes.size / frameSize)
        val out = FloatArray(FFT_SIZE)
        val start = bytes.size - take * frameSize
        for (i in 0 until take) {
            var sum = 0.0
            for (c in 0 until ch) {
                val o = start + i * frameSize + c * 2
                val lo = bytes[o].toInt() and 0xFF
                val hi = bytes[o + 1].toInt()
                sum += (((hi shl 8) or lo).toShort()) / 32768.0
            }
            out[FFT_SIZE - take + i] = (sum / ch).toFloat()
        }
        return out
    }

    /** FFT رادیکس-۲ درجا. */
    fun fft(re: DoubleArray, im: DoubleArray) {
        val n = re.size
        require(n > 0 && n and (n - 1) == 0) { "size must be a power of two" }
        require(im.size == n) { "real and imaginary parts must match" }
        var j = 0
        for (i in 1 until n) {
            var bit = n shr 1
            while (j and bit != 0) {
                j = j and bit.inv()
                bit = bit shr 1
            }
            j = j or bit
            if (i < j) {
                val tr = re[i]; re[i] = re[j]; re[j] = tr
                val ti = im[i]; im[i] = im[j]; im[j] = ti
            }
        }
        var len = 2
        while (len <= n) {
            val ang = -2.0 * PI / len
            val wr = cos(ang)
            val wi = sin(ang)
            var i = 0
            while (i < n) {
                var wpr = 1.0
                var wpi = 0.0
                for (k in 0 until len / 2) {
                    val er = re[i + k + len / 2] * wpr - im[i + k + len / 2] * wpi
                    val ei = re[i + k + len / 2] * wpr + im[i + k + len / 2] * wpi
                    re[i + k + len / 2] = re[i + k] - er
                    im[i + k + len / 2] = im[i + k] - ei
                    re[i + k] += er
                    im[i + k] += ei
                    val nwr = wpr * wr - wpi * wi
                    wpi = wpr * wi + wpi * wr
                    wpr = nwr
                }
                i += len
            }
            len = len shl 1
        }
    }
}
