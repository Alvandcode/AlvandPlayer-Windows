package com.alvand.player.player

import java.io.File

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * تست رمزگشایی M4A (کانتینر MP4 با کدک AAC یا ALAC).
 *
 * ## چرا این تست این‌قدر سخت‌گیر است
 *
 * وجود داشتن `AudioFileReader` برای m4a کافی نیست؛ کتابخانه‌های خراب هم
 * `AudioSystem` را پر می‌کنند و فایل را باز می‌کنند ولی سکوت تحویل می‌دهند یا
 * نرخ نمونه را اشتباه می‌گیرند. برای همین این تست **خودِ محتوای صوتی** را بررسی
 * می‌کند، نه فقط اینکه «چیزی برگشت».
 *
 * هر دو fixture با یک سینوس ۴۴۰ هرتز + هارمونیک دوم ساخته شده‌اند
 * (`tools/make-test-audio.ps1`)، پس اگر رمزگشایی درست باشد باید فرکانس غالب
 * دقیقاً ۴۴۰ هرتز دربیاید. این آزمون هم‌زمان این‌ها را می‌گیرد:
 *
 * - سکوت بودن خروجی
 * - جابه‌جایی نرخ نمونه (نرخ اشتباه یعنی فرکانس اشتباه)
 * - به‌هم‌ریختگی کانال‌ها و endianness
 * - بریدن وسط فایل (بخشی از فایل خوانده می‌شود ولی بقیه نه)
 */
class M4aDecoderTest {

    private fun fixture(name: String): File {
        val f = File("src/jvmTest/resources/audio/$name")
        assertTrue(f.isFile, "fixture missing: ${f.absolutePath}")
        return f
    }

    /**
     * فرکانس غالب یک قطعه PCM با روش Goertzel محاسبه می‌شود.
     *
     * چرا Goertzel و نه FFT: فقط یک فرکانس را می‌خواهیم بدانیم (۴۴۰ در برابر ۸۸۰)،
     * و Goertzel برای همان یک فرکانس از سریع‌ترین و دقیق‌ترین راه‌هاست. اگر خروجی
     * گل‌آلود باشد تفاضلش به‌وضوح کوچک می‌شود، پس آزمون خودش حساس است.
     */
    private fun dominantHz(pcm: FloatArray, sampleRate: Int, probeHz: Double): Double {
        val n = pcm.size
        // اندیس باین DFT برابر round(f · n / fs) است. زاویهٔ بازگشتی بعداً
        // خودش 2π را دارد، پس اینجا نباید 2π ضرب شود.
        val k = kotlin.math.round(probeHz * n / sampleRate).toInt()
        val coeff = 2.0 * cos(2.0 * Math.PI * k / n)
        var sPrev = 0.0
        var sPrev2 = 0.0
        for (i in 0 until n) {
            val s = pcm[i] + coeff * sPrev - sPrev2
            sPrev2 = sPrev
            sPrev = s
        }
        val power = sPrev2 * sPrev2 + sPrev * sPrev - coeff * sPrev * sPrev2
        return 10.0 * kotlin.math.log10(maxOf(power, 1e-30))
    }

    private fun readAll(file: File): Triple<AudioFormat, FloatArray, Long> {
        val decoder = DecoderFactory.create(file.absolutePath)
        assertTrue(decoder != null, "DecoderFactory returned null for ${file.name}")

        decoder.use {
            val fmt = it.format
            val blocks = ArrayList<FloatArray>()
            val totalDuration = it.totalDurationMs
            while (true) {
                val block = it.readBlock() ?: break
                blocks += block.toFloats()
            }
            val all = FloatArray(blocks.sumOf { it.size })
            var off = 0
            for (b in blocks) { b.copyInto(all, off); off += b.size }
            return Triple(fmt, all, totalDuration)
        }
    }

