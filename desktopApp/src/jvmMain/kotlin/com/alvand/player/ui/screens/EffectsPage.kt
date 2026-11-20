package com.alvand.player.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alvand.player.audio.DspSpec
import com.alvand.player.audio.DspUiLogic
import com.alvand.player.i18n.LocalStrings
import com.alvand.player.ui.components.GlassCard
import com.alvand.player.ui.components.SelectChip
import com.alvand.player.ui.theme.LocalAP
import kotlin.math.roundToInt

/**
 * صفحهٔ افکت‌ها — همهٔ جلوه‌های صوتیِ غیر از باندکشیدنِ اکولایزر.
 *
 * این صفحه از قبل در سایدبار بود ولی مقصدش صفحهٔ اکولایزر بود و هیچ صفحهٔ
 * واقعی پشتش نبود؛ حالا کنترل‌های واقعیِ همان چیزهایی را می‌دهد که موتور
 * ([com.alvand.player.player.DspChain]) از قبل اجرا می‌کرد ولی UI نداشتند:
 * تقویت باس (اسلایدر پیوسته)، بلندی صدا، اکوی اتاق، و کاهش نویز با شدت.
 *
 * همهٔ تغییرها همان مسیر [onSpecChange] را می‌روند: `SettingsStore.setDspSpec`
 * ذخیره می‌کند و موتور وسط پخش زنده اعمالش می‌کند.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EffectsPage(
    spec: DspSpec,
    accent: Color,
    onSpecChange: (DspSpec) -> Unit,
    modifier: Modifier = Modifier
) {
    val s = LocalStrings.current
    val pal = LocalAP.current

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                s.navEffects,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = pal.ink,
                modifier = Modifier.weight(1f)
            )
            SelectChip(s.eqReset, selected = false) { onSpecChange(DspUiLogic.reset()) }
        }

        // تقویت باس — پیوسته ۰..۱۰۰۰ مثل اندروید
        GlassCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        s.eqBass,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = pal.ink,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        DspUiLogic.formatBassDb(spec.bassStrength),
                        fontSize = 12.sp,
                        color = if (spec.isBassActive) accent else pal.sub
                    )
                }
                Slider(
                    value = spec.bassStrength.toFloat(),
                    onValueChange = {
                        onSpecChange(spec.copy(bassStrength = it.roundToInt().coerceIn(0, 1000)))
                    },
                    valueRange = 0f..1000f,
                    colors = SliderDefaults.colors(
                        thumbColor = accent,
                        activeTrackColor = accent,
                        inactiveTrackColor = pal.track
                    )
                )
            }
        }

        // بلندی صدا (Loudness) — گام‌های ۲ دسی‌بل تا ۱۰
        EffectChipsRow(
            title = s.eqBoost,
            chipLabels = listOf(0, 2, 4, 6, 8, 10).map { DspUiLogic.formatBoostDb(it) },
            selectedLabel = DspUiLogic.formatBoostDb(spec.volumeBoostDb),
            onSelect = { idx ->
                onSpecChange(spec.copy(volumeBoostDb = listOf(0, 2, 4, 6, 8, 10)[idx]))
            }
        )

        // اکوی اتاق — همان سه اندازهٔ موتور
        val roomLabels = listOf(s.eqReverbOff, s.eqReverbSmall, s.eqReverbMedium, s.eqReverbLarge)
        EffectChipsRow(
            title = s.eqReverb,
            chipLabels = roomLabels,
            selectedLabel = roomLabels.getOrElse(spec.reverbPreset.coerceIn(0, 3)) { s.eqReverbOff },
            onSelect = { idx -> onSpecChange(spec.copy(reverbPreset = idx)) }
        )

        // کاهش نویز — کلید روشن/خاموش + شدت وقتی روشن است
        GlassCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onSpecChange(spec.copy(noiseReduction = !spec.noiseReduction)) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        s.eqNoise,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = pal.ink,
                        modifier = Modifier.weight(1f)
                    )
                    MiniToggle(on = spec.noiseReduction, accent = accent)
                }
                if (spec.noiseReduction) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            s.eqNoiseLevel,
                            fontSize = 12.sp,
                            color = pal.sub,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            "${spec.noiseLevel}%",
                            fontSize = 12.sp,
                            color = accent
                        )
                    }
                    Slider(
                        value = spec.noiseLevel.toFloat(),
                        onValueChange = {
                            onSpecChange(spec.copy(noiseLevel = it.roundToInt().coerceIn(0, 100)))
                        },
                        valueRange = 0f..100f,
                        colors = SliderDefaults.colors(
                            thumbColor = accent,
                            activeTrackColor = accent,
                            inactiveTrackColor = pal.track
                        )
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}

/** یک ردیف عنوان + چیپ‌های انتخاب با فاصلهٔ درست (افقی و عمودی). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EffectChipsRow(
    title: String,
    chipLabels: List<String>,
    selectedLabel: String,
    onSelect: (Int) -> Unit
) {
    val pal = LocalAP.current
    GlassCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = pal.ink)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                chipLabels.forEachIndexed { idx, label ->
                    SelectChip(label, selected = label == selectedLabel) { onSelect(idx) }
                }
            }
        }
    }
}

/** کلید کوچک روشن/خاموش — همان ظاهر ToggleRow صفحهٔ اکولایزر. */
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
