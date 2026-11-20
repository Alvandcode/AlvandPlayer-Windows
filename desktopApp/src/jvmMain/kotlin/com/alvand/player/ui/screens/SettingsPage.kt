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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alvand.player.data.AppSettings
import com.alvand.player.data.ThemeMode
import com.alvand.player.i18n.LocalStrings
import com.alvand.player.player.AudioOutputs
import com.alvand.player.theme.AccentThemes
import com.alvand.player.ui.components.SelectChip
import com.alvand.player.ui.icons.Audiotrack
import com.alvand.player.ui.icons.Keyboard
import com.alvand.player.ui.icons.Palette
import com.alvand.player.ui.icons.Settings
import com.alvand.player.ui.theme.LocalAP

/**
 * صفحهٔ تنظیمات — موبه‌مو از روی طرح: ردیف‌های بخش‌دار با آیکون و توضیح.
 *
 * هر ردیف یک کارِ **واقعی** می‌کند: زبان، صدا و حالت سبک، تم و رنگ، میانبرها، و
 * نسخهٔ برنامه. هیچ ردیف نمایشی یا بی‌اثری وجود ندارد.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsPage(
    settings: AppSettings,
    systemDark: Boolean,
    volume: Float,
    accent: Color,
    onVolumeChange: (Float) -> Unit,
    onThemeModeChange: (Int) -> Unit,
    onAccentChange: (Int) -> Unit,
    onToggleLanguage: () -> Unit,
    onToggleOnlineLyrics: () -> Unit,
    onTogglePerformance: () -> Unit,
    onOutputDevice: (String?) -> Unit,
    sleepMinutes: Int = 0,
    onSleepChange: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val s = LocalStrings.current
    val pal = LocalAP.current
    var showShortcuts by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            s.settings,
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            color = pal.ink
        )

        SettingsRow(
            icon = Icons.Filled.Settings,
            title = s.setGeneral,
            subtitle = s.setGeneralSub
        )
        SubCard {
            SettingLine(s.setLanguage, s.setLanguageSub)
            SelectChip(
                label = if (settings.isPersian) "فارسی" else "English",
                selected = false,
                onClick = onToggleLanguage
            )
        }

        SettingsRow(
            icon = Icons.Filled.Audiotrack,
            title = s.setAudio,
            subtitle = s.setAudioSub
        )
        SubCard {
            Text(s.volumeLabel, fontSize = 13.sp, color = pal.sub)
            Slider(
                value = volume,
                onValueChange = onVolumeChange,
                valueRange = 0f..1f,
                colors = SliderDefaults.colors(
                    thumbColor = accent,
                    activeTrackColor = accent,
                    inactiveTrackColor = pal.track
                ),
                modifier = Modifier.fillMaxWidth()
            )
            ToggleLine(s.setLyricsOnline, onlineLyrics = settings.onlineLyricsEnabled, onToggle = onToggleOnlineLyrics)
            ToggleLine(s.setPerformance, onlineLyrics = settings.performanceMode, onToggle = onTogglePerformance)

            Text(s.outputDevice, fontSize = 13.sp, color = pal.sub, modifier = Modifier.padding(top = 10.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SelectChip(
                    label = s.outputDeviceDefault,
                    selected = settings.outputDevice == null,
                    onClick = { onOutputDevice(null) }
                )
                AudioOutputs.listDevices().forEach { name ->
                    SelectChip(
                        label = name,
                        selected = settings.outputDevice == name,
                        onClick = { onOutputDevice(name) }
                    )
                }
            }

            // تایمر خواب — قبلاً فقط با میانبر کیبورد در دسترس بود و UI نداشت
            Text(s.sleepTimer, fontSize = 13.sp, color = pal.sub, modifier = Modifier.padding(top = 10.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SelectChip(s.sleepOff, sleepMinutes == 0, onClick = { onSleepChange(0) })
                listOf(15, 30, 45, 60).forEach { min ->
                    SelectChip(
                        "$min ${if (s.language == "فارسی") "دقیقه" else "min"}",
                        sleepMinutes == min,
                        onClick = { onSleepChange(min) }
                    )
                }
            }
        }

        SettingsRow(
            icon = Icons.Filled.Palette,
            title = s.setAppearance,
            subtitle = s.setAppearanceSub
        )
        SubCard {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SelectChip(s.themeSystem, settings.themeMode == ThemeMode.SYSTEM) {
                    onThemeModeChange(ThemeMode.SYSTEM)
                }
                SelectChip(s.themeLight, settings.themeMode == ThemeMode.LIGHT) {
                    onThemeModeChange(ThemeMode.LIGHT)
                }
                SelectChip(s.themeDark, settings.themeMode == ThemeMode.DARK) {
                    onThemeModeChange(ThemeMode.DARK)
                }
            }
            Text(s.themeColor, fontSize = 13.sp, color = pal.sub, modifier = Modifier.padding(top = 10.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AccentThemes.forEach { t ->
                    SelectChip(
                        label = s.accentName(t.id),
                        selected = settings.accentTheme == t.id,
                        dotColor = t.accent(pal.isDark),
                        onClick = { onAccentChange(t.id) }
                    )
                }
            }
        }

        SettingsRow(
            icon = Icons.Filled.Keyboard,
            title = s.setShortcuts,
            subtitle = s.shortcutsHint,
            onClick = { showShortcuts = true }
        )
        SettingsRow(
            icon = Icons.Filled.Info,
            title = s.setAbout,
            subtitle = "${s.appName} ${s.appVersion}",
            onClick = { showAbout = true }
        )
        Spacer(Modifier.height(8.dp))
    }

    if (showShortcuts) {
        ShortcutsDialog(s) { showShortcuts = false }
    }
    if (showAbout) {
        AboutDialog(s) { showAbout = false }
    }
}

/** ردیفِ سربرگِ هر بخش: آیکون + عنوان + توضیح (اختیاری: کلیک‌پذیر). */
@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null
) {
    val pal = LocalAP.current
    Row(
        Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onClick)
                else Modifier
            )
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(pal.glass),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = pal.ink, modifier = Modifier.size(22.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = pal.ink)
            Text(subtitle, fontSize = 12.sp, color = pal.sub, maxLines = 2)
        }
    }
}

