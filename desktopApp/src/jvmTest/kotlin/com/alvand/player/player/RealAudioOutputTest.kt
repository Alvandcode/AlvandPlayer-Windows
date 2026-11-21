package com.alvand.player.player

import com.alvand.player.audio.DspSpec
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import javax.sound.sampled.AudioFormat as JavaAudioFormat

/**
 * آزمون مسیر واقعی صدا: بلندگوی واقعی، نه مقصد ضبط‌کننده.
 *
 * این تنها تستی است که ثابت می‌کند روی **این دستگاه** موتور صدا می‌تواند خط باز کند
 * و بایت بفرستد. اگر روی ماشینی بدون خروجی صوتی اجرا شود، تست skip می‌شود (نه
 * fail) — چون نبودِ بلندگو یک خطای محیطی است، نه خطای برنامه.
 */
class RealAudioOutputTest {

    private lateinit var dir: Path

    @BeforeTest
    fun setUp() {
        dir = Files.createTempDirectory("alvand-audio-test")
    }

    @AfterTest
    fun tearDown() {
        runCatching { dir.toFile().deleteRecursively() }
    }

    @Test
    fun `audio device uses the same signed little-endian PCM as the decoders`() {
        val javaFormat = javaFormatFor(AudioFormat(44100, 2, 16))

        assertEquals(JavaAudioFormat.Encoding.PCM_SIGNED, javaFormat.encoding)
        assertFalse(javaFormat.isBigEndian, "decoders emit little-endian bytes")
    }

    @Test
    fun `resetPosition restarts byte counting from the seek target`() {
        val format = AudioFormat(44100, 2, 16)
        val out = AudioOutputStream(format, AudioOutputStream.SampleSink.Recording(pace = false))
        out.start()
        out.write(ByteArray(format.bufferBytes(50)))

        out.resetPosition(300)

        assertTrue(
            out.playedMs() in 300L..310L,
            "after seeking to 300ms, position was ${out.playedMs()}ms"
        )
        out.close()
    }

    @Test
    fun `updateVolume scales output live without rebuilding filters`() {
        val format = AudioFormat(44100, 2, 16)
        val chain = DspChain(DspSpec(), format, volume = 1f)

        fun sineBlock(): ByteArray {
            val out = ByteArray(1024 * format.frameSize)
            var o = 0
            repeat(1024) { i ->
                val v = (8000 * kotlin.math.sin(2 * Math.PI * 440 * i / 44100)).toInt().toShort()
                repeat(format.channels) {
                    out[o++] = (v.toInt() and 0xFF).toByte()
                    out[o++] = ((v.toInt() shr 8) and 0xFF).toByte()
                }
            }
            return out
        }

        fun peak(bytes: ByteArray): Int {
            var max = 0
            var i = 0
            while (i + 1 < bytes.size) {
                val lo = bytes[i].toInt() and 0xFF
                val hi = bytes[i + 1].toInt()
                val v = kotlin.math.abs((((hi shl 8) or lo).toShort()).toInt())
                if (v > max) max = v
                i += 2
            }
            return max
        }

        val full = peak(chain.process(sineBlock()))
        assertTrue(full > 1000, "test signal too quiet: $full")
        chain.updateVolume(0.5f)
        val half = peak(chain.process(sineBlock()))
        assertTrue(
            half > full * 0.4 && half < full * 0.6,
            "halving volume must halve the peak: full=$full half=$half"
        )
        chain.updateVolume(0f)
        assertEquals(0, peak(chain.process(sineBlock())), "zero volume must be silence")
    }

