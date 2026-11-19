package com.alvand.player.data

import com.alvand.player.audio.DspSpec
import com.alvand.player.data.AppSettings
import com.alvand.player.library.LibrarySort
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * مدیریت تنظیمات ظاهری و رفتاری اپ — بین اجراها می‌ماند.
 *
 * روی اندروید این کار با DataStore انجام می‌شد؛ روی دسکتاپ Room/DataStore وجود ندارد
 * پس یک فایل properties ساده با **نوشتن اتمیک** جایگزین شده: اول در فایل موقت نوشته
 * می‌شود و بعد با `ATOMIC_MOVE` جایگزین می‌شود. اگر وسط نوشتن برنامه کرش کند،
 * فایل قبلی سالم می‌ماند (همان هدفی که DataStore داشت).
 *
 * نام کلیدها عیناً همان نام‌های اندروید است.
 */
class SettingsStore(
    private val file: java.nio.file.Path = com.alvand.player.system.AppPaths.settingsFile,
    private val scope: kotlinx.coroutines.CoroutineScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO)
) {

    private val writeLock = kotlinx.coroutines.sync.Mutex()
    private val _state = kotlinx.coroutines.flow.MutableStateFlow(readFromDisk())

    val state: kotlinx.coroutines.flow.StateFlow<AppSettings> = _state.asStateFlow()

    val current: AppSettings get() = _state.value

    private fun readFromDisk(): AppSettings {
        val loaded = runCatching {
            if (!java.nio.file.Files.exists(file)) return AppSettings()
            val props = java.util.Properties()
            java.nio.file.Files.newInputStream(file).use { props.load(it) }
            AppSettings(
                themeMode = props.getProperty("theme_mode")?.toIntOrNull()?.coerceIn(0, 2)
                    ?: AppSettings.DEFAULT_THEME_MODE,
                accentTheme = props.getProperty("accent_theme")?.toIntOrNull()
                    ?.coerceIn(0, com.alvand.player.data.AccentThemeMode.COUNT - 1) ?: com.alvand.player.data.AccentThemeMode.MONO,
                backgroundPath = props.getProperty("background_uri"),
                onboardingSeen = props.getProperty("onboarding_seen")?.toBooleanStrictOrNull() ?: false,
                onlineLyricsEnabled =
                    props.getProperty("online_lyrics_enabled")?.toBooleanStrictOrNull() ?: false,
                likedIds = props.getProperty("liked_ids")
                    ?.split(',')
                    ?.mapNotNull { it.trim().toLongOrNull() }
                    ?.toSet()
                    ?: emptySet(),
                librarySort = props.getProperty("library_sort")?.toIntOrNull()?.coerceIn(0, 4)
                    ?: com.alvand.player.library.LibrarySort.DEFAULT,
                language = com.alvand.player.data.AppLocale.normalize(props.getProperty("app_language") ?: "en"),
                dspSpec = DspSpecCodec.decode(props.getProperty("dsp_spec")) ?: com.alvand.player.audio.DspSpec(),
                volume = props.getProperty("volume")?.toFloatOrNull()?.coerceIn(0f, 1f) ?: 1f,
                outputDevice = props.getProperty("output_device")?.takeIf { it.isNotBlank() },
                queueSnapshot = props.getProperty("queue_snapshot") ?: "",
                performanceMode = props.getProperty("performance_mode")
                    ?.toBooleanStrictOrNull() ?: false,
            )
        }.getOrElse { error ->
            // رفتار عمدی: فایل تنظیمات خراب یعنی برگشت به پیش‌فرض‌ها.
            // این دقیقاً معادل ReplaceFileCorruptionHandler { emptyPreferences() } اندروید است.
            // (توجه: خرابی دیتابیس مثل پلی‌لیست‌ها متفاوت است و نباید ساکت پاک شود.)
            System.err.println("[settings] unreadable, falling back to defaults: ${error.message}")
            AppSettings()
        }
        return loaded
    }

    private fun writeToDisk(s: AppSettings) {
        val props = java.util.Properties().apply {
            setProperty("theme_mode", s.themeMode.toString())
            setProperty("accent_theme", s.accentTheme.toString())
            setProperty("onboarding_seen", s.onboardingSeen.toString())
            setProperty("online_lyrics_enabled", s.onlineLyricsEnabled.toString())
            setProperty("library_sort", s.librarySort.toString())
            setProperty("liked_ids", s.likedIds.joinToString(","))
            setProperty("app_language", s.language)
            setProperty("dsp_spec", DspSpecCodec.encode(s.dspSpec))
            setProperty("volume", s.volume.toString())
            setProperty("output_device", s.outputDevice ?: "")
            setProperty("queue_snapshot", s.queueSnapshot)
            setProperty("performance_mode", s.performanceMode.toString())
            s.backgroundPath?.let { setProperty("background_uri", it) }
        }

        runCatching {
            java.nio.file.Files.createDirectories(file.parent)
            val tmp = file.resolveSibling(file.fileName.toString() + ".tmp")
            try {
                java.nio.file.Files.newOutputStream(tmp).use { out ->
                    props.store(out, "Alvand Player settings")
                    out.flush()
                }
                try {
                    java.nio.file.Files.move(
                        tmp, file,
                        java.nio.file.StandardCopyOption.ATOMIC_MOVE,
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING
                    )
                } catch (_: java.nio.file.AtomicMoveNotSupportedException) {
                    // بعضی درایوها/فایل‌سیستم‌ها ATOMIC_MOVE ندارند؛ جایگزینی عادی بهتر از هیچ است
                    java.nio.file.Files.move(tmp, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING)
                }
            } finally {
                // فایل موقت هرگز نباید روی دیسک بماند، حتی اگر جابه‌جایی شکست بخورد
                runCatching { java.nio.file.Files.deleteIfExists(tmp) }
            }
        }.onFailure { error ->
            if (error is java.io.IOException) {
                System.err.println("[settings] write failed: ${error.message}")
            }
        }
    }

    private fun update(block: (AppSettings) -> AppSettings) {
        val next = block(_state.value)
        if (next == _state.value) return
        _state.value = next
        scope.launch { writeLock.withLock { writeToDisk(next) } }
    }

    fun setThemeMode(mode: Int) = update { it.copy(themeMode = mode.coerceIn(0, 2)) }

    fun setAccentTheme(id: Int) =
        update { it.copy(accentTheme = id.coerceIn(0, com.alvand.player.data.AccentThemeMode.COUNT - 1)) }

    fun setBackgroundPath(path: String?) = update { it.copy(backgroundPath = path) }

    fun setOnboardingSeen(seen: Boolean = true) = update { it.copy(onboardingSeen = seen) }

    fun setOnlineLyricsEnabled(enabled: Boolean) =
        update { it.copy(onlineLyricsEnabled = enabled) }

    fun setLibrarySort(mode: Int) = update { it.copy(librarySort = mode.coerceIn(0, 4)) }

    fun setLanguage(code: String) = update { it.copy(language = com.alvand.player.data.AppLocale.normalize(code)) }

    /** تنظیمات DSP — هم در فایل ذخیره می‌شود هم بلافاصله روی موتور اعمال می‌گردد */
    fun setDspSpec(spec: com.alvand.player.audio.DspSpec) = update { it.copy(dspSpec = spec) }

    /** ولوم خروجی — بین اجرا‌ها حفظ می‌شود */
    fun setVolume(volume: Float) = update { it.copy(volume = volume.coerceIn(0f, 1f)) }

    /** دستگاه خروجی صدا (نام میکسر)؛ null یعنی دستگاه پیش‌فرض سیستم */
    fun setOutputDevice(name: String?) = update { it.copy(outputDevice = name?.takeIf { n -> n.isNotBlank() }) }

    /** حالت کارایی (کاهشکیفیت برای سیستم‌های ضعیف) */
    fun setPerformanceMode(enabled: Boolean) = update { it.copy(performanceMode = enabled) }

    /** اسنپ‌شات صف playback — ذخیره و بازesti بین اجراها */
    fun setQueueSnapshot(snapshot: String) = update { it.copy(queueSnapshot = snapshot) }

    fun toggleLike(id: Long) = update {
        it.copy(likedIds = if (id in it.likedIds) it.likedIds - id else it.likedIds + id)
    }

    /** فقط برای تست — نوشتن فوری و بدون نخستین پس‌زمینه */
    suspend fun flush() = writeLock.withLock { writeToDisk(_state.value) }
}

