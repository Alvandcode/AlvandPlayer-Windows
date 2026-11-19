package com.alvand.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragData
import androidx.compose.ui.draganddrop.dragData
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.decodeToImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.alvand.player.ui.components.AppTitleBar
import com.alvand.player.ui.components.ResizeHandles
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.alvand.player.data.SettingsStore
import com.alvand.player.data.ThemeMode
import com.alvand.player.i18n.LocalStrings
import com.alvand.player.i18n.StringsEn
import com.alvand.player.i18n.StringsFa
import com.alvand.player.library.DriveRoots
import com.alvand.player.library.DroppedMediaClassifier
import com.alvand.player.library.LibraryRoots
import com.alvand.player.library.LibraryStore
import com.alvand.player.library.ScanState
import com.alvand.player.library.TagReader
import com.alvand.player.lyrics.LyricsRepository
import com.alvand.player.model.Song
import com.alvand.player.player.AudioOutputs
import com.alvand.player.player.DecoderFactory
import com.alvand.player.player.JdkPlayerEngine
import com.alvand.player.player.PlayableItem
import com.alvand.player.player.QueueSnapshot
import com.alvand.player.player.ShortcutAction
import com.alvand.player.player.Shortcuts
import com.alvand.player.player.SleepTimer
import com.alvand.player.system.AppIcons
import com.alvand.player.system.AppPaths
import com.alvand.player.system.FolderPicker
import com.alvand.player.system.MediaKeyMap
import com.alvand.player.system.MediaKeys
import com.alvand.player.system.SingleInstance
import com.alvand.player.system.SystemTheme
import com.alvand.player.system.TrayController
import com.alvand.player.ui.shell.AppShell
import com.alvand.player.ui.shell.Screen
import com.alvand.player.ui.shell.matchSearch
import com.alvand.player.ui.components.JumpToTimeDialog
import com.alvand.player.ui.components.LyricsSheet
import com.alvand.player.ui.components.SongPropertiesDialog
import com.alvand.player.ui.screens.DiscoverPage
import com.alvand.player.ui.screens.EqualizerPage
import com.alvand.player.ui.screens.HomeScreen
import com.alvand.player.ui.screens.LibraryPage
import com.alvand.player.ui.screens.PlaylistsPage
import com.alvand.player.ui.screens.RadioPage
import com.alvand.player.ui.screens.SettingsPage
import com.alvand.player.ui.screens.VisualizerPage
import com.alvand.player.data.PlaylistRepository
import com.alvand.player.data.db.Database
import com.alvand.player.data.db.LibraryDao
import com.alvand.player.data.db.PlaylistSongEntity
import com.alvand.player.ui.theme.AlvandTheme
import com.alvand.player.ui.theme.LocalAP
import com.alvand.player.ui.theme.LocalAccentTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.Window as AwtWindow
import java.io.File
import java.nio.file.Path

const val APP_NAME = "Alvand Player"

/**
 * windows-only: نگاشت KeyEvent به توکن [Shortcuts.resolve].
 *
 * عمداً حداقلی است تا با هر کیبوردی کار کند؛ `?` از روی char دسکتاپ
 * خوانده می‌شود چون کلید مستقل ندارد.
 */
private fun keyToToken(event: KeyEvent): String? {
    return when (event.key) {
        Key.Spacebar -> " "
        Key.DirectionLeft -> "arrowleft"
        Key.DirectionRight -> "arrowright"
        Key.DirectionUp -> "arrowup"
        Key.DirectionDown -> "arrowdown"
        Key.M -> "m"
        Key.S -> "s"
        Key.R -> "r"
        Key.L -> "l"
        Key.N -> "n"
        Key.Q -> "q"
        Key.P -> "p"
        Key.T -> "t"
        Key.K -> "k"
        Key.E -> "e"
        Key.I -> "i"
        Key.Slash -> "?"
        else -> {
            // fallback: حروف تنها از روی keyCode
            val code = event.key.keyCode
            if (code in Key.A.keyCode..Key.Z.keyCode) {
                ('a' + (code - Key.A.keyCode).toInt()).toString()
            } else null
        }
    }
}

/**
 * وضعیت مشترک بین کد خارج از Compose (منوی نوار وظیفه) و درخت Compose.
 */
private class AppController {
    val windowVisible = mutableStateOf(true)
    val quitRequested = mutableStateOf(false)
    var window: AwtWindow? = null
        private set

    fun attach(w: AwtWindow) { window = w }

    fun showWindow() {
        window?.let {
            it.isVisible = true
            // اگر پنجره minimize شده باشد، فقط toFront کافی نیست و باید حالت عادی شود.
            (it as? java.awt.Frame)?.let { frame ->
                if (frame.extendedState and java.awt.Frame.ICONIFIED != 0) {
                    frame.extendedState = java.awt.Frame.NORMAL
                }
            }
            it.toFront()
        }
        windowVisible.value = true
    }

