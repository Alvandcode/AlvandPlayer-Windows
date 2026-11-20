package com.alvand.player.data.db

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.sql.ResultSet

/**
 * DAO کتابخانه — پورت ۱:۱ از `LibraryDao.kt` نسخهٔ اندروید.
 *
 * Room جریان (Flow) را خودش مدیریت می‌کرد؛ اینجا خودمان یک StateFlow نگه می‌داریم و
 * بعد از هر نوشتن دوباره از دیتابیس می‌خوانیم و منتشر می‌کنیم. برای جدول‌هایی با
 * این اندازه کاملاً کافی است و هیچ کتابخانهٔ اضافه‌ای لازم ندارد.
 */
class LibraryDao(private val db: Database) {

    private val playlistsFlow = MutableStateFlow<List<PlaylistEntity>>(emptyList())
    private val recentFlow = MutableStateFlow<List<PlayHistoryEntity>>(emptyList())
    private val mostPlayedFlow = MutableStateFlow<List<PlayHistoryEntity>>(emptyList())
    private val playlistSongsFlows = mutableMapOf<Long, MutableStateFlow<List<PlaylistSongEntity>>>()

    // ---------------- Playlists ----------------

    fun observePlaylists(): Flow<List<PlaylistEntity>> {
        refreshPlaylists()
        return playlistsFlow
    }

    fun getPlaylists(): List<PlaylistEntity> =
        db.read { c ->
            c.prepareStatement("SELECT * FROM ${Schema.PLAYLISTS} ORDER BY createdAt DESC")
                .useRs { st -> st.executeQuery().useRs { rs -> rs.mapRows(::readPlaylist) } }
        }

    fun insertPlaylist(name: String, createdAt: Long = System.currentTimeMillis()): Long {
        val id = db.read { c ->
            c.prepareStatement("INSERT INTO ${Schema.PLAYLISTS} (name, createdAt) VALUES (?, ?)")
                .useRs { st ->
                    st.setString(1, name)
                    st.setLong(2, createdAt)
                    st.executeUpdate().toLong()
                }
        }
        refreshPlaylists()
        return id
    }

    fun deletePlaylist(id: Long) {
        // ON DELETE CASCADE در مهاجرت تعریف شده و PRAGMA foreign_keys روشن است
        db.read { c ->
            c.prepareStatement("DELETE FROM ${Schema.PLAYLISTS} WHERE id = ?").useRs { st ->
                st.setLong(1, id)
                st.executeUpdate()
            }
        }
        playlistSongsFlows.remove(id)
        refreshPlaylists()
    }

    fun renamePlaylist(id: Long, name: String) {
        db.read { c ->
            c.prepareStatement("UPDATE ${Schema.PLAYLISTS} SET name = ? WHERE id = ?").useRs { st ->
                st.setString(1, name)
                st.setLong(2, id)
                st.executeUpdate()
            }
        }
        refreshPlaylists()
    }

    // ---------------- Songs in playlist ----------------

    fun observePlaylistSongs(pid: Long): Flow<List<PlaylistSongEntity>> {
        val flow = playlistSongsFlows.getOrPut(pid) { MutableStateFlow(emptyList()) }
        refreshPlaylistSongs(pid)
        return flow
    }

    fun getPlaylistSongs(pid: Long): List<PlaylistSongEntity> = queryPlaylistSongs(pid)

    fun countInPlaylist(pid: Long, sid: Long): Int =
        db.read { c ->
            c.prepareStatement(
                "SELECT COUNT(*) FROM ${Schema.PLAYLIST_SONGS} WHERE playlistId = ? AND songId = ?"
            ).useRs { st ->
                st.setLong(1, pid)
                st.setLong(2, sid)
                st.executeQuery().useRs { rs -> if (rs.next()) rs.getInt(1) else 0 }
            }
        }

    /** ترتیب فقط-اضافه‌کردن: همیشه انتهای فهرست */
    fun nextPosition(pid: Long): Int =
        db.read { c ->
            c.prepareStatement(
                "SELECT COALESCE(MAX(position), -1) + 1 FROM ${Schema.PLAYLIST_SONGS} WHERE playlistId = ?"
            ).useRs { st ->
                st.setLong(1, pid)
                st.executeQuery().useRs { rs -> if (rs.next()) rs.getInt(1) else 0 }
            }
        }

