package com.alvand.player.library

import com.alvand.player.model.Song
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertContentEquals
import kotlin.concurrent.thread
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * اسکنر پوشه — بدون نیاز به نمایشگر، روی پوشهٔ موقت.
 */
class FolderScannerTest {

    private lateinit var root: Path

    @BeforeTest
    fun setUp() {
        root = Files.createTempDirectory("alvand-scan-test")
    }

    @AfterTest
    fun tearDown() {
        runCatching { root.toFile().deleteRecursively() }
    }

    private fun file(relative: String, name: String): Path {
        val dir = root.resolve(relative)
        dir.createDirectories()
        val f = dir.resolve(name)
        f.writeText("fake audio")
        return f
    }

    @Test
    fun `only supported extensions are collected`() {
        file("", "a.mp3")
        file("", "b.flac")
        file("", "c.ogg")
        file("", "notes.txt")
        file("", "cover.jpg")
        file("", "readme.md")

        val result = FolderScanner().scan(listOf(root))
        val names = result.files.map { it.fileName.toString() }.sorted()
        assertContentEquals(listOf("a.mp3", "b.flac", "c.ogg"), names)
    }

    @Test
    fun `extension match is case-insensitive`() {
        file("", "A.MP3")
        file("", "B.Flac")
        val result = FolderScanner().scan(listOf(root))
        assertEquals(2, result.files.size)
    }

    @Test
    fun `recursion reaches subfolders`() {
        file("Album1", "a.mp3")
        file("Album1/Disc2", "b.mp3")
        file("Album1/Disc2/Deep", "c.mp3")

        val result = FolderScanner().scan(listOf(root))
        assertEquals(3, result.files.size)
        assertTrue(result.scannedDirs >= 4)
    }

    @Test
    fun `hidden files and dot folders are skipped`() {
        file("", "visible.mp3")
        file("", ".hidden.mp3")
        file("", "Thumbs.db")
        file("", "desktop.ini")
        file(".hiddenFolder", "secret.mp3")

        val result = FolderScanner().scan(listOf(root))
        val names = result.files.map { it.fileName.toString() }
        assertContentEquals(listOf("visible.mp3"), names)
    }

    @Test
    fun `all 21 extensions are found on disk`() {
        Song.SUPPORTED_EXTENSIONS.forEach { ext -> file("", "song.$ext") }
        val result = FolderScanner().scan(listOf(root))
        assertEquals(21, result.files.size)
    }

    @Test
    fun `hard cap is respected`() {
        repeat(30) { i -> file("", "t$i.mp3") }
        val result = FolderScanner(maxSongs = 10).scan(listOf(root))
        assertEquals(10, result.files.size)
        assertTrue(result.truncated)
    }

    @Test
    fun `scan stops when cancelled`() {
        repeat(200) { i -> file("d$i", "t.mp3") }

        val seen = AtomicCounter()
        val result = FolderScanner(maxSongs = 8000).scan(listOf(root)) {
            seen.increment() > 1   // لغو بعد از اولین بررسی
        }
        // لغو نباید باعث خطا شود و نباید همهٔ فایل‌ها را برگرداند
        assertTrue(result.files.size < 200, "cancellation was ignored")
    }

    @Test
    fun `missing root is ignored without throwing`() {
        val result = FolderScanner().scan(listOf(root.resolve("does-not-exist")))
        assertEquals(0, result.files.size)
        assertFalse(result.truncated)
    }

    @Test
    fun `newest file comes first`() {
        val older = file("", "older.mp3")
        val newer = file("", "newer.mp3")
        Files.setLastModifiedTime(older, java.nio.file.attribute.FileTime.fromMillis(1_000_000L))
        Files.setLastModifiedTime(newer, java.nio.file.attribute.FileTime.fromMillis(9_000_000L))

        val result = FolderScanner().scan(listOf(root))
        assertEquals("newer.mp3", result.files.first().fileName.toString())
    }

    @Test
    fun `excluded folders are not walked`() {
        file("keep", "a.mp3")
        file("skip", "b.mp3")

        val result = FolderScanner(excludedDirs = setOf(root.resolve("skip"))).scan(listOf(root))
        assertEquals(1, result.files.size)
        assertEquals("a.mp3", result.files.first().fileName.toString())
    }

    private class AtomicCounter {
        private var n = 0
        fun increment(): Int = ++n
    }
}
