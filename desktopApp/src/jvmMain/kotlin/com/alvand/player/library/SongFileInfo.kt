package com.alvand.player.library

import com.alvand.player.model.Song
import java.io.File

/**
 * شناسنامهٔ فنی یک آهنگ — هر چیزی که از خودِ فایل می‌فهمیم، بدون حدس‌زدن.
 *
 * قاعدهٔ صداقت: هر چیزی که نتوانستیم بخوانیم `null` می‌ماند و UI خط‌تیره نشان
 * می‌دهد — هرگز عدد جعلی (مثلاً ۴۴۱۰۰ پیش‌فرض) به‌جای واقعیت نمی‌گذاریم.
 * هیچ‌وقت Exception پرتاب نمی‌کند؛ فایل پاک‌شده یا قفل یعنی «نامعلوم».
 */
data class SongFileInfo(
    val song: Song,
    val fileName: String,
    val extension: String,
    val exists: Boolean,
    val sizeBytes: Long?,
    val sampleRateHz: Int?,
    val channels: Int?,
    val bitsPerSample: Int?,
    /** تخمین از روی حجم/مدت — حدس مهندسی، نه تگ واقعی */
    val bitrateKbpsEstimate: Int?,
    val durationMs: Long
) {
    val hasAudioFormat: Boolean get() = sampleRateHz != null && channels != null
}

object SongFileInfoReader {

    fun read(song: Song): SongFileInfo {
        val file = runCatching { File(song.uri) }.getOrNull()
        val fileName = file?.name ?: song.uri.substringAfterLast('/').substringAfterLast('\\')
        val extension = fileName.substringAfterLast('.', "").lowercase()
        val exists = file?.isFile == true
        val size = if (exists) runCatching { file!!.length().takeIf { it > 0 } }.getOrNull() else null

        var sampleRate: Int? = null
        var channels: Int? = null
        var bits: Int? = null

        if (exists) {
            val f = file!!
            // اول jaudiotagger (همان کتابخانه‌ای که تگ را می‌خواند) — بدون وابستگی جدید
            runCatching {
                val audioFile = org.jaudiotagger.audio.AudioFileIO.read(f)
                val header = audioFile.audioHeader
                header.sampleRateAsNumber.takeIf { it > 0 }?.let { sampleRate = it }
                header.channels.takeIf { it.isNotBlank() }?.let { ch ->
                    // "2" یا "Stereo"/"Mono" — فقط عدد خالص را می‌پذیریم
                    ch.trim().toIntOrNull()?.let { channels = it }
                        ?: when {
                            ch.contains("stereo", ignoreCase = true) -> channels = 2
                            ch.contains("mono", ignoreCase = true) -> channels = 1
                            else -> Unit
                        }
                }
                header.bitsPerSample.takeIf { it > 0 }?.let { bits = it }
            }
            // fallback: AudioSystem (برای WAV/SPIهایی که jaudiotagger ندارد)
            if (sampleRate == null || channels == null) {
                runCatching {
                    val audioFileFormat = javax.sound.sampled.AudioSystem.getAudioFileFormat(f)
                    val format = audioFileFormat.format
                    if (sampleRate == null && format.sampleRate > 0) sampleRate = format.sampleRate.toInt()
                    if (channels == null && format.channels > 0) channels = format.channels
                    if (bits == null && format.sampleSizeInBits > 0) bits = format.sampleSizeInBits
                }
            }
        }

        val bitrate = if (size != null && song.durationMs > 0) {
            // بیت‌ریت متوسط کل فایل (شامل تگ/کاور) — برای همین «تخمین» است
            ((size * 8.0) / (song.durationMs / 1000.0) / 1000.0).toInt().takeIf { it > 0 }
        } else null

        return SongFileInfo(
            song = song,
            fileName = fileName,
            extension = extension,
            exists = exists,
            sizeBytes = size,
            sampleRateHz = sampleRate,
            channels = channels,
            bitsPerSample = bits,
            bitrateKbpsEstimate = bitrate,
            durationMs = song.durationMs
        )
    }

    /** ‎3.2 MB‎ / ‎812 KB‎ / ‎410 B‎ — همیشه یک رقم اعشار برای MB */
    fun formatSize(bytes: Long?): String {
        if (bytes == null || bytes < 0) return "—"
        return when {
            bytes >= 1024 * 1024 -> "${"%.1f".format(bytes / (1024.0 * 1024.0))} MB"
            bytes >= 1024 -> "${bytes / 1024} KB"
            else -> "$bytes B"
        }
    }

    fun formatRate(hz: Int?): String = hz?.let { "$it Hz" } ?: "—"

    fun formatBitrate(kbps: Int?): String = kbps?.let { "~$it kbps" } ?: "—"
}
