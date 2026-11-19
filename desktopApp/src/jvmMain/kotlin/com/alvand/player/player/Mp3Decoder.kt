package com.alvand.player.player

import javazoom.jl.decoder.Bitstream
import javazoom.jl.decoder.Decoder
import javazoom.jl.decoder.DecoderException
import javazoom.jl.decoder.Header
import javazoom.jl.decoder.SampleBuffer
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.RandomAccessFile
import kotlin.math.roundToLong

/**
 * رمزگشای MP3 با JLayer (خالص جاوا، LGPL).
 *
 * **چرا JLayer و نه ffmpeg:** بستهٔ jave/ffmpeg حدود ۲۵ مگابایت باینری بومی به
 * برنامه اضافه می‌کند، زیرپروسه می‌سازد و لایسنس برنامه را از MIT خارج می‌کند.
 * JLayer حدود ۲۰۰ کیلوبایت است، هیچ زیرپروسه‌ای نمی‌سازد و PCM ۱۶ بیتی خطی
 * تحویل می‌دهد. بهایتش این است که فقط MP3 را می‌خواند.
 * جزئیات: `docs/PORT-DIFFERENCES.md` §۱۶
 *
 * **دربارهٔ seek:** JLayer پرش مستقیم ندارد، پس هدرها را جلو می‌رویم تا به مقصد
 * برسیم. نکتهٔ حیاتی: بعد از جابه‌جاییِ موقعیت باید یک `Decoder` تازه ساخت، چون
 * وضعیت داخلی دیکودر قبلی متعلق به موقعیت قبلیِ جریان است و اولین رمزگشایی بعد
 * از seek با همان دیکودرِ قبلی روی فایل واقعی می‌ترکد. دیکودر تازه + استریم
 * تازه دقیقاً همان ترکیبی است که اول فایل جواب می‌دهد.
 */
class Mp3Decoder(private val file: File) : AudioDecoder {

    private var decoder = Decoder()
    private val samples: SampleBuffer
    private lateinit var bitstream: Bitstream

    /** هدر فریمی که خوانده شده ولی هنوز رمزگشایی نشده */
    private var pendingHeader: Header? = null

    private val detected: AudioFormat
    private val durationMs: Long

    /** تعداد نمونه در هر فریم — از روی ms-per-frame هدر محاسبه می‌شود تا به لایهٔ
     *  صوتی خاص (I/II/III) وابسته نباشیم */
    private val samplesPerFrame: Long

    /** نمونه‌های رمزگشایی‌شدهٔ هنوز تحویل‌داده‌نشده */
    private var backlog = ShortArray(0)
    private var backlogPos = 0

    /** خطاهای پیاپی رمزگشایی؛ فایل خراب نباید کل موتور را متوقف کند */
    private var decodeFailures = 0

    init {
        val header = open()
            ?: throw IllegalArgumentException("not a decodable MP3: ${file.path}")
        val frequency = header.frequency()
        val channels = if (header.mode() == Header.SINGLE_CHANNEL) 1 else 2

        detected = AudioFormat(sampleRate = frequency, channels = channels, bitsPerSample = 16)
        samplesPerFrame = (header.ms_per_frame() * frequency / 1000.0).roundToLong()
            .coerceAtLeast(1L)

        samples = SampleBuffer(frequency, channels)
        decoder.setOutputBuffer(samples)

        pendingHeader = header
        durationMs = header.total_ms(audioStreamBytes()).toLong().coerceAtLeast(0L)
    }

    override val format: AudioFormat get() = detected

    override val totalDurationMs: Long get() = durationMs

    override fun readBlock(maxFrames: Int): PcmBuffer? {
        val want = maxFrames * detected.channels
        if (want <= 0) return null

        // تا رسیدن به اندازهٔ خواسته‌شده فریم بیشتری رمزگشایی می‌کنیم، با سقف تا
        // فایل خراب باعث حلقهٔ بی‌نهایت نشود
        var guard = 0
        while (available() < want && guard++ < MAX_FRAMES_PER_BLOCK) {
            if (!decodeOneFrame()) break
        }

        val take = minOf(available(), want)
        if (take <= 0) return null

        val out = ByteArray(take * 2)
        var o = 0
        var i = backlogPos
        val end = backlogPos + take
        while (i < end) {
            val v = backlog[i].toInt()
            out[o++] = (v and 0xFF).toByte()                 // little-endian
            out[o++] = ((v shr 8) and 0xFF).toByte()
            i++
        }
        backlogPos = i
        if (backlogPos >= backlog.size) {
            backlog = ShortArray(0)
            backlogPos = 0
        }
        return PcmBuffer(detected, out)
    }

    override fun seekTo(frame: Long) {
        val target = frame.coerceAtLeast(0L)

        runCatching { bitstream.close() }
        // دیکودر تازه: وضعیت داخلی دیکودر قبلی متعلق به موقعیت قبلیِ جریان است و
        // با موقعیت تازه ناسازگار می‌شود — روی MP3 واقعی، اولین رمزگشایی بعد از
        // seek با همان دیکودرِ قبلی داخل huffman_decode می‌ترکد
        // (ArrayIndexOutOfBoundsException) و موتور فکر می‌کند فایل تمام شده و
        // می‌پرد آهنگ بعدی. دیکودر تازه + استریم تازه دقیقاً همان ترکیبی است که
        // اول فایل جواب می‌دهد (آزمایش‌شده روی فایل واقعی: ۱۶/۱۶ سالم).
        decoder = Decoder().also { it.setOutputBuffer(samples) }
        pendingHeader = open()
        resetBacklog()
        decodeFailures = 0
        if (target <= 0L) return

        var toSkip = target
        var guard = 0L
        // فقط هدر خوانده می‌شود؛ صدا رمزگشایی نمی‌شود
        while (toSkip > 0L && guard++ < MAX_SEEK_FRAMES) {
            val header = nextHeader() ?: break
            // این فریم مصرف شد و صدایی از آن نمی‌خواهیم
            releaseFrame()
            toSkip -= samplesPerFrame
        }
    }

