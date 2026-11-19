package com.alvand.player.player

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import java.nio.file.Path
import kotlin.concurrent.thread
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * موتور پخش، سرتاسری و بدون نمایشگر.
 *
 * **صدای واقعی پخش نمی‌شود** — به‌جایش یک مقصد ضبط‌کننده گذاشته می‌شود تا بتوان
 * بایت‌هایی را که موتور تولید کرده دقیق بررسی کرد. این تنها راه قابل‌اعتماد برای
 * تست در CI است.
 */
class JdkPlayerEngineTest {

    private lateinit var dir: Path
    private lateinit var scope: CoroutineScope
    private val sinks = mutableListOf<AudioOutputStream.SampleSink.Recording>()

    @BeforeTest
    fun setUp() {
        dir = Files.createTempDirectory("alvand-engine-test")
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }

    @AfterTest
    fun tearDown() {
        scope.cancel()
        runCatching { dir.toFile().deleteRecursively() }
    }

    /**
     * موتور با مقصد ضبط‌کننده.
     *
     * `pace = true` باعث می‌شود مقصد با سرعت واقعی (مثل یک دستگاه واقعی) بایت بگیرد.
     * بدون آن، آهنگ آزمایشی چنان سریع تمام می‌شود که هیچ‌وقت «در حال پخش» را
     * نمی‌بینیم و تست‌ها بی‌معنی می‌شوند.
     */
    private fun newEngine(pace: Boolean = true): JdkPlayerEngine = JdkPlayerEngine(
        scope = scope,
        outputFactory = { fmt ->
            val sink = AudioOutputStream.SampleSink.Recording(pace)
            synchronized(sinks) { sinks.add(sink) }
            AudioOutputStream(
                format = fmt,
                sink = sink
            )
        },
        decoderFactory = { uri, _ ->
            if (uri.startsWith("bad:")) null
            else SineDecoder(durationMs = 400L, frequency = 440.0)
        }
    )

    private fun item(id: Long, title: String = "T$id") =
        PlayableItem(id, title, "Artist", uri = "test:$id")

