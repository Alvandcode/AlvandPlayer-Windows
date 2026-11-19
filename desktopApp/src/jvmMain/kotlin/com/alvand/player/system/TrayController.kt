package com.alvand.player.system

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.awt.MenuItem
import java.awt.PopupMenu
import java.awt.SystemTray
import java.awt.TrayIcon as AwtTrayIcon

/**
 * آیکون نوار وظیفه (System Tray).
 *
 * معادل ویندوزی ویجت صفحهٔ اصلی اندروید: همیشه حاضر است، عنوان آهنگ را به‌عنوان
 * Tooltip نشان می‌دهد و سه کنش اصلی (قبلی/پخش/بعدی) را در اختیار می‌گذارد.
 *
 * اگر سیستم‌عامل اجازهٔ نوار وظیفه ندهد (یا در محیط بدون میزکار اجرا شود)،
 * بی‌سروصدا از کار می‌افتد و اپ بدون مشکل بالا می‌آید.
 *
 * نام کلاس عمداً `TrayController` است نه `TrayIcon` تا با `java.awt.TrayIcon`
 * قاطی نشود.
 */
class TrayController(
    private val appName: String,
    private val onShow: () -> Unit,
    private val onTogglePlay: () -> Unit,
    private val onNext: () -> Unit,
    private val onPrevious: () -> Unit,
    private val onQuit: () -> Unit
) {

    private var trayIcon: AwtTrayIcon? = null
    private var menu: PopupMenu? = null

    private val _trackTitle = MutableStateFlow("")
    val trackTitle: StateFlow<String> = _trackTitle.asStateFlow()

    fun install() {
        if (!SystemTray.isSupported()) return
        val tray = SystemTray.getSystemTray() ?: return
        val image = AppIcons.awt(64) ?: return

        val popup = PopupMenu().apply {
            add(MenuItem("Show / Hide — $appName").apply { addActionListener { onShow() } })
            addSeparator()
            add(MenuItem("Previous").apply { addActionListener { onPrevious() } })
            add(MenuItem("Play / Pause").apply { addActionListener { onTogglePlay() } })
            add(MenuItem("Next").apply { addActionListener { onNext() } })
            addSeparator()
            add(MenuItem("Quit").apply { addActionListener { onQuit() } })
        }

        runCatching {
            // در JDK 9+ دیگر متد setContextMenu وجود ندارد؛ منو در سازنده داده می‌شود
            val icon = AwtTrayIcon(image, appName, popup).apply {
                isImageAutoSize = true
                addActionListener { onShow() }
            }
            tray.add(icon)
            trayIcon = icon
            menu = popup
        }.onFailure {
            System.err.println("[tray] install failed: ${it.message}")
        }
    }

    fun setPlaying(isPlaying: Boolean) {
        val popup = menu ?: return
        popup.getItem(3)?.label = if (isPlaying) "Pause" else "Play"
    }

    fun setTrack(title: String, artist: String) {
        _trackTitle.value = if (artist.isBlank()) title else "$title — $artist"
        val tip = _trackTitle.value.ifBlank { appName }
        runCatching { trayIcon?.toolTip = tip }
    }

    fun remove() {
        runCatching { trayIcon?.let { SystemTray.getSystemTray().remove(it) } }
        trayIcon = null
        menu = null
    }
}
