package com.alvand.player.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alvand.player.audio.DspSpec
import com.alvand.player.audio.DspUiLogic
import com.alvand.player.audio.EqPresets
import com.alvand.player.i18n.LocalStrings
import com.alvand.player.ui.components.GlassCard
import com.alvand.player.ui.components.SelectChip
import com.alvand.player.ui.theme.LocalAP
import kotlin.math.roundToInt

/**
 * صفحهٔ اکولایزر — موبه‌مو از روی طرح: فیدرهای عمودی با برچسب فرکانس زیرشان،
 * انتخابگر پریست بالا، و ردیف‌های روشن/خاموش برای باس، اتاق و ویژوالایزر.
 *
 * همه چیز واقعی است: موتر پنج‌باندی [DspSpec] را زنده تغییر می‌دهد و بین اجراها
 * ذخیره می‌ماند (از طریق `onSpecChange` که به `SettingsStore.setDspSpec` وصل است).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EqualizerPage(
    spec: DspSpec,
    accent: Color,
    visualizerEnabled: Boolean,
    onSpecChange: (DspSpec) -> Unit,
    onToggleVisualizer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val s = LocalStrings.current
    val pal = LocalAP.current
    val freqs = DspUiLogic.bandFrequencies()

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                s.equalizer,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = pal.ink,
                modifier = Modifier.weight(1f)
            )
            SelectChip(
                label = presetLabel(spec),
                selected = false,
                onClick = { /* انتخابگر پایین صفحه این کار را می‌کند */ }
            )
        }

        // فیدرهای عمودی — قلب صفحه، مثل طرح
        GlassCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    freqs.forEachIndexed { index, hz ->
                        VerticalFader(
                            value = spec.bandLevels.getOrElse(index) { 0 }.toFloat(),
                            range = DspSpec.MIN_MB.toFloat()..DspSpec.MAX_MB.toFloat(),
                            enabled = spec.eqEnabled,
                            accent = accent,
                            trackColor = pal.track,
                            label = DspUiLogic.formatBandFreq(hz),
                            onChange = { mb ->
                                onSpecChange(DspUiLogic.withBand(spec, index, mb.roundToInt()))
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // ردیف‌های روشن/خاموش، همان‌هایی که طرح نشان می‌دهد
        ToggleRow(
            label = s.eqBass,
            valueLabel = DspUiLogic.formatBassDb(spec.bassStrength),
            on = spec.bassStrength > 0,
            accent = accent,
            onToggle = {
                onSpecChange(
                    spec.copy(bassStrength = if (spec.bassStrength > 0) 0 else 500)
                )
            }
        )
        ToggleRow(
            label = s.eqReverb,
            valueLabel = listOf(s.eqReverbOff, s.eqReverbSmall, s.eqReverbMedium, s.eqReverbLarge)
                .getOrElse(spec.reverbPreset) { s.eqReverbOff },
            on = spec.reverbPreset > 0,
            accent = accent,
            onToggle = {
                onSpecChange(spec.copy(reverbPreset = if (spec.reverbPreset > 0) 0 else 1))
            }
        )
        ToggleRow(
            label = s.visualizer,
            valueLabel = if (visualizerEnabled) s.eqOn else s.eqOff,
            on = visualizerEnabled,
            accent = accent,
            onToggle = onToggleVisualizer
        )

        // پریست‌ها — همان ۱۱ پریست سیستمی اندروید + حالت دستی
        Text(s.eqPresets, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = pal.ink)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SelectChip(s.eqManual, spec.isManual) {
                onSpecChange(spec.copy(preset = -1))
            }
            EqPresets.DISPLAY_ORDER.forEachIndexed { index, name ->
                SelectChip(name, spec.preset == index) {
                    onSpecChange(DspUiLogic.applyPreset(spec, index))
                }
            }
        }

        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SelectChip(if (spec.eqEnabled) s.eqOn else s.eqOff, spec.eqEnabled) {
                onSpecChange(spec.copy(eqEnabled = !spec.eqEnabled))
            }
            SelectChip(s.eqReset, false) { onSpecChange(DspUiLogic.reset()) }
        }
        Spacer(Modifier.height(8.dp))
    }
}

private fun presetLabel(spec: DspSpec): String =
    if (spec.isManual) "Custom" else EqPresets.DISPLAY_ORDER.getOrElse(spec.preset) { "Custom" }

/** ردیف «روشن/خاموش» با مقدار سمت راست — مثل ردیف‌های طرح. */
@Composable
private fun ToggleRow(
    label: String,
    valueLabel: String,
    on: Boolean,
    accent: Color,
    onToggle: () -> Unit
) {
    val pal = LocalAP.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(pal.glass)
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = pal.ink,
            modifier = Modifier.weight(1f)
        )
        Text(
            valueLabel,
            fontSize = 12.sp,
            color = if (on) accent else pal.sub,
            modifier = Modifier.padding(end = 12.dp)
        )
        MiniToggle(on = on, accent = accent)
    }
}

/** کلید کوچک روشن/خاموش. */
@Composable
private fun MiniToggle(on: Boolean, accent: Color) {
    val pal = LocalAP.current
    Box(
        Modifier
            .size(width = 38.dp, height = 22.dp)
            .clip(RoundedCornerShape(50))
            .background(if (on) accent else pal.track)
            .padding(3.dp),
        contentAlignment = if (on) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(
            Modifier
                .size(16.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.White)
        )
    }
}

/**
 * فیدر عمودی کشیدنی.
 *
 * متریال `Slider` فقط افقی است؛ برای شکلِ طرح (عمودی، مثل میکسر) اینجا
 * مستقیم روی `draggable` ساخته شده. مقدارش همان میلی‌بلِ باند است.
 */
@Composable
private fun VerticalFader(
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    enabled: Boolean,
    accent: Color,
    trackColor: Color,
    label: String,
    onChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val pal = LocalAP.current
    val density = LocalDensity.current
    val faderHeight = 150.dp

    val fraction = ((value - range.start) / (range.endInclusive - range.start)).coerceIn(0f, 1f)

    // مقدار و کال‌بک باید داخل ژستور همیشه تازه باشند، پس با rememberUpdatedState
    // نگه داشته می‌شوند (وگرنه فیدر بعد از هر تغییر «گیر» می‌کند).
    val currentValue by rememberUpdatedState(value)
    val currentOnChange by rememberUpdatedState(onChange)
    val dragState = rememberDraggableState { delta ->
        val span = range.endInclusive - range.start
        val pxRange = with(density) { faderHeight.toPx() }
        if (pxRange <= 0f) return@rememberDraggableState
        currentOnChange(
            (currentValue - delta / pxRange * span).coerceIn(range.start, range.endInclusive)
        )
    }

    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .height(faderHeight)
                .width(34.dp)
                .clip(RoundedCornerShape(17.dp))
                .background(trackColor.copy(alpha = 0.35f))
                .then(
                    if (!enabled) Modifier else Modifier.draggable(
                        state = dragState,
                        orientation = Orientation.Vertical
                    )
                ),
            contentAlignment = Alignment.BottomCenter
        ) {
            // پرشدن از پایین تا مقدار فعلی
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(faderHeight * fraction)
                    .clip(RoundedCornerShape(17.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(accent, accent.copy(alpha = 0.6f))
                        )
                    )
            )
            // دستگیره
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = faderHeight * fraction - 7.dp)
                    .size(width = 26.dp, height = 14.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(if (enabled) Color.White else pal.line),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier
                        .size(width = 12.dp, height = 2.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(pal.sub)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            label,
            fontSize = 10.sp,
            color = pal.sub,
            maxLines = 1
        )
    }
}