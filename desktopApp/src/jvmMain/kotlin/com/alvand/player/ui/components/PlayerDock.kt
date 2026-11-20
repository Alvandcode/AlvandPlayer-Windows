package com.alvand.player.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.decodeToImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alvand.player.player.RepeatMode
import com.alvand.player.system.AppIcons
import com.alvand.player.ui.components.HintTooltip
import com.alvand.player.ui.icons.Favorite
import com.alvand.player.ui.icons.FavoriteBorder
import com.alvand.player.ui.icons.Pause
import com.alvand.player.ui.icons.PlayArrow
import com.alvand.player.ui.icons.Repeat
import com.alvand.player.ui.icons.RepeatOne
import com.alvand.player.ui.icons.Shuffle
import com.alvand.player.ui.icons.SkipNext
import com.alvand.player.ui.icons.SkipPrevious
import com.alvand.player.ui.theme.LocalAP
import kotlinx.coroutines.delay
import kotlin.math.abs

/**
 * نشانِ برنامه (آیکون + نام) — بالای سایدبار و مینی‌کارت.
 */
@Composable
internal fun AppMark(appName: String) {
    val pal = LocalAP.current
    val bytes = remember {
        AppIcons::class.java.getResourceAsStream("/icons/alvand-256.png")?.use { it.readBytes() }
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (bytes != null) {
            val bmp = remember(bytes) { bytes.decodeToImageBitmap() }
            Image(
                painter = BitmapPainter(bmp, IntOffset.Zero, IntSize(bmp.width, bmp.height)),
                contentDescription = null,
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
            )
        }
        Text(appName, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = pal.sub)
    }
}

/**
 * ردیف کنترل‌های آیکونی — دقیقاً ترتیب تصویر مرجع: قبلی، پخش، بعدی، شافل، تکرار
 * (قلب اگر خواسته شود جداگانه نشان داده می‌شود).
 */
@Composable
fun TransportRow(
    isPlaying: Boolean,
    shuffleEnabled: Boolean,
    repeatMode: RepeatMode,
    accent: Color,
    liked: Boolean,
    onToggleLike: () -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    modifier: Modifier = Modifier,
    showLike: Boolean = true
) {
    val pal = LocalAP.current
    val s = com.alvand.player.i18n.LocalStrings.current
    // ترتیب چپ‌به‌راست ثابت می‌ماند، مثل نسخهٔ اندروید
    androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.ui.platform.LocalLayoutDirection provides
            androidx.compose.ui.unit.LayoutDirection.Ltr
    ) {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            HintTooltip(s.scNextPrev) {
                IconButton(onClick = onPrevious, modifier = Modifier.size(40.dp)) {
                    Icon(
                        Icons.Default.SkipPrevious,
                        contentDescription = null,
                        tint = pal.ink,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
            HintTooltip(s.scPlay) {
                Box(
                    Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(accent)
                        .clickable(onClick = onTogglePlay),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            HintTooltip(s.scNextPrev) {
                IconButton(onClick = onNext, modifier = Modifier.size(40.dp)) {
                    Icon(
                        Icons.Default.SkipNext,
                        contentDescription = null,
                        tint = pal.ink,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
            HintTooltip(s.scShuffle) {
                IconButton(onClick = onToggleShuffle, modifier = Modifier.size(38.dp)) {
                    Icon(
                        Icons.Default.Shuffle,
                        contentDescription = null,
                        tint = if (shuffleEnabled) accent else pal.sub,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            HintTooltip(s.scRepeat) {
                IconButton(onClick = onCycleRepeat, modifier = Modifier.size(36.dp)) {
                    Icon(
                        if (repeatMode == RepeatMode.ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                        contentDescription = null,
                        tint = if (repeatMode == RepeatMode.OFF) pal.sub else accent,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            if (showLike) {
                HintTooltip(s.scFav) {
                    IconButton(onClick = onToggleLike, modifier = Modifier.size(38.dp)) {
                        Icon(
                            if (liked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = null,
                            tint = if (liked) accent else pal.sub,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * نوار پیشرفت خطی با قابلیت seek — تپ = پرش مطلق، درگ افقی = کشیدن موقعیت.
 */
@Composable
fun LinearSeekBar(
    progress: Float,
    durationMs: Long,
    onSeekMs: (Long) -> Unit,
    modifier: Modifier = Modifier,
    trackColor: Color,
    progressColor: Color,
    enabled: Boolean = true,
    loopA: Float? = null,
    loopB: Float? = null
) {
    val latestSeek by rememberUpdatedState(onSeekMs)
    val latestDur by rememberUpdatedState(durationMs)

    Canvas(
        modifier
            .height(28.dp)
            .fillMaxWidth()
            .pointerInput(latestDur, enabled) {
                if (!enabled) return@pointerInput
                val slop = viewConfiguration.touchSlop
                awaitEachGesture {
                    val down = awaitFirstDown()
                    val width = size.width.toFloat()
                    if (width <= 0f) return@awaitEachGesture
                    fun fractionOf(x: Float) = (x / width).coerceIn(0f, 1f)
                    latestSeek((fractionOf(down.position.x) * latestDur).toLong())

                    var dragging = false
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break
                        if (!change.pressed) break
                        if (!dragging && abs(change.position.x - down.position.x) > slop) {
                            dragging = true
                        }
                        if (dragging) {
                            latestSeek((fractionOf(change.position.x) * latestDur).toLong())
                        }
                    }
                }
            }
    ) {
        val width = size.width
        if (width <= 0f) return@Canvas
        val barY = size.height / 2f
        val stroke = 4.dp.toPx()
        drawLine(
            color = trackColor,
            start = androidx.compose.ui.geometry.Offset(0f, barY),
            end = androidx.compose.ui.geometry.Offset(width, barY),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        val p = progress.coerceIn(0f, 1f)
        if (p > 0.001f) {
            val endX = width * p
            drawLine(
                color = progressColor,
                start = androidx.compose.ui.geometry.Offset(0f, barY),
                end = androidx.compose.ui.geometry.Offset(endX, barY),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
            drawCircle(
                color = Color.White,
                radius = 6.dp.toPx(),
                center = androidx.compose.ui.geometry.Offset(endX, barY)
            )
            drawCircle(
                color = progressColor,
                radius = 6.dp.toPx(),
                center = androidx.compose.ui.geometry.Offset(endX, barY),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
            )
        }
        // نشان‌های A و B حلقه — مثل جت‌آدیو، مستقیم روی نوار
        for (marker in listOfNotNull(loopA, loopB)) {
            val x = (width * marker.coerceIn(0f, 1f))
            drawLine(
                color = progressColor,
                start = androidx.compose.ui.geometry.Offset(x, barY - 8.dp.toPx()),
                end = androidx.compose.ui.geometry.Offset(x, barY + 8.dp.toPx()),
                strokeWidth = 2.5.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}
