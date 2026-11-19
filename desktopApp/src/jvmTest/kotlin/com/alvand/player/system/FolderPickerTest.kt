package com.alvand.player.system

import java.nio.file.Files
import kotlin.io.path.writeText
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FolderPickerTest {

    private lateinit var root: java.nio.file.Path

    @BeforeTest
    fun setUp() {
        root = Files.createTempDirectory("alvand-picker-test")
    }

    @AfterTest
    fun tearDown() {
        runCatching { root.toFile().deleteRecursively() }
    }

    @Test
    fun `audio filter accepts folders and the formats the user asked for`() {
        val filter = FolderPicker.SupportedAudioFileFilter()
        val folder = Files.createDirectory(root.resolve("music")).toFile()
        val requested = listOf("song.mp3", "song.wav", "song.flac", "song.m4a", "song.mp4")
            .map { root.resolve(it).apply { writeText("fake audio") }.toFile() }

        assertTrue(filter.accept(folder), "folders must stay selectable for navigation")
        requested.forEach { assertTrue(filter.accept(it), "${it.name} should be selectable") }
    }

    @Test
    fun `audio filter rejects formats the engine cannot decode`() {
        val filter = FolderPicker.SupportedAudioFileFilter()
        val unsupported = root.resolve("song.wma").apply { writeText("fake audio") }.toFile()

        assertFalse(filter.accept(unsupported), "an undecodable file must not look selectable")
        assertTrue(filter.description.contains("*.mp3"), "the filter should name a supported type")
    }
}
