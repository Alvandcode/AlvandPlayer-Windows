package com.alvand.player.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alvand.player.i18n.LocalStrings
import com.alvand.player.ui.components.formatTime
import com.alvand.player.ui.theme.LocalAP

/**
 * دیالوگ پرش به زمان — معادل Ctrl+T در VLC.
 *
 * ورودی `mm:ss` یا ثانیهٔ خالص می‌پذیرد؛ خارج از بازه به انتهای آهنگ clamp
 * می‌شود (همان قانون `seekTo` موتور).
 */
@Composable
fun JumpToTimeDialog(
    durationMs: Long,
    onGo: (Long) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pal = LocalAP.current
    val s = LocalStrings.current
    var text by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    fun submit() {
        val ms = parseJumpInput(text)
        if (ms == null) {
            error = true
            return
        }
        error = false
        onGo(ms)
        onClose()
    }

    Box(
        modifier.fillMaxSize().background(pal.scrim.copy(alpha = 0.55f)),
        contentAlignment = Alignment.Center
    ) {
        GlassCard(Modifier.widthIn(max = 380.dp).fillMaxWidth(0.9f)) {
            Column(
                Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        s.jumpTitle,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = pal.ink,
                        modifier = Modifier.weight(1f)
                    )
                    SelectChip(s.close, false, onClick = onClose)
                }
                Text(
                    formatTime(durationMs),
                    fontSize = 12.sp,
                    color = pal.sub
                )
                TextField(
                    value = text,
                    onValueChange = { text = it; error = false },
                    singleLine = true,
                    placeholder = { Text(s.jumpHint, color = pal.sub) },
                    isError = error,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Go
                    ),
                    keyboardActions = KeyboardActions(onGo = { submit() }),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = pal.glass,
                        unfocusedContainerColor = pal.glass,
                        disabledContainerColor = pal.glass,
                        focusedIndicatorColor = pal.line,
                        unfocusedIndicatorColor = pal.line,
                        cursorColor = pal.ink,
                        focusedTextColor = pal.ink,
                        unfocusedTextColor = pal.ink
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SelectChip(s.jumpGo, true, onClick = ::submit)
                }
            }
        }
    }
}

/**
 * پارس ورودی پرش: `m:ss` یا ثانیهٔ خالص. نامعتبر → null.
 * خروجی میلی‌ثانیه است؛ clamp با موتور است نه اینجا.
 */
fun parseJumpInput(text: String): Long? {
    val t = text.trim()
    if (t.isEmpty()) return null
    if (t.contains(":")) {
        val parts = t.split(":")
        if (parts.size != 2) return null
        val minutes = parts[0].trim().toLongOrNull() ?: return null
        val seconds = parts[1].trim().toLongOrNull() ?: return null
        if (minutes < 0 || seconds < 0 || seconds >= 60) return null
        return minutes * 60_000L + seconds * 1000L
    }
    val seconds = t.toLongOrNull() ?: return null
    if (seconds < 0) return null
    return seconds * 1000L
}
