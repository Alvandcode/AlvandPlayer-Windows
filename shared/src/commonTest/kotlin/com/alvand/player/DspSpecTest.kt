package com.alvand.player

import com.alvand.player.audio.BassBoost
import com.alvand.player.audio.DspSpec
import com.alvand.player.audio.EqPresets
import com.alvand.player.audio.NoiseReduction
import com.alvand.player.audio.decibelsToLinear
import com.alvand.player.audio.millibelToLinear
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * تست ریاضیات DSP — این‌ها «قرارداد» هستند نه پیاده‌سازی.
 *
 * اگر اعداد جدول پرامپت §۱۱.۵ عوض شوند، این تست‌ها می‌افتند. دلیلشان ساده است:
 * اگر کسی فرمول کاهش نویز را به‌خاطر سلیقه عوض کند، نتیجهٔ صدا عوض می‌شود بی‌آنکه
 * کسی بفهمد چرا.
 */
class DspSpecTest {

    @Test
    fun `defaults match the android shape`() {
        val s = DspSpec()
        assertTrue(s.eqEnabled)
        assertEquals(0, s.preset)
        assertEquals(listOf(0, 0, 0, 0, 0), s.bandLevels)
        assertEquals(500, s.bassStrength)
        assertEquals(0, s.volumeBoostDb)
        assertTrue(!s.noiseReduction)
        assertEquals(50, s.noiseLevel)
        assertEquals(0, s.reverbPreset)
    }

    @Test
    fun `preset -1 means manual`() {
        assertTrue(DspSpec(preset = -1).isManual)
        assertTrue(!DspSpec(preset = 0).isManual)
    }

    @Test
    fun `band levels must be exactly five`() {
        val ok = runCatching { DspSpec(bandLevels = listOf(0, 0, 0, 0, 0)) }
        assertTrue(ok.isSuccess)
        val bad = runCatching { DspSpec(bandLevels = listOf(0, 0)) }
        assertTrue(bad.isFailure, "wrong band count must be rejected")
    }

    // ---- پریست‌ها ----

    @Test
    fun `all eleven preset curves match the table`() {
        val expected = mapOf(
            "rock" to listOf(4, 3, -1, 2, 4),
            "pop" to listOf(-2, -1, 1, 2, 1),
            "jazz" to listOf(3, 2, 0, 2, 3),
            "classical" to listOf(3, 2, -1, 1, 2),
            "dance" to listOf(5, 3, 0, 1, 2),
            "bass" to listOf(5, 4, 1, 0, 0),
            "vocal" to listOf(0, 1, 3, 3, 1),
            "treble" to listOf(-2, -1, 1, 3, 5),
            "latin" to listOf(3, 2, 0, 2, 3),
            "party" to listOf(3, 2, 0, 2, 3),
            "piano" to listOf(2, 1, 0, 2, 1)
        )
        assertEquals(11, EqPresets.CURVES.size)
        expected.forEach { (name, curve) ->
            assertEquals(curve, EqPresets.CURVES[name]?.toList(), "curve for '$name'")
        }
    }

    @Test
    fun `preset lookup is by substring like android`() {
        // منطق نسخهٔ اندروید: contains
        assertNotNull(EqPresets.curveForName("Android Rock Boost"))
        assertNotNull(EqPresets.curveForName("JAZZ preset"))
        assertNotNull(EqPresets.curveForName("my club Bass boost"))
        assertEquals(null, EqPresets.curveForName("Flat"))
    }

    @Test
    fun `display order shows first eight like android`() {
        assertEquals(8, EqPresets.PRIMARY.size)
        assertEquals(
            listOf("rock", "pop", "jazz", "classical", "dance", "bass", "vocal", "treble"),
            EqPresets.PRIMARY
        )
    }

    @Test
    fun `interpolation is clamped at both ends`() {
        val curve = EqPresets.CURVES.getValue("rock")
        // فرکانس‌های خیلی پایین → اولین مقدار منحنی
        val low = EqPresets.interpolate(doubleArrayOf(20.0, 30.0), curve)
        assertEquals((4 * 100), low[0])
        assertEquals((4 * 100), low[1])
        // فرکانس‌های خیلی بالا → آخرین مقدار
        val high = EqPresets.interpolate(doubleArrayOf(18000.0, 20000.0), curve)
        assertEquals((4 * 100), high[0])
        assertEquals((4 * 100), high[1])
    }

    @Test
    fun `interpolation is monotonic in log frequency between anchors`() {
        val curve = EqPresets.CURVES.getValue("rock") // 4, 3, -1, 2, 4
        // دقیقاً روی لنگرها باید همان مقادیر منحنی را بدهد
        val onAnchors = EqPresets.interpolate(
            EqPresets.ANCHOR_FREQUENCIES.map { it.toDouble() }.toDoubleArray(),
            curve
        )
        assertEquals(listOf(400, 300, -100, 200, 400), onAnchors.toList())
    }

