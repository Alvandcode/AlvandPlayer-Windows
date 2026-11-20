package com.alvand.player.ui.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import com.alvand.player.ui.icons.Pause
import com.alvand.player.ui.icons.SkipNext
import com.alvand.player.ui.icons.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import kotlin.math.roundToInt
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import com.alvand.player.ui.theme.AlvandTheme
import com.alvand.player.ui.theme.LocalAccentTheme
import com.alvand.player.ui.theme.LocalAP
import java.awt.GraphicsEnvironment
import java.awt.Toolkit

/**
 * پنجرهٔ کوچک مینی‌پلیر: وقتی پنجرهٔ اصلی minimize می‌شود، این پنجرهٔ کوچکِ
 * همیشه‌روی‌صفحه در گوشهٔ پایین‌راست باز می‌شود و دکمه‌های پخش/توقف، قبلی/بعدی
 * و کم/زیاد کردن صدا دارد. بدون دکوراسیون و بدون قفل‌شدن روی برنامهٔ فعال.
 */
@Composable
fun MiniPlayerWindow(
    title: String,
    artist: String,
    isPlaying: Boolean,
    progress: Float,
    accentThemeId: Int,
    volume: Float,
    darkTheme: Boolean = true,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onVolume: (Float) -> Unit,
    onRestore: () -> Unit,
    onClose: () -> Unit
) {
    // گوشهٔ پایین‌راست «ناحیهٔ کاری» صفحه (به‌جز نوار وظیفه) — یک‌بار محاسبه می‌شود
    val position = remember {
        val gc = GraphicsEnvironment.getLocalGraphicsEnvironment()
            .defaultScreenDevice.defaultConfiguration
        val insets = Toolkit.getDefaultToolkit().getScreenInsets(gc)
        val scale = gc.defaultTransform.scaleX.toFloat().coerceAtLeast(1f)
        val workW = gc.bounds.width / scale
        val workH = (gc.bounds.height - insets.bottom) / scale
        WindowPosition(x = Dp(workW - 356f), y = Dp(workH - 138f))
    }
    val windowState = rememberWindowState(size = DpSize(336.dp, 118.dp), position = position)

    Window(
        onCloseRequest = onClose,
        visible = true,
        state = windowState,
        title = "Alvand Player",
        undecorated = true,
        resizable = false,
        alwaysOnTop = true
    ) {
        AlvandTheme(darkTheme = darkTheme, accentThemeId = accentThemeId) {
            val pal = LocalAP.current
            val accent = LocalAccentTheme.current.accent(isDark = darkTheme)
            val onAccent = Color(0xFF0C0C10)

            // پس‌زمینهٔ خود پنجره هم همرنگ کارت شود تا هیچ لبهٔ سفیدی نماند
            val awtBg = pal.bg.value
            window.background = java.awt.Color(
                (awtBg shr 16 and 0xFFu).toInt(),
                (awtBg shr 8 and 0xFFu).toInt(),
                (awtBg and 0xFFu).toInt()
            )

            // کارتِ تمام‌صفحه و بدون گوشهٔ گرد — هیچ نقطهٔ سفیدی باقی نمی‌ماند
            Surface(
                color = pal.bg,
                contentColor = pal.ink,
                modifier = Modifier.fillMaxSize()
            ) {
                Column(Modifier.padding(start = 12.dp, end = 6.dp, top = 8.dp, bottom = 10.dp)) {
                    // ردیف بالا: عنوان آهنگ (کشیدنی برای جابه‌جایی پنجره) + بازگرداندن + بستن
                    val density = androidx.compose.ui.platform.LocalDensity.current
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(
                            Modifier
                                .weight(1f)
                                .pointerInput(Unit) {
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()
                                        // پیکسل‌های Compose → پیکسل فیزیکی صفحه
                                        val scale = density.density
                                        window.setLocation(
                                            window.x + (dragAmount.x * scale).roundToInt(),
                                            window.y + (dragAmount.y * scale).roundToInt()
                                        )
                                    }
                                }
                        ) {
                            Text(
                                title,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = pal.ink
                            )
                            Text(
                                artist,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontSize = 11.sp,
                                color = pal.sub
                            )
                        }
                        IconButton(onRestore, modifier = Modifier.size(30.dp)) {
                            Icon(
                                Icons.Filled.KeyboardArrowUp,
                                contentDescription = null,
                                tint = pal.sub,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        IconButton(onClose, modifier = Modifier.size(26.dp)) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = null,
                                tint = pal.sub,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(2.dp))

                    // ردیف کنترل‌ها: قبلی | پخش/توقف | بعدی | صدا کم [اسلایدر] صدا زیاد
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        IconButton(onPrevious, modifier = Modifier.size(38.dp)) {
                            Icon(
                                Icons.Filled.SkipPrevious,
                                contentDescription = null,
                                tint = pal.ink,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Box(
                            Modifier
                                .size(42.dp)
                                .background(accent, CircleShape)
                                .clickable(onClick = onTogglePlay),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = null,
                                tint = onAccent,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        IconButton(onNext, modifier = Modifier.size(38.dp)) {
                            Icon(
                                Icons.Filled.SkipNext,
                                contentDescription = null,
                                tint = pal.ink,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(Modifier.width(4.dp))

                        IconButton(
                            { onVolume((volume - 0.1f).coerceIn(0f, 1f)) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Text(
                                "−",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = pal.sub
                            )
                        }
                        Slider(
                            value = volume.coerceIn(0f, 1f),
                            onValueChange = { onVolume(it) },
                            colors = SliderDefaults.colors(
                                thumbColor = accent,
                                activeTrackColor = accent,
                                inactiveTrackColor = pal.line
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            { onVolume((volume + 0.1f).coerceIn(0f, 1f)) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Text(
                                "+",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = pal.sub
                            )
                        }
                    }

                    Spacer(Modifier.height(6.dp))

                    // نوار پیشرفت باریک
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .background(pal.line, RoundedCornerShape(2.dp))
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth(progress.coerceIn(0f, 1f))
                                .height(3.dp)
                                .background(accent, RoundedCornerShape(2.dp))
                        )
                    }
                }
            }
        }
    }
}
