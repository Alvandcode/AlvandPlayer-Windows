package com.alvand.player.data

import com.alvand.player.data.db.Database
import com.alvand.player.data.db.LibraryDao
import com.alvand.player.model.Song
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import java.nio.file.Path
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * DAO و مخزن پلی‌لیست — پورت معنای هر کوئری نسخهٔ اندروید.
 */
class PlaylistRepositoryTest {

    private lateinit var dir: Path
    private lateinit var db: Database
    private lateinit var dao: LibraryDao
    private lateinit var repo: PlaylistRepository
    private var now = 1_000_000L

    @BeforeTest
    fun setUp() {
        dir = createTempDirectory("alvand-dao-test")
        db = Database(dir.resolve("test.db"))
        db.migrate()
        dao = LibraryDao(db)
        repo = PlaylistRepository(dao) { now }
    }

    @AfterTest
    fun tearDown() {
        runCatching { db.close() }
        runCatching { dir.toFile().deleteRecursively() }
    }

    private fun song(id: Long, title: String, artist: String = "X", album: String = "") = Song(
        id = id, title = title, artist = artist, album = album,
        uri = "C:/music/$id.mp3", durationMs = 1000L * id
    )

    // ---- پلی‌لیست‌ها ----

    @Test
    fun `playlists are listed newest first`() = runTest {
        repo.createPlaylist("First").also { now += 1000 }
        repo.createPlaylist("Second").also { now += 1000 }
        repo.createPlaylist("Third")

        val names = repo.observePlaylists().first().map { it.name }
        assertContentEquals(listOf("Third", "Second", "First"), names)
    }

    @Test
    fun `create trims the name and caps it at 60 chars`() = runTest {
        val long = "a".repeat(100)
        val id = repo.createPlaylist("   $long   ")
        assertEquals(60, dao.getPlaylists().first { it.id == id }.name.length)
    }

    @Test
    fun `create rejects a blank name`() = runTest {
        assertFailsWith<IllegalArgumentException> { repo.createPlaylist("   ") }
    }

    @Test
    fun `rename trims and caps`() = runTest {
        val id = repo.createPlaylist("Old")
        repo.renamePlaylist(id, "  New name  ")
        assertEquals("New name", dao.getPlaylists().first().name)
    }

    @Test
    fun `rename ignores a blank name`() = runTest {
        val id = repo.createPlaylist("Keep")
        repo.renamePlaylist(id, "   ")
        assertEquals("Keep", dao.getPlaylists().first().name)
    }

    @Test
    fun `delete cascades to playlist songs`() = runTest {
        val pid = repo.createPlaylist("List")
        repo.addToPlaylist(pid, song(1, "A"))
        repo.addToPlaylist(pid, song(2, "B"))
        assertEquals(2, dao.getPlaylistSongs(pid).size)

        repo.deletePlaylist(pid)
        assertTrue(dao.getPlaylists().isEmpty())
        // دیتابیس خالی است — نه اینکه رکورد یتیم مانده باشد
        val orphans = db.read { c ->
            c.createStatement().use { st ->
                st.executeQuery("SELECT COUNT(*) FROM playlist_songs").use { rs -> rs.getInt(1) }
            }
        }
        assertEquals(0, orphans)
    }

    // ---- آهنگ‌های داخل پلی‌لیست ----

    @Test
    fun `addToPlaylist refuses duplicates`() = runTest {
        val pid = repo.createPlaylist("List")
        assertTrue(repo.addToPlaylist(pid, song(1, "A")))
        assertFalse(repo.addToPlaylist(pid, song(1, "A")))
        assertEquals(1, dao.getPlaylistSongs(pid).size)
    }

    @Test
    fun `position is append only`() = runTest {
        val pid = repo.createPlaylist("List")
        repo.addToPlaylist(pid, song(1, "A"))
        repo.addToPlaylist(pid, song(2, "B"))
        repo.addToPlaylist(pid, song(3, "C"))
        assertContentEquals(listOf(0, 1, 2), dao.getPlaylistSongs(pid).map { it.position })
    }

