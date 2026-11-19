package com.alvand.player.player

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.concurrent.thread
import kotlin.math.min
import kotlin.math.sqrt

/**
 * قالب‌های صوتی پشتیبانی‌شده.
 *
 * روی ویندوز همه‌چیز روی PCM (خام) انجام می‌شود: نرخ نمونه‌برداری، عمق بیت و
 * تعداد کانال.
 */
data class AudioFormat(
    val sampleRate: Int = 44100,
    val channels: Int = 2,
    val bitsPerSample: Int = 16
) {
    val bytesPerSample: Int get() = bitsPerSample / 8
    val frameSize: Int get() = channels * bytesPerSample
    val bytesPerSecond: Int get() = sampleRate * frameSize

    /** بافر تقریباً ۳۰۰ میلی‌ثانیه */
    fun bufferBytes(millis: Int = 300): Int = bytesPerSecond * millis / 1000

    companion object {
        val STEREO_44K = AudioFormat(44100, 2, 16)
        val STEREO_48K = AudioFormat(48000, 2, 16)
        val MONO_44K = AudioFormat(44100, 1, 16)
    }
}

/** بلوک PCM خام */
class PcmBuffer(val format: AudioFormat, val bytes: ByteArray) {
    val frames: Int get() = if (format.frameSize == 0) 0 else bytes.size / format.frameSize
    val durationMs: Long
        get() = if (format.bytesPerSecond == 0) 0L
        else bytes.size.toLong() * 1000L / format.bytesPerSecond

    /** نمونه‌ها به‌صورت اعشاری در بازهٔ ‎-1..1‎ — برای تحلیل و تست */
    fun toFloats(): FloatArray {
        val out = FloatArray(bytes.size / 2)
        for (i in out.indices) {
            val lo = bytes[i * 2].toInt() and 0xFF
            val hi = bytes[i * 2 + 1].toInt()
            out[i] = (((hi shl 8) or lo).toShort()) / 32768f
        }
        return out
    }
}

/** سطح صدای مؤثر (لگاریتمی گوش انسان) */
object PerceptualVolume {

    /** @return توان گوش، بازهٔ ‎0..1‎ */
    fun fromLinear(linear: Float): Float = linear.coerceIn(0f, 1f).let { it * it }

    fun toLinear(perceptual: Float): Float = sqrt(perceptual.coerceIn(0f, 1f))
}

/**
 * خروجی صدا — لایهٔ نازک بین موتور و دستگاه.
 *
 * **چرا بدون بافر حلقوی:** بلندگوی واقعی خودش بافر دارد و `write` روی آن تا آماده
 * شدن دستگاه **بلوکه** می‌شود — یعنی خودِ دستگاه سرعت را تنظیم می‌کند. اگر اینجا
 * هم بافر اضافه کنیم، موقعیت پخش از صدای واقعی عقب‌تر می‌افتد و `pause` نمی‌تواند
 * جلوی صدای در راه را بگیرد. پس نوشتن مستقیم انجام می‌شود و چون از نخِ پخش
 * (نه نخ UI) صدا زده می‌شود، هیچ مشکلی برای رابط کاربری ندارد.
 */
