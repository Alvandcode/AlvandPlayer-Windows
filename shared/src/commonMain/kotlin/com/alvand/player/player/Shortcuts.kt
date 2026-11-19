package com.alvand.player.player

/**
 * نگاشت کیبورد به اکشن — منطق خالص و قابل تست.
 *
 * کلیدها از ایده‌های رایج پلیرها گرفته شده (Space/Arrows/S/R/M/...) ولی
 * جدول و اولویت‌ها مال الوند است و عمداً با رفتار اندروید نمی‌جنگد:
 *  - وقتی کاربر در فیلد جستجو تایپ می‌کند، هیچ شورت‌کاتی نباید آهنگ عوض کند
 *    (تصمیم در لایهٔ UI با `isTyping` گرفته می‌شود، اینجا فقط نگاشت است).
 *  - Space روی دکمهٔ فوکوس‌شده نباید دوبار عمل کند — UI باید آن را قبل از
 *    رسیدن به اینجا مصرف کند.
 */
enum class ShortcutAction {
    TOGGLE_PLAY,
    NEXT,
    PREVIOUS,
    SEEK_BACK,
    SEEK_FORWARD,
    VOLUME_UP,
    VOLUME_DOWN,
    TOGGLE_FAVORITE,
    TOGGLE_SHUFFLE,
    CYCLE_REPEAT,
    TOGGLE_MUTE,
    OPEN_LIBRARY_SEARCH,
    TOGGLE_NOW_PLAYING,
    SHOW_QUEUE,
    OPEN_MINI_MODE,
    SHOW_SHORTCUTS,
    CYCLE_SLEEP_TIMER,
    OPEN_EQUALIZER,
    SHOW_PROPERTIES,
    JUMP_TO_TIME,
    CYCLE_AB_LOOP,
    TOGGLE_VISUALIZER,
    NONE,
}

object Shortcuts {

    /**
     * @param keyLower نام کلید به حروف کوچک (`" "`, `"arrowleft"`, `"m"`, ...).
     * @param ctrl آیا Ctrl پایین است.
     * @param isTyping آیا فوکوس در فیلد متنی است — اگر true فقط Ctrl+K و Esc مجازند.
     */
    fun resolve(keyLower: String, ctrl: Boolean = false, isTyping: Boolean = false): ShortcutAction {
        if (isTyping) {
            if (ctrl && keyLower == "k") return ShortcutAction.OPEN_LIBRARY_SEARCH
            return ShortcutAction.NONE
        }
        if (ctrl && keyLower == "k") return ShortcutAction.OPEN_LIBRARY_SEARCH
        if (ctrl && keyLower == "arrowleft") return ShortcutAction.SEEK_BACK
        if (ctrl && keyLower == "arrowright") return ShortcutAction.SEEK_FORWARD
        return when (keyLower) {
            " ", "spacebar" -> ShortcutAction.TOGGLE_PLAY
            // NOTE: arrowleft به‌تنهایی «قبلی» است؛ seek با Ctrl+Arrow است.
            "arrowleft" -> ShortcutAction.PREVIOUS
            "arrowright" -> ShortcutAction.NEXT
            "arrowup" -> ShortcutAction.VOLUME_UP
            "arrowdown" -> ShortcutAction.VOLUME_DOWN
            "l" -> ShortcutAction.TOGGLE_FAVORITE
            "s" -> ShortcutAction.TOGGLE_SHUFFLE
            "r" -> ShortcutAction.CYCLE_REPEAT
            "m" -> ShortcutAction.TOGGLE_MUTE
            "n" -> ShortcutAction.TOGGLE_NOW_PLAYING
            "q" -> ShortcutAction.SHOW_QUEUE
            "p" -> ShortcutAction.OPEN_MINI_MODE
            "?" -> ShortcutAction.SHOW_SHORTCUTS
            "t" -> ShortcutAction.CYCLE_SLEEP_TIMER
            "e" -> ShortcutAction.OPEN_EQUALIZER
            "i" -> ShortcutAction.SHOW_PROPERTIES
            "j" -> ShortcutAction.JUMP_TO_TIME
            "b" -> ShortcutAction.CYCLE_AB_LOOP
            "v" -> ShortcutAction.TOGGLE_VISUALIZER
            else -> ShortcutAction.NONE
        }
    }

    /** گام seek با Ctrl+Arrow — همان ۵ ثانیهٔ استاندارد */
    const val SEEK_STEP_MS = 5_000L
}
