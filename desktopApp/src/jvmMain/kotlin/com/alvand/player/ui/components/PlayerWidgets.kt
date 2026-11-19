package com.alvand.player.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/**
 * میله‌های اکولایزر کوچک — نشانگر «در حال پخش».
 *
 * پورت ۱:۱ از `MiniBars` در `Glass.kt` نسخهٔ اندروید: ۴ میله، عرض ‎3.5dp‎،
 * فاصلهٔ ‎2.5dp‎، ارتفاع ساکن ‎4dp‎ و انیمیشن بی‌نهایت بین ‎4..8+dp‎.
 */
@Composable
fun MiniBars(
    color: Color,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    barCount: Int = 4
) {
    val transition = rememberInfiniteTransition(label = "miniBars")

    // Row لازم است، نه Box: با Box میله‌ها روی هم می‌افتند و به‌جای نوارهای کنار هم
    // یک نقطه دیده می‌شود.
    Row(
        modifier.height(16.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(2.5.dp)
    ) {
        repeat(barCount) { i ->
            val target = 8f + (i * 7 % 10)
            val heightDp by transition.animateFloat(
                initialValue = 4f,
                targetValue = if (isPlaying) target else 4f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 350 + (i * 53 % 300),
                        easing = LinearEasing
                    ),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "bar$i"
            )
            Box(
                Modifier
                    .width(3.5.dp)
                    .height(heightDp.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(color)
            )
        }
    }
}