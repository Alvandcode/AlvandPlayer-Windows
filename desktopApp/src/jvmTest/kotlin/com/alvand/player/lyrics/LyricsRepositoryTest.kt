package com.alvand.player.lyrics

import com.alvand.player.model.Song
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LyricsRepositoryTest {

    private lateinit var dir: java.nio.file.Path

    @BeforeTest
    fun setUp() {
        dir = createTempDirectory("alvand-lyrics-test")
    }

    @AfterTest
    fun tearDown() {
        runCatching { dir.toFile().deleteRecursively() }
    }

    private fun song(id: Long = 1L) = Song(id, "Yellow", "Coldplay", "Parachutes", "C:/m/$id.mp3", 266_000L)

    @Test
    fun `disabled online with empty cache is not-found without network`() {
        var calls = 0
        val repo = LyricsRepository(dir, fetch = { calls++; LrclibResult.Failed("must-not-happen") })
        val out = repo.get(song(), onlineEnabled = false)
        assertTrue(out is LyricsRepository.Outcome.NotFound)
        assertEquals(0, calls)
    }

    @Test
    fun `synced lyrics are cached on disk`() {
        var calls = 0
        val lrc = "[00:10.00]hello\n[00:12.00]world\n"
        val repo = LyricsRepository(dir, fetch = {
            calls++
            LrclibResult.Found(lrc, "hello\nworld")
        })
        val first = repo.get(song(), onlineEnabled = true)
        assertTrue(first is LyricsRepository.Outcome.Ready)
        assertEquals(1, calls)

        // نمونهٔ دوم با fetcher خراب — باید از دیسک بیاید
        val repo2 = LyricsRepository(dir, fetch = { LrclibResult.Failed("must-not-happen") })
        val second = repo2.get(song(), onlineEnabled = true)
        assertTrue(second is LyricsRepository.Outcome.Ready)
        val entry = (second as LyricsRepository.Outcome.Ready).entry as LyricsRepository.Entry.Synced
        assertEquals(2, entry.lines.size)
    }

    @Test
    fun `plain lyrics survive disk roundtrip`() {
        val repo = LyricsRepository(dir, fetch = { LrclibResult.Found(null, "just words") })
        assertTrue(repo.get(song(), onlineEnabled = true) is LyricsRepository.Outcome.Ready)
        val repo2 = LyricsRepository(dir, fetch = { LrclibResult.Failed("x") })
        val out = repo2.get(song(), onlineEnabled = true)
        val entry = (out as LyricsRepository.Outcome.Ready).entry as LyricsRepository.Entry.Plain
        assertEquals("just words", entry.text)
    }

    @Test
    fun `corrupt cache file is ignored and refetched`() {
        dir.toFile().resolve("1.lrc").writeText("[[[garbage", Charsets.UTF_8)
        var calls = 0
        val repo = LyricsRepository(dir, fetch = {
            calls++
            LrclibResult.Found("[00:10.00]fresh\n", null)
        })
        val out = repo.get(song(), onlineEnabled = true)
        assertTrue(out is LyricsRepository.Outcome.Ready)
        assertEquals(1, calls)
    }

    @Test
    fun `failures report honest reasons`() {
        val repo = LyricsRepository(dir, fetch = { LrclibResult.Failed("offline") })
        val out = repo.get(song(999L), onlineEnabled = true)
        assertTrue(out is LyricsRepository.Outcome.Failed)
        assertEquals("offline", (out as LyricsRepository.Outcome.Failed).reason)
    }

    // --- sidecar: فایل .lrc/.txt کنار خود آهنگ ---

    @Test
    fun `sidecar lrc next to the audio file wins without any network`() {
        val audio = dir.toFile().resolve("yellow.mp3")
        audio.writeBytes(ByteArray(4))
        audio.parentFile.resolve("yellow.lrc").writeText("[00:05.00]sidecar line\n", Charsets.UTF_8)
        val s = Song(1L, "Yellow", "Coldplay", "Parachutes", audio.absolutePath, 266_000L)
        var calls = 0
        val repo = LyricsRepository(dir, fetch = { calls++; LrclibResult.Failed("must-not-happen") })
        val out = repo.get(s, onlineEnabled = true) // حتی با اینترنت روشن
        assertTrue(out is LyricsRepository.Outcome.Ready)
        assertEquals(0, calls)
        val entry = (out as LyricsRepository.Outcome.Ready).entry as LyricsRepository.Entry.Synced
        assertEquals(1, entry.lines.size)
        assertEquals("sidecar line", entry.lines[0].text)
    }

    @Test
    fun `sidecar plain txt is accepted as plain lyrics`() {
        val audio = dir.toFile().resolve("yellow2.mp3")
        audio.writeBytes(ByteArray(4))
        audio.parentFile.resolve("yellow2.txt").writeText("just words\n", Charsets.UTF_8)
        val s = Song(2L, "Yellow", "Coldplay", "Parachutes", audio.absolutePath, 266_000L)
        val repo = LyricsRepository(dir, fetch = { LrclibResult.Failed("must-not-happen") })
        val out = repo.get(s, onlineEnabled = false)
        val entry = (out as LyricsRepository.Outcome.Ready).entry as LyricsRepository.Entry.Plain
        assertEquals("just words", entry.text)
    }

    @Test
    fun `remote songs never touch the filesystem for sidecars`() {
        val s = Song(3L, "Stream", "Artist", "", "https://example.com/song.mp3", 100_000L, isRemote = true)
        val repo = LyricsRepository(dir, fetch = { LrclibResult.Failed("x") })
        val out = repo.get(s, onlineEnabled = false)
        assertTrue(out is LyricsRepository.Outcome.NotFound)
    }
}
