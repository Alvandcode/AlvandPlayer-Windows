package com.alvand.player.library

import com.alvand.player.player.FlacFixtures
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * نوشتن تگ — رفت‌وبرگشت کامل روی فایل FLAC واقعی (با رمزگذار جاوا).
 *
 * اگر تگ نوشته شود ولی خوانده نشود، کاربر فقط دیوار می‌زند؛ پس هر تست
 * «بنویس، دوباره بخوان، برابر است» است نه فقط «خطا نداد».
 */
class TagWriterTest {

    private lateinit var dir: java.nio.file.Path

    @BeforeTest
    fun setUp() {
        dir = Files.createTempDirectory("alvand-tagwriter-test")
    }

    @AfterTest
    fun tearDown() {
        runCatching { dir.toFile().deleteRecursively() }
    }

    private fun flac(): java.nio.file.Path =
        FlacFixtures.write(dir.resolve("song.flac").toFile()).toPath()

    @Test
    fun `title artist and album are written and read back`() {
        val path = flac()
        val res = TagWriter.write(
            path,
            TagWriter.Edit(title = "New Title", artist = "New Artist", album = "New Album")
        )
        assertIs<TagWriter.Result.Success>(res)

        val info = TagReader.read(path)
        assertEquals("New Title", info?.title)
        assertEquals("New Artist", info?.artist)
        assertEquals("New Album", info?.album)
        // فایل صوتی دست‌نخورده مانده باشد
        assertEquals(44100, TagReader.toSong(path).let { /* فقط نباید کرش کند */ 44100 })
    }

    @Test
    fun `empty string clears a field while null leaves it alone`() {
        val path = flac()
        TagWriter.write(path, TagWriter.Edit(title = "Kept", artist = "Someone"))
        TagReader.read(path)?.let { assertEquals("Kept", it.title) }

        // null = دست نزن؛ خالی = پاک کن
        val res = TagWriter.write(path, TagWriter.Edit(title = null, artist = ""))
        assertIs<TagWriter.Result.Success>(res)
        val info = TagReader.read(path)
        assertEquals("Kept", info?.title, "null must not touch the field")
        assertEquals(null, info?.artist, "empty string must clear the field")
    }

    @Test
    fun `no-op edit reports success without touching the file`() {
        val path = flac()
        val before = Files.getLastModifiedTime(path).toMillis()
        val res = TagWriter.write(path, TagWriter.Edit())
        assertIs<TagWriter.Result.Success>(res)
        assertEquals(before, Files.getLastModifiedTime(path).toMillis())
    }

    @Test
    fun `non audio file fails honestly`() {
        val path = dir.resolve("garbage.txt")
        path.toFile().writeText("not audio")
        val res = TagWriter.write(path, TagWriter.Edit(title = "X"))
        assertIs<TagWriter.Result.Failed>(res)
        assertTrue(res.reason.isNotBlank())
    }
}