    /** صبر کردن تا شرط برقرار شود */
    private fun waitUntil(timeoutMs: Long, predicate: () -> Boolean): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (predicate()) return true
            Thread.sleep(20)
        }
        return predicate()
    }

    private fun recordedBytes(): Int =
        synchronized(sinks) { sinks.sumOf { it.bytes().size } }

    // ---------------- صف و پخش ----------------

    @Test
    fun `setQueue publishes the current item`() = runBlocking {
        val engine = newEngine()
        engine.onReady()
        engine.setQueue(listOf(item(1), item(2)), startIndex = 0, autoPlay = false)

        val st = engine.state.value
        assertEquals(2, st.queue.size)
        assertEquals(0, st.index)
        assertEquals("T1", st.current?.title)
        engine.release()
    }

    @Test
    fun `primeQueueFromLibrary loads an empty queue without autoplay`() = runBlocking {
        val engine = newEngine()
        engine.onReady()
        val primed = engine.primeQueueFromLibrary(listOf(item(1), item(2)))

        val st = engine.state.value
        assertTrue(primed, "an empty queue should be primed from the library")
        assertEquals(2, st.queue.size)
        assertEquals(0, st.index)
        assertEquals("T1", st.current?.title)
        assertTrue(!st.isPlaying, "priming the queue must not start playback")
        engine.release()
    }

    @Test
    fun `primeQueueFromLibrary never replaces an existing queue`() = runBlocking {
        val engine = newEngine()
        engine.onReady()
        engine.setQueue(listOf(item(1)), 0, autoPlay = false)
        val primed = engine.primeQueueFromLibrary(listOf(item(2), item(3)))

        assertTrue(!primed, "an existing queue must win over a later scan result")
        assertEquals(listOf(1L), engine.state.value.queue.map { it.id })
        engine.release()
    }

    @Test
    fun `primeQueueFromLibrary ignores an empty library`() = runBlocking {
        val engine = newEngine()
        engine.onReady()
        val primed = engine.primeQueueFromLibrary(emptyList())

        assertTrue(!primed, "there is nothing to prime")
        assertTrue(engine.state.value.queue.isEmpty())
        engine.release()
    }

    @Test
    fun `play produces actual audio bytes`() = runBlocking {
        val engine = newEngine()
        engine.onReady()
        engine.setQueue(listOf(item(1)), 0, autoPlay = true)

        assertTrue(waitUntil(5000) { recordedBytes() > 0 }, "engine produced no audio data")
        assertTrue(engine.state.value.isPlaying, "engine should report playing")
        engine.release()
    }

    @Test
    fun `audio bytes are not all silence`() = runBlocking {
        val engine = newEngine()
        engine.onReady()
        engine.setQueue(listOf(item(1)), 0, autoPlay = true)
        assertTrue(waitUntil(5000) { recordedBytes() > 4000 })

        val data = synchronized(sinks) { sinks.flatMap { it.bytes().toList() } }
        // یک سینوس ۴۴۰ هرتز قطعاً باید نمونه‌های غیرصفر داشته باشد
        val nonZero = data.count { it != 0.toByte() }
        assertTrue(nonZero > data.size / 10, "audio looks like silence")
        engine.release()
    }

    @Test
    fun `duration is reported from the decoder`() = runBlocking {
        val engine = newEngine()
        engine.onReady()
        engine.setQueue(listOf(item(1)), 0, autoPlay = false)
        assertTrue(
            waitUntil(5000) { engine.state.value.durationMs == 400L },
            "duration not picked up, got ${engine.state.value.durationMs}"
        )
        engine.release()
    }

    @Test
    fun `next advances to the following song`() = runBlocking {
        val engine = newEngine()
        engine.onReady()
        engine.setQueue(listOf(item(1), item(2)), 0, autoPlay = true)
        assertTrue(waitUntil(5000) { engine.state.value.index == 0 })
        engine.next()
        assertTrue(waitUntil(5000) { engine.state.value.index == 1 })
        assertEquals("T2", engine.state.value.current?.title)
        engine.release()
    }

    @Test
    fun `selecting the current card does not restart its decoder`() = runBlocking {
        val engine = newEngine()
        engine.onReady()
        engine.setQueue(listOf(item(1), item(2)), 0, autoPlay = true)
        assertTrue(waitUntil(5000) { recordedBytes() > 0 })
        val outputsBeforeReselect = synchronized(sinks) { sinks.size }

        engine.playIndex(0)

        assertEquals("T1", engine.state.value.current?.title)
        assertEquals(
            outputsBeforeReselect,
            synchronized(sinks) { sinks.size },
            "tapping the current card must not rebuild playback from scratch"
        )
        assertTrue(engine.state.value.isPlaying, "tapping the current card must not pause it")
        engine.release()
    }

    @Test
    fun `next replaces the decoder and output instead of continuing the old song`() = runBlocking {        val engine = newEngine()
        engine.onReady()
        engine.setQueue(listOf(item(1), item(2)), 0, autoPlay = true)
        assertTrue(waitUntil(5000) { recordedBytes() > 0 })
        val outputsBeforeNext = synchronized(sinks) { sinks.size }

        engine.next()

        assertTrue(waitUntil(5000) { engine.state.value.current?.title == "T2" })
        assertEquals(
            outputsBeforeNext + 1,
            synchronized(sinks) { sinks.size },
            "the new track needs a new decoder/output; otherwise the old audio keeps playing"
        )
        val newSink = synchronized(sinks) { sinks.last() }
        assertTrue(
            waitUntil(5000) { newSink.bytes().isNotEmpty() },
            "the new output never received the new track"
        )
        assertEquals(400L, engine.state.value.durationMs, "the progress bar must use the new track")
        engine.release()
    }


    @Test
    fun `next and previous walk through the queue in order`() = runBlocking {
        val engine = newEngine()
        engine.onReady()
        engine.setQueue(listOf(item(1), item(2), item(3)), 0, autoPlay = true)
        assertTrue(waitUntil(5000) { recordedBytes() > 0 })

        engine.next()
        assertEquals("T2", engine.state.value.current?.title)
        assertEquals(1, engine.state.value.index)
        // موقعیت باید از ابتدای آهنگ تازه شروع شده باشد، نه از ادامهٔ آهنگ قبلی.
        // نخ پخش ممکن است تا رسیدن به این خط یک بلوک جلو رفته باشد، پس بازه می‌دهیم.
        assertTrue(
            engine.state.value.positionMs in 0L..300L,
            "position was not restarted, got ${engine.state.value.positionMs}ms"
        )

        engine.next()
        assertEquals("T3", engine.state.value.current?.title)
        assertEquals(2, engine.state.value.index)
        assertTrue(
            engine.state.value.positionMs in 0L..300L,
            "position was not restarted, got ${engine.state.value.positionMs}ms"
        )

        engine.previous()
        assertEquals("T2", engine.state.value.current?.title)
        assertEquals(1, engine.state.value.index)

        engine.previous()
        assertEquals("T1", engine.state.value.current?.title)
        assertEquals(0, engine.state.value.index)
        engine.release()
    }

    @Test
    fun `repeat all wraps around both ends`() = runBlocking {
        val engine = newEngine()
        engine.onReady()
        engine.setQueue(listOf(item(1), item(2), item(3)), 0, autoPlay = true)
        engine.cycleRepeat()
        assertTrue(waitUntil(5000) { recordedBytes() > 0 })

        engine.next()
        assertEquals("T2", engine.state.value.current?.title)
        engine.next()
        assertEquals("T3", engine.state.value.current?.title)
        engine.next()
        assertEquals("T1", engine.state.value.current?.title)
        assertEquals(0, engine.state.value.index)

        engine.previous()
        assertEquals("T3", engine.state.value.current?.title)
        assertEquals(2, engine.state.value.index)
        engine.release()
    }

    @Test
    fun `repeat one restarts the same track on next`() = runBlocking {
        val engine = newEngine()
        engine.onReady()
        engine.setQueue(listOf(item(1), item(2)), 0, autoPlay = true)
        assertTrue(waitUntil(5000) { engine.state.value.positionMs >= 200L })
        val halfway = engine.state.value.positionMs

        engine.cycleRepeat()
        engine.cycleRepeat()
        engine.next()

        assertEquals("T1", engine.state.value.current?.title)
        assertEquals(0, engine.state.value.index)
        assertTrue(
            engine.state.value.positionMs < halfway,
            "repeat-one next must restart the track, position=${engine.state.value.positionMs}ms"
        )
        engine.release()
    }

    @Test
    fun `rapid next calls are serialized instead of landing on the same song`() = runBlocking {
        val engine = newEngine()
        engine.onReady()
        engine.setQueue(listOf(item(1), item(2), item(3)), 0, autoPlay = true)
        assertTrue(waitUntil(5000) { recordedBytes() > 0 })
        val outputsBeforeNext = synchronized(sinks) { sinks.size }

        val first = async { engine.next() }
        val second = async { engine.next() }
        first.await()
        second.await()

        assertTrue(waitUntil(5000) { engine.state.value.current?.title == "T3" })
        assertEquals(
            outputsBeforeNext + 2,
            synchronized(sinks) { sinks.size },
            "two next commands must produce two separate track transitions"
        )
        engine.release()
    }

    @Test
    fun `next at the end with repeat off stops instead of wrapping`() = runBlocking {
        val engine = newEngine()
        engine.onReady()
        engine.setQueue(listOf(item(1)), 0, autoPlay = true)
        assertTrue(waitUntil(5000) { engine.state.value.isPlaying })

        engine.next()
        assertTrue(
            waitUntil(5000) { !engine.state.value.isPlaying },
            "should stop at the end"
        )
        assertEquals(0, engine.state.value.index, "index must not wrap")
        engine.release()
    }

    @Test
    fun `next at the end with repeat all wraps`() = runBlocking {
        val engine = newEngine()
        engine.onReady()
        engine.setQueue(listOf(item(1), item(2)), 1, autoPlay = true)
        assertTrue(waitUntil(5000) { engine.state.value.index == 1 })

        engine.cycleRepeat()
        engine.next()
        assertTrue(
            waitUntil(5000) { engine.state.value.index == 0 },
            "repeat all should wrap to the start"
        )
        engine.release()
    }

    @Test
    fun `pause stops the audio but keeps the song`() = runBlocking {
        val engine = newEngine()
        engine.onReady()
        engine.setQueue(listOf(item(1)), 0, autoPlay = true)
        assertTrue(waitUntil(5000) { recordedBytes() > 0 })

        engine.pause()
        assertTrue(!engine.state.value.isPlaying)
        val bytesAtPause = recordedBytes()
        Thread.sleep(200)
        assertEquals(bytesAtPause, recordedBytes(), "audio kept flowing after pause")
        assertEquals("T1", engine.state.value.current?.title, "song must stay loaded")
        engine.release()
    }

    @Test
    fun `toggle flips between play and pause`() = runBlocking {
        val engine = newEngine()
        engine.onReady()
        engine.setQueue(listOf(item(1)), 0, autoPlay = false)

        engine.toggle()
        assertTrue(waitUntil(5000) { engine.state.value.isPlaying })
        engine.toggle()
        assertTrue(!engine.state.value.isPlaying)
        engine.release()
    }

    // ---------------- seek ----------------

    @Test
    fun `seek clamps into the song bounds`() = runBlocking {
        val engine = newEngine()
        engine.onReady()
        engine.setQueue(listOf(item(1)), 0, autoPlay = true)
        assertTrue(waitUntil(5000) { engine.state.value.durationMs == 400L })

        engine.seekTo(100_000)
        assertTrue(
            waitUntil(5000) { engine.state.value.positionMs == 400L },
            "seek past the end should clamp, got ${engine.state.value.positionMs}"
        )

        engine.seekTo(-5000)
        assertTrue(waitUntil(5000) { engine.state.value.positionMs == 0L })
        engine.release()
    }

    @Test
    fun `seek keeps the progress bar at the target instead of restarting it`() = runBlocking {
        val engine = newEngine()
        engine.onReady()
        engine.setQueue(listOf(item(1)), 0, autoPlay = true)
        assertTrue(waitUntil(5000) { engine.state.value.durationMs == 400L })

        engine.seekTo(300)
        // به نخ پخش فرصت می‌دهیم درخواست را اعمال کند؛ اگر شمارندهٔ خروجی مبنا
        // نداشته باشد، همین‌جا موقعیت به نزدیکی صفر برمی‌گردد.
        Thread.sleep(300)
        val position = engine.state.value.positionMs
        assertTrue(
            position in 300L..400L,
            "seek moved the audio to 300ms but the bar fell to ${position}ms"
        )
        engine.release()
    }

    @Test
    fun `seek into missing data stays on the track instead of skipping`() = runBlocking {
        // رمزگشایی که بلافاصله ته می‌کشد ولی مدت‌زمان واقعی اعلام می‌کند: شبیه‌سازیِ
        // حالتی که تخمین مدت‌زمان از دادهٔ واقعی بلندتر است و کاربر وسط آهنگ seek می‌کند.
        val dryDecoder = object : AudioDecoder {
            override val format = AudioFormat(44100, 2, 16)
            override val totalDurationMs = 400L
            override fun readBlock(maxFrames: Int): PcmBuffer? = null
            override fun close() {}
        }
        val engine = JdkPlayerEngine(
            scope = scope,
            outputFactory = { fmt -> AudioOutputStream(format = fmt, sink = AudioOutputStream.SampleSink.Recording(false)) },
            decoderFactory = { _, _ -> dryDecoder }
        )
        engine.onReady()
        engine.setQueue(listOf(item(1), item(2)), 0, autoPlay = false)
        assertEquals("T1", engine.state.value.current?.title)

        engine.seekTo(100)
        engine.play()
        assertTrue(
            waitUntil(5000) { !engine.state.value.isPlaying && engine.state.value.positionMs == 400L },
            "seek into missing data must stop at the end, state=${engine.state.value}"
        )
        assertEquals(0, engine.state.value.index, "must not advance to the next track")
        assertEquals("T1", engine.state.value.current?.title)
        engine.release()
    }

    @Test
    fun `setVolume applies live without restarting the track`() = runBlocking {
        val engine = newEngine()
        engine.onReady()
        engine.setQueue(listOf(item(1)), 0, autoPlay = true)
        engine.cycleRepeat() // تکرار تا آهنگ تمام نشود و فقط اثر ولوم دیده شود
        assertTrue(waitUntil(5000) { recordedBytes() > 30000 })

        engine.setVolume(0f)
        Thread.sleep(500)
        val data = synchronized(sinks) { sinks.flatMap { it.bytes().toList() } }
        val tail = data.takeLast(20000)
        assertTrue(tail.isNotEmpty())
        assertTrue(
            tail.all { it == 0.toByte() },
            "bytes written after mute must be silence"
        )
        assertTrue(engine.state.value.isPlaying, "mute must not stop playback")
        engine.release()
    }

    @Test
    fun `ab loop cycles A then B then cleared`() = runBlocking {
        val engine = newEngine()
        engine.onReady()
        engine.setQueue(listOf(item(1)), 0, autoPlay = false)
        assertEquals(null, engine.state.value.loopA)
        assertEquals(null, engine.state.value.loopB)

        engine.seekTo(100)
        engine.cycleAbLoop()
        assertEquals(100L, engine.state.value.loopA)
        assertEquals(null, engine.state.value.loopB)

        engine.seekTo(300)
        engine.cycleAbLoop()
        assertEquals(100L, engine.state.value.loopA)
        assertEquals(300L, engine.state.value.loopB)

        engine.cycleAbLoop()
        assertEquals(null, engine.state.value.loopA)
        assertEquals(null, engine.state.value.loopB)
        engine.release()
    }

    @Test
    fun `ab loop wraps playback back to A`() = runBlocking {
        val engine = newEngine()
        engine.onReady()
        engine.setQueue(listOf(item(1)), 0, autoPlay = true)
        assertTrue(waitUntil(5000) { recordedBytes() > 0 })

        engine.seekTo(50)
        engine.cycleAbLoop() // A ≈ 50
        engine.seekTo(300)
        engine.cycleAbLoop() // B ≈ 300
        assertTrue(engine.state.value.loopB != null)

        // اولین برگشت: موقعیت باید دوباره پایین بیاید
        assertTrue(
            waitUntil(3000) { engine.state.value.positionMs < 150 },
            "first wrap back to A never happened"
        )
        // اگر حلقه کار کند، موقعیت هیچ‌وقت به انتها نمی‌رسد و پخش نمی‌ایستد؛
        // اگر خراب باشد، ظرف ۴۰۰ میلی‌ثانیه به ۴۰۰ می‌رسد و می‌ایستد.
        Thread.sleep(600)
        val pos = engine.state.value.positionMs
        assertTrue(pos < 350L, "AB loop escaped: position=$pos")
        assertTrue(engine.state.value.isPlaying, "AB loop must keep playing")

        engine.clearAbLoop()
        assertEquals(null, engine.state.value.loopA)
        assertEquals(null, engine.state.value.loopB)
        engine.release()
    }

    @Test
    fun `replay gain attenuates a loud track`() = runBlocking {
        val loud = PlayableItem(1, "Loud", "A", uri = "test:1", replayGainDb = -60f)
        val engine = JdkPlayerEngine(
            scope = scope,
            outputFactory = { fmt ->
                val sink = AudioOutputStream.SampleSink.Recording(false)
                synchronized(sinks) { sinks.add(sink) }
                AudioOutputStream(format = fmt, sink = sink)
            },
            decoderFactory = { _, _ -> SineDecoder(durationMs = 2000L, frequency = 440.0) }
        )
        engine.onReady()
        engine.setQueue(listOf(loud), 0, autoPlay = true)
        assertTrue(waitUntil(5000) { recordedBytes() > 20000 })
        val data = synchronized(sinks) { sinks.flatMap { it.bytes().toList() } }
        // پیک روی نمونه‌های ۱۶ بیتی، نه بایت‌ها (بایت همیشه تا ۱۲۸ می‌رسد)
        var peak = 0
        var i = 0
        while (i + 1 < data.size) {
            val lo = data[i].toInt() and 0xFF
            val hi = data[i + 1].toInt()
            val v = kotlin.math.abs((((hi shl 8) or lo).toShort()).toInt())
            if (v > peak) peak = v
            i += 2
        }
        // ‎-60dB با کف امن ۰٫۰۵ (≈−۲۶dB) محدود می‌شود: ۲۰۰۰۰×۰٫۰۵ ≈ ۱۰۰۰
        assertTrue(peak < 2000, "replay gain -60dB must nearly silence the track, peak=$peak")
        engine.release()
    }

    @Test
    fun `position advances while playing`() = runBlocking {        val engine = newEngine()
        engine.onReady()
        engine.setQueue(listOf(item(1)), 0, autoPlay = true)

        assertTrue(
            waitUntil(3000) { engine.state.value.positionMs > 0 },
            "position never advanced, still ${engine.state.value.positionMs}"
        )
        engine.release()
    }

    // ---------------- تکرار و شافل ----------------

    @Test
    fun `cycleRepeat walks through the three modes`() {
        val engine = newEngine()
        engine.cycleRepeat()
        assertEquals(RepeatMode.ALL, engine.state.value.repeatMode)
        engine.cycleRepeat()
        assertEquals(RepeatMode.ONE, engine.state.value.repeatMode)
        engine.cycleRepeat()
        assertEquals(RepeatMode.OFF, engine.state.value.repeatMode)
        engine.release()
    }

    @Test
    fun `shuffle flag is reflected in state`() {
        val engine = newEngine()
        engine.setShuffle(true)
        assertTrue(engine.state.value.shuffle)
        engine.setShuffle(false)
        assertTrue(!engine.state.value.shuffle)
        engine.release()
    }

    // ---------------- خطا ----------------

    @Test
    fun `undecodable file reports an error without throwing`() = runBlocking {
        val engine = newEngine()
        engine.onReady()
        engine.setQueue(
            listOf(PlayableItem(1, "Bad", "A", uri = "bad:1")), 0, autoPlay = true
        )
        assertTrue(
            waitUntil(5000) { engine.state.value.error != null },
            "an undecodable file must report an error"
        )
        assertTrue(!engine.state.value.isPlaying)
        engine.release()
    }

    @Test
    fun `broken song is skipped and the next one plays`() = runBlocking {
        val engine = newEngine()
        engine.onReady()
        engine.setQueue(
            listOf(
                PlayableItem(1, "Broken", "A", uri = "bad:1"),
                item(2, "Good")
            ),
            0, autoPlay = true
        )
        // سیاست «رد کردن فایل خراب» باید ما را به آهنگ سالم برساند
        assertTrue(
            waitUntil(5000) { engine.state.value.current?.title == "Good" },
            "did not skip the broken file, current = ${engine.state.value.current?.title}"
        )
        assertTrue(waitUntil(5000) { recordedBytes() > 0 })
        engine.release()
    }

    @Test
    fun `empty queue is accepted without error`() = runBlocking {
        val engine = newEngine()
        engine.onReady()
        engine.setQueue(emptyList(), 0, autoPlay = true)
        assertEquals(-1, engine.state.value.index)
        assertNull(engine.state.value.current)
        engine.release()
    }

    @Test
    fun `start index out of range is rejected safely`() = runBlocking {
        val engine = newEngine()
        engine.onReady()
        engine.setQueue(listOf(item(1)), startIndex = 99, autoPlay = true)
        assertTrue(!engine.state.value.isPlaying, "an invalid index must not start playback")
        engine.release()
    }

    // ---------------- صف معلق ----------------

    @Test
    fun `queue set before ready is applied when the engine becomes ready`() = runBlocking {
        val engine = newEngine()
        // بدون onReady باید در صف معلق بماند و پخش نکند
        engine.setQueue(listOf(item(1), item(2)), 1, autoPlay = true)
        assertTrue(!engine.state.value.isPlaying, "must not play before ready")
        assertEquals(1, engine.state.value.index, "the requested index is remembered")

        engine.onReady()
        assertTrue(waitUntil(5000) { engine.state.value.isPlaying })
        assertEquals("T2", engine.state.value.current?.title)
        engine.release()
    }

    @Test
    fun `last queue wins when set twice before ready`() = runBlocking {
        val engine = newEngine()
        engine.setQueue(listOf(item(1), item(2)), 0, autoPlay = true)
        engine.setQueue(listOf(item(9)), 0, autoPlay = true)

        engine.onReady()
        assertTrue(waitUntil(5000) { engine.state.value.isPlaying })
        assertEquals("T9", engine.state.value.current?.title)
        engine.release()
    }

    // ---------------- صدا ----------------

    @Test
    fun `volume is accepted in and out of range`() = runBlocking {
        val engine = newEngine()
        engine.setVolume(5f)
        engine.setVolume(-1f)
        engine.setVolume(0.5f)
        engine.release()
    }
}

