package com.alvand.player.player

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import java.nio.file.Path
import kotlin.math.sin
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * تست بازتولید باگ کاربر: وقتی آهنگی در حال پخش است و کاربر آهنگ دیگری را از
 * کتابخانه انتخاب می‌کند (setQueue با autoPlay=true)، پخش باید واقعاً به
 * بایت‌های آهنگ جدید سوییچ کند — نه فقط برچسب UI را عوض کند.
 *
 * هر آهنگ با فرکانس متمایز سنتز می‌شود (۴۴۰ و ۸۸۰ هرتز)؛ تعداد عبور از صفر
 * در دنبالهٔ PCM ضبط‌شده فرکانس مؤثر را می‌دهد و نشان می‌دهد رمزگشای جدید
 * واقعاً جای رمزگشای قدیمی گرفته است.
 */
class SwitchQueueTest {

    private lateinit var dir: Path
    private lateinit var scope: CoroutineScope
    private val sinks = mutableListOf<AudioOutputStream.SampleSink.Recording>()

    @BeforeTest
    fun setUp() {
        dir = Files.createTempDirectory("switch-test")
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }

    @AfterTest
    fun tearDown() {
        scope.cancel()
        runCatching { dir.toFile().deleteRecursively() }
    }

    private fun newEngine(): JdkPlayerEngine = JdkPlayerEngine(
        scope = scope,
        outputFactory = { fmt ->
            val sink = AudioOutputStream.SampleSink.Recording(pace = true)
            synchronized(sinks) { sinks.add(sink) }
            AudioOutputStream(format = fmt, sink = sink)
        },
        decoderFactory = { uri, _ ->
            SineDecoder(
                durationMs = 30_000L,
                frequency = if (uri == "song:2") 880.0 else 440.0
            )
        }
    )

    /** فرکانس مؤثر از روی دنبالهٔ PCM (کانال اول) — از تعداد عبور از صفر */
    private fun effectiveHz(bytes: ByteArray, sampleRate: Int): Double {
        val samples = ArrayList<Short>(bytes.size / 4)
        var i = 0
        while (i + 3 < bytes.size) {
            val lo = bytes[i].toInt() and 0xFF
            val hi = bytes[i + 1].toInt()
            samples.add(((hi shl 8) or lo).toShort())
            i += 4 // استریو ۱۶بیتی: هر فریم ۴ بایت، کانال اول را نگه می‌داریم
        }
        if (samples.size < 100) return 0.0
        var crossings = 0
        for (j in 1 until samples.size) {
            if ((samples[j - 1] < 0 && samples[j] >= 0) || (samples[j - 1] >= 0 && samples[j] < 0)) crossings++
        }
        val seconds = samples.size.toDouble() / sampleRate
        return crossings / (2.0 * seconds)
    }

    private fun waitUntil(timeoutMs: Long, predicate: () -> Boolean): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (predicate()) return true
            Thread.sleep(20)
        }
        return predicate()
    }

    @Test
    fun switchingQueueWhilePlayingActuallyPlaysNewAudio() {
        val engine = newEngine()
        engine.onReady()
        runBlocking {
            engine.setQueue(listOf(PlayableItem(1, "A", "art", uri = "song:1")), 0, autoPlay = true)
        }
        assertTrue(
            waitUntil(5000) { engine.state.value.isPlaying && engine.state.value.positionMs > 0 },
            "first song never started playing; state=${engine.state.value}"
        )
        Thread.sleep(600) // کمی از آهنگ اول واقعاً پخش شود

        // حالا کاربر آهنگ دیگری را انتخاب می‌کند — setQueue جدید
        runBlocking {
            engine.setQueue(listOf(PlayableItem(2, "B", "art", uri = "song:2")), 0, autoPlay = true)
        }
        assertTrue(
            engine.state.value.current?.id == 2L,
            "current should be song 2, was ${engine.state.value.current}"
        )
        assertTrue(
            waitUntil(3000) { engine.state.value.positionMs > 0 && engine.state.value.isPlaying },
            "new song should keep playing; state=${engine.state.value}"
        )
        Thread.sleep(600) // کمی از آهنگ دوم واقعاً پخش شود

        // مهم: بایت‌های واقعیِ پس از سوییچ باید از رمزگشای جدید (۸۸۰ هرتز) بیایند
        val sink = synchronized(sinks) { sinks.lastOrNull() }
        assertTrue(sink != null, "no recording sink attached")
        val bytes = sink!!.bytes()
        assertTrue(bytes.isNotEmpty(), "no audio bytes recorded at all")
        val tailLen = minOf(bytes.size, 44100 * 2 * 2) // نیم‌ثانیهٔ آخر
        val tail = ByteArray(tailLen)
        System.arraycopy(bytes, bytes.size - tailLen, tail, 0, tailLen)
        val hz = effectiveHz(tail, 44100)
        assertTrue(
            hz > 700.0,
            "audio tail frequency is ${"%.0f".format(hz)}Hz — expected ~880Hz (song 2); engine kept playing the old decoder!"
        )
    }
}
