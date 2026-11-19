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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alvand.player.data.db.PlaylistEntity
import com.alvand.player.data.db.PlaylistSongEntity
import com.alvand.player.i18n.LocalStrings
import com.alvand.player.model.Song
import com.alvand.player.ui.components.GlassCard
import com.alvand.player.ui.components.SelectChip
import com.alvand.player.ui.components.rememberArtwork
import com.alvand.player.ui.theme.LocalAP

/**
 * صفحهٔ پلی‌لیست‌ها — موبه‌مو از روی طرح: دکمهٔ ساخت، ردیف‌های پلی‌لیست با کاور،
 * و با انتخاب هرکدام، آهنگ‌های داخلش.
 *
 * داده کاملاً واقعی از دیتابیس (PlaylistRepository) می‌آید؛ ساخت/حذف همان‌جا
 * انجام می‌شود.
 */
@Composable
fun PlaylistsPage(
    playlists: List<PlaylistEntity>,
    songsOf: (Long) -> List<PlaylistSongEntity>,
    songCountOf: (Long) -> Int,
    onCreate: (String) -> Unit,
    onDelete: (Long) -> Unit,
    onPlaySongs: (List<Song>) -> Unit,
    modifier: Modifier = Modifier
) {
    val s = LocalStrings.current
    var creating by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var openedId by remember { mutableStateOf<Long?>(null) }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                s.navPlaylists,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = LocalAP.current.ink,
                modifier = Modifier.weight(1f)
            )
            SelectChip("+ ${s.newPlaylist}", false, onClick = {
                name = ""
                creating = true
            })
        }

        if (creating) {
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    TextField(
                        value = name,
                        onValueChange = { name = it },
                        singleLine = true,
                        placeholder = { Text(s.playlistNameHint, color = LocalAP.current.sub, fontSize = 13.sp) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = LocalAP.current.glass,
                            unfocusedContainerColor = LocalAP.current.glass,
                            disabledContainerColor = LocalAP.current.glass,
                            focusedIndicatorColor = LocalAP.current.line,
                            unfocusedIndicatorColor = LocalAP.current.line,
                            disabledIndicatorColor = LocalAP.current.line,
                            cursorColor = LocalAP.current.ink,
                            focusedTextColor = LocalAP.current.ink,
                            unfocusedTextColor = LocalAP.current.ink
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SelectChip(s.playlistCreate, true, onClick = {
                            if (name.isNotBlank()) {
                                onCreate(name)
                                creating = false
                            }
                        })
                        SelectChip(s.close, false, onClick = { creating = false })
                    }
                }
            }
        }

        if (playlists.isEmpty() && !creating) {
            Text(s.noMusic, fontSize = 13.sp, color = LocalAP.current.sub)
        }

        playlists.forEach { playlist ->
            PlaylistCard(
                playlist = playlist,
                songCount = songCountOf(playlist.id),
                expanded = openedId == playlist.id,
                onToggle = { openedId = if (openedId == playlist.id) null else playlist.id },
                onDelete = { onDelete(playlist.id) },
                songs = if (openedId == playlist.id) songsOf(playlist.id) else emptyList(),
                onPlaySongs = onPlaySongs,
                deleteLabel = s.playlistDelete
            )
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun PlaylistCard(
    playlist: PlaylistEntity,
    songCount: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    songs: List<PlaylistSongEntity>,
    onPlaySongs: (List<Song>) -> Unit,
    deleteLabel: String
) {
    val pal = LocalAP.current
    val firstArt = songs.firstOrNull()?.uri
    val artwork by rememberArtwork(firstArt)
    GlassCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onToggle)
                    .padding(4.dp)
            ) {
                if (artwork != null) {
                    Image(
                        bitmap = artwork!!,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                } else {
                    Box(
                        Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Brush.verticalGradient(listOf(pal.glass, pal.track))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("♪", fontSize = 20.sp, color = pal.sub)
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        playlist.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = pal.ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "$songCount",
                        fontSize = 12.sp,
                        color = pal.sub
                    )
                }
                SelectChip(deleteLabel, false, onClick = onDelete)
            }
            if (expanded) {
                if (songs.isEmpty()) {
                    Text("—", fontSize = 12.sp, color = pal.sub)
                } else {
                    SelectChip("▶ $songCount", true, onClick = {
                        onPlaySongs(songs.map {
                            Song(
                                id = it.songId,
                                title = it.title,
                                artist = it.artist,
                                album = it.album,
                                uri = it.uri,
                                durationMs = it.durationMs,
                                isRemote = it.isRemote
                            )
                        })
                    })
                    LazyColumn(
                        Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(songs, key = { it.id }) { e ->
                            Text(
                                "${e.title} — ${e.artist}",
                                fontSize = 12.sp,
                                color = pal.ink,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
