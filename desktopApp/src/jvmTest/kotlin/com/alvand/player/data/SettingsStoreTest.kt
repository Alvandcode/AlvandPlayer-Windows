package com.alvand.player.data

import com.alvand.player.library.LibrarySort
import kotlinx.coroutines.test.runTest
import java.nio.file.Files
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * تست تنظیمات — همه‌چیز بدون نمایشگر، روی پوشهٔ موقت.
 *
 * اینجا سه چیز قفل می‌شود که در نسخهٔ اندروید با DataStore تضمین شده بود و
 * اینجا باید دستی ساخته شود: نوشتن اتمیک، بریدن مقادیر بیرون از بازه،
 * و برخورد امن با فایل خراب.
 */
class SettingsStoreTest {

    private lateinit var dir: java.nio.file.Path
    private lateinit var file: java.nio.file.Path

    @BeforeTest
    fun setUp() {
        dir = createTempDirectory("alvand-settings-test")
        file = dir.resolve("alvand_settings.properties")
    }

    @AfterTest
    fun tearDown() {
        runCatching { dir.toFile().deleteRecursively() }
    }

    @Test
    fun `defaults match the android settings defaults`() = runTest {
        val store = SettingsStore(file)
        val s = store.current
        assertEquals(ThemeMode.SYSTEM, s.themeMode)
        assertEquals(AccentThemeMode.MONO, s.accentTheme)
        assertEquals(null, s.backgroundPath)
        assertFalse(s.onboardingSeen)
        assertFalse(s.onlineLyricsEnabled)
        assertEquals(emptySet(), s.likedIds)
        assertEquals(LibrarySort.DEFAULT, s.librarySort)
        assertEquals("en", s.language)
    }

    @Test
    fun `values survive a restart`() = runTest {
        val first = SettingsStore(file)
        first.setThemeMode(ThemeMode.DARK)
        first.setAccentTheme(AccentThemeMode.OCEAN)
        first.setOnboardingSeen(true)
        first.setOnlineLyricsEnabled(true)
        first.setLibrarySort(LibrarySort.TITLE_AZ)
        first.setBackgroundPath("C:\\Pics\\cover.jpg")
        first.setLanguage("fa")
        first.setVolume(0.42f)
        first.toggleLike(42L)
        first.flush()

        val second = SettingsStore(file)
        val s = second.current
        assertEquals(ThemeMode.DARK, s.themeMode)
        assertEquals(AccentThemeMode.OCEAN, s.accentTheme)
        assertTrue(s.onboardingSeen)
        assertTrue(s.onlineLyricsEnabled)
        assertEquals(LibrarySort.TITLE_AZ, s.librarySort)
        assertEquals("C:\\Pics\\cover.jpg", s.backgroundPath)
        assertEquals("fa", s.language)
        assertEquals(0.42f, s.volume)
        assertEquals(setOf(42L), s.likedIds)
    }

    @Test
    fun `out of range values are clamped instead of crashing`() = runTest {
        val store = SettingsStore(file)
        store.setThemeMode(99)
        assertEquals(ThemeMode.DARK, store.current.themeMode)

        store.setThemeMode(-7)
        assertEquals(ThemeMode.SYSTEM, store.current.themeMode)

        store.setAccentTheme(42)
        assertEquals(AccentThemeMode.COUNT - 1, store.current.accentTheme)

        store.setAccentTheme(-3)
        assertEquals(AccentThemeMode.MONO, store.current.accentTheme)

        store.setLibrarySort(99)
        assertEquals(4, store.current.librarySort)
    }

    @Test
    fun `toggleLike adds then removes the same id`() = runTest {
        val store = SettingsStore(file)
        store.toggleLike(7L)
        assertTrue(7L in store.current.likedIds)
        store.toggleLike(7L)
        assertFalse(7L in store.current.likedIds)
    }

    @Test
    fun `unsupported language falls back to english`() = runTest {
        val store = SettingsStore(file)
        store.setLanguage("de-DE")
        assertEquals("en", store.current.language)
        store.setLanguage("fa-IR")
        assertEquals("fa", store.current.language)
    }

    @Test
    fun `atomic write leaves no temp file behind`() = runTest {
        val store = SettingsStore(file)
        store.setThemeMode(ThemeMode.LIGHT)
        store.flush()

        // نوشتن‌های غیرمسدودکننده ممکن است هنوز در صف باشند، پس کمی صبر می‌کنیم
        // تا invariant واقعی را بسنجیم: هیچ فایل موقتی نباید روی دیسک بماند.
        val deadline = System.currentTimeMillis() + 3000
        var leftovers = countTempFiles()
        while (leftovers > 0 && System.currentTimeMillis() < deadline) {
            Thread.sleep(50)
            leftovers = countTempFiles()
        }

        assertEquals(0L, leftovers, "a .tmp file was left behind")
        assertTrue(Files.exists(file))
    }

    private fun countTempFiles(): Long =
        Files.list(dir).use { s -> s.filter { it.fileName.toString().endsWith(".tmp") }.count() }

    @Test
    fun `corrupt file falls back to defaults without throwing`() = runTest {
        // بایت‌های بی‌معنا که Properties هم نمی‌تواند بخواند
        Files.write(file, byteArrayOf(0, -1, -2, -3, 66, 0, 67))

        val store = SettingsStore(file)
        assertEquals(ThemeMode.SYSTEM, store.current.themeMode)
        assertEquals(AccentThemeMode.MONO, store.current.accentTheme)
        assertEquals("en", store.current.language)
    }

    @Test
    fun `missing file yields defaults`() = runTest {
        assertFalse(Files.exists(file))
        val store = SettingsStore(file)
        assertEquals(ThemeMode.SYSTEM, store.current.themeMode)
    }
}
