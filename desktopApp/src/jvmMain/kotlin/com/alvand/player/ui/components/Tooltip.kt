package com.alvand.player.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.alvand.player.ui.theme.LocalAP
import kotlinx.coroutines.delay

/**
 * تولتیپ دسکتاپی مینیمال — بعد از ~۵۰۰ میلی‌ثانیه هاور، متن راهنما ظاهر می‌شود.
 *
 * روی [content] بدون تغییر رفتاری پیچیده می‌شود؛ فقط hover را رصد می‌کند.
 * در حالت RTL هم Popup خودش را با چیدمان ویندوز هم‌راستا می‌کند.
 */
@Composable
fun HintTooltip(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    val hoverSource = remember { MutableInteractionSource() }
    val hovered by hoverSource.collectIsHoveredAsState()
    var show by remember { mutableStateOf(false) }

    LaunchedEffect(hovered, enabled) {
        if (hovered && enabled) {
            delay(500)
            show = true
        } else {
            show = false
        }
    }

    Box(modifier.hoverable(hoverSource, enabled = enabled)) {
        content()
        if (show) {
            Popup(
                alignment = androidx.compose.ui.Alignment.TopCenter,
                properties = PopupProperties(clippingEnabled = false, focusable = false)
            ) {
                val pal = LocalAP.current
                Box(
                    Modifier
                        .padding(top = 26.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (pal.isDark) Color(0xFF2A2A33) else Color(0xFF1C1C22))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text,
                        fontSize = 11.sp,
                        color = Color.White,
                        maxLines = 2
                    )
                }
            }
        }
    }
}