    override fun close() {
        runCatching { bitstream.close() }
        resetBacklog()
    }

    // ---------------------------------------------------------------- internals

    private fun available(): Int = backlog.size - backlogPos

    /** جریان تازه از ابتدای فایل، با اولین هدر خوانده‌شده */
    private fun open(): Header? {
        val stream = BufferedInputStream(FileInputStream(file), INPUT_BUFFER)
        val fresh = Bitstream(stream)
        bitstream = fresh
        return runCatching { fresh.readFrame() }.getOrNull()
    }

    /** هدر بعدی، از صف انتظار یا مستقیم از جریان */
    private fun nextHeader(): Header? {
        val header = pendingHeader ?: runCatching { bitstream.readFrame() }.getOrNull()
        pendingHeader = null
        return header
    }

    /**
     * پایان دادن به فریمِ جاری تا `readFrame()` بعدی واقعاً هدر تازه بخواند.
     *
     * این را JLayer خودش فراموش می‌کند و اگر صدا زنیم، `readFrame()` همان هدر
     * قبلی را دوباره برمی‌گرداند: فریم بی‌پایان تکرار می‌شود و در نهایت رمزگشا از
     * انتهای بافر سرریز می‌کند (`ArrayIndexOutOfBoundsException`).
     * دقیقاً به همین دلیل `Player` خود JLayer هم همین کار را می‌کند.
     *
     * **بعد از `decodeFrame` صدا زده می‌شود، نه قبل از آن** — چون `closeFrame`
     * اشاره‌گر بیت را صفر می‌کند و رمزگشایی همان فریم به آن نیاز دارد.
     */
    private fun releaseFrame() {
        runCatching { bitstream.closeFrame() }
    }

    private fun resetBacklog() {
        backlog = ShortArray(0)
        backlogPos = 0
        samples.clear_buffer()
    }
    /** رمزگشای یک فریم و افزودنش به بافر. false یعنی پایان فایل. */
    private fun decodeOneFrame(): Boolean {
        val header = nextHeader() ?: return false

        return try {
            samples.clear_buffer()
            decoder.decodeFrame(header, bitstream)
            releaseFrame()          // باید بعد از رمزگشایی باشد
            val n = samples.bufferLength
            if (n > 0) {
                append(samples.buffer, n)
                decodeFailures = 0
            }
            true
        } catch (e: DecoderException) {
            releaseFrame()
            handleDecodeFailure(e)
        } catch (e: RuntimeException) {
            // فریم خراب گاهی با استثنای خام (مثلاً ArrayIndexOutOfBoundsException
            // داخل huffman_decode) می‌ترکد، نه DecoderException. رفتار درست برای
            // هر دو یکی است: رد شدن از فریم، نه کشتن کل آهنگ.
            releaseFrame()
            handleDecodeFailure(e)
        }
    }

    /**
     * یک فریم خراب نباید کل آهنگ را از کار بیندازد؛ چند فریم بعد دوباره تلاش
     * می‌کنیم، ولی اگر پیوسته خراب باشد خطا را بالا می‌بریم تا موتور به آهنگ
     * بعدی برود.
     */
    private fun handleDecodeFailure(e: Exception): Boolean {
        // یک فریم خراب نباید کل آهنگ را از کار بیندازد؛ چند فریم بعد دوباره
        // تلاش می‌کنیم، ولی اگر پیوسته خراب باشد خطا را بالا می‌بریم
        if (++decodeFailures > MAX_DECODE_FAILURES) throw e
        return true
    }

    private fun append(src: ShortArray, n: Int) {
        val have = available()
        val merged = ShortArray(have + n)
        System.arraycopy(backlog, backlogPos, merged, 0, have)
        System.arraycopy(src, 0, merged, have, n)
        backlog = merged
        backlogPos = 0
    }

    /** اندازهٔ فایل منهای تگ ID3v2، چون خود Bitstream از روی آن رد می‌شود */
    private fun audioStreamBytes(): Int {
        val total = runCatching { file.length() }.getOrDefault(0L)
        val audio = (total - id3v2Size()).coerceAtLeast(0L)
        return audio.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    }

    private fun id3v2Size(): Long = runCatching {
        RandomAccessFile(file, "r").use { raf ->
            val h = ByteArray(10)
            if (raf.read(h) < 10) return@use 0L
            val isId3 = h[0] == 'I'.code.toByte() &&
                h[1] == 'D'.code.toByte() &&
                h[2] == '3'.code.toByte()
            if (!isId3) return@use 0L
            // اندازهٔ synchsafe چهار بایتی، هر بایت فقط هفت بیت معنی‌دار
            val body = ((h[6].toLong() and 0xFF) shl 21) or
                ((h[7].toLong() and 0xFF) shl 14) or
                ((h[8].toLong() and 0xFF) shl 7) or
                (h[9].toLong() and 0xFF)
            val footer = if ((h[5].toInt() and 0x10) != 0) 10L else 0L
            10L + body + footer
        }
    }.getOrDefault(0L)

    private companion object {
        const val INPUT_BUFFER = 1 shl 16
        const val MAX_FRAMES_PER_BLOCK = 64
        const val MAX_DECODE_FAILURES = 16
        const val MAX_SEEK_FRAMES = 4_000_000L
    }
}