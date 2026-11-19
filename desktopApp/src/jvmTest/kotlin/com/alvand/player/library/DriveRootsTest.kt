package com.alvand.player.library

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DriveRootsTest {

    private lateinit var root: Path

    @BeforeTest
    fun setUp() {
        root = Files.createTempDirectory("alvand-drive-roots-test")
    }

    @AfterTest
    fun tearDown() {
        runCatching { root.toFile().deleteRecursively() }
    }

    @Test
    fun `all accessible drives except C are selected`() {
        val mounted = listOf(
            Path.of("C:\\"),
            Path.of("D:\\"),
            Path.of("E:\\"),
            Path.of("F:\\")
        )

        val selected = DriveRoots.defaultScanRoots(
            mountedRoots = mounted,
            systemDrive = "C:",
            windowsDirectory = "C:\\Windows",
            isAccessible = { it != Path.of("F:\\").toAbsolutePath().normalize() }
        )

        assertEquals(
            listOf(
                Path.of("D:\\").toAbsolutePath().normalize(),
                Path.of("E:\\").toAbsolutePath().normalize()
            ),
            selected
        )
    }

    @Test
    fun `an unusual system drive is also excluded along with C`() {
        val selected = DriveRoots.defaultScanRoots(
            mountedRoots = listOf(Path.of("C:\\"), Path.of("D:\\")),
            systemDrive = "D:",
            windowsDirectory = "D:\\Windows",
            isAccessible = { true }
        )

        assertTrue(selected.isEmpty(), "only the system and C drives were mounted")
    }

    @Test
    fun `dropped roots are added in one update without duplicates`() {
        val file = root.resolve("batch-roots.txt")
        val roots = LibraryRoots(file)
        val first = root.resolve("D-music").createDirectories()
        val second = root.resolve("E-music").createDirectories()

        roots.addRoots(listOf(first, second, first))

        assertEquals(
            listOf(
                first.toAbsolutePath().normalize(),
                second.toAbsolutePath().normalize()
            ),
            roots.current
        )
    }

    @Test
    fun `seeded defaults are added once and never overwrite user roots`() {
        val file = root.resolve("roots.txt")
        val roots = LibraryRoots(file)
        val music = root.resolve("Music").createDirectories()

        roots.seedDefaults(listOf(music))
        roots.seedDefaults(listOf(root.resolve("Other")))

        assertEquals(listOf(music.toAbsolutePath().normalize()), roots.current)
    }

    @Test
    fun `non-system drive roots are preserved alongside an existing C root`() {
        val file = root.resolve("merged-roots.txt")
        val roots = LibraryRoots(file)
        val system = root.resolve("C-music").createDirectories()
        val external = root.resolve("D-music").createDirectories()
        roots.add(system)

        roots.ensureDriveRoots(listOf(system, external, external))

        assertEquals(
            listOf(
                system.toAbsolutePath().normalize(),
                external.toAbsolutePath().normalize()
            ),
            roots.current
        )
    }
}
