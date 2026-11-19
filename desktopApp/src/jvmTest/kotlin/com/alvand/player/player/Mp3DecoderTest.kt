package com.alvand.player.player

import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Path
import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * تست رمزگشای MP3.
 *
 * ## چرا فایل مصنوعی، و کجا کافی نیست
 *
 * برای آفلاین و قطعی ماندن تست‌ها، فریم‌های معتبر MPEG-1 Layer III را در کد
 * می‌سازیم (هدر واقعی، دادهٔ سکوت) و هیچ باینری وارد مخزن نمی‌شود.
 *
 * این روش برای **پارس هدر، مدت‌زمان، کانال، تگ ID3v2، مسیریابی کارخانه و رد کردن
 * فایل نامعتبر** کافی است. ولی **برای رمزگشایی صدای واقعی کافی نیست**: JLayer
 * یک «مخزن بیت» (bit reservoir) بین فریم‌ها نگه می‌دارد و فریمی که دادهٔ طیفی
 * ندارد، بعد از چند فریم به سرریز می‌خورد. یعنی از این روش **نمی‌شود** جریان
 * بلند، پرش، و صدای غیرسکوت را آزمود.
 *
 * برای همین آن آزمون‌ها جدا شده‌اند: با متغیر محیطی `ALVAND_MP3_TEST_FILE` که به
 * مسیر یک MP3 واقعی اشاره کند اجرا می‌شوند، و بدون آن **skip** می‌شوند نه fail.
 */
class Mp3DecoderTest {

    private lateinit var dir: Path

    @BeforeTest
    fun setUp() {
        dir = Files.createTempDirectory("alvand-mp3-test")
    }

    @AfterTest
    fun tearDown() {
        runCatching { dir.toFile().deleteRecursively() }
    }

    /** اگر متغیر محیطی تنظیم نشده باشد، مسیر null برمی‌گردد تا تست skip شود */
    private fun realMp3(): File? {
        val path = System.getenv("ALVAND_MP3_TEST_FILE")
        if (path.isNullOrBlank()) {
            println("SKIP: set ALVAND_MP3_TEST_FILE to run against a real MP3")
            return null
        }
        val f = File(path)
        assertTrue(f.isFile, "ALVAND_MP3_TEST_FILE does not point to a file: $path")
        // این تست‌ها دربارهٔ MP3 هستند؛ اگر به فایل پشتیبانی‌نشده‌ای اشاره شده باشد
        // تمیز skip می‌شوند تا شکست بی‌معنا نداشته باشیم
        if (!DecoderFactory.canDecode(f.absolutePath) ||
            f.extension.lowercase() !in setOf("mp3", "mpga", "mp2")
        ) {
            println("SKIP: ${f.name} is not an MP3 extension; MP3 tests do not apply")
            return null
        }
        return f
    }

    // ------------------------------------------------------------ ساخت MP3 مصنوعی

    private fun mp3Frame(bitrateIndex: Int = 9, mono: Boolean = false): ByteArray {
        val kbps = intArrayOf(0, 32, 40, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320, 0)
        val frameSize = 144 * kbps[bitrateIndex] * 1000 / 44100
        val out = ByteArray(frameSize)
        out[0] = 0xFF.toByte()
        out[1] = 0xFB.toByte()                              // MPEG-1, Layer 3, no CRC
        out[2] = ((bitrateIndex and 0x0F) shl 4).toByte()    // bitrate index, 44100 Hz
        out[3] = if (mono) 0xC0.toByte() else 0x00.toByte()  // channel mode
        return out
    }

    private fun writeMp3(target: File, frames: Int, mono: Boolean = false) {
        val frame = mp3Frame(mono = mono)
        FileOutputStream(target).use { out -> repeat(frames) { out.write(frame) } }
    }

