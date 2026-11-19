package com.alvand.player.player

import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import javax.sound.sampled.AudioFileFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
// هم‌نام با AudioFormat خودمان است، پس صریحاً مستعار می‌شود
import javax.sound.sampled.AudioFormat as JAudioFormat

/**
 * رمزگشای هر فرمتی که یک `AudioFileReader` برایش ثبت شده باشد.
 *
 * الان FLAC (tوسط jFLAC) و Ogg Vorbis (tوسط پورت Tremor) از این مسیر می‌آیند.
 *
 * **چرا `AudioSystem` و نه نوشتن یک حلقهٔ رمزگشایی دستی:** هر دو کتابخانه خودشان
 * `javax.sound.sampled.spi.AudioFileReader` ثبت می‌کنند، یعنی JDK خودش فایل را
 * باز می‌کند و PCM جریانی می‌دهد. نوشتن حلقهٔ دستی یعنی دو بار پیاده‌سازی کاری که
 * این کتابخانه‌ها بهتر و آزموده‌شده انجام می‌دهند.
 *
 * **مسیر دوم:** `getAudioFileFormat` فایل را جداگانه باز می‌کند تا مدت‌زمان و
 * مشخصات را بدون رفتن به انتهای فایل بگیریم.
 */
class SpiAudioDecoder(
    private val file: File,
    extensions: Set<String>
) : AudioDecoder {

    private var source: InputStream? = null
    private var stream: AudioInputStream? = null

    private val header: AudioFileFormat
    private val totalFrames: Long
    private var framesRead = 0L

    /** یک بلاک خوانده‌شده که هنوز کامل مصرف نشده */
    private var leftover: ByteArray = ByteArray(0)
    private var leftoverPos = 0

    init {
        // اول فقط سردر را بخوان تا اگر فرمت پشتیبانی نشد، خطای تمیز بدهیم
        header = AudioSystem.getAudioFileFormat(file)
        require(extensions.isEmpty() || file.extension.lowercase() in extensions) {
            "unexpected extension: ${file.extension}"
        }
        totalFrames = header.frameLength.toLong()
            .takeIf { it > 0 } ?: estimateFrames(file, header.format)
    }

    override val format: AudioFormat = AudioFormat(
        sampleRate = header.format.sampleRate.toInt(),
        channels = header.format.channels.coerceIn(1, 2),
        bitsPerSample = header.format.sampleSizeInBits.toInt()
    )

    override val totalDurationMs: Long
        get() = (totalFrames * 1000L) / format.sampleRate

    override fun readBlock(maxFrames: Int): PcmBuffer? {
        val frameSize = format.frameSize
        val want = maxFrames * format.channels
        if (want <= 0 || frameSize <= 0) return null

        val needed = maxOf(0, (want * frameSize) - (leftover.size - leftoverPos))
        if (needed > 0) {
            val buffer = ByteArray(needed)
            var filled = 0
            val active = open()
            while (filled < needed) {
                val n = active.read(buffer, filled, needed - filled)
                if (n <= 0) break
                filled += n
            }
            if (filled <= 0) return null
            merge(buffer, filled)
        }

        val available = leftover.size - leftoverPos
        if (available <= 0) return null
        // فقط بایت‌های کامل فریم را می‌دهیم تا نیم‌فریم به DSP نرود
        val usable = (available / frameSize) * frameSize
        if (usable <= 0) return null

        val out = leftover.copyOfRange(leftoverPos, leftoverPos + usable)
        leftoverPos += usable
        if (leftoverPos >= leftover.size) {
            leftover = ByteArray(0)
            leftoverPos = 0
        }
        framesRead += usable / frameSize
        return PcmBuffer(format, out)
    }

    /**
     * پرش روی جریان.
     *
     * `AudioInputStream.skip` برای PCM بر اساس فریم حساب می‌کند و برای این
     * کتابخانه‌ها هم پشتیبانی می‌شود؛ اگر نکرد، خودش به خواندن جلو می‌رود که کند
     * است ولی **درست**.
     */
    override fun seekTo(frame: Long) {
        val target = frame.coerceAtLeast(0L)
        closeStream()
        val active = open()
        val bytesToSkip = target * format.frameSize
        val skipped = runCatching { active.skip(bytesToSkip) }.getOrDefault(0L)
        if (skipped < bytesToSkip) {
            // skip ناقص بود؛ باقی‌اش را می‌خوانیم
            var need = bytesToSkip - skipped
            val scratch = ByteArray(1 shl 16)
            while (need > 0) {
                val n = active.read(scratch, 0, minOf(need, scratch.size.toLong()).toInt())
                if (n <= 0) break
                need -= n
            }
        }
        framesRead = target
        resetLeftover()
    }

    override fun close() {
        closeStream()
        resetLeftover()
    }

    // ---------------------------------------------------------------- internals

    private fun open(): AudioInputStream {
        stream?.let { return it }
        val raw = AudioSystem.getAudioInputStream(
            BufferedInputStream(FileInputStream(file), 1 shl 16)
        )
        source = raw

        // SPIها جریان خام را با رمزگذاریِ خودِ فرمت برمی‌گردانند (مثلاً «FLAC»)
        // و یک `FormatConversionProvider` جداگانه ثبت می‌کنند که آن را به PCM
        // تبدیل می‌کند. بدون این تبدیل، به DSP عدد مفیدی نمی‌رسد.
        val pcm = if (raw.format.encoding == JAudioFormat.Encoding.PCM_SIGNED) {
            raw
        } else {
            AudioSystem.getAudioInputStream(pcmFormat(), raw)
        }
        stream = pcm
        return pcm
    }

    /** قالب PCM ۱۶ بیتی little-endian که تبدیل‌کننده‌ها پشتیبانی می‌کنند */
    private fun pcmFormat(): JAudioFormat {
        val channels = format.channels
        val rate = format.sampleRate
        val bits = 16
        return JAudioFormat(
            JAudioFormat.Encoding.PCM_SIGNED,
            rate.toFloat(),
            bits,
            channels,
            channels * bits / 8,
            rate.toFloat(),
            false
        )
    }

    private fun closeStream() {
        runCatching { stream?.close() }
        runCatching { source?.close() }
        stream = null
        source = null
    }

    private fun resetLeftover() {
        leftover = ByteArray(0)
        leftoverPos = 0
    }

    private fun merge(bytes: ByteArray, length: Int) {
        if (length <= 0) return
        val rest = leftover.size - leftoverPos
        val out = ByteArray(rest + length)
        if (rest > 0) System.arraycopy(leftover, leftoverPos, out, 0, rest)
        System.arraycopy(bytes, 0, out, rest, length)
        leftover = out
        leftoverPos = 0
    }

    companion object {
        /** اگر فایل قابل خواندن نبود null برمی‌گرداند تا خطا به UI برود نه کرش */
        fun tryCreate(file: File, extensions: Set<String>): SpiAudioDecoder? =
            runCatching { SpiAudioDecoder(file, extensions) }.getOrNull()

        /**
         * وقتی `frameLength` اعلام نشده، با خواندن سردرِ فایل تخمین می‌زنیم.
         *
         * عملاً برای FLAC و Ogg لازم نمی‌شود چون هر دو طول را در سردر می‌دهند،
         * ولی اگر نسخه‌ای آن را نداد، بهتر است مدت‌زمان صفر نباشد.
         */
        private fun estimateFrames(file: File, format: JAudioFormat): Long = runCatching {
            AudioSystem.getAudioInputStream(file).use { s ->
                var frames = 0L
                val buffer = ByteArray(1 shl 16)
                while (true) {
                    val n = s.read(buffer)
                    if (n <= 0) break
                    frames += n / format.frameSize
                }
                frames
            }
        }.getOrDefault(0L)
    }
}
