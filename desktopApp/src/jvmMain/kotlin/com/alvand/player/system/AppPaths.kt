package com.alvand.player.system

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.createDirectories

/**
 * محل ذخیرهٔ داده‌های اپ.
 *
 * روی ویندوز معادل app-data همان چیزی است که اندروید در حالت private استفاده می‌کند:
 * `%APPDATA%\Alvand`. اگر (به هر دلیلی) در دسترس نبود، به `%LOCALAPPDATA%` و بعد
 * به پوشهٔ موقت برمی‌گردیم تا اپ هیچ‌وقت به خاطر مسیر نتواند بالا نیاید.
 */
object AppPaths {

    val appData: Path by lazy { resolve() }

    val settingsFile: Path get() = appData.resolve("alvand_settings.properties")

    val crashesDir: Path get() = appData.resolve("crashes")

    val lyricsDir: Path get() = appData.resolve("lyrics")

    val dbFile: Path get() = appData.resolve("alvand.db")

    fun ensureCreated() {
        listOf(appData, crashesDir, lyricsDir).forEach { it.createDirectories() }
    }

    private fun resolve(): Path {
        val candidates = listOfNotNull(
            System.getenv("APPDATA"),
            System.getenv("LOCALAPPDATA"),
            System.getProperty("java.io.tmpdir")
        )
        for (base in candidates) {
            val p = Paths.get(base, "Alvand")
            if (runCatching { Files.createDirectories(p); true }.getOrDefault(false)) return p
        }
        return Paths.get(System.getProperty("java.io.tmpdir"), "Alvand")
            .also { it.createDirectories() }
    }
}
