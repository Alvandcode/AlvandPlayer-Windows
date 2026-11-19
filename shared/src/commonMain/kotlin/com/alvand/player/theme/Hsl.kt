package com.alvand.player.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * تبدیل رنگ ↔ HSL.
 *
 * روی اندروید این کار را `androidx.core.graphics.ColorUtils.colorToHSL` انجام می‌داد
 * که روی دسکتاپ در دسترس نیست. اینجا همان الگوریتم دقیقاً بازنویسی شده تا
 * خروجی `sanitizeAccent` با اندروید **بایت‌به‌بایت یکسان** بماند.
 *
 * قرارداد مقادیر: hue در بازهٔ [0,1) و saturation/lightness در بازهٔ [0,1].
 */
object Hsl {

    /** Color → [h, s, l] (همان ColorUtils.colorToHSL) */
    fun colorToHsl(color: Color, out: FloatArray = FloatArray(3)): FloatArray {
        val r = color.red
        val g = color.green
        val b = color.blue

        val maxV = max(r, max(g, b))
        val minV = min(r, min(g, b))
        val delta = maxV - minV

        val lightness = (maxV + minV) / 2f

        if (delta == 0f) {
            out[0] = 0f
            out[1] = 0f
        } else {
            val saturation =
                if (lightness < 0.5f) delta / (maxV + minV)
                else delta / (2f - maxV - minV)

            var hue: Float
            if (r == maxV) {
                hue = (g - b) / delta + if (g < b) 6f else 0f
            } else if (g == maxV) {
                hue = (b - r) / delta + 2f
            } else {
                hue = (r - g) / delta + 4f
            }
            hue /= 6f

            out[0] = hue
            out[1] = saturation
        }
        out[2] = lightness
        return out
    }

    /** [h, s, l] → Color (همان ColorUtils.HSLToColor) */
    fun hslToColor(h: Float, s: Float, l: Float): Color {
        val c = (1f - abs(2f * l - 1f)) * s
        val hp = h * 6f
        val x = c * (1f - abs(hp % 2f - 1f))

        val r: Float
        val g: Float
        val b: Float
        when {
            hp < 1f -> { r = c; g = x; b = 0f }
            hp < 2f -> { r = x; g = c; b = 0f }
            hp < 3f -> { r = 0f; g = c; b = x }
            hp < 4f -> { r = 0f; g = x; b = c }
            hp < 5f -> { r = x; g = 0f; b = c }
            else -> { r = c; g = 0f; b = x }
        }

        val m = l - c / 2f
        return Color(
            red = channel(r + m),
            green = channel(g + m),
            blue = channel(b + m),
            alpha = 1f
        )
    }

    fun hslToColor(hsl: FloatArray): Color = hslToColor(hsl[0], hsl[1], hsl[2])

    private fun channel(v: Float): Float = (min(255f, max(0f, v * 255f)).roundToInt()) / 255f
}
