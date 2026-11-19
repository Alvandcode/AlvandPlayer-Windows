package com.alvand.player.ui

import com.alvand.player.data.SettingsStore
import com.alvand.player.library.LibraryRoots
import com.alvand.player.library.LibraryStore
import com.alvand.player.library.ScanState
import com.alvand.player.ui.components.AppTitleBar
import com.alvand.player.model.Song
import com.alvand.player.ui.components.loadArtwork
import com.alvand.player.ui.shell.Screen
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.rememberWindowState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * تولید عکس از صفحه‌ها بدون باز کردن پنجره.
 *
 * این‌ها تست نیستند — هدفشان این است که بتوانیم ظاهر را ببینیم/بررسی کنیم بدون
 * اینکه مزاحم پنجره‌های باز روی سیستم بشویم. اجرا:
 * `gradlew :desktopApp:jvmTest --tests "*ScreenshotTest*"`
 *
 * خروجی در `desktopApp/build/screenshots/`
 */
class ScreenshotTest {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Test
    fun `render every page of the shell`() {
        // هر صفحهٔ سایدبار جداگانه رندر می‌شود تا ظاهر همه با طرح مقایسه شود.
        val songs = listOf(
            Song(1, "Midnight Drive", "Stellar Bloom", "Neon Skies", "C:/music/1.wav", 252_000, genre = "Electronic"),
            Song(2, "Better Days", "The Horizon", "Sunrise", "C:/music/2.wav", 222_000, genre = "Pop"),
            Song(3, "Lost in Space", "Echoes", "Deep Field", "C:/music/3.wav", 255_000, genre = "Ambient"),
            Song(4, "Ocean Eyes", "Luna Ray", "Tidal", "C:/music/4.wav", 208_000, genre = "Pop"),
            Song(5, "Neon Hearts", "Violet Skies", "Nightfall", "C:/music/5.wav", 236_000, genre = "Electronic"),
            Song(6, "Urban Dreams", "City Lights", "Skyline", "C:/music/6.wav", 260_000, genre = "Pop")
        )
        val pages = listOf(
            "30-home" to Screen.HOME,
            "31-library" to Screen.LIBRARY,
            "32-playlists" to Screen.PLAYLISTS,
            "33-discover" to Screen.DISCOVER,
            "34-radio" to Screen.RADIO,
            "35-equalizer" to Screen.EQUALIZER,
            "36-visualizer" to Screen.VISUALIZER,
            "37-settings" to Screen.SETTINGS
        )
        pages.forEach { (name, screen) ->
            val path = ScreenshotTool.shellPage(name, screen, songs = songs)
            assertTrue(File(path).length() > 2000, "screenshot looks empty: $path")
            println("SHOT $path")
        }
    }

    @Test
    fun `render the full shell in both themes`() {
        val out = mutableListOf<String>()
        out += ScreenshotTool.shell("01-shell-light-en", dark = false, persian = false)
        out += ScreenshotTool.shell("02-shell-dark-en", dark = true, persian = false)
        out += ScreenshotTool.shell("03-shell-light-fa", dark = false, persian = true)
        out += ScreenshotTool.shell("04-shell-dark-ocean-fa", dark = true, persian = true, accentThemeId = 3)
        out.forEach { path ->
            assertTrue(File(path).length() > 2000, "screenshot looks empty: $path")
            println("SHOT $path")
        }
    }

    @Test
    fun `render the narrow shell with the mini player`() {
        // پنجرهٔ باریک: پنل «در حال پخش» سمت راست باید به مینی‌پلیر پایین تبدیل شود
        val path = ScreenshotTool.shell(
            "05-shell-narrow-miniplayer",
            widthDp = 820,
            heightDp = 700
        )
        assertTrue(File(path).length() > 2000, "screenshot looks empty: $path")
        println("SHOT $path")
    }

    @Test
    fun `render window title bar in both themes`() {
        val light = ScreenshotTool.render("25-titlebar-light-en") {
            val state = rememberWindowState()
            AppTitleBar(state = state, title = "Alvand Player", onClose = {})
        }
        val dark = ScreenshotTool.render("26-titlebar-dark-en", dark = true) {
            val state = rememberWindowState()
            AppTitleBar(state = state, title = "Alvand Player", onClose = {})
        }
        listOf(light, dark).forEach { path ->
            assertTrue(File(path).length() > 2000, "screenshot looks empty: $path")
            println("SHOT $path")
        }
    }

    @Test
    fun `render the full window composition with real cover`() {
        // ترکیب واقعی Main: تایتل‌بار + شل روی پس‌زمینهٔ واقعی.
        val assetDir = Files.createTempDirectory("alvand-shots-window")
        try {
            val tracks = PlayerAssets.create(assetDir).map { it.wav.absolutePath }
            runBlocking { tracks.forEach { loadArtwork(it) } }
            val path = ScreenshotTool.shell(
                "27-window-full-en",
                tracks = tracks,
                widthDp = 1360,
                heightDp = 860
            )
            assertTrue(File(path).length() > 2000, "screenshot looks empty: $path")
            println("SHOT $path")
        } finally {
            assetDir.toFile().deleteRecursively()
        }
    }

