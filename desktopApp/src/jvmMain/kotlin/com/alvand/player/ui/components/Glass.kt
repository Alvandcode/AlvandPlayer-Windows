package com.alvand.player.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.alvand.player.ui.theme.LocalAP

/**
 * کارت «لیکویید گلس»: سطح نیمه‌شفاف + برق لبه + هایلایت بالا + سایه نرم.
 * روی هر بکگراندی (گرادیان، عکس، accent) شیشه‌ای دیده می‌شود.
 *
 * پورت ۱:۱ از `GlassDesign.kt` نسخهٔ اندروید v1.6.9.
 *
 * نکتهٔ مهم: هالهٔ بالا با `matchParentSize()` کشیده می‌شود، نه با `height` —
 * وگرنه خودش ارتفاع کارت را تحمیل می‌کند. خودِ گرادیان هم بر حسب **پیکسل**
 * تعریف شده (`endY = 320f`) نه dp، دقیقاً مثل اندروید.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val pal = LocalAP.current
    Surface(
        modifier = modifier.then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = shape,
        color = pal.glass,
        border = BorderStroke(1.dp, pal.glassBorder),
        shadowElevation = 10.dp
    ) {
        Box {
            // برق شیشه از بالا
            Box(
                Modifier.matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(pal.sheen, Color.Transparent.copy(alpha = 0f)),
                            startY = 0f,
                            endY = 320f
                        )
                    )
            )
            content()
        }
    }
}
