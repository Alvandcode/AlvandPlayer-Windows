package com.alvand.player.ui.shell

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alvand.player.player.EngineState
import com.alvand.player.ui.components.LinearSeekBar
import com.alvand.player.ui.components.rememberArtwork
import com.alvand.player.ui.icons.Pause
import com.alvand.player.ui.icons.PlayArrow
import com.alvand.player.ui.icons.SkipNext
import com.alvand.player.ui.icons.SkipPrevious
import com.alvand.player.ui.theme.LocalAP

/**
 * مینی‌پلیر پایین صفحه‌های فرعی — موبه‌مو از روی طرح: کاور، عنوان،
 * کنترل‌های کوچک و نوار پیشرفت باریک.
 */
@Composable
fun MiniPlayerBar(
    state: EngineState,
    accent: Color,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val pal = LocalAP.current
    val current = state.current ?: return
    val artwork by rememberArtwork(current.uri)

    Column(
        modifier
            .fillMaxWidth()
            .background(pal.glass)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
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
                        current.title.take(1).uppercase().ifBlank { "♪" },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = pal.sub
                    )
                }
            }
            Column(Modifier.weight(1f)) {
                Text(
                    current.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = pal.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    current.artist,
                    fontSize = 11.sp,
                    color = pal.sub,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onPrevious, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.SkipPrevious, null, tint = pal.ink, modifier = Modifier.size(22.dp))
            }
            Box(
                Modifier
                    .size(40.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(accent)
                    .clickable(onClick = onTogglePlay),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
            IconButton(onClick = onNext, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.SkipNext, null, tint = pal.ink, modifier = Modifier.size(22.dp))
            }
        }
        LinearSeekBar(
            progress = state.progress,
            durationMs = state.durationMs,
            onSeekMs = onSeek,
            trackColor = pal.track,
            progressColor = accent,
            enabled = state.hasKnownDuration,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