    /** بستن پنجره = پنهان شدن، نه خروج. پخش صدا نباید قطع شود. */
    fun hideWindow() { windowVisible.value = false }

    fun requestQuit() { quitRequested.value = true }
}

fun main(args: Array<String>) {
    AppPaths.ensureCreated()

    val instance = SingleInstance.acquire(AppPaths.appData)
    if (!instance.isPrimary) {
        instance.forwardToPrimary(args)
        return
    }

    val settings = SettingsStore()
    val roots = LibraryRoots()
    val scope = kotlinx.coroutines.CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val library = LibraryStore(roots, settings, scope)
    val controller = AppController()

    val engine = JdkPlayerEngine(scope, outputFactory = { fmt ->
        // دستگاه انتخابی کاربر در تنظیمات — هر آهنگ تازه (یا بازگشایی خط)
        // میکسر را از همین‌جا می‌خواند
        AudioOutputs.create(fmt, settings.current.outputDevice)
    })

    val tray = TrayController(
        appName = APP_NAME,
        onShow = { controller.showWindow() },
        onTogglePlay = { scope.launch { engine.toggle() } },
        onNext = { scope.launch { engine.next() } },
        onPrevious = { scope.launch { engine.previous() } },
        onQuit = { controller.requestQuit() }
    )
    tray.install()

    // // windows-only: تیترِ آهنگ در نوار وظیفه و آیکون سیستم
    // این معادل همان چیزی است که ویجت اندروید نشان می‌داد
    scope.launch {
        engine.state.collect { st ->
            st.current?.let { tray.setTrack(it.title, it.artist) }
            tray.setPlaying(st.isPlaying)
            library.onNowPlayingChanged(st.current?.id)
        }
    }

    val pendingExternalPaths = kotlinx.coroutines.flow.MutableStateFlow(args.toList())

    // اگر نمونهٔ دومی باز شد، پنجرهٔ همین نمونه جلو می‌آید
    instance.startListener { forwarded ->
        System.err.println("[main] second instance forwarded ${forwarded.size} argument(s)")
        controller.showWindow()
        pendingExternalPaths.value = forwarded
    }

    application {
        val windowState = rememberWindowState(size = DpSize(1040.dp, 820.dp))

        if (controller.quitRequested.value) {
            engine.release()
            tray.remove()
            instance.close()
            exitApplication()
        }

        Window(
            onCloseRequest = { controller.hideWindow() },
            state = windowState,
            title = APP_NAME,
            visible = controller.windowVisible.value,
            icon = rememberWindowIcon(),
            undecorated = true
        ) {
            controller.attach(window)
            AlvandApp(
                settings = settings,
                library = library,
                engine = engine,
                incomingPaths = pendingExternalPaths,
                windowState = windowState,
                onCloseWindow = { controller.hideWindow() }
            )
        }
    }
}

