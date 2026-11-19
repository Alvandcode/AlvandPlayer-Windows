package com.alvand.player.library

import com.alvand.player.player.DecoderFactory
import java.io.File
import java.nio.file.Path

/**
 * نتیجهٔ دسته‌بندی فایل‌های رها‌شده روی پنجره.
 *
 * پوشه‌ها به ریشه‌های کتابخانه اضافه و اسکن می‌شوند؛ فایل‌های صوتیِ پشتیبانی‌شده
 * بلافاصله در صف پخش قرار می‌گیرند و فایل‌های پشتیبانی‌نشده فقط برای پیام خطای
 * روشن نگه داشته می‌شوند تا بی‌صدا گم نشوند.
 */
data class DroppedMedia(
    val directories: List<Path>,
    val playableFiles: List<Path>,
    val unsupportedFiles: List<Path>
)

object DroppedMediaClassifier {
    fun classify(files: Collection<File?>): DroppedMedia {
        val dropped = files.filterNotNull().distinct()
        val directories = dropped
            .filter { it.isDirectory }
            .map { it.toPath().toAbsolutePath().normalize() }
            .distinct()
        val regularFiles = dropped.filter { it.isFile }
        val playableFiles = regularFiles
            .filter { DecoderFactory.canDecode(it.absolutePath) }
            .map { it.toPath().toAbsolutePath().normalize() }
        val unsupportedFiles = regularFiles
            .filterNot { DecoderFactory.canDecode(it.absolutePath) }
            .map { it.toPath().toAbsolutePath().normalize() }
        return DroppedMedia(directories, playableFiles, unsupportedFiles)
    }
}
