package com.alvand.player.system

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.TimeUnit

/**
 * تشخیص حالت روشن/تیرهٔ خود ویندوز.
 *
 * منبع اصلی: کلید رجیستری `AppsUseLightTheme`
 * (`HKCU\Software\Microsoft\Windows\CurrentVersion\Themes\Personalize`) —
 * همان چیزی که خود تنظیمات ویندوز از آن استفاده می‌کند.
 *
 * دستور `reg query` کاملاً ثابت است و هیچ ورودی کاربر در آن جای‌گذاری نمی‌شود،
 * پس از نظر امنیتی مشکلی ندارد. اگر به هر دلیلی در دسترس نبود، مقدار پیش‌فرض
 * روشن است (مثل حالتی که کاربر تم ویندوز را دستی عوض کرده).
 */
object SystemTheme {

    private const val REG_PATH = "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize"
    private const val REG_VALUE = "AppsUseLightTheme"

    private val _isSystemDark = MutableStateFlow(querySystemDark())
    val isSystemDark: StateFlow<Boolean> = _isSystemDark.asStateFlow()

    /** دوباره‌خوانی (وقتی کاربر تم ویندوز را عوض کرد) */
    fun refresh() {
        _isSystemDark.value = querySystemDark()
    }

    private fun querySystemDark(): Boolean {
        val out = runCatching {
            val p = ProcessBuilder("reg", "query", REG_PATH, "/v", REG_VALUE)
                .redirectErrorStream(true)
                .start()
            val text = p.inputStream.bufferedReader().readText()
            p.waitFor(3, TimeUnit.SECONDS)
            text
        }.getOrNull() ?: return false

        // خروجی شبیه: "    AppsUseLightTheme    REG_DWORD    0x0"
        val hex = out.trim().split(Regex("\\s+")).lastOrNull() ?: return false
        val value = hex.removePrefix("0x").removePrefix("0X").toIntOrNull(16) ?: return false
        return value == 0
    }
}