    private fun id3v2Tag(bodySize: Int): ByteArray {
        val tag = ByteArray(10 + bodySize)
        tag[0] = 'I'.code.toByte(); tag[1] = 'D'.code.toByte(); tag[2] = '3'.code.toByte()
        tag[3] = 3
        val s = bodySize
        tag[6] = ((s shr 21) and 0x7F).toByte()
        tag[7] = ((s shr 14) and 0x7F).toByte()
        tag[8] = ((s shr 7) and 0x7F).toByte()
        tag[9] = (s and 0x7F).toByte()
        return tag
    }

    // ------------------------------------------------- تست‌های آفلاین (مصنوعی)

    @Test
    fun `reads the real mp3 header`() {
        val f = dir.resolve("a.mp3").toFile()
        writeMp3(f, frames = 20)

        val d = Mp3Decoder(f)
        assertEquals(44100, d.format.sampleRate, "44100 Hz should be detected")
        assertEquals(2, d.format.channels, "stereo should be detected")
        assertEquals(16, d.format.bitsPerSample)
        d.close()
    }

    @Test
    fun `detects mono mp3`() {
        val f = dir.resolve("mono.mp3").toFile()
        writeMp3(f, frames = 10, mono = true)

        val d = Mp3Decoder(f)
        assertEquals(1, d.format.channels, "single channel mode must be detected as mono")
        d.close()
    }

    @Test
    fun `estimates duration from the frame count`() {
        // ۱۰۰ فریم در ۴۴۱۰۰ هرتز لایهٔ ۳ ⇒ هر فریم ۱۱۵۲ نمونه ⇒ ۲۶٫۱۲ میلی‌ثانیه
        val frames = 100
        val f = dir.resolve("d.mp3").toFile()
        writeMp3(f, frames = frames)

        val d = Mp3Decoder(f)
        val expected = frames * 1152.0 * 1000.0 / 44100.0
        assertTrue(
            kotlin.math.abs(d.totalDurationMs - expected) < 60,
            "duration ${d.totalDurationMs}ms should be near ${expected.toLong()}ms"
        )
        d.close()
    }

    @Test
    fun `skips an id3v2 tag when measuring duration`() {
        val f = dir.resolve("tagged.mp3").toFile()
        val frame = mp3Frame()
        FileOutputStream(f).use { out ->
            out.write(id3v2Tag(bodySize = 2048))
            repeat(40) { out.write(frame) }
        }

        val d = Mp3Decoder(f)
        val expected = 40 * 1152.0 * 1000.0 / 44100.0
        assertTrue(
            kotlin.math.abs(d.totalDurationMs - expected) < 60,
            "ID3v2 tag was not excluded: ${d.totalDurationMs}ms vs ${expected.toLong()}ms"
        )
        assertNotNull(d.readBlock(), "decoding should start right after the tag")
        d.close()
    }

    @Test
    fun `seek in the middle of a real mp3 keeps decoding audible audio`() {
        val f = realMp3() ?: return

        val d = Mp3Decoder(f)
        assertTrue(d.totalDurationMs > 60_000, "need a real song-length file, got ${d.totalDurationMs}ms")
        // وسط آهنگ — دقیقاً همان کاری که کلیک روی نوار پیشرفت می‌کند
        d.seekTo(d.totalDurationMs / 2 * d.format.sampleRate / 1000)

        var blocks = 0
        var peak = 0f
        repeat(20) {
            val block = d.readBlock() ?: return@repeat
            blocks++
            for (v in block.toFloats()) {
                val a = kotlin.math.abs(v)
                if (a > peak) peak = a
            }
        }
        d.close()
        assertTrue(blocks > 0, "seek to the middle produced no data (engine would skip the track)")
        assertTrue(peak > 0.01f, "audio after mid seek is silence (peak=$peak)")
    }

