package com.alvand.player

import com.alvand.player.lyrics.LrclibQuery
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LrclibQueryTest {

    @Test
    fun `builds the documented get url`() {
        val url = LrclibQuery.buildGetUrl("Coldplay", "Yellow", "Parachutes", 266)
        assertTrue(url.startsWith("https://lrclib.net/api/get?"))
        assertTrue(url.contains("artist_name=Coldplay"))
        assertTrue(url.contains("track_name=Yellow"))
        assertTrue(url.contains("album_name=Parachutes"))
        assertTrue(url.contains("duration=266"))
    }

    @Test
    fun `persian and spaces are encoded`() {
        val url = LrclibQuery.buildGetUrl("خواننده", "شب تنها", "", 180)
        // نباید فاصلهٔ خام یا کاراکتر چندبایتیِ خام در URL بماند
        assertTrue(!url.contains(' '))
        assertTrue(url.contains("track_name="))
    }

    @Test
    fun `extracts synced and plain lyrics`() {
        val json = """{"id":1,"trackName":"Yellow","syncedLyrics":"[00:10.00]hi","plainLyrics":"hi"}"""
        assertEquals("[00:10.00]hi", LrclibQuery.extractJsonString(json, "syncedLyrics"))
        assertEquals("hi", LrclibQuery.extractJsonString(json, "plainLyrics"))
    }

    @Test
    fun `handles escapes and null`() {
        val json = """{"a":"x\n\"y\"","b":null,"c":42}"""
        assertEquals("x\n\"y\"", LrclibQuery.extractJsonString(json, "a"))
        assertNull(LrclibQuery.extractJsonString(json, "b"))
        assertNull(LrclibQuery.extractJsonString(json, "c"))
        assertNull(LrclibQuery.extractJsonString(json, "missing"))
    }

    @Test
    fun `truncated json is not a silent half-result`() {
        assertNull(LrclibQuery.extractJsonString("""{"a":"abc""", "a"))
    }
}