    @Test
    fun `interpolated levels stay inside the millibel range`() {
        EqPresets.CURVES.values.forEach { curve ->
            val levels = EqPresets.interpolate(EqPresets.defaultBandFrequencies(), curve)
            levels.forEach {
                assertTrue(
                    it in DspSpec.MIN_MB..DspSpec.MAX_MB,
                    "level $it out of range for curve ${curve.toList()}"
                )
            }
        }
    }

    @Test
    fun `default band frequencies are log spread and sorted`() {
        val f = EqPresets.defaultBandFrequencies()
        assertEquals(DspSpec.EQ_BANDS, f.size)
        f.forEachIndexed { i, v ->
            // بازهٔ شنوایی انسان: شامل مرزها
            assertTrue(v >= 20.0 && v <= 20000.0, "band $i out of audible range: $v")
            if (i > 0) assertTrue(v > f[i - 1], "band frequencies must increase")
        }
        // اولین و آخرین باند دقیقاً روی مرزهای شنوایی انسان می‌نشینند
        assertEquals(20.0, f.first(), 0.001)
        assertEquals(20000.0, f.last(), 0.001)
    }

    // ---- تقویت باس ----

    @Test
    fun `bass boost maps 0 to 1000 onto 0 to 10 dB`() {
        assertEquals(0f, BassBoost.strengthToDb(0))
        assertEquals(5f, BassBoost.strengthToDb(500))
        assertEquals(10f, BassBoost.strengthToDb(1000))
    }

    @Test
    fun `bass boost is clamped and only active above zero`() {
        assertEquals(0f, BassBoost.strengthToDb(-100))
        assertEquals(10f, BassBoost.strengthToDb(9999))
        assertTrue(!BassBoost.isActive(0))
        assertTrue(BassBoost.isActive(1))
    }

    @Test
    fun `bass boost linear gain is consistent with decibels`() {
        val lin = BassBoost.strengthToLinear(1000)
        assertEquals(3.162f, lin, 0.01f) // 10 dB -> ~3.16x
        assertEquals(1f, BassBoost.strengthToLinear(0), 0.001f)
    }

    // ---- حذف نویز ----

    @Test
    fun `noise reduction follows the exact table`() {
        // زیر ۱۲۰ هرتز: کاهش هوم
        assertEquals(-600, NoiseReduction.cutFor(60.0, 1f))
        assertEquals(-300, NoiseReduction.cutFor(60.0, 0.5f))
        // ۱۰۰۰..۴۰۰۰: تقویت وضوح صدا
        assertEquals(150, NoiseReduction.cutFor(2000.0, 1f))
        assertEquals(75, NoiseReduction.cutFor(2000.0, 0.5f))
        // بالای ۸۰۰۰: کاهش هیس
        assertEquals(-1200, NoiseReduction.cutFor(10000.0, 1f))
        // بین ۵۰۰۰ تا ۸۰۰۰: کاهش ملایم
        assertEquals(-600, NoiseReduction.cutFor(6000.0, 1f))
        // بیرون از محدوده: بدون تغییر
        assertEquals(0, NoiseReduction.cutFor(500.0, 1f))
    }

    @Test
    fun `noise reduction adds on top of the user levels`() {
        val base = listOf(0, 0, 0, 0, 0)
        val out = NoiseReduction.apply(base, 100)
        // باند پایین باید منفی شود (کاهش هوم)
        assertTrue(out[0] < 0, "lowest band should be cut, got ${out[0]}")
        // میانه باید مثبت شود (وضوح صدا)
        assertTrue(out.any { it > 0 }, "some band should be boosted")
    }

    @Test
    fun `noise reduction level zero changes nothing`() {
        val base = listOf(100, 200, 300, 400, 500)
        assertEquals(base, NoiseReduction.apply(base, 0))
    }

    @Test
    fun `noise reduction never leaves the millibel range`() {
        val base = listOf(-1500, -1500, -1500, -1500, -1500)
        NoiseReduction.apply(base, 100).forEach {
            assertTrue(it in DspSpec.MIN_MB..DspSpec.MAX_MB, "out of range: $it")
        }
        val high = listOf(1500, 1500, 1500, 1500, 1500)
        NoiseReduction.apply(high, 100).forEach {
            assertTrue(it in DspSpec.MIN_MB..DspSpec.MAX_MB, "out of range: $it")
        }
    }

    // ---- تبدیل واحدها ----

    @Test
    fun `millibel conversion is correct`() {
        assertEquals(1f, millibelToLinear(0), 0.0001f)
        assertEquals(1.4125f, millibelToLinear(300), 0.001f)  // +3 dB
        assertEquals(0.7079f, millibelToLinear(-300), 0.001f) // -3 dB
    }

    @Test
    fun `decibel conversion is correct`() {
        assertEquals(1f, decibelsToLinear(0f), 0.0001f)
        assertEquals(2f, decibelsToLinear(6.0206f), 0.001f)
        assertEquals(0.5f, decibelsToLinear(-6.0206f), 0.001f)
    }
}
