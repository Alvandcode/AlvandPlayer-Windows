package com.alvand.player.library

import com.alvand.player.data.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertIs

/**
 * Ø¢Ø²Ù…ÙˆÙ† ÛŒÚ©Ù¾Ø§Ø±Ú†Ù‡Ù” Ú©ØªØ§Ø¨Ø®Ø§Ù†Ù‡: Ø§Ø² Ø¨Ø§Ø²Ù¾ÛŒÙ…Ø§ÛŒÛŒ Ù¾ÙˆØ´Ù‡ ØªØ§ ÙÛŒÙ„ØªØ± Ùˆ Ø³ÙˆØ±Øª.
 *
 * ÙØ§ÛŒÙ„â€ŒÙ‡Ø§ÛŒ WAV ÙˆØ§Ù‚Ø¹ÛŒ Ø³Ø§Ø®ØªÙ‡ Ù…ÛŒâ€ŒØ´ÙˆÙ†Ø¯ ØªØ§ Ù…Ø¯Øªâ€ŒØ²Ù…Ø§Ù† ÙˆØ§Ù‚Ø¹ÛŒ Ø§Ø² Ù‡Ø¯Ø± Ø®ÙˆØ§Ù†Ø¯Ù‡ Ø´ÙˆØ¯ â€” Ø¯Ù‚ÛŒÙ‚Ø§Ù‹
 * Ù…Ø«Ù„ Ú†ÛŒØ²ÛŒ Ú©Ù‡ Ú©Ø§Ø±Ø¨Ø± Ø±ÙˆÛŒ Ø¯ÛŒØ³Ú© Ø®ÙˆØ¯Ø´ Ø¯Ø§Ø±Ø¯.
 *
 * Ù‡ÛŒÚ† Ù†Ù…Ø§ÛŒØ´Ú¯Ø±ÛŒ Ù„Ø§Ø²Ù… Ù†ÛŒØ³Øª.
 */
class LibraryStoreScanTest {

    private lateinit var root: Path
    private lateinit var scope: CoroutineScope
    private lateinit var settings: SettingsStore
    private lateinit var roots: LibraryRoots
    private lateinit var store: LibraryStore

    @BeforeTest
    fun setUp() {
        root = Files.createTempDirectory("alvand-library-test")
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        settings = SettingsStore(root.resolve("settings.properties"))
        roots = LibraryRoots(root.resolve("roots.txt"))
        store = LibraryStore(roots, settings, scope)
    }

    @AfterTest
    fun tearDown() {
        scope.cancel()
        runCatching { root.toFile().deleteRecursively() }
    }

    /**
     * Ø³Ø§Ø®Øª ÛŒÚ© ÙØ§ÛŒÙ„ WAV ÙˆØ§Ù‚Ø¹ÛŒ Ø¨Ø§ Ø·ÙˆÙ„ Ù…Ø´Ø®Øµ (Ø«Ø§Ù†ÛŒÙ‡).
     *
     * Ù‡Ø¯Ø± WAV **little-endian** Ø§Ø³ØªØŒ Ø¨Ø±Ø§ÛŒ Ù‡Ù…ÛŒÙ† Ø¯Ø³ØªÛŒ Ù…ÛŒâ€ŒÙ†ÙˆÛŒØ³ÛŒÙ…Ø› `DataOutputStream`
     * big-endian Ù…ÛŒâ€ŒÙ†ÙˆÛŒØ³Ø¯ Ùˆ jaudiotagger ÙØ§ÛŒÙ„ Ø±Ø§ Ø®Ø±Ø§Ø¨ Ø§Ø¹Ù„Ø§Ù… Ù…ÛŒâ€ŒÚ©Ù†Ø¯.
     */
    private fun wav(base: Path, name: String, seconds: Int, freq: Int): File {
        val target = base.resolve(name)
        target.parent.createDirectories()
        val rate = 22050
        val samples = rate * seconds
        val f = target.toFile()

        FileOutputStream(f).use { out ->
            fun le32(v: Int) {
                out.write(v and 0xFF); out.write((v ushr 8) and 0xFF)
                out.write((v ushr 16) and 0xFF); out.write((v ushr 24) and 0xFF)
            }
            fun le16(v: Int) {
                out.write(v and 0xFF); out.write((v ushr 8) and 0xFF)
            }
            out.write("RIFF".toByteArray()); le32(36 + samples * 2)
            out.write("WAVE".toByteArray())
            out.write("fmt ".toByteArray()); le32(16)
            le16(1); le16(1); le32(rate); le32(rate * 2); le16(2); le16(16)
            out.write("data".toByteArray()); le32(samples * 2)

            val buf = ByteArray(samples * 2)
            for (i in 0 until samples) {
                val v = (8000 * kotlin.math.sin(2 * kotlin.math.PI * freq * i / rate)).toInt().toShort()
                buf[i * 2] = (v.toInt() and 0xFF).toByte()
                buf[i * 2 + 1] = ((v.toInt() shr 8) and 0xFF).toByte()
            }
            out.write(buf)
        }
        return f
    }

