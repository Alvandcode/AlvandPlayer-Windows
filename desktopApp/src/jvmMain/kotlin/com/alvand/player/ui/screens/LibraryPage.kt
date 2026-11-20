package com.alvand.player.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
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
import com.alvand.player.ui.components.ArtworkThumb
import com.alvand.player.ui.components.GlassCard
import com.alvand.player.ui.components.MiniBars
import com.alvand.player.ui.components.SelectChip
import com.alvand.player.ui.components.formatTime
import com.alvand.player.ui.components.rememberArtwork
import com.alvand.player.ui.icons.Favorite
import com.alvand.player.ui.icons.FavoriteBorder
import com.alvand.player.ui.theme.LocalAP
import com.alvand.player.ui.theme.Spacing

/** حالت گروه‌بندی صفحهٔ کتابخانهٔ تازه — شامل ژانر که طرح می‌خواهد. */
enum class LibraryGroup { SONGS, ALBUMS, ARTISTS, GENRES }

/**
 * صفحهٔ کتابخانه — ردیف‌های کامل با کاور/مدت/قلب/منو، تب‌های گروه‌بندیِ واقعی،
 * و حالت خالی با انتخاب پوشه و باز کردن فایل.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LibraryPage(
    accent: androidx.compose.ui.graphics.Color,
    store: LibraryStore,
    onChooseFolder: () -> Unit,
    onOpenFile: () -> Unit = {},
    onPlay: (Song) -> Unit,
    onPlayGroup: (List<Song>) -> Unit = {},
    onShowProperties: (Song) -> Unit = {},
    onAddToQueue: (Song) -> Unit = {},
    onToggleLike: (Song) -> Unit = {}
) {
    val s = LocalStrings.current
    val pal = LocalAP.current
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
                    Text(s.navLibrary, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = pal.ink)
                    Text(
                        text = when (scanState) {
                            is ScanState.Running -> "${s.scanning} ${scanState.found}…"
                            is ScanState.Done -> "${s.songsFound} ${scanState.total}"
                            is ScanState.Failed -> scanState.message
                            ScanState.Idle -> "${s.folders} ${store.rootCount()}"
                        },
                        fontSize = 12.sp,
                        color = pal.sub,
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
                    Text("${s.scanning} ${scanState.found}…", fontSize = 14.sp, color = pal.sub)
                }
            visible.isEmpty() -> item { EmptyLibraryCard(s, onChooseFolder, onOpenFile) }
            groupMode == LibraryGroup.SONGS ->
                itemsIndexed(visible, key = { _, song -> song.id }) { _, song ->
                    SongRow(
                        song = song,
                        isLiked = song.id in settings.likedIds,
                        isPlaying = engineNowPlayingId != null && song.id == engineNowPlayingId,
                        accent = accent,
                        onPlay = { onPlay(song) },
                        onToggleLike = { onToggleLike(song) },
                        onAddToQueue = { onAddToQueue(song) },
                        onShowProperties = { onShowProperties(song) }
                    )
                }
            else -> item {
                LibraryGroups(
                    visible = visible,
                    mode = groupMode,
                    accent = accent,
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

/** حالت خالی واقعی — یک پیام، یک توضیح، و دو کارِ عملی. */
@Composable
private fun EmptyLibraryCard(
    s: com.alvand.player.i18n.Strings,
    onChooseFolder: () -> Unit,
    onOpenFile: () -> Unit
) {
    val pal = LocalAP.current
    GlassCard(Modifier.fillMaxWidth()) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg, vertical = Spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            Text(
                text = s.noMusic,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = pal.ink
            )
            Text(
                text = s.libEmptyHint,
                fontSize = 13.sp,
                color = pal.sub,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(Modifier.height(Spacing.xs))
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                SelectChip(s.chooseFolder, true, onClick = onChooseFolder)
                SelectChip(s.openFile, false, onClick = onOpenFile)
            }
        }
    }
}

