package com.alvand.player

import com.alvand.player.theme.AccentThemes
import com.alvand.player.theme.AlvandPalette
import com.alvand.player.theme.DarkPalette
import com.alvand.player.theme.LightPalette
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * قفل روی مقادیر دقیق توکن‌ها.
 *
 * اگر کسی حتی یک کد رنگ را عوض کند، این تست می‌افتد — که دقیقاً هدف است:
 * پالت نسخهٔ ویندوز باید با نسخهٔ اندروید یکی بماند.
 */
class PaletteTokensTest {

    @Test
    fun `light palette matches android token for token`() {
        val expected = mapOf(
            "bg" to 0xFFF4F4F6L, "ink" to 0xFF111114L, "sub" to 0xFF5E5E66L,
            "line" to 0xFFDFDFE5L, "track" to 0xFFCFCFD6L, "card" to 0xFFFFFFFFL,
            "sheet" to 0xF7FFFFFFL, "glass" to 0xB8FFFFFFL, "glassBorder" to 0xD9FFFFFFL,
            "sheen" to 0x8CFFFFFFL, "scrim" to 0x61000000L
        )
        assertPalette(LightPalette, expected, isDark = false)
    }

    @Test
    fun `dark palette matches android token for token`() {
        val expected = mapOf(
            "bg" to 0xFF0C0C10L, "ink" to 0xFFF2F2F5L, "sub" to 0xFFB4B4BEL,
            "line" to 0x33FFFFFFL, "track" to 0x44FFFFFFL, "card" to 0xFF17171DL,
            "sheet" to 0xF517171DL, "glass" to 0x33FFFFFFL, "glassBorder" to 0x40FFFFFFL,
            "sheen" to 0x2EFFFFFFL, "scrim" to 0x99000000L
        )
        assertPalette(DarkPalette, expected, isDark = true)
    }

    @Test
    fun `accent themes match android token for token`() {
        val expected = listOf(
            // id, light, dark, lightDeep, darkDeep
            longArrayOf(0L, 0xFF111114L, 0xFFB9A7FFL, 0xFF111114L, 0xFF2A3A52L),
            longArrayOf(1L, 0xFF6C4CF1L, 0xFFB9A7FFL, 0xFF1B1040L, 0xFF160C33L),
            longArrayOf(2L, 0xFFE8590CL, 0xFFFFB088L, 0xFF3A1206L, 0xFF2E0E04L),
            longArrayOf(3L, 0xFF0E7490L, 0xFF67E8F9L, 0xFF04222CL, 0xFF031B23L)
        )
        assertEquals(expected.size, AccentThemes.size)
        AccentThemes.forEachIndexed { i, t ->
            val e = expected[i]
            assertEquals(e[0].toInt(), t.id, "theme[$i] id")
            assertColor(e[1], t.light, "theme[$i] light")
            assertColor(e[2], t.dark, "theme[$i] dark")
            assertColor(e[3], t.lightDeep, "theme[$i] lightDeep")
            assertColor(e[4], t.darkDeep, "theme[$i] darkDeep")
        }
    }

    /** مقایسهٔ یک رنگ با کد ARGB شش‌رقمی هشت */
    private fun assertColor(hex: Long, actual: androidx.compose.ui.graphics.Color, name: String) {
        assertEquals(
            ((hex ushr 24) and 0xFF).toDouble() / 255.0, actual.alpha.toDouble(), 0.005,
            "$name alpha"
        )
        assertEquals(
            ((hex ushr 16) and 0xFF).toDouble() / 255.0, actual.red.toDouble(), 0.005,
            "$name red"
        )
        assertEquals(
            ((hex ushr 8) and 0xFF).toDouble() / 255.0, actual.green.toDouble(), 0.005,
            "$name green"
        )
        assertEquals(
            (hex and 0xFF).toDouble() / 255.0, actual.blue.toDouble(), 0.005,
            "$name blue"
        )
    }

    private fun assertPalette(
        actual: AlvandPalette,
        expected: Map<String, Long>,
        isDark: Boolean
    ) {
        assertEquals(isDark, actual.isDark, "isDark")
        val got = mapOf(
            "bg" to actual.bg, "ink" to actual.ink, "sub" to actual.sub,
            "line" to actual.line, "track" to actual.track, "card" to actual.card,
            "sheet" to actual.sheet, "glass" to actual.glass,
            "glassBorder" to actual.glassBorder, "sheen" to actual.sheen,
            "scrim" to actual.scrim
        )
        expected.forEach { (name, hex) ->
            val c = got.getValue(name)
            // مقایسهٔ کانال‌ها جداگانه با تلورانس: دقت Float می‌تواند یک واحد اختلاف بدهد
            assertEquals(
                ((hex ushr 24) and 0xFF).toDouble() / 255.0, c.alpha.toDouble(), 0.005,
                "$name alpha"
            )
            assertEquals(
                ((hex ushr 16) and 0xFF).toDouble() / 255.0, c.red.toDouble(), 0.005,
                "$name red"
            )
            assertEquals(
                ((hex ushr 8) and 0xFF).toDouble() / 255.0, c.green.toDouble(), 0.005,
                "$name green"
            )
            assertEquals(
                (hex and 0xFF).toDouble() / 255.0, c.blue.toDouble(), 0.005,
                "$name blue"
            )
        }
    }
}
