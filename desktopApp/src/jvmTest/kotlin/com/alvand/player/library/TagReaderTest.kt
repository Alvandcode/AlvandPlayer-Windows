package com.alvand.player.library

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * حدس متادیتا از نام فایل — همان قواعدی که SongRepository اندروید دارد.
 */
class TagReaderTest {

    @Test
    fun `numbered artist - title pattern is understood`() {
        assertEquals("Artist" to "Song", TagReader.guessFromFileName("01 - Artist - Song.mp3"))
    }

    @Test
    fun `numbered title-only pattern is understood`() {
        assertEquals("01" to "Song", TagReader.guessFromFileName("01 - Song.mp3"))
    }

    @Test
    fun `plain name gives no guess`() {
        assertNull(TagReader.guessFromFileName("Just A Song.mp3"))
        assertNull(TagReader.guessFromFileName("Song.flac"))
    }

    @Test
    fun `non numeric prefix is not treated as a track number`() {
        assertNull(TagReader.guessFromFileName("Live - Artist - Song.mp3"))
    }

    @Test
    fun `a real file with no tag falls back to filename then unknown`() {
        val dir = Files.createTempDirectory("alvand-tag-test")
        try {
            // فایل صوتی واقعی نیست، پس jaudiotagger چیزی نمی‌خواند و ما به فالبک می‌رسیم
            val named = dir.resolve("07 - Bach - Air.mp3")
            named.writeText("not really audio")

            val song = TagReader.toSong(named)
            assertEquals("Air", song.title)
            assertEquals("Bach", song.artist)
            assertTrue(song.uri.endsWith("07 - Bach - Air.mp3"))
            assertTrue(song.id > 0, "disk song ids are positive")

            val plain = dir.resolve("mystery.flac")
            plain.writeText("not really audio")

            val fallback = TagReader.toSong(plain)
            assertEquals(TagReader.UNKNOWN, fallback.title)
            assertEquals(TagReader.UNKNOWN_ARTIST, fallback.artist)
        } finally {
            dir.toFile().deleteRecursively()
        }
    }

    @Test
    fun `replay gain tag is parsed in decibels`() {
        fun tagWith(first: String?) = java.lang.reflect.Proxy.newProxyInstance(
            TagReader::class.java.classLoader,
            arrayOf(org.jaudiotagger.tag.Tag::class.java)
        ) { _, method, _ ->
            when (method.name) {
                "getFirst" -> first ?: ""
                "toString" -> "fake-tag"
                else -> null
            }
        } as org.jaudiotagger.tag.Tag

        assertEquals(-7.25f, TagReader.replayGainDbOf(tagWith("-7.25 dB")))
        assertEquals(3.5f, TagReader.replayGainDbOf(tagWith("+3.5dB")))
        assertEquals(0f, TagReader.replayGainDbOf(tagWith("0 dB")))
        assertNull(TagReader.replayGainDbOf(tagWith("not a gain")))
        assertNull(TagReader.replayGainDbOf(tagWith("")))
        assertNull(TagReader.replayGainDbOf(null))
    }

    /**
     * فایل WAV واقعی: مدت‌زمان باید از هدر خوانده شود، نه از حدس.
     *
     * نکته: هدر WAV **little-endian** است، برای همین دستی می‌نویسیم نه با
     * `DataOutputStream` که big-endian است.
     */
    @Test
    fun `duration comes from the real wav header`() {
        val dir = Files.createTempDirectory("alvand-wav")
        try {
            val rate = 22050
            val seconds = 4
            val samples = rate * seconds
            val f = dir.resolve("tone.wav").toFile()
            java.io.FileOutputStream(f).use { out ->
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
                    val v = (8000 * kotlin.math.sin(2 * kotlin.math.PI * 440 * i / rate)).toInt().toShort()
                    buf[i * 2] = (v.toInt() and 0xFF).toByte()
                    buf[i * 2 + 1] = ((v.toInt() shr 8) and 0xFF).toByte()
                }
                out.write(buf)
            }

            val info = TagReader.read(dir.resolve("tone.wav"))
            assertEquals(4000L, info?.durationMs, "4-second wav should report 4000 ms")
        } finally {
            dir.toFile().deleteRecursively()
        }
    }

    @Test
    fun `reading a broken file returns null instead of throwing`() {
        val dir = Files.createTempDirectory("alvand-tag-broken")
        try {
            val f: Path = dir.resolve("broken.mp3")
            f.writeText("definitely not audio")
            assertNull(TagReader.read(f))
        } finally {
            dir.toFile().deleteRecursively()
        }
    }

    @Test
    fun `id is stable for the same path`() {
        val dir = Files.createTempDirectory("alvand-tag-id")
        try {
            val f = dir.resolve("a.mp3")
            f.writeText("x")
            assertEquals(TagReader.toSong(f).id, TagReader.toSong(f).id)
            assertEquals(SongId.forPath(f.toAbsolutePath().toString()), TagReader.toSong(f).id)
        } finally {
            dir.toFile().deleteRecursively()
        }
    }
}
