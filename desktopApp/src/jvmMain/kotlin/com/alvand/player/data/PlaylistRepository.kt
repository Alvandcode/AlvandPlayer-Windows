package com.alvand.player.data

import com.alvand.player.data.db.LibraryDao
import com.alvand.player.data.db.PlayHistoryEntity
import com.alvand.player.data.db.PlaylistEntity
import com.alvand.player.data.db.PlaylistSongEntity
import com.alvand.player.model.Song
import kotlinx.coroutines.flow.Flow

/** مدل دامنهٔ پلی‌لیست برای UI */
data class Playlist(
    val id: Long,
    val name: String,
    val songCount: Int = 0
)

/**
 * پورت `PlaylistRepository.kt` نسخهٔ اندروید، با تزریق DAO.
 *
 * منطق عیناً همان است: نام trim و به ۶۰ کاراکتر محدود، افزودن بدون تکرار،
 * تاریخچه با شمارش تجمعی و هرس ۹۰ روزه.
 */
class PlaylistRepository(
    private val dao: LibraryDao,
    private val now: () -> Long = { System.currentTimeMillis() }
) {

    fun observePlaylists(): Flow<List<PlaylistEntity>> = dao.observePlaylists()

    fun observePlaylistSongs(pid: Long): Flow<List<PlaylistSongEntity>> = dao.observePlaylistSongs(pid)

    fun observeRecent(limit: Int = 50): Flow<List<PlayHistoryEntity>> = dao.observeRecent(limit)

    fun observeMostPlayed(limit: Int = 20): Flow<List<PlayHistoryEntity>> = dao.observeMostPlayed(limit)

    fun createPlaylist(name: String): Long {
        val clean = name.trim().take(60)
        require(clean.isNotBlank()) { "empty name" }
        return dao.insertPlaylist(clean, now())
    }

    fun renamePlaylist(id: Long, name: String) {
        val clean = name.trim().take(60)
        if (clean.isBlank()) return
        dao.renamePlaylist(id, clean)
    }

    fun deletePlaylist(id: Long) = dao.deletePlaylist(id)

    /** اضافه به پلی‌لیست بدون تکرار؛ position خودکار */
    fun addToPlaylist(pid: Long, song: Song): Boolean {
        if (dao.countInPlaylist(pid, song.id) > 0) return false
        dao.insertPlaylistSong(
            PlaylistSongEntity(
                playlistId = pid,
                songId = song.id,
                uri = song.uri,
                title = song.title,
                artist = song.artist,
                album = song.album,
                durationMs = song.durationMs,
                isRemote = song.isRemote,
                position = dao.nextPosition(pid),
                addedAt = now()
            )
        )
        return true
    }

    fun removeFromPlaylist(pid: Long, songId: Long) = dao.removeFromPlaylist(pid, songId)

    fun recordPlay(song: Song) {
        val prev = dao.historyFor(song.id)
        val t = now()
        dao.upsertHistory(
            PlayHistoryEntity(
                id = prev?.id ?: 0L,
                songId = song.id,
                title = song.title,
                artist = song.artist,
                playedAt = t,
                playCount = (prev?.playCount ?: 0) + 1
            )
        )
        // نگه‌داشت ۹۰ روز اخیر
        dao.pruneHistory(t - HISTORY_RETENTION_MS)
    }

    fun clearHistory() = dao.clearHistory()

    fun entityToSong(e: PlaylistSongEntity): Song = Song(
        id = e.songId,
        title = e.title,
        artist = e.artist,
        album = e.album,
        uri = e.uri,
        durationMs = e.durationMs,
        isRemote = e.isRemote
    )

    companion object {
        const val HISTORY_RETENTION_MS = 90L * 24 * 60 * 60 * 1000
        const val MAX_NAME_LENGTH = 60
    }
}