/** وضعیت کامل تنظیمات (معادل Flowهای جدا در SettingsRepo اندروید) */
data class AppSettings(
    val themeMode: Int = DEFAULT_THEME_MODE,
    val accentTheme: Int = AccentThemeMode.MONO,
    val backgroundPath: String? = null,
    val onboardingSeen: Boolean = false,
    val onlineLyricsEnabled: Boolean = false,
    val likedIds: Set<Long> = emptySet(),
    val librarySort: Int = LibrarySort.DEFAULT,
    /** زبان اپ — مستقل از زبان سیستم‌عامل (همان رفتار per-app locale اندروید) */
    val language: String = "en",
    /** تنظیمات اکولایزر و DSP — بین اجرا‌ها می‌ماند */
    val dspSpec: com.alvand.player.audio.DspSpec = com.alvand.player.audio.DspSpec(),
    /** ولوم خروجی — بین اجراها می‌ماند */
    val volume: Float = 1f,
    /** دستگاه خروجی صدا (نام میکسر ویندوز)؛ null یعنی پیش‌فرض سیستم */
    val outputDevice: String? = null,
    /** اسنپ‌شات صف playback برای بازesti بین اجراها (خالی یعنی صف ذخیره‌نشده) */
    val queueSnapshot: String = "",
    /** حالت کارایی (کاهشکیفیت برای سیستم‌های ضعیف) */
    val performanceMode: Boolean = false,
) {
    val isPersian: Boolean get() = com.alvand.player.data.AppLocale.normalize(language) == "fa"

    companion object {
        const val DEFAULT_THEME_MODE = ThemeMode.SYSTEM
    }
}