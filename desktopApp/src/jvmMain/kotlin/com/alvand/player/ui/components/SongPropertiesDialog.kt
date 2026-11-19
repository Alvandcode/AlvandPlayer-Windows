package com.alvand.player.ui.components

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alvand.player.i18n.LocalStrings
import com.alvand.player.library.SongFileInfoReader
import com.alvand.player.model.Song
import com.alvand.player.ui.components.formatTime
import com.alvand.player.ui.theme.LocalAP
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.Desktop
import java.io.File

/**
 * شناسنامهٔ آهنگ — اوریجینال الوند.
 *
 * سه بخش صادقانه: تگ‌ها (همان که کتابخانه نشان می‌دهد)، فایل (اندازه/مسیر/
 * فرمت از روی دیسک واقعی)، صدا (نرخ/کانال/بیت فقط اگر واقعاً خوانده شد،
 * وگرنه خط‌تیره — هرگز عدد جعلی). خواندنِ بلاک‌کننده روی IO است تا UI قفل نشود.
 */
@Composable
fun SongPropertiesDialog(
    song: Song,
    liked: Boolean,
    onToggleFavorite: () -> Unit,
    onClose: () -> Unit,
    onJumpToTime: () -> Unit = {},
    loopA: Long? = null,
    loopB: Long? = null,
    onSetLoopA: () -> Unit = {},
    onSetLoopB: () -> Unit = {},
    onClearLoop: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val pal = LocalAP.current
    val s = LocalStrings.current
    val scope = rememberCoroutineScope()
    val artworkState = rememberArtwork(song.uri)
    val infoState = produceState<com.alvand.player.library.SongFileInfo?>(
        initialValue = null,
        song.uri
    ) {
        value = withContext(Dispatchers.IO) {
            runCatching { SongFileInfoReader.read(song) }.getOrNull()
        }
    }
    val info = infoState.value

    Box(
        modifier.fillMaxSize().background(pal.scrim.copy(alpha = 0.55f)),
        contentAlignment = Alignment.Center
    ) {
        GlassCard(Modifier.widthIn(max = 520.dp).fillMaxWidth(0.92f)) {
            Column(
                Modifier.verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val art = artworkState.value
                    if (art != null) {
                        Image(
                            bitmap = art,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(56.dp).clip(RoundedCornerShape(12.dp))
                        )
                    } else {
                        Box(
                            Modifier.size(56.dp).clip(RoundedCornerShape(12.dp))
                                .background(pal.track),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                song.title.take(1).uppercase().ifBlank { "♪" },
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                color = pal.sub
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            song.title,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
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
                    SelectChip(s.close, false, onClick = onClose)
                }

                SectionTitle(s.propsTags)
                PropRow(s.propsTitle, song.title)
                PropRow(s.propsArtist, song.artist.ifBlank { "—" })
                PropRow(s.propsAlbum, song.album.ifBlank { "—" })
                PropRow(
                    s.propsDuration,
                    if (song.durationMs > 0) formatTime(song.durationMs) else "—"
                )

                SectionTitle(s.propsFile)
                PropRow(s.propsName, info?.fileName ?: song.uri.substringAfterLast('/').substringAfterLast('\\'))
                PropRow(
                    s.propsPath,
                    song.uri,
                    ellipsize = true
                )
                PropRow(s.propsSize, SongFileInfoReader.formatSize(info?.sizeBytes))
                PropRow(
                    s.propsFormat,
                    info?.extension?.uppercase()?.ifBlank { "—" } ?: "—"
                )
                if (info != null && !info.exists) {
                    Text(s.propsMissing, fontSize = 12.sp, color = pal.sub)
                }

                SectionTitle(s.propsAudio)
                PropRow(s.propsSampleRate, SongFileInfoReader.formatRate(info?.sampleRateHz))
                PropRow(
                    s.propsChannels,
                    info?.channels?.let { if (it == 1) s.propsMono else s.propsStereo } ?: "—"
                )
                PropRow(
                    s.propsBits,
                    info?.bitsPerSample?.let { "$it-bit" } ?: "—"
                )
                PropRow(
                    s.propsBitrate,
                    SongFileInfoReader.formatBitrate(info?.bitrateKbpsEstimate)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SelectChip(
                        label = s.favourites,
                        selected = liked,
                        onClick = onToggleFavorite
                    )
                    if (info?.exists == true) {
                        SelectChip(s.propsShowInFolder, false, onClick = {
                            scope.launch(Dispatchers.IO) {
                                runCatching {
                                    val parent = File(song.uri).parentFile
                                    if (parent?.isDirectory == true && Desktop.isDesktopSupported()) {
                                        Desktop.getDesktop().open(parent)
                                    }
                                }
                            }
                        })
                    }
                }

                SectionTitle(s.abLoop)
                Text(
                    if (loopA != null && loopB != null) {
                        "${formatTime(loopA)} – ${formatTime(loopB)}"
                    } else if (loopA != null) {
                        "${formatTime(loopA)} – …"
                    } else s.abClear,
                    fontSize = 12.sp,
                    color = pal.sub
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SelectChip(s.jumpTitle, false, onClick = onJumpToTime)
                    SelectChip(s.abSetA, loopA != null, onClick = onSetLoopA)
                    SelectChip(s.abSetB, loopB != null, onClick = onSetLoopB)
                    if (loopA != null || loopB != null) {
                        SelectChip(s.abClear, false, onClick = onClearLoop)
                    }
                }

                SectionTitle("ReplayGain")
                PropRow(
                    "Track gain",
                    song.replayGainDb?.let { db ->
                        "%+.2f dB".format(db)
                    } ?: "—"
                )
                Spacer(Modifier.height(2.dp))
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    val pal = LocalAP.current
    Text(text, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = pal.ink)
}

@Composable
private fun PropRow(label: String, value: String, ellipsize: Boolean = false) {
    val pal = LocalAP.current
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Text(
            label,
            fontSize = 12.sp,
            color = pal.sub,
            modifier = Modifier.width(110.dp)
        )
        Text(
            value,
            fontSize = 12.sp,
            color = pal.ink,
            maxLines = if (ellipsize) 2 else 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}
