package com.alvand.player.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class JumpToTimeTest {

    @Test
    fun `minutes and seconds parse to millis`() {
        assertEquals(90_000L, parseJumpInput("1:30"))
        assertEquals(3_730_000L, parseJumpInput("62:10"))
        assertEquals(0L, parseJumpInput("0:00"))
    }

    @Test
    fun `plain seconds parse to millis`() {
        assertEquals(45_000L, parseJumpInput("45"))
        assertEquals(0L, parseJumpInput("0"))
    }

    @Test
    fun `whitespace is tolerated`() {
        assertEquals(90_000L, parseJumpInput("  1:30  "))
    }

    @Test
    fun `garbage is rejected`() {
        assertNull(parseJumpInput(""))
        assertNull(parseJumpInput("   "))
        assertNull(parseJumpInput("abc"))
        assertNull(parseJumpInput("1:2:3"))
        assertNull(parseJumpInput("1:99"))
        assertNull(parseJumpInput("-5"))
        assertNull(parseJumpInput("1:-5"))
    }
}
