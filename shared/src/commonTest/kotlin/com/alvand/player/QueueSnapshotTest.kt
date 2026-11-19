package com.alvand.player

import com.alvand.player.model.Song
import com.alvand.player.player.PlayableItem
import com.alvand.player.player.QueueSnapshot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class QueueSnapshotTest {

    private fun song(id: Long, uri: String) = Song(id, "T$id", "A", "", uri)

    @Test
    fun `encode decode roundtrip keeps order and index`() {
        val uris = listOf("C:/m/a.mp3", "C:/m/b flac.flac", "D:/x/c.ogg")
        val decoded = QueueSnapshot.decode(QueueSnapshot.encode(uris, 1))
        assertEquals(uris, decoded!!.first)
        assertEquals(1, decoded.second)
    }

    @Test
    fun `corrupt snapshot never crashes`() {
        assertNull(QueueSnapshot.decode(""))
        assertNull(QueueSnapshot.decode("garbage"))
        assertNull(QueueSnapshot.decode("v9|0|1|x"))
        assertNull(QueueSnapshot.decode("v1|abc|1|x"))
        assertNull(QueueSnapshot.decode("v1|0|2|only-one"))
    }

    @Test
    fun `uris with separators survive escaping`() {
        val uris = listOf("C:/m/a|b.mp3", "C:/m/c\\d.mp3")
        val decoded = QueueSnapshot.decode(QueueSnapshot.encode(uris, 0))
        assertEquals(uris, decoded!!.first)
    }

    @Test
    fun `rebuild drops missing files and clamps index`() {
        val library = listOf(song(1, "C:/m/a.mp3"), song(3, "C:/m/c.mp3"))
        val (items, index) = QueueSnapshot.rebuild(
            listOf("C:/m/a.mp3", "C:/m/gone.mp3", "C:/m/c.mp3"),
            2,
            library
        )
        assertEquals(listOf("C:/m/a.mp3", "C:/m/c.mp3"), items.map { it.uri })
        assertEquals(1, index)
    }

    @Test
    fun `rebuild with empty library returns empty`() {
        val (items, index) = QueueSnapshot.rebuild(listOf("C:/m/a.mp3"), 0, emptyList())
        assertTrue(items.isEmpty())
        assertEquals(-1, index)
    }

    @Test
    fun `encodeQueue uses playable uris`() {
        val queue = listOf(
            PlayableItem(1, "A", "X", uri = "C:/m/a.mp3"),
            PlayableItem(2, "B", "Y", uri = "C:/m/b.mp3")
        )
        val decoded = QueueSnapshot.decode(QueueSnapshot.encodeQueue(queue, 0))
        assertEquals(listOf("C:/m/a.mp3", "C:/m/b.mp3"), decoded!!.first)
    }
}