    @Test
    fun `songs are listed by position then added time`() = runTest {
        val pid = repo.createPlaylist("List")
        repo.addToPlaylist(pid, song(1, "A"))
        repo.addToPlaylist(pid, song(2, "B"))
        assertContentEquals(listOf(1L, 2L), dao.getPlaylistSongs(pid).map { it.songId })
    }

    @Test
    fun `metadata is cached so a deleted file does not break the playlist`() = runTest {
        val pid = repo.createPlaylist("List")
        repo.addToPlaylist(pid, song(1, "Kept", artist = "Artist", album = "Album"))
        val cached = dao.getPlaylistSongs(pid).single()

        assertEquals("Kept", cached.title)
        assertEquals("Artist", cached.artist)
        assertEquals("Album", cached.album)
        assertEquals("C:/music/1.mp3", cached.uri)
        assertEquals(1000L, cached.durationMs)
        assertFalse(cached.isRemote)
    }

    @Test
    fun `removeFromPlaylist deletes only the asked song`() = runTest {
        val pid = repo.createPlaylist("List")
        repo.addToPlaylist(pid, song(1, "A"))
        repo.addToPlaylist(pid, song(2, "B"))
        repo.removeFromPlaylist(pid, 1L)
        assertContentEquals(listOf(2L), dao.getPlaylistSongs(pid).map { it.songId })
    }

    @Test
    fun `entityToSong rebuilds a playable song`() = runTest {
        val pid = repo.createPlaylist("List")
        repo.addToPlaylist(pid, song(5, "Title", "Artist", "Album"))
        val rebuilt = repo.entityToSong(dao.getPlaylistSongs(pid).single())
        assertEquals(5L, rebuilt.id)
        assertEquals("Title", rebuilt.title)
        assertEquals("C:/music/5.mp3", rebuilt.uri)
    }

    // ---- تاریخچه ----

    @Test
    fun `history accumulates play count on the same row`() = runTest {
        repo.recordPlay(song(1, "A"))
        repo.recordPlay(song(1, "A"))
        repo.recordPlay(song(1, "A"))

        val history = dao.getRecent()
        assertEquals(1, history.size, "same song must not create extra rows")
        assertEquals(3, history.single().playCount)
    }

    @Test
    fun `recent is newest first and honours the limit`() = runTest {
        repo.recordPlay(song(1, "A")); now += 1000
        repo.recordPlay(song(2, "B")); now += 1000
        repo.recordPlay(song(3, "C"))

        assertContentEquals(listOf(3L, 2L, 1L), dao.getRecent().map { it.songId })
        assertEquals(2, dao.getRecent(limit = 2).size)
    }

    @Test
    fun `history older than 90 days is pruned`() = runTest {
        repo.recordPlay(song(1, "Old"))

        // ۹۱ روز بعد یک آهنگ تازه پخش می‌شود؛ هرس باید رکورد قدیمی را حذف کند
        now += 91L * 24 * 60 * 60 * 1000
        repo.recordPlay(song(2, "New"))

        val remaining = dao.getRecent()
        assertFalse(remaining.any { it.songId == 1L }, "entry older than 90 days should be pruned")
        assertTrue(remaining.any { it.songId == 2L }, "fresh entry must survive")
    }

    @Test
    fun `history younger than 90 days is kept`() = runTest {
        repo.recordPlay(song(1, "A"))
        now += 89L * 24 * 60 * 60 * 1000
        repo.recordPlay(song(2, "B"))

        assertEquals(2, dao.getRecent().size, "89 days is still inside the retention window")
    }

    @Test
    fun `historyFor returns null for an unknown song`() = runTest {
        assertNull(dao.historyFor(999L))
    }

    @Test
    fun `clearHistory removes everything`() = runTest {
        repo.recordPlay(song(1, "A"))
        repo.recordPlay(song(2, "B"))
        repo.clearHistory()
        assertTrue(dao.getRecent().isEmpty())
    }
}