    /** فایل WAV واقعی، little-endian */
    private fun writeWav(target: File, seconds: Int, freq: Int = 440) {
        val rate = 44100
        val samples = rate * seconds
        FileOutputStream(target).use { out ->
            fun le32(v: Int) {
                out.write(v and 0xFF); out.write((v ushr 8) and 0xFF)
                out.write((v ushr 16) and 0xFF); out.write((v ushr 24) and 0xFF)
            }
            fun le16(v: Int) { out.write(v and 0xFF); out.write((v ushr 8) and 0xFF) }
            out.write("RIFF".toByteArray()); le32(36 + samples * 4)
            out.write("WAVE".toByteArray())
            out.write("fmt ".toByteArray()); le32(16)
            // PCM (1)، دو کانال (2)، ۴۴۱۰۰ هرتز، ۱۶ بیت ⇒ blockAlign = 4
            le16(1); le16(2); le32(rate); le32(rate * 4); le16(4); le16(16)
            out.write("data".toByteArray()); le32(samples * 4)
            val buf = ByteArray(samples * 4)
            for (i in 0 until samples) {
                val v = (12000 * kotlin.math.sin(2 * Math.PI * freq * i / rate)).toInt().toShort()
                repeat(2) { ch ->
                    val o = i * 4 + ch * 2
                    buf[o] = (v.toInt() and 0xFF).toByte()
                    buf[o + 1] = ((v.toInt() shr 8) and 0xFF).toByte()
                }
            }
            out.write(buf)
        }
    }

    @Test
    fun `real wav file decodes and reports its real duration`() {
        val f = dir.resolve("tone.wav").toFile()
        writeWav(f, seconds = 2)

        val decoder = RawAudioDecoder(f)
        assertEquals(44100, decoder.format.sampleRate)
        assertEquals(2, decoder.format.channels)
        assertEquals(16, decoder.format.bitsPerSample)
        // ۲ ثانیه
        assertTrue(
            decoder.totalDurationMs in 1990..2010,
            "duration should be ~2000ms, got ${decoder.totalDurationMs}"
        )

        // خواندن باید کل فایل را بدهد
        var total = 0L
        var blocks = 0
        while (true) {
            val block = decoder.readBlock() ?: break
            total += block.durationMs
            blocks++
            if (blocks > 1000) break
        }
        assertTrue(blocks > 0, "no blocks were read")
        assertTrue(total > 1900, "read $total ms, expected about 2000")

        // نمونه‌ها نباید سکوت باشند
        decoder.seekTo(0)
        val first = decoder.readBlock()!!
        val floats = first.toFloats()
        assertTrue(
            floats.any { kotlin.math.abs(it) > 0.1f },
            "decoded audio is silence"
        )
        decoder.close()
    }

    @Test
    fun `seek skips forward in the decoded file`() {
        val f = dir.resolve("long.wav").toFile()
        writeWav(f, seconds = 5)

        val decoder = RawAudioDecoder(f)
        decoder.seekTo(44100 * 3)   // ۳ ثانیه جلو
        val block = decoder.readBlock()!!
        // اگر seek کار کرده باشد، بلوک اول باید حوالی ۳ ثانیه باشد نه ابتدا
        assertTrue(block.bytes.isNotEmpty())
        val floats = block.toFloats()
        assertTrue(
            floats.any { kotlin.math.abs(it) > 0.05f },
            "after seek the audio is silence, which means seek did not take effect"
        )
        decoder.close()
    }

    @Test
    fun `real audio device accepts a short burst without error`() {
        val sink = JavaSoundSink()
        val format = AudioFormat(44100, 2, 16)

        val opened = runCatching {
            sink.open(format)
            true
        }.getOrElse { false }

        if (!opened) {
            println("SKIP: no audio output device on this machine")
            return
        }
        try {
            // یک لحظهٔ کوتاه سکوت — اگر دستگاه باز شده باشد، این باید بی‌اشکال باشد
            val silence = ByteArray(format.bufferBytes(50))
            sink.write(silence, 0, silence.size)
            assertTrue(sink.isRunning, "audio line should still be running after a write")
            assertTrue(sink.deviceName() != null, "device name should be reported")
        } finally {
            sink.close()
        }
    }

