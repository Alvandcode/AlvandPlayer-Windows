package com.alvand.player

import com.alvand.player.player.EngineState
import com.alvand.player.player.ErrorSkipPolicy
import com.alvand.player.player.PlayableItem
import com.alvand.player.player.PlaybackQueue
import com.alvand.player.player.RepeatMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * منطق صف پخش — همان لبه‌هایی که در نسخهٔ اندروید تست نداشت.
 *
 * اینجا دقیقاً همان جاهایی است که پخش‌کننده‌های واقعی خراب می‌شوند: دکمهٔ
 * «قبلی» در سه‌ثانیهٔ اول آهنگ، تکرار یک‌باره، و رد کردن فایل خراب.
 */
class PlaybackQueueTest {

    private fun item(id: Long) = PlayableItem(id, "T$id", "A", uri = "C:/m/$id.mp3")

    private fun makeQueue(vararg ids: Long) = PlaybackQueue(
        items = ids.map { item(it) },
        index = 0,
        shuffleEnabled = false,
        repeatMode = RepeatMode.OFF
    )

    // ---- بعدی ----

    @Test
    fun `next walks the queue forward`() {
        val q = makeQueue(1, 2, 3)
        assertEquals(1, q.nextIndex())
        q.moveTo(1)
        assertEquals(2, q.nextIndex())
    }
    @Test
    fun `probe early`() {
        val q2 = makeQueue(1)
        assertEquals(1L, q2.current()?.id)
    }

    @Test
    fun `next at the end with repeat off does nothing`() {
        val q = makeQueue(1, 2, 3)
        q.moveTo(2)
        // قانون کلیدی پرامپت: نه توقف، نه چرخش
        assertEquals(-1, q.nextIndex())
        assertTrue(q.isAtEnd())
    }

    @Test
    fun `next at the end with repeat all wraps around`() {
        val q = makeQueue(1, 2, 3)
        q.setRepeat(RepeatMode.ALL)
        q.moveTo(2)
        assertEquals(0, q.nextIndex())
    }

    @Test
    fun `repeat one never advances`() {
        val q = makeQueue(1, 2, 3)
        q.setRepeat(RepeatMode.ONE)
        q.moveTo(1)
        assertEquals(1, q.nextIndex())
    }

    @Test
    fun `repeat cycles off to all to one to off`() {
        val q = makeQueue(1, 2)
        assertEquals(RepeatMode.OFF, q.repeat)
        assertEquals(RepeatMode.ALL, q.cycleRepeat())
        assertEquals(RepeatMode.ONE, q.cycleRepeat())
        assertEquals(RepeatMode.OFF, q.cycleRepeat())
    }

    @Test
    fun `empty queue never moves`() {
        val q = PlaybackQueue()
        assertEquals(-1, q.nextIndex())
        assertEquals(-1, q.previousIndex(0))
        assertTrue(q.isEmpty())
        assertTrue(q.isAtEnd())
    }

    // ---- قبلی: قانون سه ثانیه ----

    @Test
    fun `previous after 3 seconds restarts the same song`() {
        val q = makeQueue(1, 2, 3)
        q.moveTo(1)
        // ۳.۰۰۱ ثانیه گذشته، پس اول همین آهنگ
        assertEquals(1, q.previousIndex(3001))
    }

    @Test
    fun `previous within 3 seconds goes to the earlier song`() {
        val q = makeQueue(1, 2, 3)
        q.moveTo(1)
        // ۲.۹۹۹ ثانیه گذشته، پس آهنگ قبلی
        assertEquals(0, q.previousIndex(2999))
    }

    @Test
    fun `previous at the very first song just restarts it`() {
        val q = makeQueue(1, 2, 3)
        assertEquals(0, q.previousIndex(0))
    }

    @Test
    fun `previous at first song with repeat all wraps to last`() {
        val q = makeQueue(1, 2, 3)
        q.setRepeat(RepeatMode.ALL)
        assertEquals(2, q.previousIndex(0))
    }

