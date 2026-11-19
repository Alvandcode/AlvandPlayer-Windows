package com.alvand.player.system

import java.util.concurrent.atomic.AtomicBoolean

/**
 * کلیدهای مدیا (دکمه‌های Play/Pause/Next/Prev روی کیبورد و هدست ویندوز).
 *
 * ## چرا RegisterHotKey و نه WM_APPCOMMAND
 *
 * ویندوز پیام `WM_APPCOMMAND` را فقط به پنجرهٔ foreground می‌فرستد؛ وقتی اپ
 * مینیمایز است یا فوکوس ندارد (حالت پخش موزیک!) هیچ پیامی نمی‌رسد.
 * `RegisterHotKey` با VK های مدیا (0xB0 تا 0xB3) رویداد را **سراسری** می‌گیرد و
 * `WM_HOTKEY` را به صف پیام نخ ثبت‌کننده می‌فرستد — پس حتی وقتی برنامه در سینی
 * است، دکمه‌های هدست کار می‌کنند.
 *
 * محدودیت صادقانه: اگر یک برنامهٔ دیگر (مثل خود Spotify) همان کلید را قبلاً
 * رزرو کرده باشد، `RegisterHotKey` خطای 1409 می‌دهد. اینجا فقط لاگ می‌شود و اپ
 * به کار عادی ادامه می‌دهد — کلیدهای درون‌برنامه‌ای همیشه کار می‌کنند.
 *
 * SMTC (نمایش در فول‌اوت ولوم ویندوز) به WinRT نیاز دارد و مسیر جداگانه‌ای است.
 *
 * منطق خالص نگاشت در [MediaKeyMap] است تا بدون ویندوز تست شود؛ فقط بُعد
 * ویندوزی (نخ پیام، JNA) اینجاست و تنها وقتی واقعاً روی ویندوز اجرا شود.
 */
object MediaKeys {

    /** شروع؛ فقط روی ویندوز کاری می‌کند و در بقیهٔ سیستم‌ها بی‌صدا نادیده می‌رود */
    fun start(onAction: (MediaKeyMap.Action) -> Unit): Boolean {
        if (!isWindows()) return false
        return MediaKeyWindow.start(onAction)
    }

    fun stop() {
        MediaKeyWindow.stop()
    }

    fun isWindows(): Boolean =
        System.getProperty("os.name", "").contains("windows", ignoreCase = true)
}

/** نگاشت VK مدیا به اکشن — خالص و قابل تست */
object MediaKeyMap {

    enum class Action { PLAY_PAUSE, STOP, NEXT, PREVIOUS }

    // مقادیر VK از سورس مایکروسافت (WinUser.h)
    const val VK_MEDIA_PLAY_PAUSE = 0xB3
    const val VK_MEDIA_STOP = 0xB2
    const val VK_MEDIA_NEXT_TRACK = 0xB0
    const val VK_MEDIA_PREV_TRACK = 0xB1

    const val WM_HOTKEY = 0x0312

    val allKeys: IntArray = intArrayOf(
        VK_MEDIA_PLAY_PAUSE,
        VK_MEDIA_STOP,
        VK_MEDIA_NEXT_TRACK,
        VK_MEDIA_PREV_TRACK,
    )

    fun actionFor(vk: Int): Action? = when (vk) {
        VK_MEDIA_PLAY_PAUSE -> Action.PLAY_PAUSE
        VK_MEDIA_STOP -> Action.STOP
        VK_MEDIA_NEXT_TRACK -> Action.NEXT
        VK_MEDIA_PREV_TRACK -> Action.PREVIOUS
        else -> null
    }

    fun vkFor(action: Action): Int = when (action) {
        Action.PLAY_PAUSE -> VK_MEDIA_PLAY_PAUSE
        Action.STOP -> VK_MEDIA_STOP
        Action.NEXT -> VK_MEDIA_NEXT_TRACK
        Action.PREVIOUS -> VK_MEDIA_PREV_TRACK
    }

    /** آیدی hotkey ما ترکیب سادهٔ اندیس است؛ هیچ رزرو جادویی لازم نیست */
    fun hotkeyIdOf(vk: Int): Int = vk - VK_MEDIA_NEXT_TRACK + 1

    /** معکوس [hotkeyIdOf] — wParam پیام WM_HOTKEY به VK برگردانده می‌شود */
    fun hotkeyIdToVk(id: Int): Int = VK_MEDIA_NEXT_TRACK + id - 1
}

/**
 * بُعد ویندوزی — فقط اینجا JNA لود می‌شود. بارگذاری ناموفق (مثلاً روی
 * لینوکس) هم به false ختم می‌شود، نه کرش.
 */
private object MediaKeyWindow {

    private val running = AtomicBoolean(false)

    @Volatile
    private var thread: Thread? = null

    fun start(onAction: (MediaKeyMap.Action) -> Unit): Boolean {
        if (!running.compareAndSet(false, true)) return true
        val t = Thread({ runLoop(onAction) }, "alvand-media-keys").apply {
            isDaemon = true
            start()
        }
        thread = t
        return true
    }

    fun stop() {
        if (!running.compareAndSet(true, false)) return
        thread?.interrupt()
        thread = null
    }

    private fun runLoop(onAction: (MediaKeyMap.Action) -> Unit) {
        val registered = runCatching { registerAll() }.getOrDefault(false)
        if (!registered) {
            running.set(false)
            return
        }
        try {
            val msg = com.sun.jna.platform.win32.WinUser.MSG()
            val user32 = com.sun.jna.platform.win32.User32.INSTANCE
            while (running.get()) {
                val result = user32.GetMessage(msg, null, 0, 0)
                if (result <= 0) break
                if (msg.message == MediaKeyMap.WM_HOTKEY) {
                    val vk = MediaKeyMap.hotkeyIdToVk(msg.wParam.toInt())
                    MediaKeyMap.actionFor(vk)?.let { onAction(it) }
                }
            }
        } finally {
            runCatching { unregisterAll() }
            running.set(false)
        }
    }

    private fun registerAll(): Boolean {
        val user32 = com.sun.jna.platform.win32.User32.INSTANCE
        var anyOk = false
        for (vk in MediaKeyMap.allKeys) {
            // hwnd=null یعنی پیام به صف پیام همین نخ می‌رسد — نه پنجرهٔ خاص
            val ok = user32.RegisterHotKey(null, MediaKeyMap.hotkeyIdOf(vk), 0, vk)
            if (ok) {
                anyOk = true
            } else {
                System.err.println("[media-keys] RegisterHotKey failed for VK=0x" +
                    Integer.toHexString(vk) + " (maybe owned by another app)")
            }
        }
        return anyOk
    }

    private fun unregisterAll() {
        val user32 = com.sun.jna.platform.win32.User32.INSTANCE
        for (vk in MediaKeyMap.allKeys) {
            user32.UnregisterHotKey(null, MediaKeyMap.hotkeyIdOf(vk))
        }
    }
}
