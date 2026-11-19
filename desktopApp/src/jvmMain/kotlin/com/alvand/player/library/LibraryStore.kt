package com.alvand.player.library

import com.alvand.player.data.SettingsStore
import com.alvand.player.model.Song
import com.alvand.player.system.AppPaths
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.file.Path

/** وضعیت اسکن برای نمایش به کاربر */
sealed interface ScanState {
    data object Idle : ScanState
    data class Running(val found: Int) : ScanState
    data class Done(val added: Int, val total: Int, val truncated: Boolean) : ScanState
    data class Failed(val message: String) : ScanState
}

/**
 * مغز کتابخانه: نگه‌داری فهرست آهنگ‌ها، جستجو، سورت و گروه‌بندی.
 *
 * قاعدهٔ سخت: **هیچ کار سنگینی روی نخ UI انجام نمی‌شود.** بازپیمایی پوشه و خواندن
 * تگ هزاران فایل، همه روی `Dispatchers.IO` است.
 */
class LibraryStore(
    val roots: LibraryRoots,
    private val settings: SettingsStore,
    private val scope: CoroutineScope,
    private val scanner: FolderScanner = FolderScanner(excludedDirs = setOf(AppPaths.appData))
) {

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()

    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _favoritesOnly = MutableStateFlow(false)
    val favoritesOnly: StateFlow<Boolean> = _favoritesOnly.asStateFlow()

    private val _groupMode = MutableStateFlow(GroupMode.SONGS)
    val groupMode: StateFlow<GroupMode> = _groupMode.asStateFlow()

    /**
     * وضعیت تنظیمات، مستقیم از [SettingsStore].
     * UI از روی همین می‌خواند تا لازم نباشد بداند تنظیمات کجا ذخیره می‌شوند
     * (`likedIds` برای ستاره‌ها، `librarySort` برای ترتیب).
     */
    val settingsState: StateFlow<com.alvand.player.data.AppSettings> = settings.state

    /**
     * آیدی آهنگی که الان در حال پخش است.
     *
     * موتور صدا و کتابخانه هیچ وابستگی‌ای به هم ندارند؛ این فقط یک پل یک‌طرفه است
     * تا UI بتواند ردیف در حال پخش را در کتابخانه هایلایت کند.
     */
    private val _nowPlayingId = MutableStateFlow<Long?>(null)
    val nowPlayingId: StateFlow<Long?> = _nowPlayingId.asStateFlow()

    /** از موتور صدا صدا زده می‌شود */
    fun onNowPlayingChanged(songId: Long?) {
        _nowPlayingId.value = songId
    }

    enum class GroupMode { SONGS, ALBUMS, ARTISTS }

    private var scanJob: Job? = null

    /**
     * پرچم لغو. دلیلش این است که `isCancelled` باید از بیرونِ یک coroutine
     * صدا زده شود و در آنجا نمی‌شود از توابع suspend استفاده کرد.
     */
    private val scanCancelled = java.util.concurrent.atomic.AtomicBoolean(false)

    /** نتیجهٔ فیلتر + سورت روی فهرست فعلی */
    fun visibleSongs(): List<Song> = LibraryFilter.filterAndSort(
        songs = _songs.value,
        query = _query.value,
        sortMode = settings.current.librarySort,
        liked = settings.current.likedIds,
        favoritesOnly = _favoritesOnly.value
    )

    fun setQuery(value: String) { _query.value = value }

    fun setFavoritesOnly(value: Boolean) { _favoritesOnly.value = value }

    fun setGroupMode(mode: GroupMode) { _groupMode.value = mode }

    fun setSortMode(mode: Int) = settings.setLibrarySort(mode)

    fun toggleFavourite(song: Song) = settings.toggleLike(song.id)

    fun addRoot(path: Path) = roots.add(path)

    fun removeRoot(path: Path) = roots.remove(path)

    fun rootCount(): Int = roots.current.size

    fun cancelScan() {
        scanCancelled.set(true)
        scanJob?.cancel()
        scanJob = null
        _scanState.value = ScanState.Idle
    }

    // --- ناظر فایل‌سیستم: تازه‌سازی خودکار بدون «اسکن دوبارهٔ» دستی ---

    private var watcher: LibraryWatcher? = null

    /** شروع نظارت خودکار روی ریشه‌های فعلی. فراخوانی دوباره، ریشه‌های تازه را می‌گیرد */
    fun startWatching() {
        watcher?.stop()
        val w = LibraryWatcher(scope, onChanged = ::applyWatcherChanges)
        watcher = w
        w.start(roots.current)
    }

    fun stopWatching() {
        watcher?.stop()
        watcher = null
    }

    private fun applyWatcherChanges(changedFiles: Set<Path>, deletedFiles: Set<Path>) {
        // مجموعه‌های خالی یعنی OVERFLOW — تنها پاسخ امن، اسکن کامل است
        if (changedFiles.isEmpty() && deletedFiles.isEmpty()) {
            scan()
            return
        }
        val current = _songs.value
        if (current.isEmpty()) {
            scan()
            return
        }
        var next = current

        if (deletedFiles.isNotEmpty()) {
            val goneUris = deletedFiles.mapTo(HashSet()) { it.toAbsolutePath().toString() }
            next = next.filter { it.uri !in goneUris }
        }

        if (changedFiles.isNotEmpty()) {
            // تگ فایل تازه روی Dispatchers.IO خوانده و جایگزین/اضافه می‌شود
            val rebuilt = ArrayList<Song>(changedFiles.size)
            for (file in changedFiles) {
                runCatching { TagReader.toSong(file) }.getOrNull()?.let { rebuilt.add(it) }
            }
            if (rebuilt.isNotEmpty()) {
                val byUri = rebuilt.associateBy { it.uri }
                val kept = next.mapNotNull { byUri[it.uri] ?: it }
                val known = next.mapTo(HashSet()) { it.uri }
                val added = rebuilt.filter { it.uri !in known }
                next = kept + added
            }
        }

        if (next.size > FolderScanner.MAX_SONGS) {
            scan() // سقف شکسته شد — اسکن کامل قواعد MAX_SONGS را برقرار می‌کند
            return
        }
        if (next != current) _songs.value = next
    }

    /**
     * اسکن از صفر. آهنگ‌های قبلی تا پایان اسکن نگه داشته می‌شوند تا UI خالی نشود.
     */
    fun scan() {
        scanCancelled.set(false)
        scanJob?.cancel()
        _scanState.value = ScanState.Running(0)

        scanJob = scope.launch {
            val rootList = roots.current
            if (rootList.isEmpty()) {
                _scanState.value = ScanState.Done(0, 0, false)
                return@launch
            }

            try {
                val files = withContext(Dispatchers.IO) {
                    scanner.scan(rootList) { scanCancelled.get() }.files
                }

                val knownUris = _songs.value.mapTo(HashSet()) { it.uri }
                val merged = ArrayList<Song>(files.size)
                var added = 0

                for ((index, file) in files.withIndex()) {
                    // اگر کاربر لغو کرد، همین‌جا ولش کن — نباید تا آخر ادامه دهیم
                    if (scanCancelled.get()) return@launch
                    val song = withContext(Dispatchers.IO) { TagReader.toSong(file) }
                    if (knownUris.add(song.uri)) added++
                    merged.add(song)
                    if (index % 25 == 0) _scanState.value = ScanState.Running(index + 1)
                }
                val truncated = files.size >= FolderScanner.MAX_SONGS

                // آهنگ‌هایی که دیگر روی دیسک نیستند در اسکن تازه حذف می‌شوند،
                // درست مثل رفتار اسکن دوباره در اندروید
                _songs.value = merged
                _scanState.value = ScanState.Done(added, merged.size, truncated)
            } catch (t: Throwable) {
                if (t is kotlinx.coroutines.CancellationException) throw t
                _scanState.value = ScanState.Failed(t.message ?: t::class.simpleName.orEmpty())
            }
        }
    }
}
