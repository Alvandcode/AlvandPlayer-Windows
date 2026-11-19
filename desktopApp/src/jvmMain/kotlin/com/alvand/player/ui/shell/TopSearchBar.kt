package com.alvand.player.ui.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alvand.player.i18n.LocalStrings
import com.alvand.player.model.Song
import com.alvand.player.ui.components.GlassCard
import com.alvand.player.ui.components.formatTime
import com.alvand.player.ui.theme.LocalAP

/**
 * نوار جستجوی بالای شل — موبه‌مو از روی طرح: فیلد گرد با آیکون ذره‌بین، هینت،
 * و برچسب `Ctrl + K` سمت راست. آواتار هم آیکون برنامه است و به تنظیمات می‌رود
 * (زنگولهٔ طرح هیچ بک‌اندی ندارد، عمداً نیست).
 *
 * نتیجه‌ها همان لحظه از کتابخانهٔ واقعی فیلتر می‌شوند؛ کلیک پخش می‌کند.
 */
@Composable
fun TopSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    results: List<Song>,
    focusRequester: FocusRequester,
    onPlaySong: (Song) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenNotifications: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val pal = LocalAP.current
    val s = LocalStrings.current
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(Modifier.weight(1f)) {
            Column {
                TextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    placeholder = { Text(s.searchHint, color = pal.sub, fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(
                            Icons.Filled.Search,
                            contentDescription = null,
                            tint = pal.sub,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = null,
                                tint = pal.sub,
                                modifier = Modifier
                                    .size(20.dp)
                                    .clickable { onQueryChange("") }
                            )
                        } else {
                            Text("Ctrl + K", fontSize = 11.sp, color = pal.sub)
                        }
                    },
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
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                )
                if (query.isNotBlank()) {
                    GlassCard(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                    ) {
                        Column(Modifier.padding(8.dp)) {
                            if (results.isEmpty()) {
                                Text(
                                    s.searchNoResults,
                                    fontSize = 13.sp,
                                    color = pal.sub,
                                    modifier = Modifier.padding(12.dp)
                                )
                            } else {
                                results.forEach { song ->
                                    Row(
                                        Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable { onPlaySong(song) }
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Column(Modifier.weight(1f)) {
                                            Text(
                                                song.title,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = pal.ink,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                song.artist,
                                                fontSize = 11.sp,
                                                color = pal.sub,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        if (song.durationMs > 0) {
                                            Text(
                                                formatTime(song.durationMs),
                                                fontSize = 11.sp,
                                                color = pal.sub
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        AppAvatar(onClick = onOpenSettings)
        IconButton(onClick = onOpenNotifications) {
            Icon(
                Icons.Filled.Notifications,
                contentDescription = null,
                tint = pal.sub,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/** آواتار = دایرهٔ شیشه‌ای با حرف A؛ کلیک می‌رود تنظیمات. */
@Composable
private fun AppAvatar(onClick: () -> Unit) {
    val pal = LocalAP.current
    Box(
        Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(pal.glass)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text("A", fontSize = 16.sp, fontWeight = FontWeight.Black, color = pal.ink)
    }
}

/**
 * فیلتر جستجو — قابل تست، بدون UI.
 * همهٔ کلمات باید در عنوان/خواننده/آلبوم باشند (AND)، بدون حساسیت به حروف.
 */
fun matchSearch(songs: List<Song>, query: String, limit: Int = 8): List<Song> {
    val words = query.trim().lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
    if (words.isEmpty()) return emptyList()
    return songs.filter { song ->
        val hay = "${song.title} ${song.artist} ${song.album}".lowercase()
        words.all { hay.contains(it) }
    }.take(limit)
}