package com.alvand.player.ui.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alvand.player.i18n.LocalStrings
import com.alvand.player.ui.components.AppMark
import com.alvand.player.ui.components.MiniBars
import com.alvand.player.ui.icons.Equalizer
import com.alvand.player.ui.icons.Explore
import com.alvand.player.ui.icons.GraphicEq
import com.alvand.player.ui.icons.Home
import com.alvand.player.ui.icons.LibraryMusic
import com.alvand.player.ui.icons.QueueMusic
import com.alvand.player.ui.icons.Radio
import com.alvand.player.ui.icons.Settings
import com.alvand.player.ui.icons.Tune
import com.alvand.player.ui.theme.LocalAP

/**
 * سایدبار شل دسکتاپ — موبه‌مو از روی طرح: لوگو، ناوبری اصلی، ابزارها، تنظیمات،
 * و مینی‌کارت آهنگ جاری پایین.
 */
@Composable
fun Sidebar(
    current: Screen,
    onNavigate: (Screen) -> Unit,
    accent: Color,
    miniTitle: String?,
    miniArtist: String?,
    miniPlaying: Boolean,
    onMiniClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pal = LocalAP.current
    val s = LocalStrings.current
    Column(
        modifier
            .fillMaxHeight()
            .width(212.dp)
            .background(pal.glass)
            .padding(horizontal = 12.dp, vertical = 16.dp)
    ) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            AppMark(appName = "Alvand Player")
            Text(
                s.appTagline,
                fontSize = 11.sp,
                color = pal.sub,
                modifier = Modifier.padding(start = 34.dp, bottom = 12.dp)
            )

            NavItem(Icons.Filled.Home, s.navHome, current == Screen.HOME, accent) { onNavigate(Screen.HOME) }
            NavItem(Icons.Filled.LibraryMusic, s.navLibrary, current == Screen.LIBRARY, accent) { onNavigate(Screen.LIBRARY) }
            NavItem(Icons.Filled.QueueMusic, s.navPlaylists, current == Screen.PLAYLISTS, accent) { onNavigate(Screen.PLAYLISTS) }
            NavItem(Icons.Filled.Explore, s.navDiscover, current == Screen.DISCOVER, accent) { onNavigate(Screen.DISCOVER) }
            NavItem(Icons.Filled.Radio, s.navRadio, current == Screen.RADIO, accent) { onNavigate(Screen.RADIO) }

            SectionLabel(s.navTools)
            NavItem(Icons.Filled.Equalizer, s.equalizer, current == Screen.EQUALIZER, accent) { onNavigate(Screen.EQUALIZER) }
            // افکت‌ها صفحهٔ خودش را دارد: باس/بلندی/اکو/کاهش نویز
            NavItem(Icons.Filled.Tune, s.navEffects, current == Screen.EFFECTS, accent) { onNavigate(Screen.EFFECTS) }
            NavItem(Icons.Filled.GraphicEq, s.visualizer, current == Screen.VISUALIZER, accent) { onNavigate(Screen.VISUALIZER) }

            SectionLabel(s.navSettingsSection)
            NavItem(Icons.Filled.Settings, s.settings, current == Screen.SETTINGS, accent) { onNavigate(Screen.SETTINGS) }
        }
        if (miniTitle != null) {
            MiniNowCard(
                title = miniTitle,
                artist = miniArtist.orEmpty(),
                isPlaying = miniPlaying,
                onClick = onMiniClick
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    val pal = LocalAP.current
    Text(
        text,
        fontSize = 11.sp,
        color = pal.sub,
        modifier = Modifier.padding(start = 12.dp, top = 12.dp, bottom = 2.dp)
    )
}

@Composable
private fun NavItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit
) {
    val pal = LocalAP.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) accent.copy(alpha = 0.22f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (selected) accent else pal.sub,
            modifier = Modifier.size(22.dp)
        )
        Text(
            label,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) pal.ink else pal.sub,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun MiniNowCard(
    title: String,
    artist: String,
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    val pal = LocalAP.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(pal.track.copy(alpha = 0.6f))
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                title,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = pal.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                artist,
                fontSize = 11.sp,
                color = pal.sub,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        MiniBars(color = pal.sub, isPlaying = isPlaying)
    }
}
