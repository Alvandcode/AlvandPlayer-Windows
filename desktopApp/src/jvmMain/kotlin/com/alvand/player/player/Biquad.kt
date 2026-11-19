package com.alvand.player.player

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * فیلتر باک‌واوس-رُد جایگزین (biquad) — از «Audio EQ Cookbook» رابرت برژو-جانسون.
 *
 * سه نوع فیلتر لازم داریم و هر سه در همین کلاس هستند:
 *  - [peaking]: بلند/کم کردن یک باند (اکولایزر)
 *  - [highShelf]: تقویت باس
 *  - [lowShelf]: کم کردن نویز فراگیر
 *
 * ضرایب یک‌بار حساب و ذخیره می‌شوند؛ هر نمونه فقط چند ضرب و جمع انجام می‌دهد.
 */
class Biquad private constructor() {

    private var b0 = 1f
    private var b1 = 0f
    private var b2 = 0f
    private var a1 = 0f
    private var a2 = 0f

    /** تاریخچهٔ هر کانال (فیلتر حافظه دارد) */
    private val x1 = FloatArray(2)
    private val x2 = FloatArray(2)
    private val y1 = FloatArray(2)
    private val y2 = FloatArray(2)

    private fun normalize(n0: Float, n1: Float, n2: Float, d0: Float, d1: Float, d2: Float) {
        b0 = n0 / d0
        b1 = n1 / d0
        b2 = n2 / d0
        a1 = d1 / d0
        a2 = d2 / d0
    }

    /** پردازش درجا. `channels` تعداد کانال‌های واقعی داده است. */
    fun process(buffer: FloatArray, channels: Int) {
        if (channels <= 0) return
        val frames = buffer.size / channels
        val tracked = if (channels < 2) 1 else 2
        for (ch in 0 until tracked) {
            for (f in 0 until frames) {
                val i = f * channels + ch
                val x0 = buffer[i]
                val y0 = b0 * x0 + b1 * x1[ch] + b2 * x2[ch] - a1 * y1[ch] - a2 * y2[ch]
                x2[ch] = x1[ch]; x1[ch] = x0
                y2[ch] = y1[ch]; y1[ch] = y0
                buffer[i] = y0
            }
        }
    }

    companion object {
        private const val SHELF_SLOPE = 0.7071f   // Q ≈ 0.707 برای شِلف

        /** فیلتر بلندکننده/کاهندهٔ یک باند */
        fun peaking(freq: Float, sampleRate: Int, q: Float, gainDb: Float): Biquad =
            Biquad().apply {
                val w = (2 * PI * freq / sampleRate).toFloat()
                val cosw = cos(w.toDouble()).toFloat()
                val alpha = sin(w) / (2f * q)
                val amp = ampOf(gainDb)
                val den = 1f + alpha / amp
                val alphaAmp = alpha * amp
                normalize(
                    1f + alphaAmp, -2f * cosw, 1f - alphaAmp,
                    den, -2f * cosw, 1f - alpha / amp
                )
            }

        /** شِلف پایین: با کم‌کردن فرکانس، کل باندهای پایین بالا می‌روند */
        fun highShelf(freq: Float, sampleRate: Int, gainDb: Float): Biquad =
            Biquad().apply {
                val w = (2 * PI * freq / sampleRate).toFloat()
                val cosw = cos(w.toDouble()).toFloat()
                val a = ampOf(gainDb)
                val alpha = sin(w) / 2f * sqrt((a + 1 / a) * (1 / SHELF_SLOPE - 1) + 2)
                val t = 2f * sqrt(a) * alpha
                normalize(
                    a * (a + 1 + (a - 1) * cosw + t),
                    -2f * a * (a - 1 + (a + 1) * cosw),
                    a * (a + 1 + (a - 1) * cosw - t),
                    a + 1 - (a - 1) * cosw + t,
                    2f * (a - 1 - (a + 1) * cosw),
                    a + 1 - (a - 1) * cosw - t
                )
            }

        /** شِلف بالا: با کم‌کردن فرکانس، کل باندهای بالا کم می‌شوند */
        fun lowShelf(freq: Float, sampleRate: Int, gainDb: Float): Biquad =
            Biquad().apply {
                val w = (2 * PI * freq / sampleRate).toFloat()
                val cosw = cos(w.toDouble()).toFloat()
                val a = ampOf(gainDb)
                val alpha = sin(w) / 2f * sqrt((a + 1 / a) * (1 / SHELF_SLOPE - 1) + 2)
                val t = 2f * sqrt(a) * alpha
                normalize(
                    a * (a + 1 - (a - 1) * cosw + t),
                    2f * a * (a - 1 - (a + 1) * cosw),
                    a * (a + 1 - (a - 1) * cosw - t),
                    a + 1 + (a - 1) * cosw + t,
                    -2f * (a - 1 + (a + 1) * cosw),
                    a + 1 + (a - 1) * cosw - t
                )
            }

        /** بهرهٔ دسی‌بل به A */
        private fun ampOf(gainDb: Float): Float = exp(ln(10.0) * gainDb / 40.0).toFloat()
    }
}