/** کارت هر گروه (آلبوم/خواننده/ژانر) — کاور، نام، تعداد؛ کلیک = پخش همه. */
@Composable
private fun GroupCard(name: String, subtitle: String, artworkSong: Song?, accent: Color, onClick: () -> Unit) {
    val pal = LocalAP.current
    val hover = remember { MutableInteractionSource() }
    val isHovered by hover.collectIsHoveredAsState()
    val artwork by rememberArtwork(artworkSong?.uri)
    Column(
        Modifier
            .width(140.dp)
            .clip(RoundedCornerShape(Spacing.radiusMd))
            .hoverable(hover)
            .clickable(interactionSource = hover, indication = null, onClick = onClick)
    ) {
        Box(
            Modifier
                .size(140.dp)
                .clip(RoundedCornerShape(Spacing.radiusMd))
                .background(Brush.linearGradient(com.alvand.player.ui.screens.gradientFor(name)))
        ) {
            if (artwork != null) {
                Image(
                    bitmap = artwork!!,
                    contentDescription = name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    androidx.compose.material.icons.Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.5f),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(36.dp)
                )
            }
            if (isHovered) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.35f)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        Modifier
                            .size(44.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(accent),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            androidx.compose.material.icons.Icons.Filled.PlayArrow,
                            contentDescription = null,
                            tint = androidx.compose.ui.graphics.Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            name,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = pal.ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            subtitle,
            fontSize = 11.sp,
            color = pal.sub,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LibraryGroups(
    visible: List<Song>,
    mode: LibraryGroup,
    accent: Color,
    onPlayGroup: (List<Song>) -> Unit
) {
    when (mode) {
        LibraryGroup.ALBUMS -> {
            val groups = remember(visible) { LibraryFilter.groupByAlbum(visible) }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                groups.forEach { g ->
                    GroupCard(
                        name = g.name,
                        subtitle = g.artist.ifBlank { "·" },
                        artworkSong = g.artworkSong,
                        accent = accent,
                        onClick = { onPlayGroup(g.songs) }
                    )
                }
            }
        }
        LibraryGroup.ARTISTS -> {
            val groups = remember(visible) { LibraryFilter.groupByArtist(visible) }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                groups.forEach { g ->
                    GroupCard(
                        name = g.name,
                        subtitle = "${g.songs.size}",
                        artworkSong = g.songs.firstOrNull(),
                        accent = accent,
                        onClick = { onPlayGroup(g.songs) }
                    )
                }
            }
        }
        LibraryGroup.GENRES -> {
            val groups = remember(visible) { LibraryFilter.groupByGenre(visible) }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                groups.forEach { g ->
                    GroupCard(
                        name = g.name,
                        subtitle = "${g.songs.size}",
                        artworkSong = g.songs.firstOrNull(),
                        accent = accent,
                        onClick = { onPlayGroup(g.songs) }
                    )
                }
            }
        }
        LibraryGroup.SONGS -> {}
    }
}

/**
 * ردیف کامل آهنگ: کاور، عنوان/خواننده، مدت، قلب، و منو (سه‌نقطه + راست‌کلیک).
 * هاورِ دسکتاپی و نشانگرِ «در حال پخش» دارد.
 */
@Composable
private fun SongRow(
    song: Song,
    isLiked: Boolean,
    isPlaying: Boolean,
    accent: Color,
    onPlay: () -> Unit,
    onToggleLike: () -> Unit,
    onAddToQueue: () -> Unit,
    onShowProperties: () -> Unit
) {
    val pal = LocalAP.current
    val s = LocalStrings.current
    val artwork by rememberArtwork(song.uri)
    val hover = remember { MutableInteractionSource() }
    val isHovered by hover.collectIsHoveredAsState()
    var menuOpen by remember { mutableStateOf(false) }

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .hoverable(hover)
            .background(if (isPlaying) accent.copy(alpha = 0.14f) else if (isHovered) pal.glass else androidx.compose.ui.graphics.Color.Transparent)
            // راست‌کلیک = همان منوی سه‌نقطه — قرارداد دسکتاپ
            .pointerInput(song.id) {
                awaitEachGesture {
                    val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                    if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                        menuOpen = true
                        event.changes.forEach { it.consume() }
                    }
                }
            }
            .clickable(interactionSource = hover, indication = null, onClick = onPlay)
            .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        Box(Modifier.size(40.dp)) {
            if (artwork != null) {
                Image(
                    bitmap = artwork!!,
                    contentDescription = song.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp))
                )
            } else {
                Box(
                    Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp))
                        .background(pal.track),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        song.title.take(1).uppercase().ifBlank { "♪" },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = pal.sub
                    )
                }
            }
            if (isHovered && !isPlaying) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp))
                        .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        androidx.compose.material.icons.Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = androidx.compose.ui.graphics.Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else if (isPlaying) {
                MiniBars(color = accent, isPlaying = true, modifier = Modifier.fillMaxSize())
            }
        }

        Column(Modifier.weight(1f)) {
            Text(
                song.title,
                fontSize = 14.sp,
                fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.Medium,
                color = if (isPlaying) accent else pal.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(song.artist, fontSize = 12.sp, color = pal.sub, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }

        if (song.durationMs > 0) {
            Text(formatTime(song.durationMs), fontSize = 11.sp, color = pal.sub)
        }

        // قلب فقط هنگام هاور یا وقتی لایک است — شلوغ نمی‌شود
        if (isHovered || isLiked) {
            IconButton(onClick = onToggleLike, modifier = Modifier.size(32.dp)) {
                Icon(
                    if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = null,
                    tint = if (isLiked) accent else pal.sub,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Box {
            IconButton(
                onClick = { menuOpen = true },
                modifier = Modifier
                    .size(32.dp)
                    .padding(2.dp)
            ) {
                Icon(
                    Icons.Filled.MoreVert,
                    contentDescription = null,
                    tint = if (isHovered) pal.ink else pal.sub,
                    modifier = Modifier.size(18.dp)
                )
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text(s.playNow, fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Filled.PlayArrow, null, modifier = Modifier.size(18.dp)) },
                    onClick = { menuOpen = false; onPlay() }
                )
                DropdownMenuItem(
                    text = { Text(s.addToQueue, fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Filled.Add, null, modifier = Modifier.size(18.dp)) },
                    onClick = { menuOpen = false; onAddToQueue() }
                )
                DropdownMenuItem(
                    text = { Text(s.details, fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Filled.Info, null, modifier = Modifier.size(18.dp)) },
                    onClick = { menuOpen = false; onShowProperties() }
                )
            }
        }
    }
}
