package com.alvand.player

import com.alvand.player.model.Song
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * پورت `SongTest` نسخهٔ اندروید + چند مورد مخصوص ویندوز.
 *
 * تفاوت با اندروید: به‌جای `android.net.Uri` رشتهٔ مسیر/آدرس استفاده می‌شود.
 * همچنین بررسی می‌کنیم فضای آیدی هر سه نوع آهنگ (دیسک، لینک، فایل کشیده‌شده)
 * هیچ‌وقت با هم قاطی نشود.
 */
class SongTest {

    @Test
    fun `fromDirectLink rejects non-https`() {
        assertNull(Song.fromDirectLink("file:///sdcard/a.mp3"))
        assertNull(Song.fromDirectLink("http://example.com/a.mp3"))
        assertNull(Song.fromDirectLink("not a url"))
        assertNull(Song.fromDirectLink(""))
    }

    @Test
    fun `fromDirectLink rejects html url without audio ext`() {
        assertNull(Song.fromDirectLink("https://example.com/page.html"))
        assertNull(Song.fromDirectLink("https://example.com/text"))
    }

    @Test
    fun `fromDirectLink accepts mp3 and m3u8`() {
        assertNotNull(Song.fromDirectLink("https://example.com/song.mp3"))
        assertNotNull(Song.fromDirectLink("https://example.com/live.m3u8?token=1"))
        assertNotNull(Song.fromDirectLink("https://example.com/stream.mpd"))
    }

    @Test
    fun `isSupportedPath does not accept every https`() {
        assertFalse(Song.isSupportedPath("https://example.com/page.html"))
        assertFalse(Song.isSupportedPath("https://example.com/"))
        assertTrue(Song.isSupportedPath("https://example.com/a.mp3"))
        assertTrue(Song.isSupportedPath("/sdcard/a.flac"))
        assertTrue(Song.isSupportedPath("C:\\Music\\a.flac"))
        assertFalse(Song.isSupportedPath("/sdcard/notes.txt"))
    }

    @Test
    fun `every supported extension is accepted`() {
        // ۲۱ پسوند؛ هر کدام باید هم به‌عنوان مسیر محلی و هم به‌عنوان URL پذیرفته شود
        assertEquals(21, Song.SUPPORTED_EXTENSIONS.size)
        Song.SUPPORTED_EXTENSIONS.forEach { ext ->
            assertTrue(
                Song.isSupportedPath("/music/song.$ext"),
                "local path with .$ext should be supported"
            )
            assertTrue(
                Song.isSupportedPath("https://example.com/song.$ext"),
                "https url with .$ext should be supported"
            )
        }
    }

    @Test
    fun `remote ids live in their own space`() {
        val a = Song.fromDirectLink("https://example.com/a.mp3")!!
        val b = Song.fromDirectLink("https://example.com/b.mp3")!!
        assertTrue(a.id < 0)
        assertTrue(b.id < 0)
        assertTrue(a.id < -999_999L)
        assertTrue(b.id < -999_999L)
    }

    @Test
    fun `dragged-file ids live in their own space`() {
        val a = Song.localFileId("C:\\Music\\x.mp3")
        assertTrue(a < 0)
        assertTrue(a < -1_999_999L)
    }

    @Test
    fun `disk song ids never collide with the other two spaces`() {
        // آیدی آهنگ روی دیسک همیشه مثبت است
        val disk = com.alvand.player.library.SongId.forPath("C:\\Music\\song.mp3")
        assertTrue(disk > 0)

        val remote = Song.fromDirectLink("https://example.com/a.mp3")!!.id
        val dragged = Song.localFileId("C:\\Music\\x.mp3")
        assertTrue(remote < 0)
        assertTrue(dragged < 0)
    }

    @Test
    fun `localFileId is negative and stable`() {
        val id1 = Song.localFileId("C:\\Music\\a.mp3")
        val id2 = Song.localFileId("C:\\Music\\a.mp3")
        assertEquals(id1, id2)
        assertTrue(id1 < 0)
    }

    @Test
    fun `mime hint resolved`() {
        assertEquals("audio/mpeg", Song.mimeForUrl("https://x/a.mp3?x=1"))
        assertEquals("application/x-mpegURL", Song.mimeForUrl("https://x/live.m3u8"))
        assertNull(Song.mimeForUrl("https://x/noext"))
    }

    @Test
    fun `mime hint is null for the formats android leaves out`() {
        listOf("wma", "alac", "aiff", "aif", "amr", "mkv").forEach { ext ->
            assertNull(Song.mimeForUrl("https://x/a.$ext"), "$ext should have no mime hint")
        }
    }

    @Test
    fun `title is derived decoded and cleaned`() {
        val song = Song.fromDirectLink("https://example.com/My_Great%20Song.mp3")!!
        assertEquals("My Great Song", song.title)
        assertEquals("Direct link", song.artist)
        assertTrue(song.isRemote)
        assertEquals(0L, song.durationMs)
    }

    @Test
    fun `blank title falls back to Stream`() {
        // پسوند هست (پس معتبر است) ولی نام فایل ندارد، پس باید fallback بخورد
        val song = Song.fromDirectLink("https://example.com/.m3u8")!!
        assertEquals("Stream", song.title)
    }
}
