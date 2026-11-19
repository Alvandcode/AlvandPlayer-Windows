package com.alvand.player

import com.alvand.player.player.SleepTimer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SleepTimerTest {

    @Test
    fun `cycles through standard options`() {
        assertEquals(15, SleepTimer.nextOption(0))
        assertEquals(30, SleepTimer.nextOption(15))
        assertEquals(45, SleepTimer.nextOption(30))
        assertEquals(60, SleepTimer.nextOption(45))
        assertEquals(0, SleepTimer.nextOption(60))
    }

    @Test
    fun `off has no deadline and never expires`() {
        assertNull(SleepTimer.deadlineFromNow(1000L, 0))
        assertFalse(SleepTimer.isExpired(null, 999_999L))
        assertEquals(0L, SleepTimer.remainingMs(null, 1000L))
    }

    @Test
    fun `deadline is now plus minutes`() {
        assertEquals(1000L + 15 * 60_000L, SleepTimer.deadlineFromNow(1000L, 15))
    }

    @Test
    fun `expiry and remaining`() {
        val deadline = 10_000L
        assertFalse(SleepTimer.isExpired(deadline, 9_999L))
        assertTrue(SleepTimer.isExpired(deadline, 10_000L))
        assertEquals(1_000L, SleepTimer.remainingMs(deadline, 9_000L))
        assertEquals(0L, SleepTimer.remainingMs(deadline, 11_000L))
    }

    @Test
    fun `formats remaining as mm_ss`() {
        assertEquals("0:00", SleepTimer.formatRemaining(0L))
        assertEquals("15:00", SleepTimer.formatRemaining(15 * 60_000L))
        assertEquals("0:05", SleepTimer.formatRemaining(5_000L))
    }
}
