package com.alvand.player.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * پالت داینامیک استخراج‌شده از کاور آهنگ.
 *
 * - اگر کاور/رنگی نباشد، به تم مینیمال سیاه‌سفید ([MonoInk]) برمی‌گردیم.
 * - رنگ accent با HSL تنظیم می‌شود تا همیشه روی [MonoBg] خوانا و زنده باشد
 *   (نه خیلی روشن که دیده نشود، نه خاکستری مرده).
 */
data class DynamicAccent(
    val accent: Color,
    /** نسخه تیره‌تر برای گرادیان پس‌زمینه */
    val accentDark: Color,
    /** آیا accent از کاور واقعی آمده یا فالبک سیاه‌وسفید است؟ */
    val fromArtwork: Boolean
) {
    companion object {
        val Fallback = DynamicAccent(MonoInk, ArtDark2, fromArtwork = false)
    }
}

/** گرادیان پس‌زمینه صفحه پلیر بر اساس accent */
fun dynamicBackgroundBrush(accent: DynamicAccent, base: Color = MonoBg): Brush =
    Brush.verticalGradient(
        listOf(
            accent.accentDark.copy(alpha = 0.28f),
            accent.accent.copy(alpha = 0.10f),
            base
        )
    )

/** رنگ کاور → accent قابل‌استفاده در UI (روشن/تیره) */
fun sanitizeAccent(raw: Color, dark: Boolean = false): Color {
    val hsl = Hsl.colorToHsl(raw)
    // اشباع و روشنایی را در بازه خوشگل نگه دار
    hsl[1] = hsl[1].coerceIn(0.45f, 0.95f)
    hsl[2] = if (dark) hsl[2].coerceIn(0.55f, 0.80f) else hsl[2].coerceIn(0.28f, 0.55f)
    return Hsl.hslToColor(hsl)
}

/**
 * نسخهٔ تیرهٔ رنگ کاور برای گرادیان پس‌زمینه: فقط روشنایی دستکاری می‌شود،
 * اشباع دست‌نخورده می‌ماند (دقیقاً مثل اندروید).
 */
fun sanitizeAccentDarkVariant(raw: Color): Color {
    val hsl = Hsl.colorToHsl(raw)
    hsl[2] = hsl[2].coerceIn(0.12f, 0.30f)
    return Hsl.hslToColor(hsl)
}

/**
 * زنجیرهٔ فالبک accent داینامیک — پورت دقیق `rememberDynamicAccent` اندروید،
 * فقط بدون بخش کمپوزبل (که در لایهٔ UI دسکتاپ می‌نشیند).
 */
fun dynamicAccentFallback(accentTheme: AccentTheme, dark: Boolean): DynamicAccent = when {
    accentTheme.isColored ->
        DynamicAccent(accentTheme.accent(dark), accentTheme.deep(dark), fromArtwork = false)
    dark -> DynamicAccent(AlvandLavender, ArtDark2, fromArtwork = false)
    else -> DynamicAccent.Fallback
}

/** رنگ‌های استخراج‌شده از کاور یک آهنگ (خام، قبل از sanitize) */
data class ArtworkColors(val dominant: Color, val dark: Color)

/**
 * ساخت [DynamicAccent] از رنگ‌های کاور.
 * اگر کاور سیاه باشد (یعنی رنگی نداشته) به فالبک برمی‌گردد.
 */
fun dynamicAccentFromArtwork(
    colors: ArtworkColors?,
    accentTheme: AccentTheme,
    dark: Boolean
): DynamicAccent {
    val fallback = dynamicAccentFallback(accentTheme, dark)
    if (colors == null || colors.dominant == Color.Black) return fallback
    return DynamicAccent(
        accent = sanitizeAccent(colors.dominant, dark),
        accentDark = sanitizeAccentDarkVariant(colors.dark),
        fromArtwork = true
    )
}
