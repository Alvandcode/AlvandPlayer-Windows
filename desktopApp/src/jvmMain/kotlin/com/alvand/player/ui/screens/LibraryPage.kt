package com.alvand.player.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alvand.player.i18n.LocalStrings
import com.alvand.player.library.LibraryFilter
import com.alvand.player.library.LibrarySort
import com.alvand.player.library.LibraryStore
import com.alvand.player.library.ScanState
import com.alvand.player.model.Song
import com.alvand.player.ui.components.GlassCard
import com.alvand.player.ui.components.SelectChip
import com.alvand.player.ui.components.formatTime
import com.alvand.player.ui.components.rememberArtwork
import com.alvand.player.ui.theme.LocalAP
import com.alvand.player.ui.theme.Spacing

/** حالت گروه‌بندی صفحهٔ کتابخانهٔ تازه — شامل ژانر که طرح می‌خواهد. */
enum class LibraryGroup { SONGS, ALBUMS, ARTISTS, GENRES }

/**
 * صفحهٔ کتابخانه — موبه‌مو از روی طرح: تیتر، تب‌های آهنگ/آلبوم/خواننده/ژانر،
 * ردیف‌ها با کاور و مدت و منو، و حالت خالی با انتخاب پوشه.
 * مینی‌پلیر پایین صفحه را شل می‌گذارد، نه این صفحه.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LibraryPage(
    store: LibraryStore,
    onChooseFolder: () -> Unit,
    onOpenFile: () -> Unit = {},
    onPlay: (Song) -> Unit,
    onPlayGroup: (List<Song>) -> Unit = {},
    onShowProperties: (Song) -> Unit = {}
) {
    val s = LocalStrings.current
    val songs by store.songs.collectAsState()
    val scan by store.scanState.collectAsState()
    val query by store.query.collectAsState()
    val favoritesOnly by store.favoritesOnly.collectAsState()
    val settings by store.settingsState.collectAsState()
    val engineNowPlayingId by store.nowPlayingId.collectAsState()
    var groupMode by remember { mutableStateOf(LibraryGroup.SONGS) }

    // `scan` یک delegate است، پس smart-cast روی خودش ممکن نیست؛ یک val محلی می‌گیریم.
    val scanState = scan
    val visible = store.visibleSongs()

    // ساختار: کل صفحه یک LazyColumn است و هدر به‌عنوان item. قبلاً Column.verticalScroll
    // دور LazyColumn بود که در اولین پکینگ کتابخانه با "infinite maximum height"
    // کرش می‌کرد — LazyColumn هرگز نباید در ارتفاع بی‌نهایت measure شود.
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = Spacing.pageMargin, vertical = Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.xl)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(s.navLibrary, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = LocalAP.current.ink)
                    Text(
                        text = when (scanState) {
                            is ScanState.Running -> "${s.scanning} ${scanState.found}…"
                            is ScanState.Done -> "${s.songsFound} ${scanState.total}"
                            is ScanState.Failed -> scanState.message
                            ScanState.Idle -> "${s.folders} ${store.rootCount()}"
                        },
                        fontSize = 12.sp,
                        color = LocalAP.current.sub,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.width(Spacing.sm))
                SelectChip(s.chooseFolder, false, onClick = onChooseFolder)
                Spacer(Modifier.width(Spacing.sm))
                val running = scanState is ScanState.Running
                SelectChip(if (running) s.cancel else s.scan, running) {
                    if (running) store.cancelScan() else store.scan()
                }
            }
        }

        item { SearchField(query, s.searchHint, store::setQuery) }

        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GroupChip(s.groupSongs, groupMode == LibraryGroup.SONGS) { groupMode = LibraryGroup.SONGS }
                GroupChip(s.groupAlbums, groupMode == LibraryGroup.ALBUMS) { groupMode = LibraryGroup.ALBUMS }
                GroupChip(s.groupArtists, groupMode == LibraryGroup.ARTISTS) { groupMode = LibraryGroup.ARTISTS }
                GroupChip(s.groupGenres, groupMode == LibraryGroup.GENRES) { groupMode = LibraryGroup.GENRES }
            }
        }

        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SortChip2(s.sortDefault, settings.librarySort == LibrarySort.DEFAULT) { store.setSortMode(LibrarySort.DEFAULT) }
                SortChip2(s.sortTitle, settings.librarySort == LibrarySort.TITLE_AZ) { store.setSortMode(LibrarySort.TITLE_AZ) }
                SortChip2(s.sortArtist, settings.librarySort == LibrarySort.ARTIST_AZ) { store.setSortMode(LibrarySort.ARTIST_AZ) }
                SortChip2(s.sortLongest, settings.librarySort == LibrarySort.DURATION_DESC) { store.setSortMode(LibrarySort.DURATION_DESC) }
            }
        }

        item {
            SelectChip(
                label = s.favourites,
                selected = favoritesOnly,
                onClick = { store.setFavoritesOnly(!favoritesOnly) }
            )
        }

        when {
            scanState is ScanState.Running && visible.isEmpty() ->
                item {
                    Text("${s.scanning} ${scanState.found}…", fontSize = 14.sp, color = LocalAP.current.sub)
                }
            visible.isEmpty() -> item { EmptyLibraryCard(s, onChooseFolder, onOpenFile) }
            groupMode == LibraryGroup.SONGS ->
                itemsIndexed(visible, key = { _, song -> song.id }) { _, song ->
                    SongRow(
                        song = song,
                        isLiked = song.id in settings.likedIds,
                        isPlaying = engineNowPlayingId != null && song.id == engineNowPlayingId,
                        onPlay = { onPlay(song) },
                        onShowProperties = { onShowProperties(song) }
                    )
                }
            else -> item {
                LibraryGroups(
                    visible = visible,
                    mode = groupMode,
                    onPlayGroup = onPlayGroup
                )
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun SearchField(value: String, hint: String, onValueChange: (String) -> Unit) {
    val pal = LocalAP.current
    TextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        placeholder = { Text(hint, color = pal.sub, fontSize = 13.sp) },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = pal.glass,
            unfocusedContainerColor = pal.glass,
            disabledContainerColor = pal.glass,
            focusedIndicatorColor = pal.line,
            unfocusedIndicatorColor = pal.line,
            disabledIndicatorColor = pal.line,
            cursorColor = pal.ink,
            focusedTextColor = pal.ink,
            unfocusedTextColor = pal.ink
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun GroupChip(label: String, selected: Boolean, onClick: () -> Unit) {
    SelectChip(label, selected, onClick = onClick)
}

@Composable
private fun SortChip2(label: String, selected: Boolean, onClick: () -> Unit) {
    SelectChip(label, selected, onClick = onClick)
}

@Composable
private fun EmptyLibraryCard(
    s: com.alvand.player.i18n.Strings,
    onChooseFolder: () -> Unit,
    onOpenFile: () -> Unit
) {
    val pal = LocalAP.current
    GlassCard(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(horizontal = Spacing.lg, vertical = Spacing.xl)) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onChooseFolder)
                    .padding(vertical = Spacing.md)
                    .align(Alignment.CenterHorizontally),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = s.noMusic,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = pal.ink
                )
            }
            Text(
                text = s.noMusic,
                fontSize = 13.sp,
                color = pal.sub,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
private fun LibraryGroups(
    visible: List<com.alvand.player.model.Song>,
    mode: LibraryGroup,
    onPlayGroup: (List<Song>) -> Unit
) {
    // حالت SONGS اینجا نمی‌آید: در بدنهٔ اصلی صفحه به‌صورت itemsIndexed LazyColumn
    // رندر می‌شود تا مجازی‌سازی و کلیک روی اندیس واقعی کار کند.
    when (mode) {
        LibraryGroup.ALBUMS -> LibraryAlbumList(songs = visible, onPlayGroup = onPlayGroup)
        LibraryGroup.ARTISTS -> LibraryArtistList(songs = visible, onPlayGroup = onPlayGroup)
        LibraryGroup.GENRES -> LibraryGenreList(songs = visible, onPlayGroup = onPlayGroup)
        LibraryGroup.SONGS -> {}
    }
}

// --- گروه‌بندی‌های پورت‌شده ---

@Composable
private fun LibraryAlbumList(songs: List<Song>, onPlayGroup: (List<Song>) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Text("آلبوم‌ها", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = LocalAP.current.ink)
    }
}

@Composable
private fun LibraryArtistList(songs: List<Song>, onPlayGroup: (List<Song>) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Text("خوانندگان", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = LocalAP.current.ink)
    }
}

@Composable
private fun LibraryGenreList(songs: List<Song>, onPlayGroup: (List<Song>) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Text("ژانرها", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = LocalAP.current.ink)
    }
}

@Composable
private fun SongRow(
    song: Song,
    isLiked: Boolean,
    isPlaying: Boolean,
    onPlay: () -> Unit,
    onShowProperties: (Song) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md, vertical = Spacing.xs)
            .clickable(onClick = onPlay),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(song.title, fontSize = 14.sp, color = LocalAP.current.ink)
            Text(song.artist, fontSize = 12.sp, color = LocalAP.current.sub)
        }
    }
}
