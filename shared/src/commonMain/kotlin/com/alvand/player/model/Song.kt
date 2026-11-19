package com.alvand.player.model

import java.net.URLDecoder

/**
 * مدل آهنگ — هم فایل لوکال، هم لینک مستقیم.
 *
 * پورت ۱:۱ از `data/Song.kt` نسخهٔ اندروید، با یک تفاوت:
 * اندروید `uri` را از نوع `android.net.Uri` می‌گیرد که روی دسکتاپ وجود ندارد،
 * پس اینجا همان رشتهٔ URI نگه داشته می‌شود (برای فایل لوکال، مسیر کامل؛
 * برای لینک، خودِ آدرس https). بقیهٔ منطق دست‌نخورده است.
 */
data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String = "",
    val uri: String,
    val durationMs: Long = 0L,
    val isRemote: Boolean = false,
    val mimeHint: String? = null,
    /** بهرهٔ ReplayGain بر حسب دسی‌بل (تگ REPLAYGAIN_TRACK_GAIN) — null یعنی ندارد */
    val replayGainDb: Float? = null,
    /** ژانر از تگ — برای تب Genres کتابخانه */
    val genre: String = ""
) {
    companion object {
        /** پسوندهای صوتی پشتیبانی‌شده (برای فایل‌پیکر و اعتبارسنجی لینک) */
        val SUPPORTED_EXTENSIONS = setOf(
            "mp3", "wav", "flac", "ogg", "oga", "opus", "m4a",
            "aac", "wma", "alac", "aiff", "aif", "amr", "mid",
            "midi", "mp4", "mka", "mkv", "ts", "m3u8", "mpd"
        )

        /**
         * نگاشت پسوند به MIME.
         * دقت: برای `wma, alac, aiff, aif, amr, mkv` عمداً **بدون** ورودی است —
         * همان‌طور که در اندروید هم هست و انتظار می‌رود `null` برگردانند.
         */
        private val AUDIO_MIME_BY_EXT = mapOf(
            "mp3" to "audio/mpeg", "wav" to "audio/wav", "flac" to "audio/flac",
            "ogg" to "audio/ogg", "oga" to "audio/ogg", "opus" to "audio/opus",
            "m4a" to "audio/mp4", "aac" to "audio/aac", "mid" to "audio/midi",
            "midi" to "audio/midi", "m3u8" to "application/x-mpegURL",
            "mpd" to "application/dash+xml", "mp4" to "audio/mp4",
            "mka" to "audio/x-matroska", "ts" to "video/mp2t"
        )

        fun mimeForUrl(url: String): String? {
            val ext = url.substringBefore('?').substringAfterLast('.', "").lowercase()
            return AUDIO_MIME_BY_EXT[ext]
        }

        fun fromDirectLink(url: String, titleFallback: String = "Stream"): Song? {
            val trimmed = url.trim()
            // عمداً حساس به بزرگی/کوچکی حروف، درست مثل اندروید
            if (!trimmed.startsWith("https://")) return null
            // فقط https کافی نیست — باید پسوند صوتی یا هینت استریم داشته باشد
            if (!isSupportedPath(trimmed)) return null
            var title = trimmed.substringAfterLast('/').substringBefore('?')
                .ifBlank { titleFallback }
            // دیکد %20 و حذف پسوند برای عنوان تمیز
            title = runCatching { URLDecoder.decode(title, "UTF-8") }.getOrNull() ?: title
            if (title.substringAfterLast('.', "").lowercase() in SUPPORTED_EXTENSIONS) {
                title = title.substringBeforeLast('.')
            }
            title = title.replace('_', ' ').replace(Regex("\\s+"), " ").trim()
                .ifBlank { titleFallback }
            // آیدی منفی پایدار در فضای جدا از آیدی فایل‌های محلی
            val stableId = -(trimmed.hashCode().toLong() and 0x7FFFFFFFL) - 1_000_000L
            return Song(
                id = stableId,
                title = title,
                artist = "Direct link",
                uri = trimmed,
                isRemote = true,
                mimeHint = mimeForUrl(trimmed)
            )
        }

        fun isSupportedPath(path: String): Boolean {
            val p = path.trim()
            val lower = p.lowercase()
            val isHttps = lower.startsWith("https://")
            val withoutQuery = p.substringBefore('?').substringBefore('#')
            val fileName = withoutQuery.substringAfterLast('/')
            val ext = fileName.substringAfterLast('.', "").lowercase()
            if (ext.isNotEmpty() && ext in SUPPORTED_EXTENSIONS) return true
            // لینک بدون پسوند فقط اگر هینت HLS/DASH داشته باشد
            if (isHttps) {
                if (lower.contains(".m3u8") || lower.contains(".mpd")) return true
                // بقیهٔ URLهای متنی/html قبول نیست
                return false
            }
            return false
        }

        /**
         * آیدی فایل لوکالِ انتخاب‌شده توسط کاربر (مثل کشیدن فایل روی پنجره).
         * فضای منفی جدا، بدون کالیشن با آیدی لینک مستقیم.
         */
        fun localFileId(uri: String): Long {
            val h = uri.hashCode().toLong() and 0x7FFFFFFFL
            return -(h + 2_000_000L)
        }
    }
}
