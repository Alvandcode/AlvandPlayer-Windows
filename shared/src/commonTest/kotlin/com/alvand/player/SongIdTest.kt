package com.alvand.player

import com.alvand.player.library.SongId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * آیدی پایدار آهنگ‌های روی دیسک.
 *
 * مهم‌ترین ویژگی: بین دو بار اسکن نباید عوض شود، وگرنه پلی‌لیست و علاقه‌مندیِ
 * کاربر بی‌صدا خراب می‌شود.
 */
class SongIdTest {

    @Test
    fun `same path always gives the same id`() {
        val a = SongId.forPath("C:\\Music\\Album\\01 - Song.mp3")
        val b = SongId.forPath("C:\\Music\\Album\\01 - Song.mp3")
        assertEquals(a, b)
    }

    @Test
    fun `rescanning a folder does not change any id`() {
        val paths = listOf(
            "C:\\Music\\a.mp3",
            "C:\\Music\\Sub\\b.flac",
            "D:\\Other\\c.wav"
        )
        val firstPass = paths.associateWith { SongId.forPath(it) }
        val secondPass = paths.associateWith { SongId.forPath(it) }
        assertEquals(firstPass, secondPass)
    }

    @Test
    fun `path normalisation makes separators and case irrelevant`() {
        assertEquals(
            SongId.forPath("C:\\Music\\Album\\Song.mp3"),
            SongId.forPath("c:/music/album/song.mp3")
        )
    }

    @Test
    fun `ids are always positive so they cannot hit the remote id space`() {
        val ids = listOf(
            "C:\\Music\\a.mp3",
            "C:\\Music\\b.mp3",
            "Z:\\x\\y\\z.flac",
            "/home/user/Music/song.opus"
        ).map { SongId.forPath(it) }

        ids.forEach { assertTrue(it > 0, "id should be positive but was $it") }
    }

    @Test
    fun `different paths give different ids`() {
        val ids = (1..200).map { SongId.forPath("C:\\Music\\track$it.mp3") }.toSet()
        assertEquals(200, ids.size, "hash collision between distinct paths")
    }

    @Test
    fun `id fits in 56 bits`() {
        // بالاترین بیتِ بایت ششم نباید ست شود (۷ بایت = ۵۶ بیت)
        listOf("a", "b", "C:\\x.mp3", "ü".repeat(10)).forEach {
            val id = SongId.forPath(it)
            assertTrue(id < (1L shl 56), "id out of 56-bit range: $id")
        }
    }
}
