package com.alvand.player.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alvand.player.i18n.LocalStrings
import com.alvand.player.library.LibraryFilter
import com.alvand.player.model.Song
import com.alvand.player.ui.components.ArtworkThumb
import com.alvand.player.ui.components.rememberArtwork
import com.alvand.player.ui.icons.Radio
import com.alvand.player.ui.theme.LocalAP

/**
 * صفحهٔ رادیو — موبه‌مو از روی طرح: تیتر، پیام صادقانه دربارهٔ نبودِ استریم، و
 * «ایستگاه‌هایی» که **واقعی** هستند: هر خوانندهٔ کتابخانهٔ خودِ کاربر یک ایستگاه
 * است (با کلیک، همهٔ آهنگ‌های او پخش می‌شود).
 *
 * چرا این‌طور؟ رادیوی اینترنتی به سرویس آنلاین نیاز دارد و برنامه عمداً هیچ سرویس
 * ابری ندارد؛ پس به‌جای ساختن ایستگاه‌های ساختگی، همان چیزی را نشان می‌دهیم که
 * واقعاً کار می‌کند.
 */
@Composable
fun RadioPage(
    songs: List<Song>,
    onPlaySongs: (List<Song>) -> Unit,
    modifier: Modifier = Modifier
) {
    val s = LocalStrings.current
    val pal = LocalAP.current
    val artists = remember(songs) {
        LibraryFilter.groupByArtist(songs).sortedByDescending { it.songs.size }
    }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            s.navRadio,
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            color = pal.ink
        )

        RadioNotice(text = s.radioEmpty)

        if (artists.isNotEmpty()) {
            Text(
                s.cardRadioSub,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = pal.ink
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(artists, key = { it.name }) { group ->
                    StationCard(
                        artist = group.name,
                        count = group.songs.size,
                        artworkUri = group.songs.firstOrNull()?.uri,
                        onClick = { onPlaySongs(group.songs) }
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun RadioNotice(text: String) {
    val pal = LocalAP.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(pal.glass)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.linearGradient(listOf(Color(0xFFF43F5E), Color(0xFFBE123C)))
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.Radio,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
        Text(
            text,
            fontSize = 13.sp,
            color = pal.sub,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StationCard(
    artist: String,
    count: Int,
    artworkUri: String?,
    onClick: () -> Unit
) {
    val pal = LocalAP.current
    val gradient = gradientFor(artist)
    Column(
        Modifier
            .width(160.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.linearGradient(gradient)),
            contentAlignment = Alignment.Center
        ) {
            val artwork by rememberArtwork(artworkUri)
            if (artwork != null) {
                Image(
                    bitmap = artwork!!,
                    contentDescription = artist,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.28f))
                )
            }
            Text(
                artist,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(10.dp)
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            artist,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = pal.ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            "$count",
            fontSize = 11.sp,
            color = pal.sub
        )
    }
}