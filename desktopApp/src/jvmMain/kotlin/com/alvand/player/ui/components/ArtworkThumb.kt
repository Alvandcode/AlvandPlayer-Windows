package com.alvand.player.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alvand.player.ui.theme.LocalAP

/**
 * کاورِ آهنگ با جانشینِ «حرف اول عنوان».
 *
 * همهٔ صفحه‌ها از همین یکی استفاده می‌کنند تا کارت‌ها همه‌جا یک‌شکل باشند: اگر فایل
 * کاور داشته باشد خودش نشان داده می‌شود، وگرنه حرف اول عنوان روی گرادیانی از پالت.
 */
@Composable
fun ArtworkThumb(
    uri: String?,
    fallbackTitle: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    shape: Shape = RoundedCornerShape(8.dp),
    accent: Color? = null,
    placeholderTextSize: TextUnit = (size.value / 2.6f).sp,
    overlay: @Composable (State<ImageBitmap?>) -> Unit = {}
) {
    val artwork by rememberArtwork(uri)
    val pal = LocalAP.current

    Box(
        modifier
            .size(size)
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(
                        (accent ?: pal.track),
                        if (accent != null) accent.copy(alpha = 0.55f) else pal.bg
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        if (artwork != null) {
            Image(
                bitmap = artwork!!,
                contentDescription = fallbackTitle,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(size)
            )
        } else {
            Text(
                text = fallbackTitle.trim().take(1).uppercase().ifBlank { "♪" },
                fontSize = placeholderTextSize,
                fontWeight = FontWeight.Black,
                color = if (accent != null) Color.White.copy(alpha = 0.85f) else pal.sub
            )
        }
        overlay(rememberArtworkState(artwork))
    }
}

@Composable
private fun rememberArtworkState(bitmap: ImageBitmap?): State<ImageBitmap?> =
    androidx.compose.runtime.rememberUpdatedState(bitmap)

/** آواتار دایره‌ای با حرف اول نام — مثل طرح: گرادیانِ رنگی و متن سفید. */
@Composable
fun LetterAvatar(
    letter: String,
    size: Dp,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(accent, accent.copy(alpha = 0.55f))))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Text(
            letter.trim().take(1).uppercase().ifBlank { "A" },
            fontSize = (size.value / 2.1f).sp,
            fontWeight = FontWeight.Black,
            color = Color.White
        )
    }
}