    @Test
    fun `title bar strip stays neutral`() {
        // نوار عنوان باید همیشه خنثیِ تم بماند؛ این تست رنگِ همان نوار بالا را
        // چک می‌کند تا پس‌زمینهٔ پررنگِ زیرِ آن هرگز روی نوار سرریز نکند.
        val path = ScreenshotTool.render("28-titlebar-strip-check") {
            Column {
                val state = rememberWindowState()
                AppTitleBar(state = state, title = "Alvand Player", onClose = {})
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(listOf(Color(0xFF00C853), Color(0xFF00695C)))
                        )
                )
            }
}
        val bytes = File(path).readBytes()
        val image = org.jetbrains.skia.Image.makeFromEncoded(bytes)
        val bitmap = org.jetbrains.skia.Bitmap()
        bitmap.allocPixels(org.jetbrains.skia.ImageInfo.makeN32Premul(image.width, image.height))
        check(image.readPixels(bitmap, 0, 0))
        // چند نقطه از نوار بالا: همه باید خنثی روشن باشند (اشباع پایین)
        for (x in listOf(200, 520, 840)) {
            val c = bitmap.getColor(x, 20)
            val comps = listOf(
                org.jetbrains.skia.Color.getR(c),
                org.jetbrains.skia.Color.getG(c),
                org.jetbrains.skia.Color.getB(c)
            )
            val spread = comps.max() - comps.min()
            println("STRIP pixel($x,20) rgb=(${comps[0]},${comps[1]},${comps[2]})")
            assertTrue(
                comps.min() > 150 && spread < 45,
                "background is bleeding over the title bar at x=$x: rgb=(${comps[0]},${comps[1]},${comps[2]})"
            )
        }
    }

    @Test
    fun `render library screen with real scanned songs`() {
        val root = Files.createTempDirectory("alvand-shot-lib")
        try {
            writeWav(root.resolve("01 - Coldplay - Yellow.wav"), seconds = 3, freq = 440)
            writeWav(root.resolve("02 - Coldplay - Paradise.wav"), seconds = 5, freq = 494)
            writeWav(root.resolve("Album One/03 - Ed Sheeran - Photograph.wav"), seconds = 4, freq = 523)
            writeWav(root.resolve("Album One/04 - Daft Punk - Around The World.wav"), seconds = 6, freq = 587)
            writeWav(root.resolve("Album Two/05 - Miles Davis - So What.wav"), seconds = 7, freq = 349)
            writeWav(root.resolve("unknown clip.wav"), seconds = 2, freq = 220)

            val settings = SettingsStore(root.resolve("settings.properties"))
            val roots = LibraryRoots(root.resolve("roots.txt"))
            val store = LibraryStore(roots, settings, scope)
            roots.add(root)
            store.scan()

            val deadline = System.currentTimeMillis() + 60_000
            while (System.currentTimeMillis() < deadline &&
                store.scanState.value !is ScanState.Done
            ) Thread.sleep(50)

            val done = store.scanState.value as? ScanState.Done
                ?: error("scan did not finish: ${store.scanState.value}")
            println("SHOT-LIB scan added=${done.added} total=${done.total}")
            assertTrue(done.total == 6, "expected 6 songs, got ${done.total}")

            val shots = mutableListOf<String>()
            shots += ScreenshotTool.render("10-library-songs-en", widthDp = 1180, heightDp = 820) {
                com.alvand.player.ui.screens.LibraryPage(
                    store = store,
                    onChooseFolder = {},
                    onPlay = {}
                )
            }
            shots += ScreenshotTool.render("11-library-songs-fa", widthDp = 1180, heightDp = 820, persian = true) {
                com.alvand.player.ui.screens.LibraryPage(
                    store = store,
                    onChooseFolder = {},
                    onPlay = {}
                )
            }
            store.setQuery("coldplay")
            shots += ScreenshotTool.render("13-library-search-en", widthDp = 1180, heightDp = 820) {
                com.alvand.player.ui.screens.LibraryPage(
                    store = store,
                    onChooseFolder = {},
                    onPlay = {}
                )
            }
            store.setQuery("")

            shots.forEach { path ->
                assertTrue(File(path).length() > 2000, "screenshot looks empty: $path")
                println("SHOT $path")
            }
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    /** فایل WAV واقعی، little-endian (هدر WAV) */
    private fun writeWav(target: Path, seconds: Int, freq: Int) {
        target.parent.createDirectories()
        val rate = 22050
        val samples = rate * seconds
        FileOutputStream(target.toFile()).use { out ->
            fun le32(v: Int) {
                out.write(v and 0xFF); out.write((v ushr 8) and 0xFF)
                out.write((v ushr 16) and 0xFF); out.write((v ushr 24) and 0xFF)
            }
            fun le16(v: Int) { out.write(v and 0xFF); out.write((v ushr 8) and 0xFF) }
            out.write("RIFF".toByteArray()); le32(36 + samples * 2)
            out.write("WAVE".toByteArray())
            out.write("fmt ".toByteArray()); le32(16)
            le16(1); le16(1); le32(rate); le32(rate * 2); le16(2); le16(16)
            out.write("data".toByteArray()); le32(samples * 2)
            val buf = ByteArray(samples * 2)
            for (i in 0 until samples) {
                val v = (8000 * kotlin.math.sin(2 * kotlin.math.PI * freq * i / rate)).toInt().toShort()
                buf[i * 2] = (v.toInt() and 0xFF).toByte()
                buf[i * 2 + 1] = ((v.toInt() shr 8) and 0xFF).toByte()
            }
            out.write(buf)
        }
    }
}