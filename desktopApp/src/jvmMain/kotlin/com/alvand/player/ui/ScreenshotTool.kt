package com.alvand.player.ui

import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.LayoutDirection
import com.alvand.player.i18n.LocalStrings
import com.alvand.player.i18n.StringsEn
import com.alvand.player.i18n.StringsFa
import com.alvand.player.theme.accentThemeAt
import com.alvand.player.ui.theme.AlvandTheme
import com.alvand.player.ui.theme.LocalAP
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import org.jetbrains.skia.EncodedImageFormat
import java.io.File
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * رندر صفحه‌ها به تصویر PNG بدون باز کردن پنجره.
 *
 * این یک ابزار توسعه است: `ImageComposeScene` همان موتور Skia را بدون هیچ پنجرهٔ
 * واقعی اجرا می‌کند، پس می‌شود ظاهر صفحه‌ها را روی CI هم بررسی کرد.
 *
 * مسیر خروجی: `build/screenshots` (اگر وجود نداشته باشد ساخته می‌شود).
 */
object ScreenshotTool {

    private val outputDir = File("build/screenshots")

    /**
     * همهٔ رندرها روی **یک نخ ثابت** انجام می‌شوند.
     *
     * دلیل: `ImageComposeScene` از `SnapshotStateObserver` سراسری استفاده می‌کند و اگر
     * دو رندر روی دو نخ مختلف در یک JVM انجام شود، خطای
     * `Detected multithreaded access to SnapshotStateObserver` می‌دهد. آن خطا ربطی به
     * کد تحت تست ندارد و فقط باعث flaky شدن تست‌ها می‌شد، پس کل کار از نخ تست جدا
     * و سریال شده است.
     */
    private val renderThread: ExecutorService =
        Executors.newSingleThreadExecutor { r -> Thread(r, "screenshot-render").apply { isDaemon = true } }

    private fun <T> onRenderThread(block: () -> T): T {
        val future = renderThread.submit(Callable { block() })
        return future.get(2, TimeUnit.MINUTES)
    }

    /** @return مسیر فایل ساخته‌شده */
    @OptIn(androidx.compose.ui.InternalComposeUiApi::class, androidx.compose.ui.ExperimentalComposeUiApi::class)
    fun render(
        name: String,
        widthDp: Int = 1280,
        heightDp: Int = 820,
        dark: Boolean = false,
        persian: Boolean = false,
        accentThemeId: Int = 0,
        content: @Composable () -> Unit
    ): String = onRenderThread { renderOnCurrentThread(name, widthDp, heightDp, dark, persian, accentThemeId, content) }

    @OptIn(androidx.compose.ui.InternalComposeUiApi::class, androidx.compose.ui.ExperimentalComposeUiApi::class)
    private fun renderOnCurrentThread(
        name: String,
        widthDp: Int,
        heightDp: Int,
        dark: Boolean,
        persian: Boolean,
        accentThemeId: Int,
        content: @Composable () -> Unit
    ): String {
        outputDir.mkdirs()
        val file = File(outputDir, "$name.png")

        val scene = ImageComposeScene(
            width = widthDp,
            height = heightDp,
            density = Density(1f),
            layoutDirection = if (persian) LayoutDirection.Rtl else LayoutDirection.Ltr
        ) {
            CompositionLocalProvider(LocalStrings provides if (persian) StringsFa else StringsEn) {
                AlvandTheme(darkTheme = dark, accentThemeId = accentThemeId) {
                    val pal = LocalAP.current
                    Box(Modifier.fillMaxSize().background(pal.bg)) { content() }
                }
            }
        }

        try {
            val image = scene.render()
            val data = image.encodeToData(EncodedImageFormat.PNG) ?: error("PNG encode failed")
            file.writeBytes(data.bytes)
        } finally {
            scene.close()
        }
        return file.absolutePath
    }

    // ---- صفحه‌های آماده (شل دسکتاپ) ----

