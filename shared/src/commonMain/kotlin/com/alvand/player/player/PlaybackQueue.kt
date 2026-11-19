package com.alvand.player.player

/**
 * منطق صف پخش — کاملاً خالص و قابل تست، بدون هیچ وابستگی به موتور صدا.
 *
 * اینجا همان قواعدی زندگی می‌کنند که در نسخهٔ اندروید داخل `MusicPlayerManager`
 * پخش می‌شد و تست نداشت. جدا کردنشان یعنی می‌شود لبه‌هایشان را واقعاً تست کرد.
 */
class PlaybackQueue(
    private var items: List<PlayableItem> = emptyList(),
    private var index: Int = -1,
    shuffleEnabled: Boolean = false,
    repeatMode: RepeatMode = RepeatMode.OFF
) {

    var shuffle: Boolean = shuffleEnabled
        private set

    var repeat: RepeatMode = repeatMode
        private set

    /** ترتیب پخشِ واقعی وقتی شافل روشن است (اندیس‌های صف اصلی) */
    private var order: List<Int> = items.indices.toList()

    val size: Int get() = items.size
    val currentIndex: Int get() = index
    fun items(): List<PlayableItem> = items
    fun isEmpty(): Boolean = items.isEmpty()

    fun current(): PlayableItem? = items.getOrNull(index)

    fun setQueue(newItems: List<PlayableItem>, startIndex: Int) {
        items = newItems
        index = if (newItems.isEmpty()) -1 else startIndex.coerceIn(0, newItems.lastIndex)
        order = items.indices.toList()
        if (shuffle) order = order.shuffled()
    }

    fun setShuffle(enabled: Boolean) {
        if (shuffle == enabled) return
        shuffle = enabled
        val currentId = current()?.id
        if (enabled) {
            // آهنگ فعلی نباید وسط صف بپرد — کاربر گیج می‌شود
            order = items.indices.filter { items[it].id != currentId }.shuffled()
            if (currentId != null && index >= 0) order = listOf(index) + order
        } else {
            order = items.indices.toList()
        }
    }

    fun cycleRepeat(): RepeatMode {
        repeat = repeat.next()
        return repeat
    }

    /**
     * تنظیم مستقیم حالت تکرار.
     *
     * فقط برای تست و بازیابی وضعیت ذخیره‌شده؛ در برنامه از [cycleRepeat] استفاده می‌شود
     * تا کاربر همان چرخهٔ OFF → ALL → ONE را ببیند.
     */
    fun setRepeat(mode: RepeatMode) {
        repeat = mode
    }

    /**
     * آهنگ بعدی.
     *
     * @return اندیس جدید، یا ‎-1‎ اگر نباید حرکت کنیم
     */
    fun nextIndex(): Int {
        if (items.isEmpty()) return -1
        if (repeat == RepeatMode.ONE) return index
        if (shuffle) {
            val pos = order.indexOf(index)
            return when {
                pos < 0 -> order.firstOrNull() ?: -1
                pos == order.lastIndex -> if (repeat == RepeatMode.ALL) order.first() else -1
                else -> order[pos + 1]
            }
        }
        return when {
            index < items.lastIndex -> index + 1
            repeat == RepeatMode.ALL -> 0
            // قاعدهٔ اصلی: در انتهای صف با تکرار خاموش، هیچ اتفاقی نمی‌افتد
            else -> -1
        }
    }

    /**
     * آهنگ قبلی.
     *
     * قانون نسخهٔ اندروید: اگر بیش از ۳ ثانیه گذشته، اول به ابتدای همین آهنگ برگرد؛
     * وگرنه یکی عقب برو. اگر در ابتدای صف بودیم، فقط به ابتدای آهنگ برگرد.
     */
    fun previousIndex(positionMs: Long): Int {
        if (items.isEmpty()) return -1
        if (positionMs > PREVIOUS_RESTART_THRESHOLD_MS) return index

        return if (shuffle) {
            val pos = order.indexOf(index)
            when {
                pos < 0 -> index
                pos == 0 -> if (repeat == RepeatMode.ALL) order.last() else index
                else -> order[pos - 1]
            }
        } else {
            when {
                index > 0 -> index - 1
                repeat == RepeatMode.ALL -> items.lastIndex
                else -> index
            }
        }
    }

    /** جابه‌جایی به اندیس مشخص، با احترام به حالت تکرارِ یک‌بار */
    fun moveTo(target: Int): Int {
        if (items.isEmpty()) return -1
        if (target !in items.indices) return index
        index = target
        return index
    }

    /**
     * جابه‌جایی یک آیتم در صف (مرتب‌سازی دستی صف توسط کاربر).
     * اندیس جاری با جابه‌جایی درست به‌روز می‌شود تا آهنگ در حال پخش عوض نشود.
     * @return اندیس آیتم جابه‌جا‌شده یا -1 اگر ورودی نامعتبر بود
     */
    fun move(from: Int, to: Int): Int {
        if (items.isEmpty()) return -1
        if (from !in items.indices || to !in items.indices) return -1
        if (from == to) return -1   // جابه‌جایی به خودش بی‌اثر است؛ مثل ورودی نامعتبر
        val mutable = items.toMutableList()
        val item = mutable.removeAt(from)
        mutable.add(to, item)
        items = mutable
        index = when (index) {
            from -> to
            in minOf(from, to)..maxOf(from, to) ->
                if (from < to) index - 1 else index + 1
            else -> index
        }
        return to
    }

    /** آیا رسیدیم به انتهای صف؟ (برای تشخیص پایان پخش طبیعی) */
    fun isAtEnd(): Boolean {
        if (items.isEmpty()) return true
        if (repeat == RepeatMode.ONE || repeat == RepeatMode.ALL) return false
        if (shuffle) return order.indexOf(index) == order.lastIndex
        return index == items.lastIndex
    }

    companion object {
        /** آستانهٔ برگشت به ابتدای آهنگ، همان ۳ ثانیهٔ نسخهٔ اندروید */
        const val PREVIOUS_RESTART_THRESHOLD_MS = 3000L
    }
}

/**
 * سیاست خطا و رد کردن فایل خراب.
 *
 * پورت مستقیم `PlaybackService.widgetListener` نسخهٔ اندروید:
 *  - تا ۳ خطای پشت‌سرهم مجاز است؛ بعد از آن دیگر نمی‌پرد
 *  - با هر پخش موفق، شمارنده صفر می‌شود
 */
class ErrorSkipPolicy(private val maxConsecutiveErrors: Int = MAX_CONSECUTIVE_ERRORS) {

    var consecutiveErrors: Int = 0
        private set

    fun onPlayingStarted() {
        consecutiveErrors = 0
    }

    /**
     * @param hasNext آیا آهنگ بعدی‌ای وجود دارد
     * @return آیا باید به آهنگ بعدی پرید
     */
    fun shouldSkip(hasNext: Boolean): Boolean {
        if (consecutiveErrors >= maxConsecutiveErrors) return false
        if (!hasNext) return false
        consecutiveErrors++
        return true
    }

    fun reset() {
        consecutiveErrors = 0
    }

    companion object {
        const val MAX_CONSECUTIVE_ERRORS = 3
    }
}
