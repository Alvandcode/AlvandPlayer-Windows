package com.alvand.player.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.padding
import com.alvand.player.ui.theme.Spacing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alvand.player.i18n.LocalStrings
import com.alvand.player.model.Song
import com.alvand.player.ui.components.GlassCard
import com.alvand.player.ui.components.SelectChip
import com.alvand.player.ui.components.rememberArtwork
import com.alvand.player.ui.icons.Explore
import com.alvand.player.ui.icons.KeyboardArrowRight
import com.alvand.player.ui.icons.LibraryMusic
import com.alvand.player.ui.icons.PlayArrow
import com.alvand.player.ui.icons.Radio
import com.alvand.player.ui.icons.Settings
import com.alvand.player.ui.shell.Screen
import com.alvand.player.ui.theme.LocalAP
import java.time.LocalTime

/**
 * آفتاب کوچک کنارِ سلام — همان نشانِ هواشناسیِ کنار «Good Morning» در طرح.
 *
 * کوچک و ساده است، پس مستقیم کشیده می‌شود: یک دایرهٔ توپر و هشت پرتو.
 */
@Composable
private fun SunGlyph(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val r = size.minDimension * 0.30f
        val c = center
        drawCircle(Color.White, radius = r, center = c)
        val inner = size.minDimension * 0.40f
        val outer = size.minDimension * 0.48f
        for (i in 0 until 8) {
            val a = i * Math.PI / 4.0
            drawLine(
                color = Color.White,
                start = Offset(
                    c.x + (inner * kotlin.math.cos(a)).toFloat(),
                    c.y + (inner * kotlin.math.sin(a)).toFloat()
                ),
                end = Offset(
                    c.x + (outer * kotlin.math.cos(a)).toFloat(),
                    c.y + (outer * kotlin.math.sin(a)).toFloat()
                ),
                strokeWidth = size.minDimension * 0.09f,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        }
    }
}

/**
 * یک خط‌کوه ساده با قله‌های نامتقارن — برای منظرهٔ بنر.
 *
 * هر لایه با `seed` متفاوت کمی جابه‌جا می‌شود تا سه کوه روی هم طبیعی دیده شوند.
 */
private fun mountainPath(w: Float, h: Float, baseY: Float, peak: Float, seed: Int): Path {
    val path = Path()
    path.moveTo(0f, h)
    val steps = 6
    for (i in 0..steps) {
        val x = w * i / steps
        // قله‌ها یک‌درمیان بالا و پایین می‌روند و با seed جابه‌جا می‌شوند
        val wave = if (i % 2 == 0) peak else peak * 0.72f
        val jitter = ((i * 37 + seed * 13) % 9) / 100f
        val y = baseY - h * (wave + jitter) * 0.5f
        path.lineTo(x, y)
    }
    path.lineTo(w, h)
    path.close()
    return path
}

/**
 * صفحهٔ خانه — موبه‌مو از روی طرح: سلام زمانی، بنر هیرو با گرادیان، دو دکمه،
 * چهار کارت ویژگی، و «اخیراً پخش‌شده» از تاریخچهٔ واقعی دیتابیس.
 */