    /**
     * شل کامل با **محتوای واقعی صفحه** — برای دیدن همان چیزی که کاربر می‌بیند.
     *
     * @param screen صفحهٔ فعال شل
     * @param songs آهنگ‌های کتابخانه (اختیاری؛ خالی یعنی خالی)
     */
    fun shellPage(
        name: String,
        screen: com.alvand.player.ui.shell.Screen,
        dark: Boolean = false,
        persian: Boolean = false,
        accentThemeId: Int = 0,
        widthDp: Int = 1280,
        heightDp: Int = 820,
        songs: List<com.alvand.player.model.Song> = emptyList(),
        progress: Float = 0.34f,
        selectedIndex: Int = 1,
        tracks: List<String> = emptyList()
    ): String = render(
        name = name,
        widthDp = widthDp,
        heightDp = heightDp,
        dark = dark,
        persian = persian,
        accentThemeId = accentThemeId
    ) {
        val accent = accentThemeAt(accentThemeId).accent(dark)
        val queue = queueFor(tracks)
        val index = selectedIndex.coerceIn(0, queue.lastIndex)

        com.alvand.player.ui.shell.AppShell(
            screen = screen,
            onNavigate = {},
            accent = accent,
            miniTitle = queue[index].title,
            miniArtist = queue[index].artist,
            miniPlaying = true,
            onMiniClick = {},
            query = "",
            onQueryChange = {},
            searchResults = emptyList(),
            searchFocusRequester = androidx.compose.runtime.remember { androidx.compose.ui.focus.FocusRequester() },
            onPlaySong = {},
            content = {
                when (screen) {
                    com.alvand.player.ui.shell.Screen.HOME -> com.alvand.player.ui.screens.HomeScreen(
                        recentSongs = songs,
                        accent = accent,
                        onPlayMix = {},
                        onBrowse = {},
                        onNavigate = {},
                        onPlaySong = {},
                        onViewAllRecent = {}
                    )
                    com.alvand.player.ui.shell.Screen.DISCOVER ->
                        com.alvand.player.ui.screens.DiscoverPage(songs = songs, onPlaySongs = {})
                    com.alvand.player.ui.shell.Screen.RADIO ->
                        com.alvand.player.ui.screens.RadioPage(songs = songs, onPlaySongs = {})
                    com.alvand.player.ui.shell.Screen.EQUALIZER ->
                        com.alvand.player.ui.screens.EqualizerPage(
                            spec = com.alvand.player.audio.DspSpec(preset = 2),
                            accent = accent,
                            visualizerEnabled = false,
                            onSpecChange = {},
                            onToggleVisualizer = {}
                        )
                    com.alvand.player.ui.shell.Screen.SETTINGS ->
                        com.alvand.player.ui.screens.SettingsPage(
                            settings = com.alvand.player.data.AppSettings(),
                            systemDark = dark,
                            volume = 0.7f,
                            accent = accent,
                            onVolumeChange = {},
                            onThemeModeChange = {},
                            onAccentChange = {},
                            onToggleLanguage = {},
                            onToggleOnlineLyrics = {},
                            onTogglePerformance = {},
                            onOutputDevice = {}
                        )
                    com.alvand.player.ui.shell.Screen.VISUALIZER ->
                        com.alvand.player.ui.screens.VisualizerPage(
                            state = engineStateFor(queue, index, progress),
                            accent = accent,
                            liked = false,
                            onToggleLike = {},
                            onTogglePlay = {},
                            onNext = {},
                            onPrevious = {},
                            onToggleShuffle = {},
                            onCycleRepeat = {},
                            onSeek = {}
                        )
                    com.alvand.player.ui.shell.Screen.PLAYLISTS ->
                        com.alvand.player.ui.screens.PlaylistsPage(
                            playlists = emptyList(),
                            songsOf = { emptyList() },
                            songCountOf = { 0 },
                            onCreate = {},
                            onDelete = {},
                            onPlaySongs = {}
                        )
                    com.alvand.player.ui.shell.Screen.LIBRARY ->
                        com.alvand.player.ui.screens.LibraryPage(
                            store = storeFor(name),
                            onChooseFolder = {},
                            onPlay = {}
                        )
                }
            },
            engineState = engineStateFor(queue, index, progress),
            liked = false,
            onToggleLike = {},
            onTogglePlay = {},
            onNext = {},
            onPrevious = {},
            onSeek = {},
            onToggleShuffle = {},
            onCycleRepeat = {},
            onSelectQueueIndex = {},
            onOpenLyrics = {},
            onOpenSettings = {}
        )
    }

    private fun queueFor(tracks: List<String>) = if (tracks.size >= 3) {
        listOf(
            com.alvand.player.player.PlayableItem(1, "Dreamy Night", "Alvand", uri = tracks[0], durationMs = 245_000),
            com.alvand.player.player.PlayableItem(2, "Midnight Drive", "Alvand", uri = tracks[1], durationMs = 198_000),
            com.alvand.player.player.PlayableItem(3, "Paper Boats", "Alvand", uri = tracks[2], durationMs = 312_000)
        )
    } else {
        listOf(
            com.alvand.player.player.PlayableItem(1, "Dreamy Night", "Alvand", uri = "C:/music/1.wav", durationMs = 245_000),
            com.alvand.player.player.PlayableItem(2, "Midnight Drive", "Alvand", uri = "C:/music/2.wav", durationMs = 198_000),
            com.alvand.player.player.PlayableItem(3, "Paper Boats", "Alvand", uri = "C:/music/3.wav", durationMs = 312_000)
        )
    }

    private fun engineStateFor(
        queue: List<com.alvand.player.player.PlayableItem>,
        index: Int,
        progress: Float
    ) = com.alvand.player.player.EngineState(
        current = queue[index],
        isPlaying = true,
        positionMs = (queue[index].durationMs * progress).toLong(),
        durationMs = queue[index].durationMs,
        queue = queue,
        index = index,
        shuffle = true,
        repeatMode = com.alvand.player.player.RepeatMode.ALL
    )

    /** کتابخانهٔ موقت برای رندر صفحهٔ Library در ابزار اسکرین‌شات. */
    private fun storeFor(name: String): com.alvand.player.library.LibraryStore {
        val dir = java.io.File("build/screenshots-store").apply { mkdirs() }
        val settings = com.alvand.player.data.SettingsStore(dir.toPath().resolve("s.properties"))
        val roots = com.alvand.player.library.LibraryRoots(dir.toPath().resolve("r.txt"))
        return com.alvand.player.library.LibraryStore(
            roots = roots,
            settings = settings,
            scope = kotlinx.coroutines.CoroutineScope(
                kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Default
            )
        )
    }

    /**
     * کل شل (سایدبار + جستجو + صفحه + پنل در حال پخش) با وضعیت ساختگی موتور.
     *
     * هدف فقط دیدن شکل نهایی است، پس وضعیت از پیش ساخته می‌شود تا نمایشگر لازم نباشد.
     */
    fun shell(
        name: String,
        dark: Boolean = false,
        persian: Boolean = false,
        accentThemeId: Int = 0,
        widthDp: Int = 1280,
        heightDp: Int = 820,
        progress: Float = 0.34f,
        selectedIndex: Int = 1,
        tracks: List<String> = emptyList()
    ): String = shellPage(
        name = name,
        screen = com.alvand.player.ui.shell.Screen.HOME,
        dark = dark,
        persian = persian,
        accentThemeId = accentThemeId,
        widthDp = widthDp,
        heightDp = heightDp,
        progress = progress,
        selectedIndex = selectedIndex,
        tracks = tracks
    )
}
