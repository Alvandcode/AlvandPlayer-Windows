package com.alvand.player.data.db

/** پلی‌لیست — پورت `PlaylistEntity` */
data class PlaylistEntity(
    val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * آهنگ داخل پلی‌لیست — `songId` همان `Song.id` است.
 * uri/title/artist/album/duration کش می‌شود تا اگر فایل از دیسک حذف شد،
 * پلی‌لیست نشکند (تصمیم عمدی نسخهٔ اندروید).
 */
data class PlaylistSongEntity(
    val id: Long = 0,
    val playlistId: Long,
    val songId: Long,
    val uri: String,
    val title: String,
    val artist: String,
    val album: String = "",
    val durationMs: Long = 0L,
    val isRemote: Boolean = false,
    val position: Int = 0,
    val addedAt: Long = System.currentTimeMillis()
)

/** رکورد تاریخچهٔ پخش */
data class PlayHistoryEntity(
    val id: Long = 0,
    val songId: Long,
    val title: String,
    val artist: String,
    val playedAt: Long = System.currentTimeMillis(),
    val playCount: Int = 1
)
