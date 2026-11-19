package com.alvand.player

import com.alvand.player.library.LibraryFilter
import com.alvand.player.library.LibrarySort
import com.alvand.player.model.Song
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * پورت `LibraryFilterTest` نسخهٔ اندروید.
 *
 * تفاوت: به‌جای `Uri` یک رشتهٔ ساده استفاده می‌شود.
 * موارد بیشتر: ترتیب مراحل (فیلتر علاقه‌مندی → جستجو → سورت) و هر ۵ حالت سورت.
 */
class LibraryFilterTest {

    private fun song(
        id: Long,
        title: String,
        artist: String,
        album: String = "",
        dur: Long = 180000L
    ) = Song(id, title, artist, album, "C:/music/$id.mp3", dur)

    @Test
    fun `search filters by title artist album`() {
        val list = listOf(
            song(1, "Yellow", "Coldplay"),
            song(2, "Photograph", "Ed Sheeran"),
            song(3, "Night", "Coldplay", album = "Dreamy")
        )
        assertEquals(2, LibraryFilter.filterAndSort(list, "coldplay", 0, emptySet(), false).size)
        assertEquals(1, LibraryFilter.filterAndSort(list, "photo", 0, emptySet(), false).size)
        assertEquals(1, LibraryFilter.filterAndSort(list, "dreamy", 0, emptySet(), false).size)
        assertEquals(3, LibraryFilter.filterAndSort(list, "", 0, emptySet(), false).size)
    }

    @Test
    fun `favoritesOnly keeps only liked`() {
        val list = listOf(song(1, "A", "X"), song(2, "B", "Y"), song(3, "C", "Z"))
        val out = LibraryFilter.filterAndSort(list, "", 0, setOf(2L), true)
        assertEquals(listOf(2L), out.map { it.id })
    }

    @Test
    fun `sort title az is case-insensitive`() {
        val list = listOf(song(1, "yellow", "b"), song(2, "Apple", "a"), song(3, "Moon", "c"))
        val out = LibraryFilter.filterAndSort(list, "", LibrarySort.TITLE_AZ, emptySet(), false)
        assertEquals(listOf("Apple", "Moon", "yellow"), out.map { it.title })
    }

    @Test
    fun `sort artist az is case-insensitive`() {
        val list = listOf(song(1, "A", "zebra"), song(2, "B", "Apple"), song(3, "C", "mango"))
        val out = LibraryFilter.filterAndSort(list, "", LibrarySort.ARTIST_AZ, emptySet(), false)
        assertEquals(listOf("Apple", "mango", "zebra"), out.map { it.artist })
    }

    @Test
    fun `sort longest first`() {
        val list = listOf(song(1, "A", "X", dur = 1000), song(2, "B", "Y", dur = 5000))
        val out = LibraryFilter.filterAndSort(list, "", LibrarySort.DURATION_DESC, emptySet(), false)
        assertEquals(2L, out.first().id)
    }

    @Test
    fun `sort shortest first`() {
        val list = listOf(song(1, "A", "X", dur = 1000), song(2, "B", "Y", dur = 5000))
        val out = LibraryFilter.filterAndSort(list, "", LibrarySort.DURATION_ASC, emptySet(), false)
        assertEquals(1L, out.first().id)
    }

    @Test
    fun `default sort preserves scan order`() {
        val list = listOf(song(3, "C", "Z"), song(1, "A", "X"), song(2, "B", "Y"))
        val out = LibraryFilter.filterAndSort(list, "", LibrarySort.DEFAULT, emptySet(), false)
        assertEquals(listOf(3L, 1L, 2L), out.map { it.id })
    }

    @Test
    fun `combined fav plus search plus sort`() {
        val list = listOf(
            song(1, "Yellow", "Coldplay", dur = 2000),
            song(2, "Yellow Submarine", "Beatles", dur = 5000),
            song(3, "Green", "Coldplay", dur = 9000)
        )
        val out = LibraryFilter.filterAndSort(list, "yellow", LibrarySort.DURATION_DESC, setOf(1L, 2L), true)
        assertEquals(listOf(2L, 1L), out.map { it.id })
    }

    @Test
    fun `groupByAlbum clusters and sorts`() {
        val list = listOf(
            song(1, "A1", "X", album = "Zebra"),
            song(2, "A2", "X", album = "Zebra"),
            song(3, "B1", "Y", album = "apple")
        )
        val groups = LibraryFilter.groupByAlbum(list)
        assertEquals(2, groups.size)
        assertEquals("apple", groups.first().name)
        assertEquals(2, groups.last().songs.size)
    }

    @Test
    fun `groupByAlbum artist is the most frequent one`() {
        val list = listOf(
            song(1, "A1", "X", album = "Mix"),
            song(2, "A2", "X", album = "Mix"),
            song(3, "A3", "Y", album = "Mix")
        )
        val group = LibraryFilter.groupByAlbum(list).single()
        assertEquals("X", group.artist)
    }

    @Test
    fun `blank album falls back to Unknown Album`() {
        val groups = LibraryFilter.groupByAlbum(listOf(song(1, "A", "X", album = "  ")))
        assertEquals("Unknown Album", groups.single().name)
    }

    @Test
    fun `groupByArtist counts albums`() {
        val list = listOf(
            song(1, "A", "Coldplay", album = "A1"),
            song(2, "B", "Coldplay", album = "A2"),
            song(3, "C", "Coldplay", album = "A1")
        )
        val groups = LibraryFilter.groupByArtist(list)
        assertEquals(1, groups.size)
        assertEquals(3, groups.first().songs.size)
        assertEquals(2, groups.first().albums)
    }

    @Test
    fun `blank artist falls back to Unknown Artist`() {
        val groups = LibraryFilter.groupByArtist(listOf(song(1, "A", "")))
        assertEquals("Unknown Artist", groups.single().name)
    }

    @Test
    fun `indexOf never returns negative`() {
        val list = listOf(song(1, "A", "X"), song(2, "B", "Y"))
        assertEquals(1, LibraryFilter.indexOf(list, 2L))
        assertEquals(0, LibraryFilter.indexOf(list, 999L))
    }
}