    private fun awaitScan() {
        val deadline = System.currentTimeMillis() + 60_000
        while (System.currentTimeMillis() < deadline) {
            val s = store.scanState.value
            if (s is ScanState.Done || s is ScanState.Failed) return
            Thread.sleep(50)
        }
        error("scan did not finish in time, state = ${store.scanState.value}")
    }

    @Test
    fun `adding a root stores exactly one path`() {
        wav(root, "01 - A - One.wav", 2, 440)
        roots.add(root)
        // java.nio.file.Path ÛŒÚ© Iterable Ø§Ø³ØªØ› Ø§Ú¯Ø± Ù„ÛŒØ³Øª + Ù…Ø³ÛŒØ± Ø¨Ù†ÙˆÛŒØ³ÛŒÙ…ØŒ Ù…Ø³ÛŒØ± Ø¨Ù‡
        // Ú†Ù†Ø¯ ØªÚ©Ù‡Ù” Ø¬Ø¯Ø§ Ù…ÛŒâ€ŒØ´Ú©Ù†Ø¯ Ùˆ Ø§Ø³Ú©Ù† Ù‡ÛŒÚ†â€ŒÚ†ÛŒØ² Ù¾ÛŒØ¯Ø§ Ù†Ù…ÛŒâ€ŒÚ©Ù†Ø¯
        assertEquals(1, roots.current.size)
        assertEquals(root.toAbsolutePath().normalize(), roots.current.single())
    }

    @Test
    fun `scan finds songs reads duration and derives metadata from filename`() {
        wav(root, "01 - Coldplay - Yellow.wav", 3, 440)
        wav(root, "Album One/02 - Ed Sheeran - Photograph.wav", 5, 494)
        wav(root, "notes.txt", 1, 100)
        roots.add(root)
        store.scan()
        awaitScan()

        assertIs<ScanState.Done>(store.scanState.value)
        val songs = store.songs.value
        assertEquals(2, songs.size, "text file must not be treated as a song")

        val yellow = songs.single { it.title == "Yellow" }
        assertEquals("Coldplay", yellow.artist)
        assertEquals(3000L, yellow.durationMs, "duration should come from the real WAV header")
        assertTrue(yellow.id > 0, "disk songs get positive ids")
        assertTrue(yellow.uri.endsWith("Yellow.wav"))

        val photo = songs.single { it.title == "Photograph" }
        assertEquals("Ed Sheeran", photo.artist)
    }

    @Test
    fun `file without the number pattern falls back to unknown`() {
        wav(root, "mystery track.wav", 2, 300)
        roots.add(root)
        store.scan()
        awaitScan()

        val song = store.songs.value.single()
        assertEquals(TagReader.UNKNOWN, song.title)
        assertEquals(TagReader.UNKNOWN_ARTIST, song.artist)
    }

    @Test
    fun `rescanning keeps ids and does not report new songs`() {
        wav(root, "01 - A - One.wav", 2, 440)
        roots.add(root)

        store.scan()
        awaitScan()
        val first = store.songs.value.map { it.id to it.uri }
        assertEquals(1, (store.scanState.value as ScanState.Done).added)

        store.scan()
        awaitScan()
        val second = store.songs.value.map { it.id to it.uri }

        assertEquals(first, second, "ids must stay stable across rescans")
        assertEquals(0, (store.scanState.value as ScanState.Done).added, "nothing new the second time")
    }

