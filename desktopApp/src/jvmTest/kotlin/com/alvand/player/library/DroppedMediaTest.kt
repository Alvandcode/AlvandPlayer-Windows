package com.alvand.player.library

import java.nio.file.Files
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DroppedMediaTest {

    private lateinit var root: java.nio.file.Path

    @BeforeTest
    fun setUp() {
        root = Files.createTempDirectory("alvand-drop-test")
    }

    @AfterTest
    fun tearDown() {
        runCatching { root.toFile().deleteRecursively() }
    }

    @Test
    fun `folders playable files and unsupported files are separated`() {
        val folder = root.resolve("music").createDirectories()
        val playable = root.resolve("song.mp3").apply { writeText("fake audio") }
        val unsupported = root.resolve("song.wma").apply { writeText("fake audio") }

        val result = DroppedMediaClassifier.classify(
            listOf(folder.toFile(), playable.toFile(), unsupported.toFile(), null)
        )

        assertEquals(listOf(folder.toAbsolutePath().normalize()), result.directories)
        assertEquals(listOf(playable.toAbsolutePath().normalize()), result.playableFiles)
        assertEquals(listOf(unsupported.toAbsolutePath().normalize()), result.unsupportedFiles)
    }

    @Test
    fun `extension matching ignores case and duplicates`() {
        val playable = root.resolve("SONG.FLAC").apply { writeText("fake audio") }
        val result = DroppedMediaClassifier.classify(listOf(playable.toFile(), playable.toFile()))

        assertTrue(result.unsupportedFiles.isEmpty())
        assertEquals(1, result.playableFiles.size)
    }
}