/** کارتِ کوچک زیرمجموعهٔ یک بخش (مثل جعبه‌های سفید داخل ردیف‌ها). */
@Composable
private fun SubCard(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(LocalAP.current.glass)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) { content() }
}

/** دیالوگ فهرست میانبرهای کیبورد — قبلاً ردیف تنظیمات کاملاً مرده بود. */
@Composable
private fun ShortcutsDialog(s: com.alvand.player.i18n.Strings, onClose: () -> Unit) {
    val pal = LocalAP.current
    androidx.compose.ui.window.Dialog(onDismissRequest = onClose) {
        Column(
            Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(pal.card)
                .padding(20.dp)
                .widthIn(max = 420.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(s.setShortcuts, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = pal.ink)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(s.scActionCol, fontSize = 12.sp, color = pal.sub)
                Text(s.scKeyCol, fontSize = 12.sp, color = pal.sub)
            }
            listOf(
                s.scPlay to "Space",
                s.scNextPrev to "← / →",
                s.scSeek to "Ctrl + ← / →",
                s.scVolume to "↑ / ↓",
                s.scMute to "M",
                s.scFav to "L",
                s.scShuffle to "S",
                s.scRepeat to "R",
                s.scSearch to "Ctrl + K",
                s.scSleep to "T",
                s.scEq to "E",
                s.scProps to "I",
                s.scJump to "J",
                s.scLoop to "B",
                s.scViz to "V"
            ).forEach { (action, key) ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(action, fontSize = 13.sp, color = pal.ink)
                    Text(key, fontSize = 13.sp, color = pal.sub)
                }
            }
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                SelectChip(s.close, true, onClick = onClose)
            }
        }
    }
}

/** دیالوگ دربارهٔ برنامه — قبلاً ردیف «درباره» مرده بود. */
@Composable
private fun AboutDialog(s: com.alvand.player.i18n.Strings, onClose: () -> Unit) {
    val pal = LocalAP.current
    androidx.compose.ui.window.Dialog(onDismissRequest = onClose) {
        Column(
            Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(pal.card)
                .padding(20.dp)
                .widthIn(max = 380.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(s.appName, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = pal.ink)
            Text(s.appVersion, fontSize = 12.sp, color = pal.sub)
            Text(
                s.aboutBody,
                fontSize = 13.sp,
                color = pal.ink,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Text(s.aboutLicense, fontSize = 11.sp, color = pal.sub)
            Spacer(Modifier.height(4.dp))
            SelectChip(s.close, true, onClick = onClose)
        }
    }
}

@Composable
private fun SettingLine(title: String, subtitle: String) {
    val pal = LocalAP.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = pal.ink)
            Text(subtitle, fontSize = 12.sp, color = pal.sub)
        }
    }
}

@Composable
private fun ToggleLine(title: String, onlineLyrics: Boolean, onToggle: () -> Unit) {
    val pal = LocalAP.current
    val s = LocalStrings.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onToggle)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 14.sp, color = pal.ink, modifier = Modifier.weight(1f))
        SelectChip(
            label = if (onlineLyrics) s.eqOn else s.eqOff,
            selected = onlineLyrics,
            onClick = onToggle
        )
    }
}