@Composable
fun HomeScreen(
    recentSongs: List<Song>,
    accent: Color,
    onPlayMix: () -> Unit,
    onBrowse: () -> Unit,
    onNavigate: (Screen) -> Unit,
    onPlaySong: (Song) -> Unit,
    onViewAllRecent: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pal = LocalAP.current
    val s = LocalStrings.current
    Column(
        modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.pageMargin, vertical = Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.xl)
    ) {
        HeroBanner(
            accent = accent,
            onPlayMix = onPlayMix,
            onBrowse = onBrowse
        )

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FeatureCard(
                title = s.cardLibraryTitle,
                subtitle = s.cardLibrarySub,
                icon = { Icon(Icons.Filled.LibraryMusic, null, tint = Color.White, modifier = Modifier.size(24.dp)) },
                gradient = listOf(Color(0xFF8B5CF6), Color(0xFF5B2FD6)),
                onClick = { onNavigate(Screen.LIBRARY) },
                modifier = Modifier.weight(1f)
            )
            FeatureCard(
                title = s.cardDiscoverTitle,
                subtitle = s.cardDiscoverSub,
                icon = { Icon(Icons.Filled.Explore, null, tint = Color.White, modifier = Modifier.size(24.dp)) },
                gradient = listOf(Color(0xFF14B8A6), Color(0xFF0E7490)),
                onClick = { onNavigate(Screen.DISCOVER) },
                modifier = Modifier.weight(1f)
            )
            FeatureCard(
                title = s.cardRadioTitle,
                subtitle = s.cardRadioSub,
                icon = { Icon(Icons.Filled.Radio, null, tint = Color.White, modifier = Modifier.size(24.dp)) },
                gradient = listOf(Color(0xFFF43F5E), Color(0xFFBE123C)),
                onClick = { onNavigate(Screen.RADIO) },
                modifier = Modifier.weight(1f)
            )
            FeatureCard(
                title = s.cardSettingsTitle,
                subtitle = s.cardSettingsSub,
                icon = { Icon(Icons.Filled.Settings, null, tint = Color.White, modifier = Modifier.size(24.dp)) },
                gradient = listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8)),
                onClick = { onNavigate(Screen.SETTINGS) },
                modifier = Modifier.weight(1f)
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                s.recentlyPlayed,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = pal.ink,
                modifier = Modifier.weight(1f)
            )
            SelectChip(s.viewAll, false, onClick = onViewAllRecent)
        }

        if (recentSongs.isEmpty()) {
            Text(s.noMusic, fontSize = 13.sp, color = pal.sub)
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(recentSongs, key = { it.id }) { song ->
                    RecentCard(song = song, onClick = { onPlaySong(song) })
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun HeroBanner(
    accent: Color,
    onPlayMix: () -> Unit,
    onBrowse: () -> Unit
) {
    val s = LocalStrings.current
    val hour = LocalTime.now().hour
    val greeting = when {
        hour < 12 -> s.greetingMorning
        hour < 18 -> s.greetingAfternoon
        else -> s.greetingEvening
    }
Box(
        Modifier
            .fillMaxWidth()
            .height(210.dp)
            .clip(RoundedCornerShape(Spacing.radiusXl))
    ) {
        // منظرهٔ کوهستانی غروب — همان تصویرِ بنر طرح. چون عکسی همراه برنامه نیست،
        // خودش کشیده می‌شود: آسمان گرادیانی، خورشید، و سه لایهٔ کوه.
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            drawRect(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF2B1055),
                        Color(0xFF7B2D8E),
                        Color(0xFFD4508A),
                        Color(0xFFF58A5B)
                    )
                )
            )
            // خورشید
            drawCircle(
                color = Color(0xFFFFD08A).copy(alpha = 0.95f),
                radius = h * 0.10f,
                center = Offset(w * 0.72f, h * 0.52f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color(0xFFFFD08A).copy(alpha = 0.45f), Color.Transparent),
                    center = Offset(w * 0.72f, h * 0.52f),
                    radius = h * 0.30f
                ),
                radius = h * 0.30f,
                center = Offset(w * 0.72f, h * 0.52f)
            )
            // سه لایهٔ کوه، از دور به نزدیک
            drawPath(
                path = mountainPath(w, h, baseY = h * 0.68f, peak = 0.42f, seed = 1),
                brush = Brush.verticalGradient(
                    listOf(Color(0xFF5B2A86), Color(0xFF3A1A5C))
                )
            )
            drawPath(
                path = mountainPath(w, h, baseY = h * 0.80f, peak = 0.56f, seed = 2),
                brush = Brush.verticalGradient(
                    listOf(Color(0xFF3B1A5E), Color(0xFF241040))
                )
            )
            drawPath(
                path = mountainPath(w, h, baseY = h * 0.96f, peak = 0.74f, seed = 3),
                brush = Brush.verticalGradient(
                    listOf(Color(0xFF1C0B33), Color(0xFF120722))
                )
            )
            // تیرگیِ ملایم برای خوانا ماندن متن
            drawRect(
                Brush.horizontalGradient(
                    listOf(
                        Color.Black.copy(alpha = 0.55f),
                        Color.Black.copy(alpha = 0.10f),
                        Color.Transparent
                    )
                )
            )
        }

        Column(
            Modifier
                .align(Alignment.CenterStart)
                .padding(horizontal = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // آفتاب کوچک کنارِ سلام — همان نشانِ هواشناسیِ طرح
                SunGlyph(Modifier.size(Spacing.xs * 4))
                Spacer(Modifier.width(Spacing.xs))
                Text(greeting, fontSize = 13.sp, color = Color.White)
            }
            Text(
                s.heroTitle,
                fontSize = 30.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
            Text(s.heroSubtitle, fontSize = 13.sp, color = Color.White.copy(alpha = 0.85f))
            Spacer(Modifier.height(Spacing.sm))
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Row(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(accent)
                        .clickable(onClick = onPlayMix)
                        .padding(horizontal = Spacing.md, vertical = Spacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    Icon(Icons.Filled.PlayArrow, null, tint = Color.White, modifier = Modifier.size(Spacing.md))
                    Text(s.playYourMix, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Row(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.16f))
                        .clickable(onClick = onBrowse)
                        .padding(horizontal = Spacing.md, vertical = Spacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    Icon(Icons.Filled.Search, null, tint = Color.White, modifier = Modifier.size(Spacing.md))
                    Text(s.browse, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun FeatureCard(
    title: String,
    subtitle: String,
    icon: @Composable () -> Unit,
    gradient: List<Color>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier
            .clip(RoundedCornerShape(Spacing.radiusMd))
            .background(Brush.linearGradient(gradient))
            .clickable(onClick = onClick)
            .padding(Spacing.md)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(Spacing.lg)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.22f)),
                    contentAlignment = Alignment.Center
                ) {
                    icon()
                }
                Spacer(Modifier.weight(1f))
                Icon(
                    Icons.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(Spacing.md)
                )
            }
            Text(
                title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                subtitle,
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun RecentCard(
    song: Song,
    onClick: () -> Unit
) {
    val pal = LocalAP.current
    val artwork by rememberArtwork(song.uri)
    Column(
        Modifier
            .width(Spacing.xxxl)
            .clip(RoundedCornerShape(Spacing.radiusMd))
            .clickable(onClick = onClick)
    ) {
        if (artwork != null) {
            Image(
                bitmap = artwork!!,
                contentDescription = song.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(Spacing.xxxl)
                    .clip(RoundedCornerShape(Spacing.radiusMd))
            )
        } else {
            Box(
                Modifier
                    .size(Spacing.xxxl)
                    .clip(RoundedCornerShape(Spacing.radiusMd))
                    .background(Brush.verticalGradient(listOf(pal.glass, pal.track))),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    song.title.take(1).uppercase().ifBlank { "♪" },
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Black,
                    color = pal.sub
                )
            }
        }
        Spacer(Modifier.height(Spacing.xs))
        Text(
            song.title,
            fontSize = 12.sp,
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
}