/**
 * بافر حلقوی — زیرساختی که نمی‌گذارد نخ پخش و نخ صوتی همدیگر را قفل کنند.
 */
class PcmRingBufferTest {

    @Test
    fun `write then read returns the same bytes`() {
        val ring = PcmRingBuffer(1024)
        val data = ByteArray(100) { (it % 128).toByte() }
        assertEquals(100, ring.write(data, 0, data.size))
        assertEquals(100, ring.available())

        val read = ring.read(100)!!
        assertContentEquals(data, read)
        assertEquals(0, ring.available())
    }

    @Test
    fun `write stops at capacity instead of overwriting`() {
        val ring = PcmRingBuffer(64)
        assertEquals(64, ring.write(ByteArray(100), 0, 100), "must not exceed capacity")
        assertEquals(0, ring.freeSpace())
        assertEquals(0, ring.write(ByteArray(10), 0, 10), "a full buffer refuses writes")
    }

    @Test
    fun `wrap-around keeps the data in order`() {
        val ring = PcmRingBuffer(10)
        val first = ByteArray(8) { it.toByte() }
        ring.write(first, 0, first.size)
        ring.read(8)
        assertEquals(0, ring.available())

        // حالا نوشتن از وسط بافر عبور می‌کند
        val second = ByteArray(6) { (it + 50).toByte() }
        assertEquals(6, ring.write(second, 0, second.size))
        val got = ring.read(6)!!
        assertContentEquals(second, got, "wrap-around must preserve byte order")
    }

    @Test
    fun `a partial read keeps the remainder`() {
        val ring = PcmRingBuffer(64)
        val data = ByteArray(10) { it.toByte() }
        ring.write(data, 0, 10)
        assertEquals(4, ring.read(4)!!.size)
        assertEquals(6, ring.available())
        assertContentEquals(data.copyOfRange(4, 10), ring.read(6))
    }

    @Test
    fun `clear drops pending data`() {
        val ring = PcmRingBuffer(64)
        ring.write(ByteArray(20), 0, 20)
        ring.clear()
        assertEquals(0, ring.available())
    }

    @Test
    fun `readBlocking returns null on timeout`() {
        val ring = PcmRingBuffer(64)
        assertNull(ring.readBlocking(defaultTimeoutMs = 5))
    }

    @Test
    fun `readBlocking returns as soon as data arrives`() {
        val ring = PcmRingBuffer(1024)
        thread(start = true, isDaemon = true) {
            Thread.sleep(30)
            ring.write(byteArrayOf(1, 2, 3, 4), 0, 4)
        }
        val out = ring.readBlocking(defaultTimeoutMs = 3000)
        assertTrue(out != null && out.size == 4, "blocking read should pick up the late write")
    }
}
