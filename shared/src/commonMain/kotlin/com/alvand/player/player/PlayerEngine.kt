package com.alvand.player.player

import com.alvand.player.audio.DspSpec

/**
 * رابط موتور پخش — تنها نقطهٔ تماس UI با صدا.
 *
 * قوانینی که هر پیاده‌سازی باید رعایت کند (از §۱۱.۱ پرامپت):
 *
 *  - **هیچ متدی نباید Exception پرتاب کند.** خطا فقط از راه `state.error` گزارش می‌شود.
 *  - همهٔ متدها از هر نخی قابل فراخوانی‌اند.
 *  - `previous()`: اگر موقعیت > ۳ ثانیه بود، اول به ابتدای آهنگ می‌پرد؛ وگرنه آهنگ قبلی.
 *  - `next()` در انتهای صف با تکرار خاموش: **هیچ کاری نمی‌کند** (نه توقف، نه چرخش).
 *  - استریم زنده مدت‌زمان ندارد و کشیدن روی آن کار نمی‌کند.
 *  - تنظیمات DSP باید روی شروع پخش، تعویض دستگاه خروجی، تغییر نرخ نمونه‌برداری و
 *    ری‌استارت موتور دوباره اعمال شوند.
 */
interface PlayerEngine {

    val state: kotlinx.coroutines.flow.StateFlow<EngineState>

    suspend fun setQueue(items: List<PlayableItem>, startIndex: Int, autoPlay: Boolean)
    suspend fun play()
    suspend fun pause()
    suspend fun toggle()
    suspend fun next()
    suspend fun previous()
    suspend fun seekTo(ms: Long)
    suspend fun setVolume(v: Float)

    fun setShuffle(enabled: Boolean)
    fun cycleRepeat()
    /** جابه‌جایی آیتم در صف — برای مرتب‌سازی دستی صف توسط کاربر */
    fun moveInQueue(from: Int, to: Int)
    /** افزودن یک آهنگ به انتهای صف (بدون قطع پخش جاری) */
    fun addToQueue(item: PlayableItem)
    /** حذف یک آیتم از صف؛ اگر آهنگ در حال پخش باشد، پخش به آیتم بعدی می‌رود */
    fun removeFromQueue(index: Int)
    /** پاک کردن کامل صف و توقف پخش */
    fun clearQueue()
    fun setDsp(spec: DspSpec)
    fun release()
}

/** قانون سرریز صف (overflow) — همان رفتار «آخرین‌برنده» در پرامپت §۱۱.۱ */
data class PendingQueue(
    val items: List<PlayableItem>,
    val startIndex: Int,
    val autoPlay: Boolean
) {
    fun isValid(): Boolean = items.isNotEmpty() && startIndex in items.indices
}