    @Test
    fun `a file that is not mp3 is rejected instead of decoded`() {
        val f = dir.resolve("fake.mp3").toFile()
        FileOutputStream(f).use { out ->
            out.write("RIFF".toByteArray())
            out.write(ByteArray(200))
        }
        assertTrue(
            runCatching { Mp3Decoder(f) }.isFailure,
            "a non-MP3 file must be rejected, not silently decoded"
        )
        assertNull(DecoderFactory.create(f.absolutePath), "factory must return null")
    }

    @Test
    fun `empty file is rejected`() {
        val f = dir.resolve("empty.mp3").toFile()
        f.writeBytes(ByteArray(0))
        assertTrue(runCatching { Mp3Decoder(f) }.isFailure)
        assertNull(DecoderFactory.create(f.absolutePath))
    }

    @Test
    fun `factory recognises supported extensions`() {
        assertTrue(DecoderFactory.canDecode("C:/x/song.mp3"))
        assertTrue(DecoderFactory.canDecode("C:/x/SONG.MP3"), "case must be ignored")
        assertTrue(DecoderFactory.canDecode("C:\\x\\song.mp3"), "backslashes must work")
        assertTrue(DecoderFactory.canDecode("C:/x/song.mpga"))
        assertTrue(DecoderFactory.canDecode("C:/x/song.mp2"))
        assertTrue(DecoderFactory.canDecode("C:/x/song.wav"))
        // flac و ogg از milestone بعدی اضافه شدند
        assertTrue(DecoderFactory.canDecode("C:/x/song.flac"))
        assertTrue(DecoderFactory.canDecode("C:/x/song.ogg"))
        assertTrue(DecoderFactory.canDecode("C:/music.no-extension") == false)
    }

    @Test
    fun `factory builds a decoder for mp3`() {
        val f = dir.resolve("via-factory.mp3").toFile()
        writeMp3(f, frames = 3)
        val d = DecoderFactory.create(f.absolutePath)
        assertNotNull(d, "factory must build an Mp3Decoder for .mp3")
        assertEquals(44100, d.format.sampleRate)
        d.close()
    }

    @Test
    fun `seek past the end returns no data instead of crashing`() {
        val f = dir.resolve("seek-end.mp3").toFile()
        writeMp3(f, frames = 10)

        val d = Mp3Decoder(f)
        d.seekTo(44100 * 600)   // ده دقیقه جلوتر از هر فایل ده‌فریمی
        assertNull(d.readBlock(), "seeking past the end must report end of file")
        d.close()
    }

    @Test
    fun `seek to the middle keeps the stream readable`() {
        val f = dir.resolve("mid.mp3").toFile()
        writeMp3(f, frames = 100)

        val d = Mp3Decoder(f)
        // وسط فایل: هر فریم MP3 در ۴۴۱۰۰ هرتز ۱۱۵۲ نمونه است
        d.seekTo(50L * 1152)
        val block = d.readBlock()
        assertNotNull(block, "seeking to the middle must not report end of file")
        assertTrue(block.bytes.isNotEmpty(), "seeking to the middle must leave data to read")
        d.close()
    }

    @Test
    fun `seek to zero makes the file readable again`() {
        val f = dir.resolve("zero.mp3").toFile()
        writeMp3(f, frames = 3)

        val d = Mp3Decoder(f)
        assertNotNull(d.readBlock())
        d.seekTo(999_999)
        d.seekTo(0)
        assertNotNull(d.readBlock(), "seek(0) must return to the start of the file")
        d.close()
    }

    // ----------------------------------------------------- تست‌های فایل واقعی

