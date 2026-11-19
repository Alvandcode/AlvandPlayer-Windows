package com.alvand.player.library

import com.alvand.player.model.Song
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SongFileInfoTest {

    private lateinit var dir: java.nio.file.Path

    @BeforeTest
    fun setUp() {
        dir = createTempDirectory("alvand-fileinfo-test")
    }

    @AfterTest
    fun tearDown() {
        runCatching { dir.toFile().deleteRecursively() }
    }

    private fun songFor(file: java.io.File, durationMs: Long = 180_000L): Song =
        Song(1L, "T", "A", "", file.absolutePath, durationMs)

    @Test
    fun `missing file never crashes and reports unknown`() {
        val info = SongFileInfoReader.read(songFor(java.io.File(dir.toFile(), "gone.mp3")))
        assertFalse(info.exists)
        assertNull(info.sizeBytes)
        assertNull(info.sampleRateHz)
        assertNull(info.channels)
        assertEquals("mp3", info.extension)
    }

    @Test
    fun `size and bitrate estimate come from the real file`() {
        val f = dir.resolve("a.mp3").toFile()
        f.writeBytes(ByteArray(450_000)) // 450KB روی 180 ثانیه ≈ 20kbps
        val info = SongFileInfoReader.read(songFor(f))
        assertTrue(info.exists)
        assertEquals(450_000L, info.sizeBytes)
        assertEquals(20, info.bitrateKbpsEstimate)
    }

    @Test
    fun `zero duration means no bitrate guess`() {
        val f = dir.resolve("b.wav").toFile()
        f.writeBytes(ByteArray(1000))
        val info = SongFileInfoReader.read(songFor(f, durationMs = 0L))
        assertNull(info.bitrateKbpsEstimate)
    }

    @Test
    fun `formats are stable`() {
        assertEquals("3.0 MB", SongFileInfoReader.formatSize(3_145_728L))
        assertEquals("812 KB", SongFileInfoReader.formatSize(831_488L))
        assertEquals("410 B", SongFileInfoReader.formatSize(410L))
        assertEquals("—", SongFileInfoReader.formatSize(null))
        assertEquals("44100 Hz", SongFileInfoReader.formatRate(44100))
        assertEquals("—", SongFileInfoReader.formatRate(null))
        assertEquals("~128 kbps", SongFileInfoReader.formatBitrate(128))
        assertEquals("—", SongFileInfoReader.formatBitrate(null))
    }
}
