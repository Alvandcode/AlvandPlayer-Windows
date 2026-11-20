package com.alvand.player.ui.shell

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alvand.player.i18n.LocalStrings
import com.alvand.player.player.EngineState
import com.alvand.player.player.PlayableItem
import com.alvand.player.player.RepeatMode
import com.alvand.player.ui.components.LinearSeekBar
import com.alvand.player.ui.components.MiniBars
import com.alvand.player.ui.components.TransportRow
import com.alvand.player.ui.components.rememberArtwork
import com.alvand.player.ui.icons.Favorite
import com.alvand.player.ui.icons.FavoriteBorder
import com.alvand.player.ui.icons.GraphicEq
import com.alvand.player.ui.icons.KeyboardArrowDown
import com.alvand.player.ui.icons.KeyboardArrowUp
import com.alvand.player.ui.icons.Lyrics
import com.alvand.player.ui.components.formatTime
import com.alvand.player.ui.theme.LocalAP

/**
 * پنل «در حال پخش» سمت راست — موبه‌مو از روی طرح: سربرگ، کاور بزرگ با قلب،
 * عنوان، نوار پیشرفت با زمان‌ها، کنترل‌ها، صف تاشو، و ردیف متن آهنگ.
 */
@Composable
fun NowPlayingPanel(
    state: EngineState,
    accent: Color,
    liked: Boolean,
    onToggleLike: () -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onSelectQueueIndex: (Int) -> Unit,
    onMoveQueueItem: (Int, Int) -> Unit = { _, _ -> },
    onRemoveFromQueue: (Int) -> Unit = {},
    onClearQueue: () -> Unit = {},
    onOpenLyrics: () -> Unit,
    onShowProperties: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val pal = LocalAP.current
    val s = LocalStrings.current
    var queueExpanded by remember { mutableStateOf(true) }
    val current = state.current

    Column(
        modifier
            .fillMaxHeight()
            .width(300.dp)
            .background(pal.glass)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Filled.GraphicEq,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                s.nowPlaying,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = pal.ink,
                modifier = Modifier.weight(1f)
            )
            var menuOpen by remember { mutableStateOf(false) }
            Box {
                IconButton(
                    onClick = { menuOpen = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Filled.MoreVert,
                        contentDescription = null,
                        tint = pal.sub,
                        modifier = Modifier.size(20.dp)
                    )
                }
                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(s.details, fontSize = 13.sp) },
                        onClick = {
                            menuOpen = false
                            onShowProperties()
                        }
                    )
                }
            }
        }

        if (current != null) {
            NowCover(
                item = current,
                liked = liked,
                onToggleLike = onToggleLike,
                accent = accent
            )
            Column {
                Text(
                    current.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = pal.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    current.artist,
                    fontSize = 12.sp,
                    color = pal.sub,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Column {
                LinearSeekBar(
                    progress = state.progress,
                    durationMs = state.durationMs,
                    onSeekMs = onSeek,
                    trackColor = pal.track,
                    progressColor = accent,
                    enabled = state.hasKnownDuration,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(formatTime(state.positionMs), fontSize = 11.sp, color = pal.sub)
                    Text(
                        if (state.hasKnownDuration) formatTime(state.durationMs) else "--:--",
                        fontSize = 11.sp,
                        color = pal.sub
                    )
                }
            }
        } else {
            Text(s.lyricsNoMusic, fontSize = 13.sp, color = pal.sub)
        }

        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            TransportRow(
                isPlaying = state.isPlaying,
                shuffleEnabled = state.shuffle,
                repeatMode = state.repeatMode,
                accent = accent,
                liked = liked,
                onToggleLike = onToggleLike,
                onTogglePlay = onTogglePlay,
                onNext = onNext,
                onPrevious = onPrevious,
                onToggleShuffle = onToggleShuffle,
                onCycleRepeat = onCycleRepeat,
                showLike = false
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${s.queueTitle} (${state.queue.size})",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = pal.ink,
                modifier = Modifier.weight(1f)
            )
            if (state.queue.isNotEmpty()) {
                var clearMenu by remember { mutableStateOf(false) }
                Box {
                    com.alvand.player.ui.components.HintTooltip(s.clearQueue) {
                        IconButton(
                            onClick = { clearMenu = true },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = null,
                                tint = pal.sub,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    DropdownMenu(expanded = clearMenu, onDismissRequest = { clearMenu = false }) {
                        DropdownMenuItem(
                            text = { Text(s.clearQueue, fontSize = 13.sp) },
                            onClick = {
                                clearMenu = false
                                onClearQueue()
                            }
                        )
                    }
                }
            }
            IconButton(
                onClick = { queueExpanded = !queueExpanded },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    if (queueExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = pal.sub,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        if (queueExpanded) {
            LazyColumn(
                Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                itemsIndexed(state.queue, key = { _, item -> item.id }) { index, item ->
                    QueueRow(
                        index = index,
                        item = item,
                        isCurrent = index == state.index,
                        isPlaying = state.isPlaying,
                        accent = accent,
                        onClick = { onSelectQueueIndex(index) },
                        onMoveUp = { onMoveQueueItem(index, index - 1) },
                        onMoveDown = { onMoveQueueItem(index, index + 1) },
                        onRemove = { onRemoveFromQueue(index) }
                    )
                }
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onOpenLyrics)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                Icons.Filled.Lyrics,
                contentDescription = null,
                tint = pal.sub,
                modifier = Modifier.size(20.dp)
            )
            Text(
                s.lyrics,
                fontSize = 13.sp,
                color = pal.ink,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun NowCover(
    item: PlayableItem,
    liked: Boolean,
    onToggleLike: () -> Unit,
    accent: Color
) {
    val pal = LocalAP.current
    val artwork by rememberArtwork(item.uri)
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.verticalGradient(listOf(pal.glass, pal.track)))
    ) {
        if (artwork != null) {
            Image(
                bitmap = artwork!!,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        IconButton(
            onClick = onToggleLike,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .size(36.dp)
        ) {
            Icon(
                if (liked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = null,
                tint = if (liked) accent else Color.White,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun QueueRow(
    index: Int,
    item: PlayableItem,
    isCurrent: Boolean,
    isPlaying: Boolean,
    accent: Color,
    onClick: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit
) {
    val pal = LocalAP.current
    val artwork by rememberArtwork(item.uri)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (isCurrent) accent.copy(alpha = 0.16f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (isCurrent) {
            MiniBars(color = accent, isPlaying = isPlaying, modifier = Modifier.width(18.dp))
        } else {
            Text(
                "${index + 1}",
                fontSize = 12.sp,
                color = pal.sub,
                modifier = Modifier.width(18.dp)
            )
        }
        if (artwork != null) {
            Image(
                bitmap = artwork!!,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
        } else {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(pal.track),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    item.title.take(1).uppercase().ifBlank { "♪" },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = pal.sub
                )
            }
        }
        Column(Modifier.weight(1f)) {
            Text(
                item.title,
                fontSize = 12.sp,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                color = pal.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                item.artist,
                fontSize = 11.sp,
                color = pal.sub,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (item.durationMs > 0) {
            Text(formatTime(item.durationMs), fontSize = 11.sp, color = pal.sub)
        }
        // مرتب‌سازی دستی صف — دکمه‌های واقعی با هدف کلیک قابل قبول (قرارداد دسکتاپ)
        Column {
            com.alvand.player.ui.components.HintTooltip(com.alvand.player.i18n.LocalStrings.current.moveUp) {
                IconButton(onClick = onMoveUp, modifier = Modifier.size(22.dp)) {
                    Icon(
                        Icons.Filled.KeyboardArrowUp,
                        contentDescription = null,
                        tint = pal.sub,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            com.alvand.player.ui.components.HintTooltip(com.alvand.player.i18n.LocalStrings.current.moveDown) {
                IconButton(onClick = onMoveDown, modifier = Modifier.size(22.dp)) {
                    Icon(
                        Icons.Filled.KeyboardArrowDown,
                        contentDescription = null,
                        tint = pal.sub,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
        // حذف از صف — دکمهٔ همیشه‌نمایان اما کم‌رنگ
        IconButton(onClick = onRemove, modifier = Modifier.size(24.dp)) {
            Icon(
                Icons.Filled.Close,
                contentDescription = com.alvand.player.i18n.LocalStrings.current.removeFromQueue,
                tint = pal.sub,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