@Composable
private fun rememberWindowIcon(): Painter? {
    val bytes = remember {
        AppIcons::class.java.getResourceAsStream("/icons/alvand-256.png")?.use { it.readBytes() }
    }
    return remember(bytes) {
        bytes?.let {
            val bmp = it.decodeToImageBitmap()
            BitmapPainter(
                image = bmp,
                srcOffset = IntOffset.Zero,
                srcSize = IntSize(bmp.width, bmp.height)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
private fun AlvandApp(
    settings: SettingsStore,
    library: LibraryStore,
    engine: JdkPlayerEngine,
    incomingPaths: kotlinx.coroutines.flow.MutableStateFlow<List<String>>,
    windowState: WindowState,
    onCloseWindow: () -> Unit
) {
    val state by settings.state.collectAsState()
    val systemDark by SystemTheme.isSystemDark.collectAsState()
    val engineState by engine.state.collectAsState()
    val librarySongs by library.songs.collectAsState()
    val scanState by library.scanState.collectAsState()
    val externalPaths by incomingPaths.collectAsState()

    // صفحهٔ فعال شل — دقیقاً همان آیتم‌های سایدبار طرح.
    var screen by remember { mutableStateOf(Screen.HOME) }
    var searchQuery by remember { mutableStateOf("") }
    val searchFocus = remember { FocusRequester() }

    // windows-only: تایمر خواب + ولوم کیبورد + پرچم بازیابی صف — همه حافظه‌ای‌اند،
    // فقط خودِ صف در Settings می‌ماند. عمداً بدون هیچ وابستگی جدید.
    var sleepMinutes by remember { mutableStateOf(0) }
    var sleepDeadlineMs by remember { mutableStateOf<Long?>(null) }
    var sleepRemainingLabel by remember { mutableStateOf<String?>(null) }
    var volumeLevel by remember { mutableStateOf(state.volume) }
    var prevVolume by remember { mutableStateOf(state.volume) }
    var queueRestoredOnce by remember { mutableStateOf(false) }

    // windows-only: دیالوگ‌ها و شیت متن آهنگ — همه حافظه‌ای‌اند، نه ذخیره‌شده
    var showJumpDialog by remember { mutableStateOf(false) }
    var propertiesSong by remember { mutableStateOf<Song?>(null) }

    // windows-only: شیت لیریکس — مخزن کش با حافظه + دیسک، شبکه فقط با رضایت
    var showLyrics by remember { mutableStateOf(false) }
    var lyricsOutcome by remember { mutableStateOf<LyricsRepository.Outcome?>(null) }
    var lyricsNonce by remember { mutableStateOf(0) }
    val lyricsRepo = remember { LyricsRepository(AppPaths.lyricsDir) }

// پلی‌لیست‌ها از دیتابیس واقعی می‌آیند (نه دادهٔ ساختگی): خودِ صفحهٔ
    // «پلی‌لیست‌ها» همین جریان را می‌خواند و همین‌جا ساخت/حذف انجام می‌شود.
    val db = remember { Database(AppPaths.dbFile).also { it.migrate() } }
    val playlistsRepo = remember(db) { PlaylistRepository(LibraryDao(db)) }
    val playlists by playlistsRepo.observePlaylists().collectAsState(initial = emptyList())
    // آهنگ‌های هر پلی‌لیست کش می‌شوند تا با هر بازشدن، دیتابیس دوباره خوانده نشود
    var playlistSongs by remember { mutableStateOf<Map<Long, List<PlaylistSongEntity>>>(emptyMap()) }
    LaunchedEffect(playlists) {
        val missing: List<Long> = playlists.map { it.id }.filter { playlistSongs[it] == null }
        if (missing.isEmpty()) return@LaunchedEffect
        val loaded: Map<Long, List<PlaylistSongEntity>> = withContext(Dispatchers.IO) {
            val out = LinkedHashMap<Long, List<PlaylistSongEntity>>(missing.size)
            for (id in missing) {
                out[id] = runCatching {
                    playlistsRepo.observePlaylistSongs(id).first()
                }.getOrDefault(emptyList())
            }
            out
        }
        playlistSongs = playlistSongs + loaded
    }

    val coroutineScope = rememberCoroutineScope()

    // // windows-only: موتور وقتی آماده شد، صفِ معلقِ قبلی را باز می‌کند
    LaunchedEffect(Unit) { engine.onReady() }

    // windows-only: با هر تغییر صف، اسنپ‌شات فشرده ذخیره می‌شود.
    // عمداً وقتی صف خالی است چیزی پاک نمی‌کنیم تا بوتِ خالیِ اول،
    // اسنپ‌شاتِ ذخیره‌شدهٔ قبلی را نپراند.
    LaunchedEffect(engineState.queue, engineState.index) {
        if (engineState.queue.isNotEmpty()) {
            settings.setQueueSnapshot(
                QueueSnapshot.encodeQueue(engineState.queue, engineState.index)
            )
        }
    }

    // بازیابی صف پس از اسکن: اول اسنپ‌شات ذخیره‌شده، اگر نبود همان پرایم کتابخانه.
    // هرگز autoplay نمی‌کند — صدا فقط با دست کاربر شروع می‌شود.
    LaunchedEffect(scanState, librarySongs, engineState.queue.isEmpty()) {
        if (queueRestoredOnce) return@LaunchedEffect
        if (scanState !is ScanState.Done) return@LaunchedEffect
        if (!engineState.queue.isEmpty()) { queueRestoredOnce = true; return@LaunchedEffect }
        if (librarySongs.isEmpty()) return@LaunchedEffect
        queueRestoredOnce = true
        val snapshot = settings.current.queueSnapshot
        val decoded = runCatching { QueueSnapshot.decode(snapshot) }.getOrNull()
        if (decoded != null) {
            val (items, newIndex) = QueueSnapshot.rebuild(decoded.first, decoded.second, librarySongs)
            if (items.isNotEmpty()) {
                engine.setQueue(items, newIndex, autoPlay = false)
                return@LaunchedEffect
            }
        }
        // fallback: همان رفتار قبلی — پرایم از کتابخانهٔ visible بدون پخش
        val visible = library.visibleSongs()
        if (visible.isNotEmpty()) {
            engine.primeQueueFromLibrary(visible.map { PlayableItem.fromSong(it) })
        }
    }

    // windows-only: تیک‌تاک تایمر خواب — ۱ ثانیه‌ای، بدون نخ اضافه.
    LaunchedEffect(sleepDeadlineMs) {
        val deadline = sleepDeadlineMs ?: run {
            sleepRemainingLabel = null
            return@LaunchedEffect
        }
        while (true) {
            delay(1_000)
            val now = System.currentTimeMillis()
            if (!SleepTimer.isExpired(deadline, now)) {
                sleepRemainingLabel = SleepTimer.formatRemaining(SleepTimer.remainingMs(deadline, now))
                continue
            }
            // منقضی شد: فقط اگر واقعاً در حال پخش است pause کن
            if (engine.state.value.isPlaying) engine.pause()
            sleepMinutes = 0
            sleepDeadlineMs = null
            sleepRemainingLabel = null
            break
        }
    }

    // ولوم روی موتور می‌نشیند و در تنظیمات می‌ماند تا بین اجراها حفظ شود
    LaunchedEffect(volumeLevel) {
        engine.setVolume(volumeLevel)
        settings.setVolume(volumeLevel)
    }

    val toggleMute: () -> Unit = {
        volumeLevel = if (volumeLevel > 0f) {
            prevVolume = volumeLevel; 0f
        } else {
            if (prevVolume <= 0f) 1f else prevVolume
        }
    }

    // تنظیمات DSP ذخیره‌شده باید بلافاصله روی موتور بنشیند و با هر تغییر دنبال شود
    LaunchedEffect(state.dspSpec) { engine.setDsp(state.dspSpec) }

    // دستگاه خروجی صدا: هر تغییر، خط پخش را با میکسر تازه باز می‌کند
    LaunchedEffect(state.outputDevice) { engine.setOutputDevice(state.outputDevice) }

    val isDark = when (state.themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        else -> systemDark
    }

    LaunchedEffect(state.themeMode) {
        if (state.themeMode == ThemeMode.SYSTEM) SystemTheme.refresh()
    }

    val strings = remember(state.isPersian) { if (state.isPersian) StringsFa else StringsEn }
    var notice by remember { mutableStateOf<String?>(null) }
    var isDropActive by remember { mutableStateOf(false) }

    // پیام‌های موقت (مثلاً فرمت پشتیبانی‌نشده) نباید برای همیشه روی صفحه بمانند.
    LaunchedEffect(notice) {
        if (notice != null) {
            delay(10_000)
            notice = null
        }
    }

    /**
     * مدیریت یکجای فایل‌ها و پوشه‌های بیرونی: انتخاب از دیالوگ، رها کردن روی
     * پنجره، آرگومان خط فرمان و فایلِ بازشده با دوبار کلیک.
     */
    suspend fun openExternalPaths(rawPaths: List<String>, autoplayFiles: Boolean) {
        val requested = rawPaths.toSet()
        val media = DroppedMediaClassifier.classify(requested.map(::File))
        if (media.directories.isNotEmpty()) {
            withContext(Dispatchers.IO) {
                library.roots.addRoots(media.directories)
                library.scan()
            }
        }

        val items = withContext(Dispatchers.IO) {
            media.playableFiles.mapNotNull { path ->
                if (!DecoderFactory.canDecode(path.toString())) return@mapNotNull null
                runCatching { PlayableItem.fromSong(TagReader.toSong(path)) }.getOrNull()
            }
        }
        if (items.isNotEmpty()) {
            engine.setQueue(items, 0, autoPlay = autoplayFiles)
            notice = null
        } else if (media.unsupportedFiles.isNotEmpty()) {
            notice = "${strings.unsupportedFormat} ${media.unsupportedFiles.first().fileName}"
        }
        incomingPaths.value = incomingPaths.value - requested
    }

    // فایلِ بازشده از بیرون باید بلافاصله پخش شود؛ پوشهٔ انتخاب‌شده فقط اسکن.
    val openAudioFile: () -> Unit = {
        // دیالوگ Swing باید روی نخ EDT باز شود
        Thread {
            FolderPicker.pickAudioFile(strings.openFile)?.let { chosen ->
                coroutineScope.launch {
                    openExternalPaths(listOf(chosen.absolutePath), autoplayFiles = true)
                }
            }
        }.apply { isDaemon = true; start() }
    }

    // همان رفتار «انتخاب پوشه + اسکن» باید هم در کتابخانه و هم در حالت خالی
    // صفحهٔ پخش در دسترس باشد؛ پس یک‌بار اینجا تعریف می‌کنیم.
    val chooseMusicFolder: () -> Unit = {
        // دیالوگ Swing باید روی نخ EDT باز شود
        Thread {
            FolderPicker.pickDirectory(strings.chooseFolder)?.let { chosen ->
                coroutineScope.launch {
                    openExternalPaths(listOf(chosen.absolutePath), autoplayFiles = false)
                }
            }
        }.apply { isDaemon = true; start() }
    }

    val handleDroppedFiles: (List<File>) -> Unit = { files ->
        coroutineScope.launch {
            openExternalPaths(files.map { it.absolutePath }, autoplayFiles = true)
        }
    }

    // فایل‌های رسیده از نمونهٔ دوم یا آرگومان اجرا، بعد از آماده شدن موتور باز می‌شوند.
    LaunchedEffect(externalPaths) {
        if (externalPaths.isNotEmpty()) {
            openExternalPaths(externalPaths, autoplayFiles = true)
        }
    }

    // windows-only: چرخهٔ تایمر خواب — خاموش → ۱۵ → ۳۰ → ۴۵ → ۶۰ → خاموش
    val cycleSleep: () -> Unit = {
        sleepMinutes = SleepTimer.nextOption(sleepMinutes)
        sleepDeadlineMs = SleepTimer.deadlineFromNow(System.currentTimeMillis(), sleepMinutes)
        sleepRemainingLabel = sleepDeadlineMs?.let {
            SleepTimer.formatRemaining(SleepTimer.remainingMs(it, System.currentTimeMillis()))
        }
    }

    // windows-only: آهنگِ در حال پخش (PlayableItem) → مدل Song برای دیالوگ‌ها.
    // اول در کتابخانه پیدایش می‌کنیم تا مدت/تگ دقیق باشد، اگر نبود از خودِ صف.
    fun currentSong(): Song? {
        val item = engineState.current ?: return null
        return library.songs.value.firstOrNull { it.id == item.id }
            ?: Song(
                id = item.id,
                title = item.title,
                artist = item.artist,
                album = item.album,
                uri = item.uri,
                durationMs = item.durationMs
            )
    }

    // windows-only: بارگذاری متن با باز شدن شیت، عوض شدن آهنگ، یا تلاش دوباره.
    // شبکه فقط وقتی لمس می‌شود که کاربر جستجوی آنلاین را روشن کرده باشد.
    LaunchedEffect(showLyrics, engineState.current?.id, state.onlineLyricsEnabled, lyricsNonce) {
        if (!showLyrics) return@LaunchedEffect
        val song = currentSong()
        if (song == null) {
            lyricsOutcome = null
            return@LaunchedEffect
        }
        if (!state.onlineLyricsEnabled) {
            // کش آفلاین هنوز خوانده می‌شود، ولی شبکه نه
            lyricsOutcome = withContext(Dispatchers.IO) {
                lyricsRepo.get(song, onlineEnabled = false).takeIf {
                    it is LyricsRepository.Outcome.Ready
                }
            }
            return@LaunchedEffect
        }
        lyricsOutcome = null
        lyricsOutcome = withContext(Dispatchers.IO) {
            lyricsRepo.get(song, onlineEnabled = true)
        }
    }

    // windows-only: اجرای اکشن کیبورد — همه از یک قیف می‌گذرند تا رفتار یکی بماند.
    fun handleShortcut(action: ShortcutAction): Boolean {
        when (action) {
            ShortcutAction.NONE -> return false
            ShortcutAction.TOGGLE_PLAY -> coroutineScope.launch { engine.toggle() }
            ShortcutAction.NEXT -> coroutineScope.launch { engine.next() }
            ShortcutAction.PREVIOUS -> coroutineScope.launch { engine.previous() }
            ShortcutAction.SEEK_BACK -> coroutineScope.launch {
                val cur = engine.state.value
                if (cur.hasKnownDuration) engine.seekTo(cur.positionMs - Shortcuts.SEEK_STEP_MS)
            }
            ShortcutAction.SEEK_FORWARD -> coroutineScope.launch {
                val cur = engine.state.value
                if (cur.hasKnownDuration) engine.seekTo(cur.positionMs + Shortcuts.SEEK_STEP_MS)
            }
            ShortcutAction.VOLUME_UP -> {
                prevVolume = volumeLevel
                volumeLevel = (volumeLevel + 0.05f).coerceIn(0f, 1f)
            }
            ShortcutAction.VOLUME_DOWN -> {
                prevVolume = volumeLevel
                volumeLevel = (volumeLevel - 0.05f).coerceIn(0f, 1f)
            }
            ShortcutAction.TOGGLE_MUTE -> toggleMute()
            ShortcutAction.TOGGLE_SHUFFLE -> engine.setShuffle(!engineState.shuffle)
            ShortcutAction.CYCLE_REPEAT -> engine.cycleRepeat()
            ShortcutAction.TOGGLE_FAVORITE -> {
                engineState.current?.let { settings.toggleLike(it.id) }
            }
            ShortcutAction.OPEN_LIBRARY_SEARCH -> {
                searchQuery = ""
                searchFocus.requestFocus()
            }
            ShortcutAction.TOGGLE_NOW_PLAYING, ShortcutAction.SHOW_QUEUE -> {
                if (screen == Screen.HOME) screen = Screen.VISUALIZER
            }
            ShortcutAction.OPEN_MINI_MODE, ShortcutAction.SHOW_SHORTCUTS -> {
                notice = strings.shortcutsHint
            }
            ShortcutAction.CYCLE_SLEEP_TIMER -> cycleSleep()
            ShortcutAction.OPEN_EQUALIZER -> screen = Screen.EQUALIZER
            ShortcutAction.SHOW_PROPERTIES -> {
                propertiesSong = currentSong()
            }
            ShortcutAction.JUMP_TO_TIME -> showJumpDialog = true
            ShortcutAction.CYCLE_AB_LOOP -> coroutineScope.launch { engine.cycleAbLoop() }
            ShortcutAction.TOGGLE_VISUALIZER -> {
                screen = if (screen == Screen.VISUALIZER) Screen.HOME else Screen.VISUALIZER
            }
        }
        return true
    }

    // کلیدهای مدیای سخت‌افزاری (هدست/کیبورد) — سراسری ویندوزی، حتی وقتی
    // پنجره در سینی است. از همان قیف handleShortcut می‌گذرد تا رفتار یکی بماند.
    DisposableEffect(Unit) {
        MediaKeys.start { action ->
            when (action) {
                MediaKeyMap.Action.PLAY_PAUSE -> handleShortcut(ShortcutAction.TOGGLE_PLAY)
                MediaKeyMap.Action.NEXT -> handleShortcut(ShortcutAction.NEXT)
                MediaKeyMap.Action.PREVIOUS -> handleShortcut(ShortcutAction.PREVIOUS)
                MediaKeyMap.Action.STOP -> coroutineScope.launch { engine.pause() }
            }
        }
        onDispose { MediaKeys.stop() }
    }

    CompositionLocalProvider(
        LocalStrings provides strings,
        LocalLayoutDirection provides
            if (state.isPersian) LayoutDirection.Rtl else LayoutDirection.Ltr
    ) {
        AlvandTheme(darkTheme = isDark, accentThemeId = state.accentTheme) {
            val pal = LocalAP.current

            // در هر اجرا، درایوهای قابل‌دسترس به‌جز C هم به ریشه‌ها اضافه می‌شوند تا
            // اگر درایو تازه‌ای وصل شده باشد، جا نماند. انتخاب‌های ذخیره‌شدهٔ کاربر
            // حذف نمی‌شوند.
            LaunchedEffect(Unit) {
                val defaults = withContext(Dispatchers.IO) { DriveRoots.defaultScanRoots() }
                library.roots.ensureDriveRoots(defaults)
            }

            // مثل اندروید که در شروع کتابخانه را از روی MediaStore پر می‌کند،
            // اگر پوشه‌ای هست ولی کتابخانه خالی است، یک‌بار اسکن می‌کنیم
            LaunchedEffect(library.roots.roots.collectAsState().value.size) {
                if (library.rootCount() > 0 && library.songs.value.isEmpty()) library.scan()
                // ناظر فایل‌سیستم: فایل‌های جدید/حذف‌شده بدون اسکن دستی دیده می‌شوند
                library.startWatching()
            }

            // قاب پنجره: نوار عنوان اختصاصی + محتوا. عرض محتوا سقف دارد تا در
            // پنجره‌های خیلی عریض، چیدمان از هم نپاشد و وسط بماند.
            Column(
                Modifier
                    .fillMaxSize()
                    .background(pal.bg)
            ) {
                AppTitleBar(state = windowState, title = APP_NAME, onClose = onCloseWindow)
                Box(Modifier.weight(1f)) {
                    Box(
                        Modifier.fillMaxSize(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .widthIn(max = 1160.dp)
                    // windows-only: میانبرهای کیبورد — یک قیف واحد، بدون جنگ با تایپ.
                    // وقتی تب کتابخانه است و کلید حرفیِ تنها می‌آید، فرض می‌کنیم
                    // کاربر در فیلد جستجو تایپ می‌کند و شورت‌کات را نمی‌بلعیم.
                    .onPreviewKeyEvent { event ->
                        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                        if (event.key == Key.Escape) {
                            // برگشت سراسری مثل دکمهٔ Back اندروید: اول اورلی‌ها
                            // (شناسنامه، متن آهنگ، پرش به زمان)، بعد برگشت به خانه.
                            when {
                                showJumpDialog -> showJumpDialog = false
                                propertiesSong != null -> propertiesSong = null
                                showLyrics -> showLyrics = false
                                screen != Screen.HOME -> screen = Screen.HOME
                                else -> return@onPreviewKeyEvent false
                            }
                            return@onPreviewKeyEvent true
                        }
                        val token = keyToToken(event) ?: return@onPreviewKeyEvent false
                        // Space روی دکمهٔ فوکوس‌شده نباید دوبار عمل کند — اگر دکمه‌ای
                        // فوکوس است، خودِ Compose آن را مصرف می‌کند؛ اینجا فقط وقتی
                        // وارد می‌شویم که مصرف نشده باشد (Preview قبل از مصرف است،
                        // پس برای Space کمی محافظه‌کاریم و همان رفتار استاندارد را نگه می‌داریم).
                        val ctrl = event.isCtrlPressed
                        val likelyTyping = screen == Screen.LIBRARY &&
                            token.length == 1 && token[0].isLetter() && !ctrl
                        val action = Shortcuts.resolve(token, ctrl, likelyTyping)
                        if (action == ShortcutAction.NONE) return@onPreviewKeyEvent false
                        handleShortcut(action)
                    }
                    .dragAndDropTarget(
                        shouldStartDragAndDrop = { event ->
                            runCatching { event.dragData() is DragData.FilesList }.getOrDefault(false)
                        },
                        target = object : DragAndDropTarget {
                            override fun onEntered(event: DragAndDropEvent) {
                                isDropActive = true
                            }

                            override fun onExited(event: DragAndDropEvent) {
                                isDropActive = false
                            }

                            override fun onEnded(event: DragAndDropEvent) {
                                isDropActive = false
                            }

                            override fun onDrop(event: DragAndDropEvent): Boolean {
                                val files = runCatching {
                                    (event.dragData() as? DragData.FilesList)
                                        ?.readFiles().orEmpty().map(::File)
                                }.getOrDefault(emptyList())
                                if (files.isEmpty()) return false
                                handleDroppedFiles(files)
                                return true
                            }
                        }
                    )
            ) {
                val accent = LocalAccentTheme.current.accent(isDark)
                val likedNow = engineState.current?.id?.let { it in state.likedIds } ?: false
                val toggleLikeNow: () -> Unit = {
                    engineState.current?.let { cur ->
                        val song = library.songs.value.find { it.id == cur.id }
                        if (song != null) library.toggleFavourite(song)
                        else settings.toggleLike(cur.id)
                    }
                }
                val playSongs: (List<Song>) -> Unit = { songs ->
                    if (songs.isNotEmpty()) {
                        coroutineScope.launch {
                            engine.setQueue(songs.map { PlayableItem.fromSong(it) }, 0, autoPlay = true)
                        }
                    }
                }

                AppShell(
                    screen = screen,
                    onNavigate = { screen = it },
                    accent = accent,

                    miniTitle = engineState.current?.title,
                    miniArtist = engineState.current?.artist,
                    miniPlaying = engineState.isPlaying,
                    onMiniClick = { screen = Screen.VISUALIZER },

                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    searchResults = remember(librarySongs, searchQuery) {
                        matchSearch(librarySongs, searchQuery)
                    },
                    searchFocusRequester = searchFocus,
                    onPlaySong = { song -> playSongs(listOf(song)) },

                    content = {
                        when (screen) {
                            Screen.HOME -> HomeScreen(
                                recentSongs = librarySongs.take(14),
                                accent = accent,
                                onPlayMix = { playSongs(librarySongs) },
                                onBrowse = { screen = Screen.LIBRARY },
                                onNavigate = { screen = it },
                                onPlaySong = { playSongs(listOf(it)) },
                                onViewAllRecent = { screen = Screen.LIBRARY }
                            )

                            Screen.LIBRARY -> LibraryPage(
                                store = library,
                                onChooseFolder = chooseMusicFolder,
                                onOpenFile = openAudioFile,
                                onPlay = { song ->
                                    val queue = library.visibleSongs()
                                    val index = queue.indexOfFirst { it.id == song.id }
                                    if (index >= 0) {
                                        coroutineScope.launch {
                                            engine.setQueue(
                                                queue.map { PlayableItem.fromSong(it) },
                                                index,
                                                autoPlay = true
                                            )
                                        }
                                    }
                                },
                                onPlayGroup = playSongs,
                                onShowProperties = { song -> propertiesSong = song }
                            )

                            Screen.PLAYLISTS -> PlaylistsPage(
                                playlists = playlists,
                                songsOf = { id -> playlistSongs[id].orEmpty() },
                                songCountOf = { id -> playlistSongs[id].orEmpty().size },
                                onCreate = { name -> coroutineScope.launch { playlistsRepo.createPlaylist(name) } },
                                onDelete = { id -> coroutineScope.launch { playlistsRepo.deletePlaylist(id) } },
                                onPlaySongs = playSongs
                            )

                            Screen.DISCOVER -> DiscoverPage(
                                songs = librarySongs,
                                onPlaySongs = playSongs
                            )

                            Screen.RADIO -> RadioPage(
                                songs = librarySongs,
                                onPlaySongs = playSongs
                            )

                            Screen.EQUALIZER -> EqualizerPage(
                                spec = state.dspSpec,
                                accent = accent,
                                visualizerEnabled = screen == Screen.VISUALIZER,
                                onSpecChange = settings::setDspSpec,
                                onToggleVisualizer = {
                                    screen = if (screen == Screen.VISUALIZER) Screen.EQUALIZER else Screen.VISUALIZER
                                }
                            )

                            Screen.VISUALIZER -> VisualizerPage(
                                state = engineState,
                                accent = accent,
                                liked = likedNow,
                                onToggleLike = toggleLikeNow,
                                onTogglePlay = { coroutineScope.launch { engine.toggle() } },
                                onNext = { coroutineScope.launch { engine.next() } },
                                onPrevious = { coroutineScope.launch { engine.previous() } },
                                onToggleShuffle = { engine.setShuffle(!engineState.shuffle) },
                                onCycleRepeat = { engine.cycleRepeat() },
                                onSeek = { ms -> coroutineScope.launch { engine.seekTo(ms) } }
                            )

                            Screen.SETTINGS -> SettingsPage(
                                settings = state,
                                systemDark = systemDark,
                                volume = volumeLevel,
                                accent = accent,
                                onVolumeChange = { volumeLevel = it.coerceIn(0f, 1f) },
                                onThemeModeChange = settings::setThemeMode,
                                onAccentChange = settings::setAccentTheme,
                                onToggleLanguage = {
                                    settings.setLanguage(if (state.isPersian) "en" else "fa")
                                },
                                onToggleOnlineLyrics = {
                                    settings.setOnlineLyricsEnabled(!state.onlineLyricsEnabled)
                                },
                                onTogglePerformance = {
                                    settings.setPerformanceMode(!state.performanceMode)
                                },
                                onOutputDevice = { name -> settings.setOutputDevice(name) }
                            )
                        }
                    },

                    engineState = engineState,
                    liked = likedNow,
                    onToggleLike = toggleLikeNow,
                    onTogglePlay = { coroutineScope.launch { engine.toggle() } },
                    onNext = { coroutineScope.launch { engine.next() } },
                    onPrevious = { coroutineScope.launch { engine.previous() } },
                    onSeek = { ms -> coroutineScope.launch { engine.seekTo(ms) } },
                    onToggleShuffle = { engine.setShuffle(!engineState.shuffle) },
                    onCycleRepeat = { engine.cycleRepeat() },
                    onSelectQueueIndex = { i -> coroutineScope.launch { engine.playIndex(i) } },
                    onMoveQueueItem = { from, to -> engine.moveInQueue(from, to) },
                    onOpenLyrics = { showLyrics = true },
                    onShowProperties = { propertiesSong = currentSong() },
                    onOpenSettings = { screen = Screen.SETTINGS },
                    onOpenNotifications = { /* TODO */ }
                )

                if (notice != null) {
                    Box(
                        Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 8.dp)
                            .clip(RoundedCornerShape(50))
                            .background(pal.card)
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(notice.orEmpty(), color = pal.ink, fontSize = 12.sp)
                    }
                }

                propertiesSong?.let { song ->
                    SongPropertiesDialog(
                        song = song,
                        liked = song.id in state.likedIds,
                        onToggleFavorite = { settings.toggleLike(song.id) },
                        onClose = { propertiesSong = null },
                        onJumpToTime = { propertiesSong = null; showJumpDialog = true },
                        loopA = engineState.loopA,
                        loopB = engineState.loopB,
                        onSetLoopA = { coroutineScope.launch { engine.setLoopStart() } },
                        onSetLoopB = { coroutineScope.launch { engine.setLoopEnd() } },
                        onClearLoop = { coroutineScope.launch { engine.clearAbLoop() } }
                    )
                }
                if (showLyrics) {
                    LyricsSheet(
                        song = currentSong(),
                        positionMs = engineState.positionMs,
                        outcome = lyricsOutcome,
                        onlineEnabled = state.onlineLyricsEnabled,
                        accent = LocalAccentTheme.current.accent(isDark),
                        onToggleOnline = {
                            settings.setOnlineLyricsEnabled(!state.onlineLyricsEnabled)
                        },
                        onRetry = { lyricsNonce++ },
                        onClose = { showLyrics = false }
                    )
                }
                if (showJumpDialog) {
                    JumpToTimeDialog(
                        durationMs = engineState.durationMs,
                        onGo = { ms -> coroutineScope.launch { engine.seekTo(ms) } },
                        onClose = { showJumpDialog = false }
                    )
                }
                if (isDropActive) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(pal.bg.copy(alpha = 0.82f))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            strings.dropFiles,
                            color = pal.ink,
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
        ResizeHandles(windowState)
    }
}
    }
}
}
