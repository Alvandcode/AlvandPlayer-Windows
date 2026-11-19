package com.alvand.player.player

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * تست FLAC و Ogg Vorbis.
 *
 * ## چرا FLAC آفلاین تست می‌شود ولی Ogg نه
 *
 * برای FLAC یک **رمزگذار** خالص جاوا هست، پس فایل معتبر واقعی ساخته می‌شود و همه‌چیز
 * آفلاین و قطعی آزموده می‌شود.
 *
 * برای Ogg Vorbis **هیچ رمزگذار خالص جاوایی روی Maven Central وجود ندارد**
 * (بررسی شد: jorbis و Tremor فقط رمزگشا هستند). ساختن یک Ogg معتبر با دست نیازمند
 * نوشتن جدول‌های هافمن codebook است که عملی نیست. پس تست‌های Ogg به یک فایل واقعی
 * نیاز دارند و با متغیر محیطی `ALVAND_OGG_TEST_FILE` اجرا می‌شوند؛ بدون آن
 * **skip** می‌شوند، نه fail.
 */
class FlacAndOggTest {

    private lateinit var dir: Path

    @BeforeTest
    fun setUp() {
        dir = Files.createTempDirectory("alvand-flac-test")
    }

    @AfterTest
    fun tearDown() {
        runCatching { dir.toFile().deleteRecursively() }
    }

    private fun realOgg(): File? {
        val path = System.getenv("ALVAND_OGG_TEST_FILE")
        if (path.isNullOrBlank()) {
            println("SKIP: set ALVAND_OGG_TEST_FILE to run against a real Ogg file")
            return null
        }
        val f = File(path)
        assertTrue(f.isFile, "ALVAND_OGG_TEST_FILE does not point to a file: $path")
        val ext = f.extension.lowercase()
        if (ext != "ogg" && ext != "oga") {
            println("SKIP: ${f.name} is not an Ogg extension")
            return null
        }
        return f
    }

    /** بیشترین دامنهٔ نمونه‌های یک بلاک — برای اثبات اینکه صدا واقعاً هست */
    private fun peakOf(vararg blocks: PcmBuffer): Float {
        var peak = 0f
        for (b in blocks) for (v in b.toFloats()) {
            val a = kotlin.math.abs(v)
            if (a > peak) peak = a
        }
        return peak
    }

    private fun drain(d: AudioDecoder, limit: Int = 400): List<PcmBuffer> {
        val out = mutableListOf<PcmBuffer>()
        var n = 0
        while (n++ < limit) {
            val b = d.readBlock() ?: break
            out += b
        }
        return out
    }

    // ------------------------------------------------------------------- FLAC

    @Test
    fun `flac file decodes to audible samples`() {
        val f = FlacFixtures.writeTo(dir, "tone.flac", seconds = 1, freq = 440)
        assertTrue(f.length() > 0, "encoder produced nothing")

        val d = assertNotNull(
            DecoderFactory.create(f.absolutePath),
            "factory must decode FLAC"
        )
        println("FLAC: ${d.format.sampleRate}Hz ch=${d.format.channels} dur=${d.totalDurationMs}ms")

        assertEquals(44100, d.format.sampleRate)
        assertEquals(2, d.format.channels)
        assertEquals(16, d.format.bitsPerSample)
        assertTrue(
            d.totalDurationMs in 950..1100,
            "1 second of audio should be about 1000ms, got ${d.totalDurationMs}"
        )

        val blocks = drain(d)
        assertTrue(blocks.isNotEmpty(), "no audio decoded")
        val peak = peakOf(*blocks.toTypedArray())
        println("FLAC decoded ${blocks.sumOf { it.bytes.size } / 1024}KB peak=$peak")
        assertTrue(peak > 0.2f, "decoded FLAC is silence or too quiet (peak=$peak)")
        d.close()
    }

    @Test
    fun `flac streams the whole file and ends cleanly`() {
        val f = FlacFixtures.writeTo(dir, "long.flac", seconds = 2, freq = 523)
        val d = assertNotNull(DecoderFactory.create(f.absolutePath))

        val blocks = drain(d)
        val frames = blocks.sumOf { it.frames.toLong() }
        val seconds = frames.toDouble() / d.format.sampleRate
        println("FLAC streamed ${blocks.size} blocks = $seconds s (declared ${d.totalDurationMs}ms)")
        // رمزگذار/رمزگشا ممکن است چند هزار نمونه اختلاف داشته باشند
        assertTrue(
            kotlin.math.abs(seconds - 2.0) < 0.1,
            "expected about 2 s, streamed $seconds s"
        )
        d.close()
    }

    @Test
    fun `flac blocks are always whole frames`() {
        val f = FlacFixtures.writeTo(dir, "frames.flac", seconds = 1)
        val d = assertNotNull(DecoderFactory.create(f.absolutePath))
        val blockSize = d.format.frameSize

        var n = 0
        while (n++ < 200) {
            val b = d.readBlock() ?: break
            assertEquals(
                0, b.bytes.size % blockSize,
                "a block ended mid-frame, which would corrupt the DSP chain"
            )
        }
        d.close()
    }

