package com.alvand.player.data

import com.alvand.player.audio.DspSpec
import java.nio.file.Files
import java.nio.file.Path
import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DspSpecCodecTest {

    @Test
    fun `round trips every field`() {
        val spec = DspSpec(
            eqEnabled = false,
            preset = -1,
            bandLevels = listOf(-1500, 750, 0, -300, 1500),
            bassStrength = 800,
            volumeBoostDb = 7,
            noiseReduction = true,
            noiseLevel = 83,
            reverbPreset = 4
        )
        assertEquals(spec, DspSpecCodec.decode(DspSpecCodec.encode(spec)))
    }

    @Test
    fun `round trips the default spec`() {
        val spec = DspSpec()
        assertEquals(spec, DspSpecCodec.decode(DspSpecCodec.encode(spec)))
    }

    @Test
    fun `null and empty decode to null`() {
        assertNull(DspSpecCodec.decode(null))
        assertNull(DspSpecCodec.decode(""))
        assertNull(DspSpecCodec.decode("   "))
    }

    @Test
    fun `garbage never crashes and falls back to defaults`() {
        val bad = listOf(
            "x",
            "1;2",
            "1;-1;0,0,0;500;0;0;50;0",           // باندها کم‌اند
            "1;-1;0,0,0,0,0,0;500;0;0;50;0",     // باندها زیادند
            "a;b;c;d;e;f;g;h",                    // همه غیرعددی
            "1;-1;1,2,x,4,5;500;0;0;50;0",        // یک باند خراب
            "1;-1;0,0,0,0,0;500;0;0;50;0;9;9"     // بخش‌های اضافه
        )
        bad.forEach { raw ->
            val decoded = DspSpecCodec.decode(raw)
            if (decoded != null) {
                // اگر رمزگشایی موفق شد، باید داده‌های معتبر و در بازه باشد
                assertEquals(DspSpec.EQ_BANDS, decoded.bandLevels.size, "raw=$raw")
                decoded.bandLevels.forEach {
                    assertTrue(it in DspSpec.MIN_MB..DspSpec.MAX_MB, "raw=$raw value=$it")
                }
            }
        }
    }

    @Test
    fun `out of range values are clamped`() {
        val decoded = DspSpecCodec.decode("1;999;99999,-99999,0,0,0;99999;999;1;999;999")
        assertNotEquals(null, decoded)
        val d = decoded!!
        assertTrue(d.preset in -1..16)
        assertTrue(d.bassStrength in 0..1000)
        assertTrue(d.volumeBoostDb in 0..10)
        assertTrue(d.noiseLevel in 0..100)
        d.bandLevels.forEach { assertTrue(it in DspSpec.MIN_MB..DspSpec.MAX_MB) }
    }

    @Test
    fun `encoding differs between presets`() {
        val a = DspSpecCodec.encode(DspSpec(preset = 0))
        val b = DspSpecCodec.encode(DspSpec(preset = 3))
        assertNotEquals(a, b)
    }
}

class DspPersistenceTest {

    private lateinit var dir: Path

    @BeforeTest
    fun setUp() {
        dir = Files.createTempDirectory("alvand-dsp")
    }

    @AfterTest
    fun tearDown() {
        runCatching { dir.toFile().deleteRecursively() }
    }

    @Test
    fun `dsp settings survive a reload`() = runBlocking {
        val file = dir.resolve("settings.properties")
        val store = SettingsStore(file)
        val spec = DspSpec(
            preset = -1,
            bandLevels = listOf(600, 0, -600, 300, -300),
            bassStrength = 750,
            volumeBoostDb = 4,
            noiseReduction = true,
            noiseLevel = 77
        )
        store.setDspSpec(spec)
        store.flush()

        // نمونهٔ تازه از همان فایل می‌خواند
        val reloaded = SettingsStore(file)
        assertEquals(spec, reloaded.current.dspSpec)
    }

    @Test
    fun `default is used when the key is absent`() {
        val store = SettingsStore(dir.resolve("fresh.properties"))
        assertEquals(DspSpec(), store.current.dspSpec)
    }

    @Test
    fun `a corrupt dsp value does not break the rest of the settings`() {
        val file = dir.resolve("corrupt.properties")
        Files.newOutputStream(file).use { out ->
            out.write(
                """
                theme_mode=2
                app_language=fa
                dsp_spec=total-garbage
                """.trimIndent().toByteArray()
            )
        }
        val store = SettingsStore(file)
        // بقیهٔ تنظیمات باید سالم خوانده شود…
        assertEquals(2, store.current.themeMode)
        assertEquals("fa", store.current.language)
        // …و فقط DSP به پیش‌فرض برگشته
        assertEquals(DspSpec(), store.current.dspSpec)
    }
}