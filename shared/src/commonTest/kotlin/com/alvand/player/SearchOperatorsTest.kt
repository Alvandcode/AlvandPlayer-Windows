package com.alvand.player

import com.alvand.player.library.LibraryFilter
import com.alvand.player.model.Song
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SearchOperatorsTest {

    private fun song(id: Long, title: String, artist: String, album: String = "") =
        Song(id, title, artist, album, "C:/m/$id.mp3")

    private fun filter(q: String) = LibraryFilter.filterAndSort(
        listOf(
            song(1, "Yellow", "Coldplay", "Parachutes"),
            song(2, "Photograph", "Ed Sheeran", "X"),
            song(3, "Yellow Submarine", "Beatles", "Revolver")
        ),
        q, 0, emptySet(), false
    ).map { it.id }

    @Test
    fun `free text keeps old behavior`() {
        assertEquals(listOf(1L, 3L), filter("yellow"))
    }

    @Test
    fun `artist operator narrows`() {
        assertEquals(listOf(1L), filter("artist:coldplay yellow"))
        assertEquals(listOf(1L), filter("artist:coldplay"))
        assertEquals(emptyList(), filter("artist:coldplay photograph"))
    }

    @Test
    fun `album and title operators`() {
        assertEquals(listOf(1L), filter("album:parachutes"))
        assertEquals(listOf(3L), filter("title:submarine"))
        assertEquals(listOf(1L), filter("title:yellow artist:coldplay"))
    }

    @Test
    fun `persian operators work`() {
        val out = LibraryFilter.filterAndSort(
            listOf(song(1, "شب", "خواننده‌ای", "آلبوم آبی")),
            "خواننده:خواننده", 0, emptySet(), false
        )
        assertEquals(1, out.size)
    }

    @Test
    fun `quoted values with spaces`() {
        val out = LibraryFilter.filterAndSort(
            listOf(song(1, "A", "Cold Play", "")),
            "artist:\"cold play\"", 0, emptySet(), false
        )
        assertEquals(1, out.size)
    }

    @Test
    fun `unknown operator falls back to free text`() {
        // نباید چیزی را گم کند — فقط متن آزاد است
        assertTrue(filter("foo:bar").isEmpty())
    }
}
