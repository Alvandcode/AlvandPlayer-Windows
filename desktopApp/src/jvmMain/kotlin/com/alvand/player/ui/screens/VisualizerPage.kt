package com.alvand.player.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alvand.player.i18n.LocalStrings
import com.alvand.player.player.EngineState
import com.alvand.player.ui.components.LinearSeekBar
import com.alvand.player.ui.components.TransportRow
import com.alvand.player.ui.components.formatTime
import com.alvand.player.ui.theme.LocalAP

/**
 * صفحهٔ ویژوالایزر — موبه‌مو از روی طرح: دایرهٔ بزرگِ وسط با حلقهٔ طیفِ صوتی که
 * دورش می‌چرخد و کنترل‌های پخش زیرش.
 *
 * داده کاملاً واقعی است: میله‌ها از `EngineState.spectrum` می‌آیند که نخ پخش ~۱۱
 * بار در ثانیه تازه‌اش می‌کند.
 */
@Composable
fun VisualizerPage(
    state: EngineState,
    accent: Color,
    liked: Boolean,
    onToggleLike: () -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val pal = LocalAP.current
    val s = LocalStrings.current
    val current = state.current

    // یک انیمیشن آرام برای چرخش حلقه، حتی وقتی ساکن است
    val transition = rememberInfiniteTransition(label = "vizRing")
    val spin by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(24_000, easing = LinearEasing), RepeatMode.Restart),
        label = "spin"
    )
    // اگر طیف نداریم (صفحه ساکت)، چند میلهٔ ساختگیِ ملایم می‌سازیم تا حلقه خالی نباشد
    val bars = remember(state.spectrum) {
        if (state.spectrum.isEmpty()) List(48) { 0.06f } else state.spectrum
    }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            s.visualizer,
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            color = pal.ink
        )

        // حلقهٔ طیف دور دایرهٔ وسط — قلب ویژوالایزر
        Box(
            Modifier
                .fillMaxWidth()
                .height(320.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val maxRadius = minOf(size.width, size.height) / 2f - 12.dp.toPx()

                // یک حلقهٔ نازکِ پایه (پس‌زمینهٔ ملایم)
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(accent.copy(alpha = 0.10f), Color.Transparent),
                        center = center,
                        radius = maxRadius
                    ),
                    radius = maxRadius,
                    center = center
                )
                drawCircle(
                    color = accent.copy(alpha = 0.35f),
                    radius = maxRadius - 18.dp.toPx(),
                    center = center,
                    style = Stroke(width = 1.dp.toPx())
                )

                // میله‌های طیف دور حلقه
                val n = bars.size
                val inner = maxRadius - 30.dp.toPx()
                for (i in 0 until n) {
                    val amp = bars[i].coerceIn(0.05f, 1f)
                    val angle = (i.toFloat() / n) * 2f * Math.PI.toFloat()
                    val h = amp * 24.dp.toPx()
                    val mid = inner - h / 2f
                    val x = center.x + (mid * kotlin.math.cos(angle))
                    val y = center.y + (mid * kotlin.math.sin(angle))
                    drawLine(
                        brush = Brush.horizontalGradient(listOf(accent, accent.copy(alpha = 0.35f))),
                        start = Offset(
                            x - 2f * kotlin.math.cos(angle),
                            y - 2f * kotlin.math.sin(angle)
                        ),
                        end = Offset(
                            x + h * kotlin.math.cos(angle),
                            y + h * kotlin.math.sin(angle)
                        ),
                        strokeWidth = 3.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }

            // دایرهٔ وسط با نشان برنامه
            Box(
                Modifier
                    .size(140.dp)
                    .background(
                        Brush.linearGradient(
                            listOf(accent.copy(alpha = 0.35f), accent.copy(alpha = 0.08f))
                        ),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "A",
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Black,
                    color = accent
                )
            }
        }

        if (current != null) {
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    current.title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = pal.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    current.artist,
                    fontSize = 13.sp,
                    color = pal.sub,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Column(Modifier.fillMaxWidth()) {
                LinearSeekBar(
                    progress = state.progress,
                    durationMs = state.durationMs,
                    onSeekMs = onSeek,
                    trackColor = pal.track,
                    progressColor = accent,
                    enabled = state.hasKnownDuration,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(formatTime(state.positionMs), fontSize = 11.sp, color = pal.sub)
                    Text(
                        if (state.hasKnownDuration) formatTime(state.durationMs) else "--:--",
                        fontSize = 11.sp,
                        color = pal.sub
                    )
                }
            }

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
                showLike = true
            )
        } else {
            Text(s.lyricsNoMusic, fontSize = 13.sp, color = pal.sub, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(8.dp))
    }
}