    @Test
    fun `deleted files disappear from the library after a rescan`() {
        val f = wav(root, "01 - A - One.wav", 2, 440)
        roots.add(root)
        store.scan()
        awaitScan()
        assertEquals(1, store.songs.value.size)

        f.delete()
        store.scan()
        awaitScan()
        assertEquals(0, store.songs.value.size, "a deleted file must not stay in the library")
    }

    @Test
    fun `empty scan reports zero instead of failing`() {
        store.scan()
        awaitScan()
        val done = assertIs<ScanState.Done>(store.scanState.value)
        assertEquals(0, done.total)
        assertTrue(store.songs.value.isEmpty())
    }

    @Test
    fun `search sort and favourites filter the scanned library`() {
        wav(root, "01 - Coldplay - Yellow.wav", 2, 440)
        wav(root, "02 - Coldplay - Paradise.wav", 8, 494)
        wav(root, "03 - Ed Sheeran - Photograph.wav", 5, 523)
        roots.add(root)
        store.scan()
        awaitScan()

        // Ø¬Ø³ØªØ¬Ùˆ Ø±ÙˆÛŒ Ù†Ø§Ù… Ø®ÙˆØ§Ù†Ù†Ø¯Ù‡
        store.setQuery("coldplay")
        assertEquals(2, store.visibleSongs().size)

        // Ø¬Ø³ØªØ¬Ùˆ Ø±ÙˆÛŒ Ø¹Ù†ÙˆØ§Ù†
        store.setQuery("photograph")
        assertEquals(1, store.visibleSongs().size)

        // Ø³ÙˆØ±Øª Ø¨Ø± Ø§Ø³Ø§Ø³ Ø·ÙˆÙ„
        store.setQuery("")
        store.setSortMode(com.alvand.player.library.LibrarySort.DURATION_DESC)
        assertEquals("Paradise", store.visibleSongs().first().title)

        store.setSortMode(com.alvand.player.library.LibrarySort.DURATION_ASC)
        assertEquals("Yellow", store.visibleSongs().first().title)

        // Ø¹Ù„Ø§Ù‚Ù‡â€ŒÙ…Ù†Ø¯ÛŒâ€ŒÙ‡Ø§
        store.setSortMode(com.alvand.player.library.LibrarySort.DEFAULT)
        val target = store.songs.value.single { it.title == "Photograph" }
        store.toggleFavourite(target)
        store.setFavoritesOnly(true)
        assertEquals(listOf("Photograph"), store.visibleSongs().map { it.title })
    }

    @Test
    fun `grouping by album and artist works on the scanned library`() {
        wav(root, "01 - Coldplay - Yellow.wav", 2, 440)
        wav(root, "02 - Coldplay - Paradise.wav", 3, 494)
        wav(root, "03 - Ed Sheeran - Photograph.wav", 4, 523)
        roots.add(root)
        store.scan()
        awaitScan()

        val songs = store.songs.value
        // ÙØ§ÛŒÙ„â€ŒÙ‡Ø§ÛŒ WAV ØªÚ¯ Ø¢Ù„Ø¨ÙˆÙ… Ù†Ø¯Ø§Ø±Ù†Ø¯ØŒ Ù¾Ø³ Ù‡Ù…Ù‡ Ø¯Ø± Â«Unknown AlbumÂ» Ù…ÛŒâ€ŒÙ†Ø´ÛŒÙ†Ù†Ø¯
        val albums = LibraryFilter.groupByAlbum(songs)
        assertEquals(1, albums.size)
        assertEquals("Unknown Album", albums.single().name)
        // Ø®ÙˆØ§Ù†Ù†Ø¯Ù‡Ù” Ù¾Ø±ØªØ¹Ø¯Ø§Ø¯ Ø¨Ø§ÛŒØ¯ Coldplay Ø¨Ø§Ø´Ø¯ (Û² Ø¢Ù‡Ù†Ú¯ Ø¯Ø± Ø¨Ø±Ø§Ø¨Ø± Û±)
        assertEquals("Coldplay", albums.single().artist)

        val artists = LibraryFilter.groupByArtist(songs)
        assertEquals(listOf("Coldplay", "Ed Sheeran"), artists.map { it.name })
    }
}
