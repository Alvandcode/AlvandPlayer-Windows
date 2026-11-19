package com.alvand.player.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alvand.player.i18n.LocalStrings
import com.alvand.player.lyrics.LrcParser
import com.alvand.player.lyrics.LyricsRepository
import com.alvand.player.model.Song
import com.alvand.player.ui.theme.LocalAP

/**
 * شیت متن آهنگ — اوریجینال الوند.
 *
 *  - سنک‌شده: خط فعال پررنگ وسط‌نما با اسکرول خودکار (فقط با عوض شدن خط،
 *    نه با هر تیک موقعیت — تا با اسکرول دستی کاربر نجنگد).
 *  - ساده: متن کامل قابل اسکرول با بج «بدون زمان‌بندی».
 *  - حریم خصوصی اول: اگر جستجوی آنلاین خاموش است، به‌جای درخواست، توضیح و
 *    دکمهٔ فعال‌سازی نشان داده می‌شود. هیچ‌وقت بدون رضایت شبکه لمس نمی‌شود.
 */
@Composable
fun LyricsSheet(
    song: Song?,
    positionMs: Long,
    outcome: LyricsRepository.Outcome?,
    onlineEnabled: Boolean,
    accent: Color,
    onToggleOnline: () -> Unit,
    onRetry: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pal = LocalAP.current
    val s = LocalStrings.current

    Box(
        modifier.fillMaxSize().background(pal.scrim.copy(alpha = 0.55f)),
        contentAlignment = Alignment.Center
    ) {
        GlassCard(Modifier.widthIn(max = 520.dp).fillMaxWidth(0.92f)) {
            Column(
                Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            s.lyrics,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = pal.ink
                        )
                        Text(
                            song?.let { "${it.title} — ${it.artist}" } ?: s.lyricsNoMusic,
                            fontSize = 12.sp,
                            color = pal.sub,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    SelectChip(s.close, false, onClick = onClose)
                }

                when {
                    song == null -> CenterNote(s.lyricsNoMusic)

                    !onlineEnabled && outcome !is LyricsRepository.Outcome.Ready -> {
                        Text(s.lyricsPrivacy, fontSize = 13.sp, color = pal.sub)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SelectChip(s.lyricsEnable, false, onClick = onToggleOnline)
                        }
                        if (outcome is LyricsRepository.Outcome.Failed) {
                            CenterNote(errorText(outcome.reason))
                        }
                    }

                    outcome == null -> CenterNote(s.lyricsLoading)

                    outcome is LyricsRepository.Outcome.Ready -> when (val e = outcome.entry) {
                        is LyricsRepository.Entry.Synced -> SyncedLines(
                            lines = e.lines,
                            positionMs = positionMs,
                            accent = accent
                        )
                        is LyricsRepository.Entry.Plain -> {
                            Text(
                                s.lyricsUntimed,
                                fontSize = 11.sp,
                                color = pal.sub,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Box(Modifier.fillMaxWidth().height(320.dp)) {
                                Column(Modifier.verticalScroll(rememberScrollState())) {
                                    Text(e.text, fontSize = 14.sp, color = pal.ink, lineHeight = 24.sp)
                                }
                            }
                        }
                    }

                    outcome is LyricsRepository.Outcome.NotFound -> {
                        CenterNote(s.lyricsNotFound)
                        if (onlineEnabled) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                SelectChip(s.lyricsRetry, false, onClick = onRetry)
                            }
                        }
                    }

                    outcome is LyricsRepository.Outcome.Failed -> {
                        CenterNote(errorText(outcome.reason))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SelectChip(s.lyricsRetry, false, onClick = onRetry)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CenterNote(text: String) {
    val pal = LocalAP.current
    Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
        Text(
            text,
            fontSize = 13.sp,
            color = pal.sub,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun errorText(reason: String): String {
    val s = LocalStrings.current
    return when (reason) {
        "offline" -> s.lyricsOffline
        "timeout" -> s.lyricsTimeout
        "rate-limited" -> s.lyricsRateLimited
        "server-error" -> s.lyricsServerError
        else -> s.lyricsError
    }
}

@Composable
private fun SyncedLines(
    lines: List<com.alvand.player.lyrics.LrcLine>,
    positionMs: Long,
    accent: Color
) {
    val pal = LocalAP.current
    val listState = rememberLazyListState()
    val active = LrcParser.activeIndex(lines, positionMs)
    var lastScrolled by remember { mutableStateOf(-1) }

    // فقط با عوض شدن خط اسکرول می‌کنیم — نه با هر تیک — تا با دست کاربر نجنگد
    LaunchedEffect(active) {
        if (active >= 0 && active != lastScrolled) {
            lastScrolled = active
            runCatching {
                listState.animateScrollToItem((active - 2).coerceAtLeast(0))
            }
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxWidth().height(340.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        itemsIndexed(lines, key = { i, l -> "$i-${l.timeMs}" }) { index, line ->
            val isActive = index == active
            Row(verticalAlignment = Alignment.CenterVertically) {
                // خط نازک accent کنار خطِ در حال خوانده شدن — امضای الوند
                Box(
                    Modifier
                        .padding(end = 10.dp)
                        .size(width = 3.dp, height = 20.dp)
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(2.dp))
                        .background(if (isActive) accent else Color.Transparent)
                )
                Text(
                    line.text,
                    fontSize = if (isActive) 16.sp else 13.sp,
                    fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Normal,
                    color = if (isActive) pal.ink else pal.sub,
                    lineHeight = if (isActive) 26.sp else 22.sp,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
