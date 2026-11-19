package com.alvand.player.library

import com.alvand.player.model.Song
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile
import kotlin.io.path.name

/**
 * اسکنر پوشهٔ موسیقی.
 *
 * اندروید از MediaStore می‌خواند (`IS_MUSIC != 0 AND DURATION > 30000`)، ولی ویندوز
 * چنین فهرست مرکزی‌ای ندارد. پس به‌جای آن پوشه را بازپیما می‌کنیم.
 *
 * قواعدی که از نسخهٔ اندروید آمده و اینجا رعایت می‌شود:
 *  - سقف ۸۰۰۰ آهنگ (همان `LIMIT 8000` در کوئری اندروید)
 *  - ترتیب پیش‌فرض: جدیدترین فایل‌ها اول (معادل `DATE_ADDED DESC`)
 *  - اسکن قابل لغو است
 */
class FolderScanner(
    private val maxSongs: Int = MAX_SONGS,
    private val excludedDirs: Set<Path> = emptySet()
) {

    data class Result(
        val files: List<Path>,
        val truncated: Boolean,
        val scannedDirs: Int
    )

    /**
     * بازپیمایی بازگشتی و برگرداندن مسیر فایل‌های صوتی.
     * @param isCancelled برای لغو همکاری — اگر true شود اسکن همان‌جا متوقف می‌شود.
     */
    fun scan(roots: List<Path>, isCancelled: () -> Boolean = { false }): Result {
        val found = ArrayList<Path>()
        var truncated = false
        var scannedDirs = 0
        val visited = HashSet<Path>()

        for (root in roots) {
            if (isCancelled()) break
            if (!Files.isDirectory(root)) continue
            if (!visited.add(root.toAbsolutePath().normalize())) continue
            if (isExcluded(root)) continue

            Files.walkFileTree(root, object : SimpleFileVisitor<Path>() {
                override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
                    if (isCancelled()) return FileVisitResult.TERMINATE
                    if (isExcluded(dir)) return FileVisitResult.SKIP_SUBTREE
                    scannedDirs++
                    return FileVisitResult.CONTINUE
                }

                override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                    if (isCancelled()) return FileVisitResult.TERMINATE
                    if (attrs.isRegularFile && isSupportedAudioFile(file)) {
                        found.add(file)
                        if (found.size >= maxSongs) {
                            truncated = true
                            return FileVisitResult.TERMINATE
                        }
                    }
                    return FileVisitResult.CONTINUE
                }

                override fun visitFileFailed(file: Path, exc: java.io.IOException): FileVisitResult {
                    // فایل قفل‌شده یا بدون دسترسی نباید کل اسکن را خراب کند
                    return FileVisitResult.CONTINUE
                }
            })
        }

        // جدیدترین اول، مثل DATE_ADDED DESC
        val sorted = found
            .mapNotNull { p -> runCatching { p to Files.getLastModifiedTime(p).toMillis() }.getOrNull() }
            .sortedByDescending { it.second }
            .map { it.first }

        return Result(sorted, truncated, scannedDirs)
    }

    private fun isExcluded(dir: Path): Boolean {
        val n = dir.fileName?.toString()?.lowercase() ?: return false
        if (n.startsWith(".")) return true
        if (n == "appdata" || n == "\$recycle.bin" || n == "system volume information") return true
        val normalized = dir.toAbsolutePath().normalize()
        return excludedDirs.any { runCatching { normalized.startsWith(it) }.getOrDefault(false) }
    }

    companion object {
        /** همان LIMIT 8000 نسخهٔ اندروید */
        const val MAX_SONGS = 8000

        fun isSupportedAudioFile(file: Path): Boolean {
            val ext = runCatching { file.extension.lowercase() }.getOrNull() ?: return false
            // فایل‌های مخفی/سیستمی مثل Thumbs.db یا desktop.ini
            if (file.name.startsWith(".")) return false
            if (file.name.equals("desktop.ini", ignoreCase = true)) return false
            return ext in Song.SUPPORTED_EXTENSIONS
        }

        fun isSupportedAudioFile(file: java.io.File): Boolean =
            isSupportedAudioFile(file.toPath())
    }
}
