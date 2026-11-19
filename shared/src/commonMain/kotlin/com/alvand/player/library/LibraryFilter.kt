package com.alvand.player.library

import com.alvand.player.model.Song

/** حالت مرتب‌سازی کتابخانه — در SettingsStore ذخیره می‌شود */
object LibrarySort {
    const val DEFAULT = 0 // ترتیب اسکن (جدیدترین اول)
    const val TITLE_AZ = 1
    const val ARTIST_AZ = 2
    const val DURATION_DESC = 3
    const val DURATION_ASC = 4
}

/**
 * منطق خالص فیلتر + سورت کتابخانه.
 * قانون: favoritesOnly اول، بعد search، بعد sort پایدار.
 *
 * پورت ۱:۱ از `data/LibraryFilter.kt` نسخهٔ اندروید (که آنجا هم از اندروید مستقل
 * بود و قابل تست طراحی شده بود).
 */
object LibraryFilter {

    fun filterAndSort(
        songs: List<Song>,
        query: String,
        sortMode: Int,
        liked: Set<Long>,
        favoritesOnly: Boolean
    ): List<Song> {
        var out = songs
        if (favoritesOnly) {
            out = out.filter { it.id in liked }
        }
        val q = query.trim()
        if (q.isNotEmpty()) {
            out = out.filter { matchesQuery(it, q) }
        }
        return when (sortMode) {
            LibrarySort.TITLE_AZ -> out.sortedWith(compareBy({ it.title.lowercase() }, { it.artist.lowercase() }))
            LibrarySort.ARTIST_AZ -> out.sortedWith(compareBy({ it.artist.lowercase() }, { it.title.lowercase() }))
            LibrarySort.DURATION_DESC -> out.sortedByDescending { it.durationMs }
            LibrarySort.DURATION_ASC -> out.sortedBy { it.durationMs }
            else -> out
        }
    }

    /** ایندکس امن برای playList روی لیست فیلترشده */
    fun indexOf(queue: List<Song>, songId: Long): Int =
        queue.indexOfFirst { it.id == songId }.coerceAtLeast(0)

    /**
     * جستجوی عملگری اوریجینال الوند — بدون وابستگی جدید، بدون تغییر رفتار قدیمی.
     *
     *  - متن آزاد (بدون `:`) دقیقاً مثل قبل روی عنوان/خواننده/آلبوم می‌گردد.
     *  - عملگرها: `artist:`, `album:`, `title:` (و معادل فارسی `خواننده:`
     *    `آلبوم:` `عنوان:`) — هر کدام AND می‌شوند.
     *  - مقدار می‌تواند داخل `"..."` باشد تا فاصله داشته باشد:
     *    `artist:"cold play" night`
     *  - عملگر ناشناخته (`foo:bar`) به‌عنوان متن آزاد حساب می‌شود، نه خطا.
     */
    fun matchesQuery(song: Song, rawQuery: String): Boolean {
        val q = rawQuery.trim()
        if (q.isEmpty()) return true
        val tokens = tokenize(q)
        if (tokens.none { isKnownOperator(it) }) {
            val lower = q.lowercase()
            return song.title.lowercase().contains(lower) ||
                song.artist.lowercase().contains(lower) ||
                song.album.lowercase().contains(lower)
        }
        var free = StringBuilder()
        for (tok in tokens) {
            val op = parseOperator(tok)
            if (op == null) {
                if (free.isNotEmpty()) free.append(' ')
                free.append(tok.removeSurrounding("\""))
                continue
            }
            val field = op.first
            val value = op.second
            if (value.isBlank()) continue
            val v = value.lowercase()
            val ok = when (field) {
                "artist" -> song.artist.lowercase().contains(v)
                "album" -> song.album.lowercase().contains(v)
                "title" -> song.title.lowercase().contains(v)
                else -> true
            }
            if (!ok) return false
        }
        val freeText = free.toString().trim().lowercase()
        if (freeText.isEmpty()) return true
        return song.title.lowercase().contains(freeText) ||
            song.artist.lowercase().contains(freeText) ||
            song.album.lowercase().contains(freeText)
    }

    private fun isKnownOperator(token: String): Boolean {
        val idx = token.indexOf(':')
        if (idx <= 0) return false
        // مقدار داخل کوتیشن ممکن است `:` داشته باشد — فقط پیشوند بیرون مهم است
        val head = token.substring(0, idx).lowercase().trim('"')
        return head in KNOWN_FIELDS
    }

    private fun parseOperator(token: String): Pair<String, String>? {
        val idx = token.indexOf(':')
        if (idx <= 0) return null
        var head = token.substring(0, idx).lowercase().trim()
        head = head.trim('"')
        val field: String = when (head) {
            "artist", "خواننده" -> "artist"
            "album", "آلبوم" -> "album"
            "title", "عنوان", "آهنگ" -> "title"
            else -> return null
        }
        var value = token.substring(idx + 1).trim()
        if (value.startsWith("\"") && value.endsWith("\"") && value.length >= 2) {
            value = value.substring(1, value.length - 1)
        }
        return Pair(field, value)
    }

    private val KNOWN_FIELDS = setOf("artist", "album", "title", "خواننده", "آلبوم", "عنوان", "آهنگ")

    /** توکنایز ساده با احترام به `"..."` تا مقدار عملگر فاصله داشته باشد */
    internal fun tokenize(query: String): List<String> {
        val out = ArrayList<String>()
        val cur = StringBuilder()
        var inQuotes = false
        for (c in query) {
            when {
                c == '"' -> { inQuotes = !inQuotes; cur.append(c) }
                c.isWhitespace() && !inQuotes -> {
                    if (cur.isNotEmpty()) { out.add(cur.toString()); cur.clear() }
                }
                else -> cur.append(c)
            }
        }
        if (cur.isNotEmpty()) out.add(cur.toString())
        return out
    }

    /** گروه‌بندی آلبوم — کلید نام آلبوم، هنرمند = پرتعدادترین خوانندهٔ گروه */
    fun groupByAlbum(songs: List<Song>): List<AlbumGroup> =
        songs.groupBy { it.album.trim().ifBlank { "Unknown Album" } }
            .map { (album, list) ->
                AlbumGroup(
                    name = album,
                    artist = list.groupBy { it.artist }.maxByOrNull { it.value.size }?.key ?: "",
                    songs = list,
                    artworkSong = list.firstOrNull()
                )
            }
            .sortedBy { it.name.lowercase() }

    /** گروه‌بندی خواننده — تعداد آلبوم‌های متمایز */
    fun groupByArtist(songs: List<Song>): List<ArtistGroup> =
        songs.groupBy { it.artist.trim().ifBlank { "Unknown Artist" } }
            .map { (artist, list) ->
                ArtistGroup(name = artist, songs = list, albums = list.map { it.album }.distinct().size)
            }
            .sortedBy { it.name.lowercase() }

    /** گروه‌بندی ژانر — خالی‌ها زیر «Other» می‌روند تا گم نشوند */
    fun groupByGenre(songs: List<Song>): List<GenreGroup> =
        songs.groupBy { it.genre.trim().ifBlank { "Other" } }
            .map { (genre, list) -> GenreGroup(name = genre, songs = list) }
            .sortedBy { it.name.lowercase() }
}

data class AlbumGroup(
    val name: String,
    val artist: String,
    val songs: List<Song>,
    val artworkSong: Song? = null
)

data class ArtistGroup(
    val name: String,
    val songs: List<Song>,
    val albums: Int = 1
)

data class GenreGroup(
    val name: String,
    val songs: List<Song>
)
