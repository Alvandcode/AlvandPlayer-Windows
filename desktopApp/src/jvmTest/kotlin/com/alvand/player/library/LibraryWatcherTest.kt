package com.alvand.player.library

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LibraryWatcherTest {

    private lateinit var dir: Path

    @BeforeTest
    fun setUp() {
        dir = createTempDirectory("alvand-watcher-test")
    }

    @AfterTest
    fun tearDown() {
        runCatching { dir.toFile().deleteRecursively() }
    }

    @Test
    fun `audio file creation is reported after debounce`() {
        val latch = CountDownLatch(1)
        var reported: Set<Path> = emptySet()
        val watcher = LibraryWatcher(
            scope = kotlinx.coroutines.CoroutineScope(Dispatchers.Default),
            debounceMs = 300L,
            onChanged = { changed, _ ->
                reported = changed
                latch.countDown()
            }
        )
        watcher.start(listOf(dir))
        try {
            // برای اینکه WatchService فرصت ثبت پوشه را پیدا کند
            Thread.sleep(300)
            val song = dir.resolve("song1.mp3")
            Files.write(song, byteArrayOf(0, 1, 2, 3))
            assertTrue(latch.await(15, TimeUnit.SECONDS), "watcher did not report the new file")
            assertTrue(reported.any { it.fileName.toString() == "song1.mp3" }, "reported=$reported")
        } finally {
            watcher.stop()
        }
    }

    @Test
    fun `deletion is reported as deleted`() {
        val song = dir.resolve("gone.mp3")
        Files.write(song, byteArrayOf(1, 2, 3))
        Thread.sleep(200)

        val latch = CountDownLatch(1)
        var deleted: Set<Path> = emptySet()
        val watcher = LibraryWatcher(
            scope = kotlinx.coroutines.CoroutineScope(Dispatchers.Default),
            debounceMs = 300L,
            onChanged = { _, gone ->
                if (gone.isNotEmpty()) {
                    deleted = gone
                    latch.countDown()
                }
            }
        )
        watcher.start(listOf(dir))
        try {
            Thread.sleep(500)
            runCatching { Files.delete(song) }
            assertTrue(latch.await(15, TimeUnit.SECONDS), "watcher did not report the deletion")
            assertTrue(deleted.any { it.fileName.toString() == "gone.mp3" }, "deleted=$deleted")
        } finally {
            watcher.stop()
        }
    }

    @Test
    fun `non audio files are ignored`() {
        val latch = CountDownLatch(1)
        var called = false
        val watcher = LibraryWatcher(
            scope = kotlinx.coroutines.CoroutineScope(Dispatchers.Default),
            debounceMs = 300L,
            onChanged = { _, _ ->
                called = true
                latch.countDown()
            }
        )
        watcher.start(listOf(dir))
        try {
            Thread.sleep(300)
            Files.write(dir.resolve("notes.txt"), byteArrayOf(1))
            Files.write(dir.resolve("image.png"), byteArrayOf(1))
            // هرگز نباید فراخوان شود — با مهلت کمتر از debounce تکراری ولی متعارف
            assertEquals(false, latch.await(4, TimeUnit.SECONDS), "non-audio files must be ignored")
            assertEquals(false, called)
        } finally {
            watcher.stop()
        }
    }
}
