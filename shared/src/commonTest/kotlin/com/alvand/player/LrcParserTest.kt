package com.alvand.player

import com.alvand.player.lyrics.LrcParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LrcParserTest {

    @Test
    fun `parses basic lines`() {
        val lines = LrcParser.parse("[00:10.00]hello\n[00:12.50]world\n")
        assertEquals(2, lines.size)
        assertEquals(10_000L, lines[0].timeMs)
        assertEquals("hello", lines[0].text)
        assertEquals(12_500L, lines[1].timeMs)
    }

    @Test
    fun `three-digit fraction is milliseconds`() {
        val lines = LrcParser.parse("[00:10.250]ms\n")
        assertEquals(10_250L, lines.single().timeMs)
    }

    @Test
    fun `multiple timestamps on one line fan out`() {
        val lines = LrcParser.parse("[00:10.00][00:12.00]repeat\n")
        assertEquals(2, lines.size)
        assertEquals(10_000L, lines[0].timeMs)
        assertEquals(12_000L, lines[1].timeMs)
    }

    @Test
    fun `metadata and garbage are ignored`() {
        val lines = LrcParser.parse("[ar:Coldplay]\n[ti:Yellow]\n[length:04:26]\nno timestamp\n[99:99.99]bad\n[00:20.00]\n[00:30.00]ok\n")
        assertEquals(1, lines.size)
        assertEquals("ok", lines.single().text)
    }

    @Test
    fun `output is sorted by time`() {
        val lines = LrcParser.parse("[00:30.00]b\n[00:10.00]a\n")
        assertEquals(listOf("a", "b"), lines.map { it.text })
    }

    @Test
    fun `active index tracks playback`() {
        val lines = LrcParser.parse("[00:10.00]a\n[00:20.00]b\n[00:30.00]c\n")
        assertEquals(-1, LrcParser.activeIndex(lines, 9_999L))
        assertEquals(0, LrcParser.activeIndex(lines, 10_000L))
        assertEquals(1, LrcParser.activeIndex(lines, 25_000L))
        assertEquals(2, LrcParser.activeIndex(lines, 99_000L))
        assertEquals(-1, LrcParser.activeIndex(emptyList(), 50_000L))
    }

    @Test
    fun `blank input is empty`() {
        assertTrue(LrcParser.parse("").isEmpty())
        assertTrue(LrcParser.parse("   \n").isEmpty())
    }
}