    // ---- شافل ----

    @Test
    fun `shuffle keeps the current song first`() {
        val q = makeQueue(1, 2, 3, 4, 5)
        q.moveTo(2)
        q.setShuffle(true)
        // آهنگ فعلی باید همان بماند، نه اینکه وسط صف بیفتد
        assertEquals(2, q.currentIndex)
        assertEquals(3L, q.current()?.id)
    }

    @Test
    fun `shuffle still visits every song`() {
        val q = makeQueue(1, 2, 3, 4, 5)
        q.setShuffle(true)
        // آهنگ فعلی جزو بازدید است، پس اول آن را هم حساب می‌کنیم
        val visited = mutableSetOf<Long>()
        q.current()?.let { visited.add(it.id) }
        var guard = 0
        while (guard++ < 50) {
            val n = q.nextIndex()
            if (n < 0) break
            q.moveTo(n)
            visited.add(q.current()?.id ?: 0)
        }
        assertEquals(setOf(1L, 2L, 3L, 4L, 5L), visited, "shuffle must not lose songs")
    }

    @Test
    fun `turning shuffle off restores the natural order`() {
        val q = makeQueue(1, 2, 3)
        q.moveTo(1)
        q.setShuffle(true)
        q.setShuffle(false)
        // با شافل خاموش، بعد از آهنگ دوم باید سومی بیاید
        assertEquals(2, q.nextIndex())
    }

    // ---- ساخت صف ----

    @Test
    fun `start index is clamped into range`() {
        val q = PlaybackQueue()
        q.setQueue(listOf(item(1), item(2)), startIndex = 99)
        assertEquals(1, q.currentIndex)
    }

    @Test
    fun `setting an empty queue clears the index`() {
        val q = makeQueue(1, 2)
        q.setQueue(emptyList(), 0)
        assertEquals(-1, q.currentIndex)
        assertTrue(q.isEmpty())
    }

    @Test
    fun `moveTo ignores out of range targets`() {
        val q = makeQueue(1, 2, 3)
        q.moveTo(1)
        q.moveTo(99)
        assertEquals(1, q.currentIndex, "an invalid move must not break the queue")
    }

    // ---- مرتب‌سازی دستی صف ----

    @Test
    fun `move reorders items without changing current when untouched`() {
        val q = makeQueue(1, 2, 3, 4) // current = آیتم ۱ (اندیس ۰)
        assertEquals(0, q.move(2, 0)) // مقدار بازگشتی: اندیس تازهٔ آیتم جابه‌جاشده
        assertEquals(listOf(3L, 1L, 2L, 4L), q.items().map { it.id })
        // آیتم در حال پخش (۱) با جابه‌جایی، یک خانه عقب می‌رود ولی همان آیتم می‌ماند
        assertEquals(1, q.currentIndex)
        assertEquals(1L, q.current()?.id)
    }

    @Test
    fun `move tracks the playing item across reorder`() {
        val q = makeQueue(1, 2, 3)
        q.moveTo(1) // current = آیتم ۲ (اندیس ۱)
        q.move(1, 0) // آیتم ۲ به اول
        assertEquals(0, q.currentIndex)
        assertEquals(2L, q.current()?.id)
    }

    @Test
    fun `move adjusts current when the playing item is overtaken`() {
        val q = makeQueue(1, 2, 3)
        q.moveTo(2) // current = ۳ (اندیس ۲)
        q.move(0, 2) // آیتم ۱ به آخر — آیتم ۳ یک خانه عقب می‌رود
        assertEquals(1, q.currentIndex)
        assertEquals(3L, q.current()?.id)
        assertEquals(listOf(2L, 3L, 1L), q.items().map { it.id })
    }

    @Test
    fun `move rejects invalid inputs`() {
        val q = makeQueue(1, 2, 3)
        assertEquals(-1, q.move(-1, 0))
        assertEquals(-1, q.move(0, 99))
        assertEquals(-1, q.move(1, 1)) // جابه‌جایی به خودش
        assertEquals(listOf(1L, 2L, 3L), q.items().map { it.id })
    }
}

