package com.alvand.player.system

/**
 * بارگذاری آیکون‌ها از resources.
 *
 * PNG برای آیکون پنجره و نوار وظیفه، ICO فقط برای بسته‌بندی (jpackage/WiX).
 * اگر آیکونی نبود، `null` برمی‌گردانیم و UI آیکون پیش‌فرض JVM را می‌گیرد —
 * یعنی نبودِ آیکون هیچ‌وقت باعث کرش نمی‌شود.
 */
object AppIcons {

    /** بایت‌های خام PNG در اندازهٔ خواسته‌شده */
    fun pngBytes(size: Int): ByteArray? =
        AppIcons::class.java.getResourceAsStream("/icons/alvand-$size.png")
            ?.use { it.readBytes() }

    /** تصویر AWT برای نوار وظیفه و System Tray */
    fun awt(size: Int = 64): java.awt.Image? {
        val bytes = pngBytes(size) ?: return null
        return runCatching { javax.imageio.ImageIO.read(java.io.ByteArrayInputStream(bytes)) }
            .getOrNull()
    }
}
