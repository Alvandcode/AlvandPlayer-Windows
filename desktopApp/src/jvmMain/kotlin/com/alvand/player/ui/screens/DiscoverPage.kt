package com.alvand.player.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alvand.player.i18n.LocalStrings
import com.alvand.player.library.LibraryFilter
import com.alvand.player.model.Song
import com.alvand.player.ui.components.ArtworkThumb
import com.alvand.player.ui.components.SelectChip
import com.alvand.player.ui.components.formatTime
import com.alvand.player.ui.components.rememberArtwork
import com.alvand.player.ui.icons.Album
import com.alvand.player.ui.icons.BarChart
import com.alvand.player.ui.icons.Category
import com.alvand.player.ui.icons.MusicNote
import com.alvand.player.ui.icons.NewReleases
import com.alvand.player.ui.icons.PlayArrow
import com.alvand.player.ui.theme.LocalAP

/**
 * صفحهٔ اکتشاف — موبه‌مو از روی طرح: چهار تب، ردیف «پلی‌لیست‌های منتخب» با کارت‌های
 * کاور و بخش «تازه‌ها».
 *
 * صادق باشیم: چارت و پلی‌لیستِ آنلاین وصل نیست (برنامه هیچ سرویس ابری ندارد)، پس
 * محتوا از **کتابخانهٔ واقعی خودِ کاربر** ساخته می‌شود: آلبوم‌های اخیر به‌عنوان
 * پلی‌لیست، آهنگ‌های تازهٔ اسکن‌شده، و ژانرهای واقعی تگ‌ها.
 */
@Composable
fun DiscoverPage(
    songs: List<Song>,
    mostPlayed: List<Song> = emptyList(),
    onPlaySongs: (List<Song>) -> Unit,
    modifier: Modifier = Modifier
) {
    val s = LocalStrings.current
    val pal = LocalAP.current
    var tab by remember { mutableStateOf(DiscoverTab.FOR_YOU) }

    // آلبوم‌های واقعی به‌عنوان «پلی‌لیست‌های منتخب»، تازه‌ترین اسکن اول
    val albums = remember(songs) { LibraryFilter.groupByAlbum(songs).reversed() }
    val fresh = remember(songs) { songs.take(12) }
    val genres = remember(songs) { LibraryFilter.groupByGenre(songs) }
    // فال‌بک چارت وقتی تاریخچهٔ پخش خالی است: طولانی‌ترین‌ها
    val fallbackChart = remember(songs) { songs.sortedByDescending { it.durationMs }.take(20) }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            s.navDiscover,
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            color = pal.ink
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SelectChip(s.forYou, tab == DiscoverTab.FOR_YOU) { tab = DiscoverTab.FOR_YOU }
            SelectChip(s.charts, tab == DiscoverTab.CHARTS) { tab = DiscoverTab.CHARTS }
            SelectChip(s.newReleases, tab == DiscoverTab.NEW_RELEASES) { tab = DiscoverTab.NEW_RELEASES }
            SelectChip(s.groupGenres, tab == DiscoverTab.GENRES) { tab = DiscoverTab.GENRES }
        }

        if (songs.isEmpty()) {
            Text(s.discoverEmpty, fontSize = 13.sp, color = pal.sub)
        } else {
            when (tab) {
                DiscoverTab.FOR_YOU -> {
                    SectionTitle(s.featuredPlaylists)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(albums, key = { it.name }) { group ->
                            PlaylistTile(
                                title = group.name,
                                subtitle = "${group.artist} · ${group.songs.size}",
                                artworkUri = group.artworkSong?.uri,
                                gradient = gradientFor(group.name),
                                onClick = { onPlaySongs(group.songs) }
                            )
                        }
                    }
                    // «برای شما» = پرپخش‌ترین‌ها؛ قبلاً همان لیستِ تازه‌ها بود (تکراری)
                    if (mostPlayed.isNotEmpty()) {
                        SectionTitle(s.mostPlayed)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(mostPlayed, key = { "fy-${it.id}" }) { song ->
                                SongTile(song = song, onClick = { onPlaySongs(listOf(song)) })
                            }
                        }
                    }
                }

                DiscoverTab.CHARTS -> {
                    // چارت واقعی = پرپخش‌ترین‌های کتابخانه (تاریخچهٔ واقعی پخش)
                    // قبلاً «طولانی‌ترین» نشان داده می‌شد — برچسب با محتوا نمی‌خواند.
                    val chart = mostPlayed.ifEmpty { fallbackChart }
                    SectionTitle(s.charts)
                    chart.forEachIndexed { index, song ->
                        ChartRow(
                            rank = index + 1,
                            song = song,
                            onClick = { onPlaySongs(listOf(song)) }
                        )
                    }
                }

                DiscoverTab.NEW_RELEASES -> {
                    SectionTitle(s.newReleases)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(fresh, key = { it.id }) { song ->
                            SongTile(song = song, onClick = { onPlaySongs(listOf(song)) })
                        }
                    }
                }

                DiscoverTab.GENRES -> {
                    SectionTitle(s.groupGenres)
                    FlowGrid(genres.map { it.name to it.songs.size }) { name ->
                        onPlaySongs(genres.firstOrNull { it.name == name }?.songs.orEmpty())
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

enum class DiscoverTab { FOR_YOU, CHARTS, NEW_RELEASES, GENRES }

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = LocalAP.current.ink,
        modifier = Modifier.padding(top = 4.dp)
    )
}

