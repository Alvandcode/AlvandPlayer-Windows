-- 001_initial.sql — پورت ۱:۱ از اسکیمای Room نسخهٔ اندروید v1.6.9
-- (app/schemas/com.alvand.player.data.local.AlvandDatabase/1.json)
--
-- توجه: نام ستون‌ها عیناً همان camelCase خود Room است، نه snake_case.
-- منبع بر پرامپت مقدم است (قانون ۴.۸ پرامپت: اگر پرامپت و سورس اختلاف داشتند،
-- سورس مقدم است).
--
-- این فایل فقط‌رو‌به‌جلو است: هیچ‌وقت ویرایشش نمی‌کنیم، به‌جایش فایل جدید
-- با شمارهٔ بعدی می‌سازیم.
--
-- ترتیب مهم است: اول جدول، بعد ایندکس‌هایش.

CREATE TABLE IF NOT EXISTS playlists (
    id        INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    name      TEXT    NOT NULL,
    createdAt INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS playlist_songs (
    id         INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    playlistId INTEGER NOT NULL,
    songId     INTEGER NOT NULL,
    uri        TEXT    NOT NULL,
    title      TEXT    NOT NULL,
    artist     TEXT    NOT NULL,
    album      TEXT    NOT NULL,
    durationMs INTEGER NOT NULL,
    isRemote   INTEGER NOT NULL,
    position   INTEGER NOT NULL,
    addedAt    INTEGER NOT NULL,
    FOREIGN KEY (playlistId) REFERENCES playlists (id) ON UPDATE NO ACTION ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS play_history (
    id        INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    songId    INTEGER NOT NULL,
    title     TEXT    NOT NULL,
    artist    TEXT    NOT NULL,
    playedAt  INTEGER NOT NULL,
    playCount INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS index_playlist_songs_playlistId ON playlist_songs (playlistId);
CREATE INDEX IF NOT EXISTS index_playlist_songs_songId    ON playlist_songs (songId);
CREATE INDEX IF NOT EXISTS index_play_history_songId      ON play_history (songId);
CREATE INDEX IF NOT EXISTS index_play_history_playedAt    ON play_history (playedAt);
