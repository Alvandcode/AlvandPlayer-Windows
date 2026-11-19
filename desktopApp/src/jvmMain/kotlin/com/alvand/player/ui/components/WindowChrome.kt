package com.alvand.player.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import com.alvand.player.ui.theme.LocalAP

/**
 * نوار عنوان اختصاصی — جایگزین تایتل‌بار سفید ویندوز.
 *
 * مشکل: تایتل‌بار پیش‌فرض ویندوز هیچ ربطی به تم و UI برنامه نداشت و پنجره حس
 * «صفحهٔ وب» می‌داد. این نوار با همان پس‌زمینهٔ تم رندر می‌شود، پس جزئی از خودِ
 * برنامه به نظر می‌رسد. رفتارها عین استاندارد ویندوز است: درگ برای جابه‌جایی،
 * دابل‌کلیک برای ماکسیمایز، و سه دکمهٔ آشنای مینیمایز/ماکسیمایز/بستن.
 */
@Composable
fun AppTitleBar(
    state: WindowState,
    title: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pal = LocalAP.current
    val density = LocalDensity.current.density
    val maximized = state.placement == WindowPlacement.Maximized
    // نقطهٔ شروع درگ — با مختصات مطلق صفحه، نه دلتای ایونت.
    //
    // چرا مطلق؟ وقتی پنجره زیر موسِ ثابت جابه‌جا می‌شود، سیستم‌عامل ایونت حرکتی
    // تازه می‌فرستد؛ اگر آن را مثل حرکت کاربر با دلتا جمع کنیم، حلقهٔ فیدبک
    // درست می‌شود و پنجره می‌لرزد/از زیر موس فرار می‌کند. با مبنای ثابت، ایونت
    // اضافی دلتای صفر می‌دهد و پنجره سفت به موس می‌چسبد.
    var dragStartMouse by remember { mutableStateOf<java.awt.Point?>(null) }
    var dragStartPos by remember { mutableStateOf<WindowPosition.Absolute?>(null) }

    Row(
        modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(pal.bg)
            .pointerInput(maximized) {
                if (maximized) return@pointerInput
                detectDragGestures(
                    onDragStart = {
                        dragStartMouse = runCatching {
                            java.awt.MouseInfo.getPointerInfo()?.location
                        }.getOrNull()
                        dragStartPos = state.position as? WindowPosition.Absolute
                    },
                    onDragEnd = { dragStartMouse = null; dragStartPos = null },
                    onDragCancel = { dragStartMouse = null; dragStartPos = null },
                    onDrag = { change, _ ->
                        change.consume()
                        val mouse0 = dragStartMouse
                        val pos0 = dragStartPos
                        val mouse1 = runCatching {
                            java.awt.MouseInfo.getPointerInfo()?.location
                        }.getOrNull()
                        if (mouse0 != null && mouse1 != null && pos0 != null) {
                            state.position = WindowPosition(
                                pos0.x + ((mouse1.x - mouse0.x) / density).dp,
                                pos0.y + ((mouse1.y - mouse0.y) / density).dp
                            )
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        state.placement =
                            if (maximized) WindowPlacement.Floating else WindowPlacement.Maximized
                    }
                )
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = pal.sub,
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp)
        )
        CaptionButton(
            hoverColor = pal.glass,
            iconColor = pal.sub,
            onClick = { state.isMinimized = true }
        ) { s, c ->
            drawLine(
                color = c,
                start = Offset(s * 0.22f, s / 2f),
                end = Offset(s * 0.78f, s / 2f),
                strokeWidth = 1.5.dp.toPx()
            )
        }
        CaptionButton(
            hoverColor = pal.glass,
            iconColor = pal.sub,
            onClick = {
                state.placement =
                    if (maximized) WindowPlacement.Floating else WindowPlacement.Maximized
            }
        ) { s, c ->
            if (maximized) {
                // حالت restore: دو مربع روی هم
                drawRect(
                    color = c,
                    topLeft = Offset(s * 0.32f, s * 0.18f),
                    size = androidx.compose.ui.geometry.Size(s * 0.5f, s * 0.5f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx())
                )
                drawRect(
                    color = c,
                    topLeft = Offset(s * 0.18f, s * 0.32f),
                    size = androidx.compose.ui.geometry.Size(s * 0.5f, s * 0.5f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx())
                )
            } else {
                drawRect(
                    color = c,
                    topLeft = Offset(s * 0.22f, s * 0.22f),
                    size = androidx.compose.ui.geometry.Size(s * 0.56f, s * 0.56f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx())
                )
            }
        }
        CaptionButton(
            hoverColor = Color(0xFFE81123),
            hoverIconColor = Color.White,
            iconColor = pal.sub,
            onClick = onClose
        ) { s, c ->
            val w = 1.5.dp.toPx()
            drawLine(c, Offset(s * 0.22f, s * 0.22f), Offset(s * 0.78f, s * 0.78f), w)
            drawLine(c, Offset(s * 0.78f, s * 0.22f), Offset(s * 0.22f, s * 0.78f), w)
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun CaptionButton(
    hoverColor: Color,
    iconColor: Color,
    onClick: () -> Unit,
    hoverIconColor: Color = iconColor,
    drawGlyph: DrawScope.(sizePx: Float, color: Color) -> Unit
) {
    var hovered by remember { mutableStateOf(false) }
    val glyphColor = if (hovered) hoverIconColor else iconColor
    Box(
        Modifier
            .width(46.dp)
            .fillMaxHeight()
            .background(if (hovered) hoverColor else Color.Transparent)
            .onPointerEvent(PointerEventType.Enter) { hovered = true }
            .onPointerEvent(PointerEventType.Exit) { hovered = false }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    val up = waitForUpOrCancellation()
                    if (up != null) onClick()
                    down.consume()
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(12.dp)) { drawGlyph(size.minDimension, glyphColor) }
    }
}

/**
 * دستگیره‌های تغییر اندازه برای پنجرهٔ بدون قاب.
 *
 * پنجرهٔ undecorated دستگیرهٔ سیستمی ندارد، پس هشت ناحیهٔ نامرئی (۴ لبه + ۴ گوشه)
 * خودمان می‌گذاریم. در حالت ماکسیمایز غیرفعال‌اند. حداقل اندازه رعایت می‌شود تا
 * پنجره گم نشود.
 */
@Composable
fun ResizeHandles(
    state: WindowState,
    modifier: Modifier = Modifier,
    minSize: DpSize = DpSize(640.dp, 480.dp),
    grip: Dp = 6.dp
) {
    if (state.placement == WindowPlacement.Maximized) return
    val density = LocalDensity.current.density

    fun apply(dxPx: Float, dyPx: Float, left: Boolean, top: Boolean) {
        val pos = state.position
        if (pos !is WindowPosition.Absolute) return
        var newW = state.size.width
        var newH = state.size.height
        var newX = pos.x
        var newY = pos.y
        if (dxPx != 0f) {
            if (left) {
                val want = (state.size.width - (dxPx / density).dp).coerceAtLeast(minSize.width)
                newX = pos.x + (state.size.width - want)
                newW = want
            } else {
                newW = (state.size.width + (dxPx / density).dp).coerceAtLeast(minSize.width)
            }
        }
        if (dyPx != 0f) {
            if (top) {
                val want = (state.size.height - (dyPx / density).dp).coerceAtLeast(minSize.height)
                newY = pos.y + (state.size.height - want)
                newH = want
            } else {
                newH = (state.size.height + (dyPx / density).dp).coerceAtLeast(minSize.height)
            }
        }
        state.size = DpSize(newW, newH)
        state.position = WindowPosition(newX, newY)
    }

    Box(modifier.fillMaxSize()) {
        // لبه‌ها
        ResizeZone(Modifier.align(Alignment.CenterStart).fillMaxHeight().width(grip)) { dx, _ -> apply(dx, 0f, left = true, top = false) }
        ResizeZone(Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(grip)) { dx, _ -> apply(dx, 0f, left = false, top = false) }
        ResizeZone(Modifier.align(Alignment.TopCenter).fillMaxWidth().height(grip)) { _, dy -> apply(0f, dy, left = false, top = true) }
        ResizeZone(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(grip)) { _, dy -> apply(0f, dy, left = false, top = false) }
        // گوشه‌ها
        ResizeZone(Modifier.align(Alignment.TopStart).size(grip * 2)) { dx, dy -> apply(dx, dy, left = true, top = true) }
        ResizeZone(Modifier.align(Alignment.TopEnd).size(grip * 2)) { dx, dy -> apply(dx, dy, left = false, top = true) }
        ResizeZone(Modifier.align(Alignment.BottomStart).size(grip * 2)) { dx, dy -> apply(dx, dy, left = true, top = false) }
        ResizeZone(Modifier.align(Alignment.BottomEnd).size(grip * 2)) { dx, dy -> apply(dx, dy, left = false, top = false) }
    }
}

@Composable
private fun ResizeZone(
    modifier: Modifier = Modifier,
    onDrag: (dxPx: Float, dyPx: Float) -> Unit
) {
    Box(
        modifier.pointerInput(Unit) {
            detectDragGestures { change, dragAmount ->
                change.consume()
                onDrag(dragAmount.x, dragAmount.y)
            }
        }
    )
}
