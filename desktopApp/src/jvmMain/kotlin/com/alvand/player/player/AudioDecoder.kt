package com.alvand.player.player

import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteOrder
import java.util.concurrent.Executors
import kotlin.concurrent.thread
import kotlin.math.abs

/**
 * رمزگشای فایل‌های صوتی روی دیسک.
 *
 * **وضعیت فعلی و صادقانه:**
 *
 * | فرمت | وضعیت | پیاده‌سازی |
 * |---|---|---|
 * | WAV / AU | کامل | `RawAudioDecoder` (خود JDK) |
 * | MP3 | کامل | `Mp3Decoder` (JLayer، خالص جاوا) |
 * | FLAC | کامل | `SpiAudioDecoder` (jFLAC) |
 * | Ogg Vorbis | کامل | `SpiAudioDecoder` (پورت Tremor) |
 * | M4A (AAC/ALAC) | کامل | `SpiAudioDecoder` (خالص جاوا، Apache-2.0) |
 * | AIFF | فقط تشخیص نوع | `RawAudioDecoder` |
 * | WMA, Opus | **پشتیبانی نمی‌شود** | — |
 *
 * فرمت‌های فشردهٔ باقی‌مانده فعلاً خطا می‌دهند و موتور طبق سیاست «رد کردن فایل
 * خراب» از آن‌ها می‌گذرد. دلیل و گزینه‌های بررسی‌شده در
 * `docs/PORT-DIFFERENCES.md` §۱۶ آمده است.
 */
interface AudioDecoder : AutoCloseable {

    val format: AudioFormat

    /** مدت‌زمان کل فایل، اگر معلوم باشد */
    val totalDurationMs: Long

    /**
     * خواندن بعدی بلوک نمونه.
     * @return null یعنی پایان فایل
     */
    fun readBlock(maxFrames: Int = 4096): PcmBuffer?

    /** پرش به نمونهٔ مشخص. برای seek. */
    fun seekTo(frame: Long) = Unit

    fun isLive(): Boolean = false

    override fun close() = Unit
}

/** رمزگشای فایل‌های خام با JDK */
class RawAudioDecoder(private val file: File) : AudioDecoder {

    private val raf: RandomAccessFile = RandomAccessFile(file, "r")
    private val layout = readWav(raf)
    private val detected: AudioFormat = layout.format
    private var framesRead: Long = 0

    override val format: AudioFormat get() = detected

    override val totalDurationMs: Long
        get() = (layout.dataLength * 1000L) / detected.bytesPerSecond

    override fun readBlock(maxFrames: Int): PcmBuffer? {
        val want = maxFrames * detected.frameSize
        if (want <= 0) return null
        val consumed = framesRead * detected.frameSize
        val remaining = layout.dataLength - consumed
        if (remaining <= 0) return null
        val toRead = minOf(want.toLong(), remaining).toInt()

        raf.seek(layout.dataStart + consumed)
        val buf = ByteArray(toRead)
        val n = raf.read(buf, 0, toRead)
        if (n <= 0) return null

        framesRead += n / detected.frameSize
        return if (n < toRead) PcmBuffer(detected, buf.copyOf(n)) else PcmBuffer(detected, buf)
    }

    override fun seekTo(frame: Long) {
        framesRead = frame.coerceAtLeast(0)
    }

    override fun close() {
        runCatching { raf.close() }
    }

    private companion object {
        /** ساختار بلوک‌های WAV که برای ما لازم است */
        data class WavLayout(
            val format: AudioFormat,
            val dataStart: Long,
            val dataLength: Long
        )

        /**
         * پیمایش بلوک‌های RIFF/WAVE.
         *
         * عمداً آدرس‌دهی ثابت نکرده‌ایم: فایل‌های واقعی وسط بلوک `fmt `،
         * بلوک‌های اضافه مثل `LIST` یا `fact` دارند و فرض کردنِ جایگاه دقیق،
         * خواندنِ نرخ نمونهٔ اشتباه را می‌سازد.
         */
        fun readWav(raf: RandomAccessFile): WavLayout {
            raf.seek(0)
            val riff = ByteArray(4)
            if (raf.read(riff) < 4) return fallback(raf)
            val riffId = String(riff, Charsets.US_ASCII)
            readLE32(raf)              // اندازهٔ RIFF
            val wave = ByteArray(4)
            raf.readFully(wave)
            if (riffId != "RIFF" || String(wave, Charsets.US_ASCII) != "WAVE") {
                return fallback(raf)
            }

            var format = AudioFormat()
            var dataStart = -1L
            var dataLength = 0L

            while (raf.filePointer < raf.length() - 8) {
                val id = ByteArray(4)
                raf.readFully(id)
                val chunkId = String(id, Charsets.US_ASCII)
                val size = readLE32(raf)
                val chunkStart = raf.filePointer

                when (chunkId) {
                    "fmt " -> {
                        val buf = ByteArray(16)
                        raf.readFully(buf)
                        val bb = java.nio.ByteBuffer.wrap(buf).order(ByteOrder.LITTLE_ENDIAN)
                        format = AudioFormat(
                            sampleRate = bb.getInt(4),       // offset 4 داخل بلوک
                            channels = bb.getShort(2).toInt(),   // offset 2 داخل بلوک
                            bitsPerSample = bb.getShort(14).toInt() // offset 14 داخل بلوک
                        )
                    }
                    "data" -> {
                        // بعضی فایل‌ها اندازهٔ اعلام‌نشده دارند (۰ یا ‎-1‎)
                        val remaining = raf.length() - chunkStart
                        dataStart = chunkStart
                        dataLength = if (size in 1..remaining) size else remaining
                    }
                }

                if (dataStart >= 0 && format.sampleRate > 0) break
                // بلوک‌های WAV همیشه به بایت زوج می‌رسند
                raf.seek(chunkStart + size + (size and 1L))
                if (size <= 0) break
            }

            if (dataStart < 0) {
                dataStart = 44
                dataLength = (raf.length() - 44).coerceAtLeast(0)
            }
            return WavLayout(format, dataStart, dataLength)
        }

        fun fallback(raf: RandomAccessFile): WavLayout {
            raf.seek(0)
            val tag = ByteArray(4)
            if (raf.read(tag) < 4) tag.fill(0)
            val format = when (String(tag, Charsets.US_ASCII)) {
                "FORM" -> AudioFormat(44100, 2, 16)   // AIFF
                ".snd" -> AudioFormat(8000, 1, 8)     // AU
                else -> AudioFormat(44100, 2, 16)
            }
            return WavLayout(format, 0L, 0L)
        }

        fun readLE32(raf: RandomAccessFile): Long {
            val b = ByteArray(4)
            raf.readFully(b)
            return (b[0].toLong() and 0xFF) or
                ((b[1].toLong() and 0xFF) shl 8) or
                ((b[2].toLong() and 0xFF) shl 16) or
                ((b[3].toLong() and 0xFF) shl 24)
        }
    }
}