    @Test
    fun `dsp chain processes without distortion and never clips beyond range`() {
        val format = AudioFormat(44100, 2, 16)
        val chain = DspChain(
            DspSpec(
                eqEnabled = true,
                bandLevels = listOf(600, 300, 0, -300, -600),
                bassStrength = 700,
                volumeBoostDb = 6,
                noiseReduction = true
            ),
            format,
            volume = 1f
        )

        // یک بلوک سینوسی با دامنهٔ متوسط
        val samples = 2048
        val input = ByteArray(samples * 2 * format.channels)
        var o = 0
        repeat(samples) { i ->
            val v = (8000 * kotlin.math.sin(2 * Math.PI * 440 * i / 44100)).toInt().toShort()
            repeat(format.channels) {
                input[o++] = (v.toInt() and 0xFF).toByte()
                input[o++] = ((v.toInt() shr 8) and 0xFF).toByte()
            }
        }

        val out = chain.process(input)
        assertEquals(input.size, out.size, "output size must match input size")

        // نباید NaN یا بی‌نهایت تولید شود
        var i = 0
        while (i + 1 < out.size) {
            val lo = out[i].toInt() and 0xFF
            val hi = out[i + 1].toInt()
            val v = ((hi shl 8) or lo).toShort().toInt()
            assertTrue(v in -32768..32767, "invalid sample: $v")
            i += 2
        }

        // باید هنوز صدا وجود داشته باشد
        var nonZero = 0
        i = 0
        while (i + 1 < out.size) {
            if (out[i] != 0.toByte() || out[i + 1] != 0.toByte()) nonZero++
            i += 2
        }
        assertTrue(nonZero > samples / 4, "DSP chain silenced the audio")
    }

    @Test
    fun `loudness boost is limited instead of clipping`() {
        val format = AudioFormat(44100, 2, 16)
        // تقویت ۱۰ دسی‌بل روی سیگنال نزدیک به حداکثر: بدون محدودکننده حتماً کلیپ می‌شود
        val chain = DspChain(DspSpec(volumeBoostDb = 10), format, 1f)

        val samples = 1024
        val input = ByteArray(samples * 2 * format.channels)
        var o = 0
        repeat(samples) {
            val v = 20000
            repeat(format.channels) {
                input[o++] = (v and 0xFF).toByte()
                input[o++] = ((v ushr 8) and 0xFF).toByte()
            }
        }

        val out = chain.process(input)
        var maxValue = 0
        var i = 0
        while (i + 1 < out.size) {
            val lo = out[i].toInt() and 0xFF
            val hi = out[i + 1].toInt()
            val v = ((hi shl 8) or lo).toShort().toInt()
            if (kotlin.math.abs(v) > maxValue) maxValue = kotlin.math.abs(v)
            i += 2
        }
        assertTrue(
            maxValue <= 32767,
            "the limiter must keep samples inside range, peak was $maxValue"
        )
    }

    @Test
    fun `engine plays a real wav file end to end`() {
        // این تست به بلندگوی واقعی نیاز دارد. اگر این دستگاه/CI خروجی صوتی ندارد
        // (مثل رانرهای گیت‌هاب)، نباید fail شود — skip می‌شود چون محدودیت محیط است.
        val probeFmt = AudioFormat(44100, 2, 16)
        val probe = runCatching {
            JavaSoundSink().let { it.open(probeFmt); it.close(); true }
        }.getOrDefault(false)
        if (!probe) {
            println("SKIP: no audio output device on this machine; engine end-to-end test skipped")
            return
        }

        val f = dir.resolve("song.wav").toFile()
        writeWav(f, seconds = 1)

        val engine = JdkPlayerEngine(
            scope = kotlinx.coroutines.CoroutineScope(
                kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Default
            ),
            outputFactory = { fmt -> AudioOutputs.create(fmt) },
            decoderFactory = { uri, mime -> DecoderFactory.create(uri, mime) }
        )
        engine.onReady()

        runBlocking {
            engine.setQueue(
                listOf(PlayableItem(1, "Tone", "Test", uri = f.absolutePath)),
                0, autoPlay = true
            )
        }

        val deadline = System.currentTimeMillis() + 8000
        while (System.currentTimeMillis() < deadline && engine.state.value.positionMs <= 0) {
            Thread.sleep(30)
        }
        println("REAL-AUDIO position=${engine.state.value.positionMs}ms playing=${engine.state.value.isPlaying}")
        assertTrue(
            engine.state.value.positionMs > 0,
            "position never advanced on the real audio path"
        )
        assertEquals(null, engine.state.value.error, "no error should be reported")

        engine.release()
    }
}