    @Test
    fun `real mp3 produces audible samples`() {
        val f = realMp3() ?: return

        val d = DecoderFactory.create(f.absolutePath)
        assertNotNull(d, "factory must be able to decode a real MP3")
        println(
            "REAL-MP3 ${f.name}: ${d.format.sampleRate}Hz ch=${d.format.channels} " +
                "dur=${d.totalDurationMs}ms"
        )

        assertTrue(
            d.format.sampleRate in listOf(8000, 11025, 16000, 22050, 24000, 32000, 44100, 48000),
            "unexpected sample rate ${d.format.sampleRate}"
        )
        assertTrue(d.format.channels in 1..2, "unexpected channel count ${d.format.channels}")
        assertTrue(d.totalDurationMs > 0, "a real file must report a positive duration")

        var peak = 0f
        var total = 0L
        var blocks = 0
        while (blocks < 300) {
            val block = d.readBlock() ?: break
            total += block.bytes.size
            for (v in block.toFloats()) {
                val a = kotlin.math.abs(v)
                if (a > peak) peak = a
            }
            blocks++
        }
        println("REAL-MP3 decoded ${total / 1024}KB in $blocks blocks, peak=$peak")
        assertTrue(blocks > 0, "no audio decoded")
        assertTrue(peak > 0.01f, "decoded real MP3 is silence (peak=$peak)")
        d.close()
    }

    @Test
    fun `real mp3 streams all the way to the end`() {
        val f = realMp3() ?: return

        val d = DecoderFactory.create(f.absolutePath)!!
        var blocks = 0
        while (d.readBlock() != null) blocks++
        assertTrue(blocks > 0, "streaming produced nothing")

        // هر بلوک حداکثر ۴۰۹۶ «فریم» (نمونه برای هر کانال) دارد.
        // مدت = تعداد فریم ÷ نرخ نمونه — نه تقسیم بر بایت بر ثانیه.
        val declaredSec = d.totalDurationMs / 1000.0
        val decodedSec = blocks.toDouble() * 4096 / d.format.sampleRate
        println("REAL-MP3 declared=${declaredSec}s decoded=$decodedSec s")
        assertTrue(
            kotlin.math.abs(declaredSec - decodedSec) / declaredSec < 0.10,
            "declared $declaredSec s but decoded about $decodedSec s"
        )
        d.close()
    }

    @Test
    fun `real mp3 plays end to end through the engine`() {
        val f = realMp3() ?: return

        val engine = JdkPlayerEngine(
            scope = kotlinx.coroutines.CoroutineScope(
                kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Default
            ),
            outputFactory = { fmt -> AudioOutputs.create(fmt) },
            decoderFactory = { uri, mime -> DecoderFactory.create(uri, mime) }
        )
        engine.onReady()
        try {
            runBlocking {
                engine.setQueue(
                    listOf(PlayableItem(1, f.name, "Real", uri = f.absolutePath)),
                    0, autoPlay = true
                )
            }
            val deadline = System.currentTimeMillis() + 10_000
            while (System.currentTimeMillis() < deadline && engine.state.value.positionMs <= 0) {
                Thread.sleep(30)
            }
            val state = engine.state.value
            println("REAL-MP3 engine position=${state.positionMs}ms playing=${state.isPlaying}")
            assertNull(state.error, "the engine must not report an error")
            assertTrue(state.positionMs > 0, "position never advanced on the real audio path")
        } finally {
            engine.release()
        }
    }

    @Test
    fun `real mp3 seek lands on audio`() {
        val f = realMp3() ?: return

        val d = DecoderFactory.create(f.absolutePath)!!
        // وسط فایل، نه انتهایش — فایل‌های کوتاه جای دیگری برای پرش ندارند
        val target = d.format.sampleRate.toLong() * (d.totalDurationMs / 2000)
        assertTrue(target > 0, "file must be long enough to seek inside")
        d.seekTo(target)

        val block = assertNotNull(d.readBlock(), "seek must land inside the file")
        var peak = 0f
        for (v in block.toFloats()) {
            val a = kotlin.math.abs(v)
            if (a > peak) peak = a
        }
        println("REAL-MP3 after seek to ${target / d.format.sampleRate}s: peak=$peak")
        assertTrue(peak > 0.01f, "seek landed on silence (peak=$peak)")
        d.close()
    }
}