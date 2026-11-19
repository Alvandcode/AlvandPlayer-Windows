package com.alvand.player.player

/**
 * وضعیت موتور پخش — تنها منبع حقیقت UI.
 *
 * همه‌چیز UI از همین می‌خواند، پس موتور و UI کاملاً از هم جدا می‌مانند و می‌شود
 * موتور را با یک پیاده‌سازی ساختگی تست کرد.
 */
data class EngineState(
    val current: PlayableItem? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val queue: List<PlayableItem> = emptyList(),
    val index: Int = -1,
    val shuffle: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val error: String? = null,
    /** برای استریم زنده: مدت‌زمان نامعلوم است و کشیدن معنا ندارد */
    val isLiveStream: Boolean = false,
    /** حلقهٔ A–B (میلی‌ثانیه) — null یعنی خاموش */
    val loopA: Long? = null,
    val loopB: Long? = null,
    /** طیف لحظه‌ای برای ویژوالایزر: ۲۴ باند ۰..۱ */
    val spectrum: List<Float> = emptyList()
) {
    /** مدت‌زمان فقط وقتی معتبر است که مقدار معنادار داشته باشد */
    val hasKnownDuration: Boolean get() = durationMs > 1 && !isLiveStream

    /** نسبت پیشرفت، همیشه در بازهٔ ‎0..1‎ */
    val progress: Float
        get() = if (hasKnownDuration) {
            (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        } else 0f

    val isSeeking: Boolean get() = false
}

/** آیتمی که موتور می‌تواند پخش کند */
data class PlayableItem(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String = "",
    /** مسیر فایل محلی، یا آدرس HTTPS برای لینک مستقیم */
    val uri: String,
    val durationMs: Long = 0L,
    val isRemote: Boolean = false,
    val mimeHint: String? = null,
    /** بهرهٔ ReplayGain بر حسب دسی‌بل — null یعنی ندارد */
    val replayGainDb: Float? = null
) {
    companion object {
        fun fromSong(song: com.alvand.player.model.Song): PlayableItem = PlayableItem(
            id = song.id,
            title = song.title,
            artist = song.artist,
            album = song.album,
            uri = song.uri,
            durationMs = song.durationMs,
            isRemote = song.isRemote,
            mimeHint = song.mimeHint,
            replayGainDb = song.replayGainDb
        )
    }
}

enum class RepeatMode {
    OFF, ALL, ONE;

    fun next(): RepeatMode = when (this) {
        OFF -> ALL
        ALL -> ONE
        ONE -> OFF
    }

    companion object {
        fun of(value: Int): RepeatMode = entries.getOrElse(value) { OFF }
    }
}
