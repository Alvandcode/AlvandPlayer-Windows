package com.alvand.player.system

import java.awt.BorderLayout
import java.awt.Color
import java.awt.ComponentOrientation
import java.awt.Font
import java.awt.GraphicsEnvironment
import java.awt.Image
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.ImageIcon
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JProgressBar
import javax.swing.JWindow
import javax.swing.SwingConstants
import javax.swing.SwingUtilities
import javax.swing.Timer
import javax.swing.border.EmptyBorder

/**
 * اسپلش سبک Swing که بلافاصله بعد از اجرای پروسه دیده می‌شود.
 *
 * اولین فریم Compose چند ثانیه طول می‌کشد (بارگذاری skiko و ساخت اولین صفحه)،
 * ولی در همین فاصله کاربر آیکون را در تسک‌بار می‌بیند و فکر می‌کند برنامه
 * هنگ کرده. این پنجرهٔ کوچکِ بدون کادر همان لحظه «الان باز می‌شود» را نشان
 * می‌دهد و به‌محض نمایش پنجرهٔ اصلی dispose می‌شود.
 *
 * هر خطایی در اسپلش هرگز نباید برنامه را از کار بیندازد — فقط null برمی‌گردد.
 *
 * @return کلوئرِ بستن اسپلش، یا null اگر ساخت آن ممکن نشد
 */
fun showSplash(appName: String): AutoCloseable? = try {
    var window: JWindow? = null
    SwingUtilities.invokeAndWait {
        window = JWindow().apply {
            contentPane.add(buildSplashContent(appName))
            setSize(340, 216)
            val scr = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .defaultScreenDevice.defaultConfiguration.bounds
            setLocation(
                scr.x + (scr.width - width) / 2,
                scr.y + (scr.height - height) / 2
            )
            isAlwaysOnTop = true
            toFront()
            isVisible = true
        }
    }
    // اطمینان: اگر پنجرهٔ اصلی هرگز باز نشد، اسپلش بعد از ۳۰ ثانیه خودش می‌رود
    val guard = Timer(30_000) { window?.dispose() }
    guard.isRepeats = false
    guard.start()
    AutoCloseable {
        SwingUtilities.invokeLater {
            guard.stop()
            window?.dispose()
        }
    }
} catch (t: Throwable) {
    null
}

private val SPLASH_BG = Color(0x14, 0x14, 0x1B)
private val SPLASH_INK = Color(0xF2, 0xF2, 0xF7)
private val SPLASH_SUB = Color(0x9B, 0x9B, 0xA8)

private fun buildSplashContent(appName: String): JPanel = JPanel(BorderLayout()).apply {
    background = SPLASH_BG
    componentOrientation = ComponentOrientation.RIGHT_TO_LEFT
    border = BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(SPLASH_BG.darker(), 1),
        EmptyBorder(20, 24, 18, 24)
    )

    val icon = AppIcons.awt(64)?.let {
        ImageIcon(it.getScaledInstance(72, 72, Image.SCALE_SMOOTH))
    }

    add(
        Box.createVerticalBox().apply {
            add(Box.createVerticalGlue())
            add(JLabel(icon, SwingConstants.CENTER).apply { alignmentX = 0.5f })
            add(
                JLabel(appName, SwingConstants.CENTER).apply {
                    alignmentX = 0.5f
                    font = Font("Segoe UI", Font.BOLD, 19)
                    foreground = SPLASH_INK
                    border = EmptyBorder(12, 0, 3, 0)
                }
            )
            add(
                JLabel("در حال باز شدن…", SwingConstants.CENTER).apply {
                    alignmentX = 0.5f
                    font = Font("Segoe UI", Font.PLAIN, 12)
                    foreground = SPLASH_SUB
                    border = EmptyBorder(0, 0, 14, 0)
                }
            )
            add(Box.createVerticalGlue())
        },
        BorderLayout.CENTER
    )

    // نوار پیشرفت نامعین با رنگ accent — فقط برای حس «فعال بودن»
    val accent = Color(0x8B, 0x7C, 0xF6)
    add(
        JProgressBar().apply {
            isIndeterminate = true
            foreground = accent
            background = SPLASH_BG
            border = BorderFactory.createEmptyBorder()
            preferredSize = java.awt.Dimension(292, 5)
        },
        BorderLayout.SOUTH
    )
}
