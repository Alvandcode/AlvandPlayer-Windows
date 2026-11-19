package com.alvand.player.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.alvand.player.ui.theme.LocalAP

/**
 * چیپ انتخاب.
 *
 * // windows-only: در اندروید از `FilterChip` متریال ۳ استفاده شده که در نسخهٔ
 * دسکتاپ هنوز experimental است. این نسخه همان ظاهر را با پالت اپ می‌سازد تا
 * بعداً بشود جایش را عوض کرد بدون تغییر ظاهری.
 */
@Composable
fun SelectChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    dotColor: Color? = null,
    onClick: () -> Unit
) {
    val pal = LocalAP.current
    val shape = RoundedCornerShape(50)

    Row(
        modifier = modifier
            .clip(shape)
            .background(if (selected) pal.ink else pal.glass)
            .border(1.dp, if (selected) pal.ink else pal.glassBorder, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (dotColor != null) {
            androidx.compose.foundation.layout.Box(
                Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
        }
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) pal.bg else pal.ink
        )
    }
}
