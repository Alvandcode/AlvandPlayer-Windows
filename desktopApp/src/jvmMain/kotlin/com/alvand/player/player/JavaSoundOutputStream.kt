package com.alvand.player.player

import javax.sound.sampled.AudioFormat as JavaAudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.SourceDataLine
import java.util.concurrent.atomic.AtomicBoolean

/**
 * مقصد نهایی نمونه‌ها روی بلندگوی واقعی.
 *
 * **چرا `javax.sound` و نه WASAPI مستقیم:** صدای JDK روی ویندوز خودش روی
 * WASAPI سوار است (WASAPI → DirectSound → MME) و تأخیر کمی دارد. برای این
 * milestone کافی است و هیچ وابستگی بومی نمی‌آورد. اگر بعداً تأخیر کمتر یا کنترل
 * دستگاه لازم شد، می‌شود یک [AudioOutputStream.SampleSink] دیگر نوشت و بقیهٔ کد
 * دست‌نخورده می‌ماند، چون همه چیز از راه همین رابط رد می‌شود.
 *
 * **رفتار با سایر برنامه‌ها:** خط انحصاری باز نمی‌شود. یعنی مثل هر پخش‌کنندهٔ
 * دیگری با موزیک‌پلیرهای دیگر هم‌زمان کار می‌کند و چیزی را مصادره نمی‌کند — همان
 * رفتاری که `AudioAttributes(USAGE_MEDIA)` در اندروید دارد.
 */
class JavaSoundSink(
    /** نام میکسر انتخابی کاربر؛ null یا نام‌ناپیدا = دستگاه پیش‌فرض سیستم */
    private val mixerName: String? = null
) : AudioOutputStream.SampleSink {

    private var line: SourceDataLine? = null
    private val started = AtomicBoolean(false)

    override fun open(format: AudioFormat) {
        val javaFormat = javaFormatFor(format)
        val info = DataLine.Info(SourceDataLine::class.java, javaFormat)
        val opened = lineOn(mixerName, info)
            ?: AudioSystem.getLine(info) as SourceDataLine   // فال‌بک: پیش‌فرض سیستم
        opened.open(javaFormat, format.bufferBytes(300))
        opened.start()
        line = opened
        started.set(true)
    }

    /**
     * خط روی میکسر انتخابی — اگر نام پیدا نشد یا خط پشتیبانی نشد، null تا
     * فال‌بک به دستگاه پیش‌فرض انجام شود. دستگاه اشتباه هرگز نباید پخش را
     * از کار بیندازد.
     */
    private fun lineOn(name: String?, info: DataLine.Info): SourceDataLine? {
        if (name.isNullOrBlank()) return null
        val mixer = AudioSystem.getMixerInfo().firstOrNull { it.name == name } ?: return null
        return runCatching {
            val m = AudioSystem.getMixer(mixer)
            if (!m.isLineSupported(info)) return null
            m.getLine(info) as SourceDataLine
        }.getOrNull()
    }

    override fun write(data: ByteArray, offset: Int, length: Int) {
        line?.write(data, offset, length)
    }

    override fun close() {
        started.set(false)
        runCatching {
            line?.stop()
            line?.close()
        }
        line = null
    }

    val isRunning: Boolean get() = started.get() && line?.isOpen == true

    /** نام دستگاه خروجی جاری، برای نمایش در About */
    fun deviceName(): String? = runCatching { line?.toString() }.getOrNull()
}

/**
 * قالب PCM دستگاه.
 *
 * همهٔ رمزگشاهای موتور بایت‌های signed little-endian تولید می‌کنند و
 * [PcmBuffer.toFloats] هم دقیقاً همین را می‌خواند؛ پس قالب خط صوتی هم باید
 * همان باشد. اگر unsigned اعلام شود، همان بایت‌ها با آفست DC پخش می‌شوند.
 */
internal fun javaFormatFor(format: AudioFormat): JavaAudioFormat = JavaAudioFormat(
    format.sampleRate.toFloat(),
    format.bitsPerSample,
    format.channels,
    true,   // signed
    false   // little endian
)

/**
 * ساخت خروجی استاندارد.
 *
 * WASAPI مستقیم در milestone 8 (یکپارچگی با ویندوز) اضافه می‌شود؛ آن موقع فقط
 * همین یک تابع عوض می‌شود.
 */
object AudioOutputs {

    /** قالب متعارف برای تشخیص پشتیبانی میکسرها در فهرست دستگاه‌ها */
    private val PROBE_FORMAT = JavaAudioFormat(44_100f, 16, 2, true, false)

    /** فهرست نام دستگاه‌های خروجی که خط SourceDataLine پشتیبانی می‌کنند */
    fun listDevices(): List<String> =
        runCatching {
            AudioSystem.getMixerInfo().mapNotNull { info ->
                val probe = DataLine.Info(SourceDataLine::class.java, PROBE_FORMAT)
                if (runCatching { AudioSystem.getMixer(info).isLineSupported(probe) }.getOrDefault(false)) {
                    info.name
                } else null
            }
        }.getOrDefault(emptyList())

    fun create(format: AudioFormat, deviceName: String? = null): AudioOutputStream =
        AudioOutputStream(format, JavaSoundSink(deviceName))
}