/**
 * سیاست «فایل خراب را رد کن و ادامه بده».
 *
 * این رفتار از `PlaybackService.widgetListener` نسخهٔ اندروید آمده: تا ۳ خطای
 * پشت‌سرهم مجاز است و بعد از آن دیگر نمی‌پرد، تا اپ در حلقهٔ بی‌نهایت نیفتد.
 */
class ErrorSkipPolicyTest {

    @Test
    fun `skips up to three times then gives up`() {
        val p = ErrorSkipPolicy()
        assertTrue(p.shouldSkip(hasNext = true))
        assertTrue(p.shouldSkip(hasNext = true))
        assertTrue(p.shouldSkip(hasNext = true))
        // خطای چهارم: دیگر نباید بپرد
        assertFalse(p.shouldSkip(hasNext = true))
    }

    @Test
    fun `never skips when there is no next song`() {
        val p = ErrorSkipPolicy()
        assertFalse(p.shouldSkip(hasNext = false))
        assertEquals(0, p.consecutiveErrors, "a non-skip must not count as an error")
    }

    @Test
    fun `successful playback resets the counter`() {
        val p = ErrorSkipPolicy()
        p.shouldSkip(hasNext = true)
        p.shouldSkip(hasNext = true)
        assertEquals(2, p.consecutiveErrors)

        p.onPlayingStarted()
        assertEquals(0, p.consecutiveErrors)
        // حالا دوباره سه فرصت داریم
        assertTrue(p.shouldSkip(hasNext = true))
        assertTrue(p.shouldSkip(hasNext = true))
        assertTrue(p.shouldSkip(hasNext = true))
        assertFalse(p.shouldSkip(hasNext = true))
    }

    @Test
    fun `manual reset also clears the counter`() {
        val p = ErrorSkipPolicy()
        p.shouldSkip(hasNext = true)
        p.reset()
        assertEquals(0, p.consecutiveErrors)
    }
}

/** وضعیت موتور — محاسبات پیشرفت و مدت‌زمان */
class EngineStateTest {

    private fun item(id: Long) = PlayableItem(id, "T$id", "A", uri = "C:/m/$id.mp3")

    @Test
    fun `progress is zero when duration is unknown`() {
        val s = EngineState(current = item(1), durationMs = 0)
        assertFalse(s.hasKnownDuration)
        assertEquals(0f, s.progress)
    }

    @Test
    fun `live streams are not seekable and have no progress`() {
        val s = EngineState(current = item(1), durationMs = 0, isLiveStream = true)
        assertFalse(s.hasKnownDuration)
        assertEquals(0f, s.progress)
    }

    @Test
    fun `a one millisecond duration is treated as unknown`() {
        // آستانهٔ پرامپت: duration بزرگ‌تر از ۱ میلی‌ثانیه
        val s = EngineState(current = item(1), durationMs = 1)
        assertFalse(s.hasKnownDuration)
    }

    @Test
    fun `progress is computed and clamped`() {
        val s = EngineState(current = item(1), positionMs = 50_000, durationMs = 200_000)
        assertTrue(s.hasKnownDuration)
        assertEquals(0.25f, s.progress, 0.001f)

        val past = s.copy(positionMs = 300_000)
        assertEquals(1f, past.progress, 0.001f)

        val before = s.copy(positionMs = 0)
        assertEquals(0f, before.progress, 0.001f)
    }

    @Test
    fun `repeat mode lookup falls back to off`() {
        assertEquals(RepeatMode.OFF, RepeatMode.of(0))
        assertEquals(RepeatMode.ALL, RepeatMode.of(1))
        assertEquals(RepeatMode.ONE, RepeatMode.of(2))
        assertEquals(RepeatMode.OFF, RepeatMode.of(99))
    }
}
