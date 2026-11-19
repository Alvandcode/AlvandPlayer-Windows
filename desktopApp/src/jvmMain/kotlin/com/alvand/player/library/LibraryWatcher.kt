package com.alvand.player.library

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.StandardWatchEventKinds.ENTRY_CREATE
import java.nio.file.StandardWatchEventKinds.ENTRY_DELETE
import java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY
import java.nio.file.StandardWatchEventKinds.OVERFLOW
import java.nio.file.WatchEvent
import java.nio.file.WatchKey
import java.nio.file.WatchService
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * ناظر فایل‌سیستم: فایل جدید/حذف‌شده/تغییرکرده را در ریشه‌های کتابخانه می‌بیند و
 * بعد از یک آرامش کوتاه (debounce) به مصرف‌کننده گزارش می‌دهد، بدون نیاز به
 * «اسکن دوبارهٔ» دستی.
 *
 * طراحی:
 *  - فقط فایل‌های صوتی (همان فیلتر [FolderScanner.isSupportedAudioFile]) گزارش می‌شوند.
 *  - پوشهٔ تازه در رویداد CREATE بازگشتی ثبت می‌شود تا بازپیمایی لازم نباشد.
 *  - پوشه‌های سیستمی ($RECYCLE.BIN و ...) مثل اسکنر رد می‌شوند.
 *  - اگر بافر رویداد سرریز شود (OVERFLOW)، مجموعه‌های خالی گزارش می‌شود تا
 *    مصرف‌کننده یک اسکن کامل انجام دهد — این تنها مسیر امن برای همگام‌شدن دوباره.
 *  - همهٔ خطاها بلعیده می‌شوند؛ ناظر هرگز نباید باعث کرش اپ شود.
 */
class LibraryWatcher(
    private val scope: CoroutineScope,
    private val debounceMs: Long = 800L,
    private val onChanged: (changed: Set<Path>, deleted: Set<Path>) -> Unit
) {

    private val watcher: WatchService? by lazy {
        runCatching { FileSystems.getDefault().newWatchService() }.getOrNull()
    }

    private val keys = ConcurrentHashMap<WatchKey, Path>()
    private val changed = ConcurrentHashMap.newKeySet<Path>()
    private val deleted = ConcurrentHashMap.newKeySet<Path>()
    private val stopped = AtomicBoolean(true)
    private var thread: Thread? = null

    /**
     * شروع نظارت روی ریشه‌ها (بازگشتی). فراخوانی دوباره، نظارت قبلی را می‌بندد و
     * روی ریشه‌های تازه ثبت می‌کند (برای addRoot/removeRoot).
     */
    fun start(roots: Collection<Path>) {
        val ws = watcher ?: return
        stop()
        stopped.set(false)
        for (root in roots) {
            runCatching { registerRecursive(ws, root) }
        }
        thread = Thread({ loop(ws) }, "alvand-library-watcher").apply {
            isDaemon = true
            start()
        }
    }

    fun stop() {
        stopped.set(true)
        thread?.interrupt()
        thread = null
        runCatching {
            // فقط کلیدها لغو می‌شوند؛ خود WatchService طول عمر کل آبجکت را دارد،
            // چون بعد از stop() دوباره start() ممکن است (بستنش یعنی
            // ثبت‌های دوباره هیچ‌وقت کار نمی‌کنند).
            keys.keys.forEach { it.cancel() }
            keys.clear()
        }
        changed.clear()
        deleted.clear()
    }

    /** آیا پوشه جزو پوشه‌های سیستمی رد‌شده است؟ همان قواعد اسکنر */
    private fun isExcluded(dir: Path): Boolean {
        val n = dir.fileName?.toString()?.lowercase() ?: return false
        if (n.startsWith(".")) return true
        if (n == "appdata" || n == "\$recycle.bin" || n == "system volume information") return true
        return false
    }

    private fun registerRecursive(ws: WatchService, root: Path) {
        if (!Files.isDirectory(root)) return
        if (isExcluded(root)) return
        Files.walkFileTree(root, object : SimpleFileVisitor<Path>() {
            override fun preVisitDirectory(dir: Path, attrs: java.nio.file.attribute.BasicFileAttributes): java.nio.file.FileVisitResult {
                if (isExcluded(dir)) return java.nio.file.FileVisitResult.SKIP_SUBTREE
                runCatching {
                    val key = dir.register(ws, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY)
                    keys[key] = dir
                }
                return java.nio.file.FileVisitResult.CONTINUE
            }
        })
    }

    private fun loop(ws: WatchService) {
        while (!stopped.get()) {
            val key = runCatching { ws.take() }.getOrNull() ?: break
            if (!drain(key)) continue
            if (changed.isNotEmpty() || deleted.isNotEmpty()) {
                waitForQuiet(ws)
                deliver()
            }
        }
    }

    /** رویدادهای یک key را به مجموعه‌های pending اضافه می‌کند و key را reset می‌کند */
    private fun drain(key: WatchKey): Boolean {
        val dir = keys[key]
        var overflow = false
        for (raw in key.pollEvents()) {
            val ev = raw as WatchEvent<*>
            if (ev.kind() == OVERFLOW) {
                overflow = true
                continue
            }
            val relative = runCatching { ev.context() as Path }.getOrNull() ?: continue
            val full = dir?.resolve(relative) ?: continue
            collect(ev, full)
        }
        runCatching { key.reset() }
        if (overflow) {
            // بافر سرریز شده: هیچ راه مطمئنی برای دانستن چیزی که از دست رفت نیست؛
            // مجموعه‌های خالی یعنی «ناخوانا» → مصرف‌کننده اسکن کامل می‌کند
            changed.clear()
            deleted.clear()
            deliver()
        }
        return !overflow
    }

    private fun collect(ev: WatchEvent<*>, full: Path) {
        when (ev.kind()) {
            ENTRY_CREATE, ENTRY_MODIFY -> {
                if (Files.isDirectory(full)) {
                    runCatching { keys[full.register(watcher ?: return, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY)] = full }
                } else if (FolderScanner.isSupportedAudioFile(full)) {
                    if (Files.exists(full)) changed.add(full) else deleted.add(full)
                }
            }
            ENTRY_DELETE -> {
                if (FolderScanner.isSupportedAudioFile(full)) deleted.add(full)
            }
        }
    }

    /**
     * تا آرامش رویدادها صبر می‌کند: تا وقتی رویداد تازه می‌آید، تحویل به تعویق
     * می‌افتد — وگرنه فایل ناتمامِ در حال ذخیره تگ خوانده می‌شود.
     */
    private fun waitForQuiet(ws: WatchService) {
        val start = System.currentTimeMillis()
        while (!stopped.get()) {
            val remaining = debounceMs - (System.currentTimeMillis() - start)
            if (remaining <= 0) return
            val slice = if (remaining < 200) remaining else 200
            val key = runCatching { ws.poll(slice, TimeUnit.MILLISECONDS) }.getOrNull() ?: continue
            drain(key)
        }
    }

    private fun deliver() {
        if (stopped.get()) {
            changed.clear()
            deleted.clear()
            return
        }
        val c = changed.toSet()
        val d = deleted.toSet()
        changed.clear()
        deleted.clear()
        if (c.isEmpty() && d.isEmpty()) return
        scope.launch(Dispatchers.IO) { onChanged(c, d) }
    }
}