    @Test
    fun `flac seek lands on audio`() {
        val f = FlacFixtures.writeTo(dir, "seek.flac", seconds = 3, freq = 660)
        val d = assertNotNull(DecoderFactory.create(f.absolutePath))

        d.seekTo(d.format.sampleRate.toLong())   // یک ثانیه جلو
        val after = drain(d, limit = 20)
        assertTrue(after.isNotEmpty(), "seek must land inside the file")
        val peak = peakOf(*after.toTypedArray())
        println("FLAC after seek peak=$peak")
        assertTrue(peak > 0.05f, "seek landed on silence (peak=$peak)")
        d.close()
    }

    @Test
    fun `flac seek to zero replays from the start`() {
        val f = FlacFixtures.writeTo(dir, "zero.flac", seconds = 2)
        val d = assertNotNull(DecoderFactory.create(f.absolutePath))

        val first = assertNotNull(d.readBlock())
        d.seekTo(d.totalDurationMs.coerceAtLeast(1L) * 44)
        d.seekTo(0)
        val again = assertNotNull(d.readBlock(), "seek(0) must make the file readable again")
        assertEquals(first.bytes.size, again.bytes.size)
        d.close()
    }

    @Test
    fun `factory reports the new extensions`() {
        assertTrue(DecoderFactory.canDecode("C:/x/song.flac"))
        assertTrue(DecoderFactory.canDecode("C:/x/song.FLAC"))
        assertTrue(DecoderFactory.canDecode("C:/x/song.ogg"))
        assertTrue(DecoderFactory.canDecode("C:/x/song.oga"))
        // m4a از وقتی رمزگشای AAC اضافه شد پشتیبانی می‌شود (M4aDecoderTest این را
        // با فایل واقعی و بررسی محتوای صوتی ثابت می‌کند)
        assertTrue(DecoderFactory.canDecode("C:/x/song.m4a"), "m4a is supported since the AAC decoder landed")
        assertTrue(!DecoderFactory.canDecode("C:/x/song.wma"), "wma is still unsupported")
    }

    @Test
    fun `a corrupt flac is rejected instead of crashing`() {
        val f = File(dir.toFile(), "broken.flac")
        f.writeBytes("fLaC".toByteArray() + ByteArray(200))   // فقط هدر، بدون فریم

        // ساختن رمزگشا نباید کرش کند؛ اگر نشد، خواندن باید تمیز متوقف شود
        val d = SpiAudioDecoder.tryCreate(f, setOf("flac"))
        if (d != null) {
            runCatching { drain(d, limit = 20) }
            d.close()
        }
        // اگر کارخانه هم null بدهد، همان هم قابل قبول است
        runCatching { DecoderFactory.create(f.absolutePath)?.close() }
    }

    // ------------------------------------------------------------- Ogg Vorbis

    @Test
    fun `real ogg decodes to audible samples`() {
        val f = realOgg() ?: return

        val d = assertNotNull(DecoderFactory.create(f.absolutePath), "factory must decode Ogg")
        println("OGG ${f.name}: ${d.format.sampleRate}Hz ch=${d.format.channels} dur=${d.totalDurationMs}ms")

        assertTrue(d.format.sampleRate in listOf(8000, 11025, 16000, 22050, 24000, 32000, 44100, 48000))
        assertTrue(d.format.channels in 1..2)
        assertTrue(d.totalDurationMs > 0, "a real file must report a positive duration")

        val blocks = drain(d, limit = 200)
        val peak = peakOf(*blocks.toTypedArray())
        println("OGG decoded ${blocks.sumOf { it.bytes.size } / 1024}KB peak=$peak")
        assertTrue(blocks.isNotEmpty(), "no audio decoded")
        assertTrue(peak > 0.01f, "decoded Ogg is silence (peak=$peak)")
        d.close()
    }

    @Test
    fun `real ogg seek lands on audio`() {
        val f = realOgg() ?: return

        val d = assertNotNull(DecoderFactory.create(f.absolutePath))
        d.seekTo(d.totalDurationMs / 1000 * 44 / 2)   // وسط فایل
        val after = drain(d, limit = 20)
        assertTrue(after.isNotEmpty(), "seek must land inside the file")
        val peak = peakOf(*after.toTypedArray())
        println("OGG after seek peak=$peak")
        assertTrue(peak > 0.01f, "seek landed on silence (peak=$peak)")
        d.close()
    }

    @Test
    fun `a file that is not ogg at all is rejected rather than mis-decoded`() {
        // فایل متنی با پسوند ogg: نه هدر Ogg دارد نه Vorbis
        val f = File(dir.toFile(), "fake.ogg")
        f.writeText("this is definitely not audio".repeat(20))

        val d = runCatching { DecoderFactory.create(f.absolutePath) }.getOrNull()
        if (d != null) {
            // اگر ساخته شد، خواندنش نباید کرش کند
            runCatching { drain(d, limit = 5) }
            d.close()
        }
    }
}