    fun insertPlaylistSong(e: PlaylistSongEntity): Long {
        val id = db.read { c ->
            c.prepareStatement(
                """
                INSERT INTO ${Schema.PLAYLIST_SONGS}
                (playlistId, songId, uri, title, artist, album, durationMs, isRemote, position, addedAt)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()
            ).useRs { st ->
                st.setLong(1, e.playlistId)
                st.setLong(2, e.songId)
                st.setString(3, e.uri)
                st.setString(4, e.title)
                st.setString(5, e.artist)
                st.setString(6, e.album)
                st.setLong(7, e.durationMs)
                st.setInt(8, if (e.isRemote) 1 else 0)
                st.setInt(9, e.position)
                st.setLong(10, e.addedAt)
                st.executeUpdate().toLong()
            }
        }
        refreshPlaylistSongs(e.playlistId)
        return id
    }

    fun removeFromPlaylist(pid: Long, sid: Long) {
        db.read { c ->
            c.prepareStatement(
                "DELETE FROM ${Schema.PLAYLIST_SONGS} WHERE playlistId = ? AND songId = ?"
            ).useRs { st ->
                st.setLong(1, pid)
                st.setLong(2, sid)
                st.executeUpdate()
            }
        }
        refreshPlaylistSongs(pid)
    }

    // ---------------- History ----------------

    fun observeRecent(limit: Int = 50): Flow<List<PlayHistoryEntity>> {
        refreshRecent(limit)
        return recentFlow
    }

    /** مرتب بر اساس تعداد پخش، نه زمان — سهمیهٔ «پرتکرارترین‌ها» */
    fun observeMostPlayed(limit: Int = 20): Flow<List<PlayHistoryEntity>> {
        refreshMostPlayed(limit)
        return mostPlayedFlow
    }
    fun getMostPlayed(limit: Int = 20): List<PlayHistoryEntity> =
        db.read { c ->
            c.prepareStatement("SELECT * FROM ${Schema.PLAY_HISTORY} ORDER BY playCount DESC, playedAt DESC LIMIT ?")
                .useRs { st ->
                    st.setInt(1, limit)
                    st.executeQuery().useRs { rs -> rs.mapRows(::readHistory) }
                }
        }

    fun getRecent(limit: Int = 50): List<PlayHistoryEntity> =
        db.read { c ->
            c.prepareStatement("SELECT * FROM ${Schema.PLAY_HISTORY} ORDER BY playedAt DESC LIMIT ?")
                .useRs { st ->
                    st.setInt(1, limit)
                    st.executeQuery().useRs { rs -> rs.mapRows(::readHistory) }
                }
        }

    fun historyFor(sid: Long): PlayHistoryEntity? =
        db.read { c ->
            c.prepareStatement("SELECT * FROM ${Schema.PLAY_HISTORY} WHERE songId = ? LIMIT 1")
                .useRs { st ->
                    st.setLong(1, sid)
                    st.executeQuery().useRs { rs -> if (rs.next()) readHistory(rs) else null }
                }
        }

    /**
     * معادل `OnConflictStrategy.REPLACE` روم.
     *
     * نکتهٔ مهم که روم پنهان می‌کرد: وقتی `id = 0` باشد، یعنی «آیدی بساز» و SQLite
     * باید خودش آیدی بدهد. اگر 0 را واقعاً بنویسیم، همهٔ رکوردها روی یک کلید می‌افتند
     * و رکوردهای قبلی بی‌صدا بازنویسی می‌شوند — دقیقاً همان اشتباهی که اول پیاده
     * کرده بودیم. پس برای `id == 0` مقدار NULL می‌فرستیم.
     */
    fun upsertHistory(e: PlayHistoryEntity) {
        db.read { c ->
            c.prepareStatement(
                """
                INSERT OR REPLACE INTO ${Schema.PLAY_HISTORY}
                (id, songId, title, artist, playedAt, playCount)
                VALUES (?, ?, ?, ?, ?, ?)
                """.trimIndent()
            ).useRs { st ->
                if (e.id == 0L) st.setNull(1, java.sql.Types.INTEGER) else st.setLong(1, e.id)
                st.setLong(2, e.songId)
                st.setString(3, e.title)
                st.setString(4, e.artist)
                st.setLong(5, e.playedAt)
                st.setInt(6, e.playCount)
                st.executeUpdate()
            }
        }
        refreshRecent()
    }

    fun pruneHistory(before: Long) {
        db.read { c ->
            c.prepareStatement("DELETE FROM ${Schema.PLAY_HISTORY} WHERE playedAt < ?").useRs { st ->
                st.setLong(1, before)
                st.executeUpdate()
            }
        }
        refreshRecent()
    }

    fun clearHistory() {
        db.read { c ->
            c.createStatement().useRs { it.executeUpdate("DELETE FROM ${Schema.PLAY_HISTORY}") }
        }
        refreshRecent()
    }

    // ---------------- refresh ----------------

    private fun refreshPlaylists() {
        playlistsFlow.value = getPlaylists()
    }

    private fun refreshPlaylistSongs(pid: Long) {
        playlistSongsFlows[pid]?.value = queryPlaylistSongs(pid)
    }

    private fun refreshRecent(limit: Int = 50) {
        recentFlow.value = getRecent(limit)
        mostPlayedFlow.value = getMostPlayed(20)
    }

    private fun refreshMostPlayed(limit: Int = 20) {
        mostPlayedFlow.value = getMostPlayed(limit)
    }

    private fun queryPlaylistSongs(pid: Long): List<PlaylistSongEntity> =
        db.read { c ->
            c.prepareStatement(
                "SELECT * FROM ${Schema.PLAYLIST_SONGS} WHERE playlistId = ? ORDER BY position ASC, addedAt ASC"
            ).useRs { st ->
                st.setLong(1, pid)
                st.executeQuery().useRs { rs -> rs.mapRows(::readPlaylistSong) }
            }
        }

    // ---------------- row mappers ----------------

    private fun readPlaylist(rs: ResultSet) = PlaylistEntity(
        id = rs.getLong("id"),
        name = rs.getString("name"),
        createdAt = rs.getLong("createdAt")
    )

    private fun readPlaylistSong(rs: ResultSet) = PlaylistSongEntity(
        id = rs.getLong("id"),
        playlistId = rs.getLong("playlistId"),
        songId = rs.getLong("songId"),
        uri = rs.getString("uri"),
        title = rs.getString("title"),
        artist = rs.getString("artist"),
        album = rs.getString("album"),
        durationMs = rs.getLong("durationMs"),
        isRemote = rs.getInt("isRemote") != 0,
        position = rs.getInt("position"),
        addedAt = rs.getLong("addedAt")
    )

    private fun readHistory(rs: ResultSet) = PlayHistoryEntity(
        id = rs.getLong("id"),
        songId = rs.getLong("songId"),
        title = rs.getString("title"),
        artist = rs.getString("artist"),
        playedAt = rs.getLong("playedAt"),
        playCount = rs.getInt("playCount")
    )
}

// ---------------- JDBC helpers ----------------

/**
 * `kotlin.use` فقط روی `Closeable` کار می‌کند ولی `ResultSet` و `Statement` فقط
 * `AutoCloseable` هستند؛ این کمکی همان الگو را برای آن‌ها می‌دهد.
 *
 * نوع عمومی `S : Statement` عمدی است تا زیرنوع واقعی (`PreparedStatement`) حفظ شود
 * و متدهایی مثل `setLong` در دسترس بمانند.
 */
internal inline fun <S : java.sql.Statement, T> S.useRs(block: (S) -> T): T =
    try {
        block(this)
    } finally {
        runCatching { close() }
    }

internal inline fun <T> ResultSet.useRs(block: (ResultSet) -> T): T =
    try {
        block(this)
    } finally {
        runCatching { close() }
    }

internal fun <T> ResultSet.mapRows(map: (ResultSet) -> T): List<T> {
    val out = ArrayList<T>()
    while (next()) out.add(map(this))
    return out
}