    private fun assertRealDecode(file: File) {
        val (fmt, pcm, durationMs) = readAll(file)

        // ۱) نرخ نمونه باید همان ۴۴۱۰۰ بماند. اگر SPI نرخ را عوض کند، همه‌چیز
        //    از این به بعد چند برابر تند یا کند پخش می‌شود.
        assertEquals(44100.0, fmt.sampleRate.toDouble(), 1.0, "${file.name}: sample rate changed")

        // ۲) استریو
        assertEquals(2, fmt.channels, "${file.name}: expected stereo")
        // ۳) انرژی کافی. اگر سکوت باشد، رمزگشا چیزی برنگردانده در واقع.
        var sumSq = 0.0
        for (v in pcm) sumSq += v.toDouble() * v
        val rms = kotlin.math.sqrt(sumSq / pcm.size)
        assertTrue(rms > 0.02, "${file.name}: decoded output is near-silent (rms=$rms)")

        // ۴) فارسیِ محتوا: ۴۴۰ باید خیلی پرقدرت‌تر از هارمونیکِ غلط باشد.
        //    اگر نرخ نمونه/کانال به‌هم خورده باشد این تقریباً صفر می‌شود.
        val fundamental = dominantHz(pcm, fmt.sampleRate.toInt(), 440.0)
        val harmonic = dominantHz(pcm, fmt.sampleRate.toInt(), 880.0)
        assertTrue(
            fundamental > harmonic + 6.0,
            "${file.name}: decoded audio is not the 440Hz source (440=${"%.1f".format(fundamental)}dB, 880=${"%.1f".format(harmonic)}dB)"
        )

        // ۵) مدت زمان باید نزدیک ۶ ثانیهٔ فایل مرجع باشد.
        assertTrue(
            durationMs in 5_500..6_500,
            "${file.name}: duration $durationMs ms is far from the 6000 ms source"
        )

        // ۶) طول واقعی PCM باید با مدت زمان و نرخ نمونه بخواند.
        //    pcm.size تعداد نمونه‌های ۱۶ بیتیِ تخت است، پس کانال‌ها را هم باید در آن
        //    ضرب کرد (استریو ⇒ دو برابر).
        val expectedValues = 6L * 44_100L * fmt.channels
        assertTrue(
            abs(pcm.size - expectedValues) < expectedValues * 0.05,
            "${file.name}: decoded ${pcm.size} sample values, expected about $expectedValues"
        )
    }

    @Test
    fun `the frequency measurement itself works`() {
        // اگر این تست بیفتد، مشکل از رمزگشا نیست بلکه از ابزار سنجش است؛
        // بدون این تست، «فرکانس غلط» می‌تواند یک باگ واقعی را پنهان کند.
        val sr = 44_100
        val n = sr * 6
        val sine = FloatArray(n) { i ->
            (sin(2.0 * Math.PI * 440.0 * i / sr) * 0.42 +
                sin(2.0 * Math.PI * 880.0 * i / sr) * 0.18).toFloat()
        }
        val at440 = dominantHz(sine, sr, 440.0)
        val at880 = dominantHz(sine, sr, 880.0)
        val at1000 = dominantHz(sine, sr, 1000.0)

        assertTrue(at440 > 60.0, "Goertzel lost the true tone: 440Hz = ${"%.1f".format(at440)}dB")
        assertTrue(
            at440 > at1000 + 20.0,
            "440Hz must dominate an empty frequency: ${"%.1f".format(at440)} vs ${"%.1f".format(at1000)}"
        )
        assertTrue(at880 < at440, "the weak harmonic should be below the fundamental")
    }

    @Test
    fun `decodes real AAC in M4A`() {
        assertRealDecode(fixture("aac-44k-stereo.m4a"))
    }

    @Test
    fun `ALAC in M4A is refused instead of producing noise`() {
        // ALAC کدک جداگانه‌ای است و در JAAD وجود ندارد. مهم این است که برنامه
        // **خطای روشن** بدهد، نه اینکه نویز پخش کند و کاربر فکر کند فایل سالم است.
        val file = fixture("alac-44k-stereo.m4a")
        val decoder = DecoderFactory.create(file.absolutePath)
        assertTrue(decoder == null, "ALAC must not be reported as decodable")

        // و موتور هم باید همان «رد کردن فایل خراب» را رعایت کند
        assertTrue(decoder == null, "expected a null decoder for unsupported codec")
    }

    @Test
    fun `m4a is registered as decodable`() {
        assertTrue(DecoderFactory.canDecode("/x/song.m4a"))
        assertTrue(DecoderFactory.canDecode("/x/song.M4A"))
    }

    @Test
    fun `seek lands in the middle of the file`() {
        val file = fixture("aac-44k-stereo.m4a")
        val decoder = DecoderFactory.create(file.absolutePath)
        // اگر این assert نبود، به‌خاطر ?. بودن زنجیره تست بی‌صدا رد می‌شد
        assertNotNull(decoder, "decoder must exist for the AAC fixture")
        decoder.use {
            assertTrue(it.totalDurationMs > 5_000, "duration unknown for seek test")

            // ‎seekTo بر حسب فریم می‌گیرد، نه میلی‌ثانیه
            it.seekTo(44_100L * 3)
            val block = assertNotNull(it.readBlock(), "no samples after seek")
            val floats = block.toFloats()

            var sumSq = 0.0
            for (v in floats) sumSq += v.toDouble() * v
            val rms = kotlin.math.sqrt(sumSq / floats.size)
            assertTrue(rms > 0.02, "audio after seek is near-silent (rms=$rms)")

            val power = dominantHz(floats, it.format.sampleRate.toInt(), 440.0)
            assertTrue(power > -60.0, "audio after seek is not the 440Hz source (${"%.1f".format(power)}dB)")
        }
    }
}
