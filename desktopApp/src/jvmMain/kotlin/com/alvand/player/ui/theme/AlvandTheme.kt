package com.alvand.player.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.alvand.player.theme.AccentTheme
import com.alvand.player.theme.AccentThemes
import com.alvand.player.theme.DarkPalette
import com.alvand.player.theme.LightPalette
import com.alvand.player.theme.accentThemeAt
import com.alvand.player.theme.tintedWith

/** پالت فعال در درخت Compose */
val LocalAP = staticCompositionLocalOf { LightPalette }

/** تم رنگی فعال در درخت Compose */
val LocalAccentTheme = staticCompositionLocalOf { AccentThemes[0] }

/** اسکیم پایه مینیمال (فالبک وقتی کاور/رنگی در دسترس نیست) */
val BaseScheme = lightColorScheme(
    primary = com.alvand.player.theme.MonoInk,
    onPrimary = Color.White,
    secondary = com.alvand.player.theme.MonoInk,
    onSecondary = Color.White,
    background = com.alvand.player.theme.MonoBg,
    onBackground = com.alvand.player.theme.MonoInk,
    surface = Color.White,
    onSurface = com.alvand.player.theme.MonoInk,
    surfaceVariant = Color.White,
    onSurfaceVariant = com.alvand.player.theme.MonoSub,
    outline = com.alvand.player.theme.MonoLine
)

/**
 * تم الوند با accent و حالت روشن/تیره.
 *
 * تفاوت با اندروید: آنجا `LocalAP` داخل خود تم ساخته می‌شد و accent داینامیک کاور
 * جداگانه به بقیهٔ صفحه‌ها داده می‌شد. اینجا امضا ساده‌تر است چون در milestone 1
 * هنوز پخش صدایی نداریم؛ امضای `dynamic` از قبل آماده است تا milestone 4 بدون
 * تغییر ساختاری اضافه شود.
 */
@Composable
fun AlvandTheme(
    darkTheme: Boolean = false,
    accentThemeId: Int = 0,
    content: @Composable () -> Unit
) {
    val accentTheme: AccentTheme = remember(accentThemeId) { accentThemeAt(accentThemeId) }
    val base = if (darkTheme) DarkPalette else LightPalette
    val pal = base.tintedWith(accentTheme, darkTheme)
    val accent = accentTheme.accent(darkTheme)

    val onAccent = if (darkTheme) Color(0xFF0C0C10) else Color.White

    val scheme = if (darkTheme) {
        darkColorScheme(
            primary = accent,
            onPrimary = onAccent,
            secondary = accent,
            onSecondary = onAccent,
            background = pal.bg,
            onBackground = pal.ink,
            surface = pal.card,
            onSurface = pal.ink,
            surfaceVariant = pal.card,
            onSurfaceVariant = pal.sub,
            outline = pal.line
        )
    } else {
        lightColorScheme(
            primary = accent,
            onPrimary = onAccent,
            secondary = accent,
            onSecondary = onAccent,
            background = pal.bg,
            onBackground = pal.ink,
            surface = pal.card,
            onSurface = pal.ink,
            surfaceVariant = pal.card,
            onSurfaceVariant = pal.sub,
            outline = pal.line
        )
    }

    CompositionLocalProvider(
        LocalAP provides pal,
        LocalAccentTheme provides accentTheme
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography = MaterialTheme.typography,
            content = content
        )
    }
}