class AudioOutputStream(
    val format: AudioFormat,
    private val sink: SampleSink
) : AutoCloseable {

    val isOpen = AtomicBoolean(false)

    /** بایت‌هایی که واقعاً به دستگاه داده شده — مبنای محاسبهٔ موقعیت پخش */
    private val writtenBytes = AtomicLong(0)

    /**
     * مبنای موقعیت بعد از seek.
     *
     * شمارندهٔ بایت همیشه از صفر شروع می‌شود، ولی موقعیت واقعی آهنگ بعد از seek
     * صفر نیست. این مبنا همان اختلاف را نگه می‌دارد تا نوار پیشرفت به ابتدا برنگردد.
     */
    private val baseMs = AtomicLong(0)

    fun start() {
        if (!isOpen.compareAndSet(false, true)) return
        writtenBytes.set(0)
        baseMs.set(0)
        sink.open(format)
    }

    /**
     * نوشتن نمونه‌ها. اگر دستگاه آماده نباشد، اینجا تا آماده شدنش صبر می‌کند —
     * و همین صبر، سرعت پخش را با سرعت دستگاه هم‌تراز می‌کند.
     */
    fun write(bytes: ByteArray, offset: Int = 0, length: Int = bytes.size) {
        if (!isOpen.get() || length <= 0) return
        sink.write(bytes, offset, length)
        writtenBytes.addAndGet(length.toLong())
    }

    /** بایت‌هایی که واقعاً پخش شده‌اند */
    fun playedBytes(): Long = writtenBytes.get()

    fun playedMs(): Long {
        val bps = format.bytesPerSecond.toLong()
        if (bps == 0L) return baseMs.get()
        return baseMs.get() + writtenBytes.get() * 1000L / bps
    }

    /**
     * صفر کردن شمارندهٔ بایت‌های پخش‌شده.
     *
     * بعد از `seek` لازم است، وگرنه موقعیت گزارش‌شده از نقطهٔ درستی جلوتر می‌ماند.
     * مبنای جدید هم همان‌جا ثبت می‌شود تا موقعیت از seek به بعد ادامه پیدا کند.
     */
    fun resetPosition(positionMs: Long = 0) {
        writtenBytes.set(0)
        baseMs.set(positionMs.coerceAtLeast(0))
    }

    override fun close() {
        if (!isOpen.compareAndSet(true, false)) return
        runCatching { sink.close() }
    }

    /** مقصد نهایی نمونه‌ها — در تست با یک بافر حافظه جایگزین می‌شود */
    interface SampleSink {
        fun open(format: AudioFormat)
        fun write(data: ByteArray, offset: Int, length: Int)
        fun close()

        /**
         * مقصد ضبط‌کننده برای تست.
         *
         * @param pace اگر true باشد، بایت‌ها با سرعت واقعی دستگاه تحویل داده می‌شوند
         *   (یعنی هر بلوک به اندازهٔ زمانی که طول می‌کشد معطل می‌کند). بدون این،
         *   آهنگ آزمایشی چنان سریع تمام می‌شود که موتور هیچ‌وقت «در حال پخش» را
         *   نشان نمی‌دهد و تست‌ها بی‌معنا می‌شوند.
         */
        class Recording(private val pace: Boolean = false) : SampleSink {
            private var format: AudioFormat? = null
            val collected = java.io.ByteArrayOutputStream()
            var opened = false
                private set
            var closed = false
                private set

            override fun open(format: AudioFormat) {
                this.format = format
                opened = true
            }

            override fun write(data: ByteArray, offset: Int, length: Int) {
                collected.write(data, offset, length)
                if (pace) {
                    val f = format ?: return
                    if (f.bytesPerSecond > 0) {
                        val millis = length.toLong() * 1000L / f.bytesPerSecond
                        if (millis > 0) {
                            try {
                                Thread.sleep(millis)
                            } catch (_: InterruptedException) {
                                Thread.currentThread().interrupt()
                            }
                        }
                    }
                }
            }

            override fun close() {
                closed = true
            }

            fun bytes(): ByteArray = collected.toByteArray()
        }
    }
}

/**
 * بافر حلقوی تک‌نویسنده/تک‌خواننده، بدون قفل.
 *
 * الان در مسیر پخش استفاده نمی‌شود (دلیلش در [AudioOutputStream] آمده) ولی برای
 * رمزگشای شبکه در milestone 3b لازم می‌شود، چون خواندن از شبکه ناهمگام است و
 * نمی‌شود مستقیم صدا داد.
 */
class PcmRingBuffer(private val capacity: Int) {

    private val buffer = ByteArray(capacity)
    private val writeIndex = AtomicLong(0)
    private val readIndex = AtomicLong(0)
    private val open = AtomicBoolean(true)

    /** بایت‌های آمادهٔ خواندن */
    fun available(): Int = (writeIndex.get() - readIndex.get()).coerceAtLeast(0).toInt()

    fun freeSpace(): Int = capacity - available()

    fun write(src: ByteArray, offset: Int, length: Int): Int {
        val space = freeSpace()
        if (space <= 0) return 0
        val n = min(space, length)
        val w = writeIndex.get()
        // پیچیدن دور بافر در دو تکه انجام می‌شود
        val first = min(n, capacity - (w % capacity).toInt())
        val writePos = (w % capacity).toInt()
        System.arraycopy(src, offset, buffer, writePos, first)
        if (n > first) {
            System.arraycopy(src, offset + first, buffer, 0, n - first)
        }
        writeIndex.addAndGet(n.toLong())
        return n
    }

    /**
     * @param defaultTimeoutMs اگر چیزی آماده نبود، چقدر صبر کنیم
     * @return بایت‌های خوانده‌شده، یا null اگر بافر بسته باشد
     */
    fun readBlocking(defaultTimeoutMs: Int): ByteArray? {
        var waited = 0
        while (true) {
            val avail = available()
            if (avail > 0) return read(avail)
            if (!open.get()) return null
            if (waited >= defaultTimeoutMs) return null
            try {
                Thread.sleep(1)
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
                return null
            }
            waited += 1
        }
    }

    fun read(max: Int): ByteArray? {
        val avail = available()
        if (avail <= 0) return null
        val n = min(avail, max)
        val r = readIndex.get()
        val first = min(n, capacity - (r % capacity).toInt())
        val readPos = (r % capacity).toInt()
        val out = ByteArray(n)
        System.arraycopy(buffer, readPos, out, 0, first)
        if (n > first) {
            System.arraycopy(buffer, 0, out, first, n - first)
        }
        readIndex.addAndGet(n.toLong())
        return out
    }

    fun clear() {
        readIndex.set(writeIndex.get())
    }

    fun close() {
        open.set(false)
    }
}
