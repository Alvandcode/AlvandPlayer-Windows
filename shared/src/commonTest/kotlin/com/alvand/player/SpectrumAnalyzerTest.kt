package com.alvand.player

import com.alvand.player.audio.SpectrumAnalyzer
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SpectrumAnalyzerTest {

    private fun sine440(sr: Int = 44100, seconds: Double = 1.0): FloatArray {
        val n = (sr * seconds).toInt()
        return FloatArray(n) { i -> sin(2 * PI * 440 * i / sr).toFloat() * 0.8f }
    }

    @Test
    fun `fft of an impulse is flat`() {
        val re = DoubleArray(1024)
        val im = DoubleArray(1024)
        re[0] = 1.0
        SpectrumAnalyzer.fft(re, im)
        for (k in 0 until 1024) {
            assertTrue(abs(re[k] - 1.0) < 1e-9, "bin $k should be 1.0, was ${re[k]}")
            assertTrue(abs(im[k]) < 1e-9, "imag[$k] should be 0.0")
        }
    }

    @Test
    fun `fft rejects non power of two`() {
        val out = runCatching {
            SpectrumAnalyzer.fft(DoubleArray(100), DoubleArray(100))
        }
        assertTrue(out.isFailure, "non power-of-two size must be rejected")
    }

    @Test
    fun `silence maps to all zeros`() {
        val bands = SpectrumAnalyzer.analyze(FloatArray(2048), 44100)
        assertEquals(SpectrumAnalyzer.BANDS, bands.size)
        assertTrue(bands.all { it == 0f }, "silence must give zeros")
    }

    @Test
    fun `a 440hz tone peaks near its band`() {
        val bands = SpectrumAnalyzer.analyze(sine440(), 44100)
        val peak = bands.indices.maxByOrNull { bands[it] } ?: -1
        // باند ۴۴۰ هرتز: 24 * ln(440/60) / ln(16000/60) ≈ ۸٫۶
        assertTrue(peak in 7..10, "440Hz must peak near band 8-9, peaked at $peak")
        assertEquals(1f, bands[peak], "peak band must normalize to 1.0")
    }

    @Test
    fun `mixMonoTail takes the freshest samples and averages channels`() {
        // استریو: کانال چپ سکوت، راست سینوس — میانگین باید نصف دامنه باشد
        val frames = 2048
        val bytes = ByteArray(frames * 4)
        for (i in 0 until frames) {
            val v = (sin(2 * PI * 440 * i / 44100) * 16000).toInt().toShort()
            bytes[i * 4] = 0
            bytes[i * 4 + 1] = 0
            bytes[i * 4 + 2] = (v.toInt() and 0xFF).toByte()
            bytes[i * 4 + 3] = ((v.toInt() shr 8) and 0xFF).toByte()
        }
        val mono = SpectrumAnalyzer.mixMonoTail(bytes, channels = 2)
        assertEquals(1024, mono.size)
        var peak = 0f
        for (v in mono) peak = maxOf(peak, abs(v))
        // دامنهٔ راست 16000/32768 ≈ ۰٫۴۹، میانگین دو کانال ≈ ۰٫۲۴
        assertTrue(peak > 0.15f && peak < 0.35f, "channel average is wrong: $peak")
    }
}
