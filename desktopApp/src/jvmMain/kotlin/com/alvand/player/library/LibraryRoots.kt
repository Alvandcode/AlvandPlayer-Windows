package com.alvand.player.library

import com.alvand.player.system.AppPaths
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import kotlin.io.path.createDirectories
import kotlin.io.path.exists

/**
 * پوشه‌های موسیقی که کاربر انتخاب کرده — بین اجراها می‌ماند.
 *
 * هر مسیر در یک خط نوشته می‌شود. روی ویندوز امن است چون ویندوز اجازهٔ کاراکتر خط‌جدید
 * در نام فایل نمی‌دهد.
 *
 * مسیر پیش‌فرض: همهٔ درایوهای قابل‌دسترس به‌جز درایو سیستم/C.
 */
class LibraryRoots(
    private val file: Path = AppPaths.appData.resolve("library_roots.txt"),
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {

    private val writeLock = Mutex()
    private val _roots = MutableStateFlow(readFromDisk())

    val roots: StateFlow<List<Path>> = _roots.asStateFlow()

    val current: List<Path> get() = _roots.value

    fun add(path: Path) {
        addRoots(listOf(path))
    }

    /** چند ریشه را در یک به‌روزرسانی اضافه می‌کند تا فقط یک اسکن تازه راه بیفتد. */
    fun addRoots(paths: Collection<Path>) {
        val normalized = paths
            .map { it.toAbsolutePath().normalize() }
            .filter { candidate -> _roots.value.none { it == candidate } }
            .distinct()
        if (normalized.isEmpty()) return
        // نکتهٔ مهم: `java.nio.file.Path` خودش `Iterable<Path>` است (تکه‌های مسیر را
        // برمی‌گرداند). پس مجموعهٔ مسیرهای نرمال‌شده را جداگانه می‌سازیم و بعد به
        // فهرست اضافه می‌کنیم تا تکه‌های مسیر به‌اشتباه ریشه نشوند.
        update(_roots.value + normalized)
    }

    fun remove(path: Path) {
        val normalized = path.toAbsolutePath().normalize()
        update(_roots.value.filterNot { it == normalized })
    }

    /**
     * افزودن ریشه‌های خودکار بدون حذف انتخاب‌های کاربر.
     *
     * این متد در هر اجرا صدا زده می‌شود تا اگر درایو تازه‌ای وصل شده باشد، جا
     * نماند؛ ولی ریشه‌ای که کاربر حذف کرده، در همین اجرا دوباره برمی‌گردد. این
     * رفتار آگاهانه است چون درخواست فعلی، اسکن همهٔ درایوهای غیرسیستمی است.
     */
    fun ensureDriveRoots(defaults: Collection<Path>) {
        val normalized = defaults
            .map { it.toAbsolutePath().normalize() }
            .filter { candidate -> _roots.value.none { it == candidate } }
            .distinct()
        if (normalized.isEmpty()) return
        update(_roots.value + normalized)
    }

    /**
     * افزودن یک‌جای ریشه‌های پیش‌فرض.
     *
     * چرا یک‌جا؟ اگر درایوها تک‌تک اضافه شوند، هر تغییر یک اسکن تازه راه می‌اندازد
     * و اسکن قبلی لغو می‌شود؛ با یک به‌روزرسانی، فقط یک اسکن کامل انجام می‌شود.
     */
    fun seedDefaults(defaults: List<Path>) {
        if (_roots.value.isNotEmpty() || defaults.isEmpty()) return
        update(defaults.map { it.toAbsolutePath().normalize() }.distinct())
    }

    private fun update(next: List<Path>) {
        _roots.value = next
        scope.launch {
            writeLock.withLock {
                runCatching {
                    file.parent.createDirectories()
                    val tmp = file.resolveSibling(file.fileName.toString() + ".tmp")
                    Files.writeString(
                        tmp, next.joinToString("\n"),
                        StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING,
                        StandardOpenOption.WRITE
                    )
                    try {
                        Files.move(
                            tmp, file,
                            StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING
                        )
                    } catch (_: AtomicMoveNotSupportedException) {
                        Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING)
                    }
                }.onFailure { System.err.println("[library] roots write failed: ${it.message}") }
            }
        }
    }

    private fun readFromDisk(): List<Path> {
        if (!file.exists()) return emptyList()
        return runCatching {
            Files.readString(file)
                .lineSequence()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .map { runCatching { Path.of(it).toAbsolutePath().normalize() }.getOrNull() }
                .filterNotNull()
                .toList()
        }.getOrDefault(emptyList())
    }

    companion object {
        /** پوشهٔ Music ویندوز: مسیر رجیستری، با fallback به متغیر محیطی */
        fun windowsMusicFolder(): Path? {
            val fromEnv = System.getenv("USERPROFILE")
                ?.let { Path.of(it, "Music") }
            if (fromEnv != null && Files.isDirectory(fromEnv)) return fromEnv

            val knownFolder = runCatching {
                val key = "HKEY_CURRENT_USER\\Software\\Microsoft\\Windows\\CurrentVersion\\Explorer\\User Shell Folders"
                val process = ProcessBuilder("reg", "query", key, "/v", "Music")
                    .redirectErrorStream(true)
                    .start()
                val out = process.inputStream.bufferedReader().readText()
                process.waitFor(3, java.util.concurrent.TimeUnit.SECONDS)
                out
            }.getOrNull() ?: return null

            // خروجی شبیه: "    Music    REG_EXPAND_SZ    %USERPROFILE%\Music"
            val raw = knownFolder.trim().split(Regex("\\s+")).lastOrNull() ?: return null
            val expanded = raw.replace("%USERPROFILE%", System.getenv("USERPROFILE") ?: "")
            return Path.of(expanded).takeIf { Files.isDirectory(it) }
        }
    }
}