/**
 * کارخانهٔ ساخت رمزگشا.
 *
 * جدا بودنش یعنی می‌توان در تست با یک رمزگشای ساختگی کل موتور را بدون صدای
 * واقعی اجرا کرد.
 */
object DecoderFactory {

    /** فرمت‌هایی که JDK خودش می‌خواند */
    private val RAW_EXTENSIONS = setOf("wav", "aif", "aiff", "au")

    /** MP3 با JLayer؛ `mpga`/`mp2` هم گاهی به‌جای `mp3` دیده می‌شود */
    private val MP3_EXTENSIONS = setOf("mp3", "mpga", "mp2")

    /** فرمت‌هایی که یک `AudioFileReader` ثبت‌شده برایشان هست */
    private val SPI_EXTENSIONS = setOf("flac", "ogg", "oga")

    /**
     * M4A: کانتینر MP4 با کدک AAC. این مسیر دیکودر اختصاصی خودمان را دارد
     * (نه SPI)، چون کتابخانه‌های SPI این فرمت بافرشان را با `encoding` اشتباه
     * برمی‌گردانند. توضیح کامل در `M4aDecoder.kt`.
     * `mp4`/`m4b` هم کانتینر یکسانی دارند ولی ممکن است ویدیو هم داشته باشند که
     * اینجا فقط صدایشان خوانده می‌شود.
     */
    private val MP4_EXTENSIONS = setOf("m4a", "m4b", "mp4")

    private val SUPPORTED = RAW_EXTENSIONS + MP3_EXTENSIONS + SPI_EXTENSIONS + MP4_EXTENSIONS

    /** پسوندهایی که موتور واقعاً می‌تواند رمزگشایی کند (نه فقط در کتابخانه فهرست شوند). */
    val playableExtensions: Set<String> get() = SUPPORTED

    fun canDecode(uri: String): Boolean = extensionOf(uri) in SUPPORTED

    /** پسوند کوچک‌شدهٔ فایل، بدون حساسیت به بزرگی حروف و بدون گرفتار شدن در نقطه‌های مسیر */
    private fun extensionOf(uri: String): String {
        val name = uri.substringAfterLast('/').substringAfterLast('\\')
        return name.substringAfterLast('.', "").lowercase()
    }

    fun create(uri: String, mimeHint: String? = null): AudioDecoder? {
        if (uri.startsWith("https://", ignoreCase = true)) return null   // در milestone 3b
        val file = File(uri)
        if (!file.isFile) return null
        val ext = file.extension.lowercase()
        return when {
            ext in RAW_EXTENSIONS -> runCatching { RawAudioDecoder(file) }.getOrNull()
            ext in MP3_EXTENSIONS -> runCatching { Mp3Decoder(file) }.getOrNull()
            ext in SPI_EXTENSIONS -> SpiAudioDecoder.tryCreate(file, SPI_EXTENSIONS)
            ext in MP4_EXTENSIONS -> runCatching { M4aDecoder.open(file) }.getOrNull()
            else -> null
        }
    }
}

/**
 * رمزگشای ساختگی برای تست.
 *
 * تولید یک سینوس، پس می‌شود بدون فایل واقعی کل مسیر پخش و DSP را تست کرد.
 */
class SineDecoder(
    override val format: AudioFormat = AudioFormat(44100, 2, 16),
    private val durationMs: Long = 1000L,
    private val frequency: Double = 440.0
) : AudioDecoder {

    private var frame = 0L
    private val totalFrames = durationMs * format.sampleRate / 1000L
    private var seekTarget = 0L

    override val totalDurationMs: Long get() = durationMs

    override fun readBlock(maxFrames: Int): PcmBuffer? {
        if (frame < seekTarget) frame = seekTarget
        if (frame >= totalFrames) return null
        val n = minOf(maxFrames.toLong(), totalFrames - frame).toInt()
        val out = ByteArray(n * format.frameSize)
        val buf = java.nio.ByteBuffer.wrap(out).order(ByteOrder.LITTLE_ENDIAN)
        repeat(n) {
            val t = (frame + it).toDouble() / format.sampleRate
            val v = (kotlin.math.sin(2 * Math.PI * frequency * t) * 20000).toInt().toShort()
            repeat(format.channels) { buf.putShort(v) }
        }
        frame += n
        return PcmBuffer(format, out)
    }

    override fun seekTo(frame: Long) {
        // مثل دیکودرهای واقعی، عقب و جلو می‌رود (نه فقط جلو)
        val target = frame.coerceIn(0, totalFrames)
        this.frame = target
        seekTarget = target
    }
}