/** رنگِ کارت از روی نام مشتق می‌شود تا هر آلبوم رنگ ثابت خودش را داشته باشد. */
private val TILE_GRADIENTS = listOf(
    listOf(Color(0xFF8B5CF6), Color(0xFF5B2FD6)),
    listOf(Color(0xFF14B8A6), Color(0xFF0E7490)),
    listOf(Color(0xFFF43F5E), Color(0xFFBE123C)),
    listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8)),
    listOf(Color(0xFFF59E0B), Color(0xFFB45309)),
    listOf(Color(0xFF10B981), Color(0xFF047857))
)

internal fun gradientFor(seed: String): List<Color> =
    TILE_GRADIENTS[(seed.hashCode().let { if (it == Int.MIN_VALUE) 0 else kotlin.math.abs(it) }) % TILE_GRADIENTS.size]

@Composable
private fun PlaylistTile(
    title: String,
    subtitle: String,
    artworkUri: String?,
    gradient: List<Color>,
    onClick: () -> Unit
) {
    val artwork by rememberArtwork(artworkUri)
    Box(
        Modifier
            .width(150.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(gradient))
            .clickable(onClick = onClick)
    ) {
        Box(Modifier.fillMaxWidth().height(150.dp)) {
            if (artwork != null) {
                Image(
                    bitmap = artwork!!,
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0.55f to Color.Transparent,
                                1f to Color.Black.copy(alpha = 0.55f)
                            )
                        )
                )
            }
Column(
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    subtitle,
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (artwork == null) {
                Icon(
                    Icons.Filled.Album,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.55f),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(44.dp)
                )
            }
        }
    }
}

@Composable
private fun SongTile(song: Song, onClick: () -> Unit) {
    val artwork by rememberArtwork(song.uri)
    Column(
        Modifier
            .width(140.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        Box(
            Modifier
                .size(140.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Brush.linearGradient(gradientFor(song.album.ifBlank { song.title })))
        ) {
            if (artwork != null) {
                Image(
                    bitmap = artwork!!,
                    contentDescription = song.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    Icons.Filled.MusicNote,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(40.dp)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            song.title,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = LocalAP.current.ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            song.artist,
            fontSize = 11.sp,
            color = LocalAP.current.sub,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ChartRow(rank: Int, song: Song, onClick: () -> Unit) {
    val pal = LocalAP.current
    val artwork by rememberArtwork(song.uri)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "$rank",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = pal.sub,
            modifier = Modifier.width(24.dp)
        )
        ArtworkThumb(
            uri = song.uri,
            fallbackTitle = song.title,
            size = 40.dp
        )
        Column(Modifier.weight(1f)) {
            Text(
                song.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = pal.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                song.artist,
                fontSize = 12.sp,
                color = pal.sub,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (song.durationMs > 0) {
            Text(formatTime(song.durationMs), fontSize = 11.sp, color = pal.sub)
        }
    }
}

/** شبکهٔ ژانرها — چیپ‌هایی که با کلیک، همهٔ آهنگ‌های آن ژانر را پخش می‌کنند. */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun FlowGrid(items: List<Pair<String, Int>>, onPick: (String) -> Unit) {
    val pal = LocalAP.current
    androidx.compose.foundation.layout.FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.forEach { (name, count) ->
            Row(
                Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(pal.glass)
                    .clickable { onPick(name) }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Filled.Category,
                    contentDescription = null,
                    tint = pal.sub,
                    modifier = Modifier.size(18.dp)
                )
                Text(name, fontSize = 13.sp, color = pal.ink)
                Text("$count", fontSize = 11.sp, color = pal.sub)
            }
        }
    }
}