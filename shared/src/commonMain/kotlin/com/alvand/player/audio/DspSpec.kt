package com.alvand.player.audio

import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.exp

/**
 * تنظیمات DSP — پورت ۱:۱ از `EqualizerManager.kt` نسخهٔ اندروید.
 *
 * واحدها عیناً همان‌های اندروید است تا UI و ذخیره‌سازی یکسان بماند:
 *  - سطوح باند: **میلی‌بل** (millibel) در بازهٔ ‎-1500..+1500‎
 *  - تقویت باس: ‎0..1000‎
 *  - تقویت صدا: ‎0..10‎ دسی‌بل
 *  - شدت نویز: ‎0..100‎
 */
data class DspSpec(
    val eqEnabled: Boolean = true,
    /** ‎-1‎ یعنی دستی؛ وگرنه شمارهٔ پریست در [EQ_PRESETS] */
    val preset: Int = 0,
    val bandLevels: List<Int> = List(EQ_BANDS) { 0 },
    val bassStrength: Int = 500,
    val volumeBoostDb: Int = 0,
    val noiseReduction: Boolean = false,
    val noiseLevel: Int = 50,
    val reverbPreset: Int = 0
) {
    init {
        require(bandLevels.size == EQ_BANDS) {
            "bandLevels must have exactly $EQ_BANDS entries, got ${bandLevels.size}"
        }
    }

    val isManual: Boolean get() = preset < 0
    val isBassActive: Boolean get() = bassStrength > 0
    val isBoostActive: Boolean get() = volumeBoostDb > 0
    val isReverbActive: Boolean get() = reverbPreset > 0

    companion object {
        const val EQ_BANDS = 5
        const val MIN_MB = -1500
        const val MAX_MB = 1500
    }
}

/**
 * پریست‌های اکولایزر.
 *
 * **تفاوت عمدی با اندروید:** روی اندروید این پریست‌ها از خود دستگاه
 * (`android.media.audiofx.Equalizer`) خوانده می‌شدند و به تنظیمات دستگاه وابسته
 * بودند. روی ویندوز چنین چیزی وجود ندارد، پس طبق §۱۱.۴ پرامپت همین ۱۱ منحنی
 * **کانونی** شدند و در برنامه ثابت هستند.
 */
object EqPresets {

    /** فرکانس‌های لنگر — همان مقادیر fallback اندروید */
    val ANCHOR_FREQUENCIES = intArrayOf(60, 230, 910, 3600, 14000)

    /** نام → منحنی بر حسب دسی‌بل در ۵ فرکانس لنگر. عیناً از پرامپت §۱۱.۵. */
    val CURVES: Map<String, IntArray> = linkedMapOf(
        "rock" to intArrayOf(4, 3, -1, 2, 4),
        "pop" to intArrayOf(-2, -1, 1, 2, 1),
        "jazz" to intArrayOf(3, 2, 0, 2, 3),
        "classical" to intArrayOf(3, 2, -1, 1, 2),
        "dance" to intArrayOf(5, 3, 0, 1, 2),
        "bass" to intArrayOf(5, 4, 1, 0, 0),
        "vocal" to intArrayOf(0, 1, 3, 3, 1),
        "treble" to intArrayOf(-2, -1, 1, 3, 5),
        "latin" to intArrayOf(3, 2, 0, 2, 3),
        "party" to intArrayOf(3, 2, 0, 2, 3),
        "piano" to intArrayOf(2, 1, 0, 2, 1)
    )

    /** ترتیب نمایش: اول ۸ تا مثل اندروید، بقیه در ادامه */
    val DISPLAY_ORDER: List<String> = listOf(
        "rock", "pop", "jazz", "classical", "dance", "bass", "vocal", "treble",
        "latin", "party", "piano"
    )

    /** اول ۸ پریست، همان‌طور که نسخهٔ اندروید نشان می‌داد */
    val PRIMARY: List<String> = DISPLAY_ORDER.take(8)

    fun nameAt(index: Int): String = DISPLAY_ORDER.getOrElse(index) { "rock" }

    fun indexOfName(name: String): Int = DISPLAY_ORDER.indexOfFirst { it.equals(name, true) }

    /**
     * پیدا کردن منحنی از روی نام — همان منطق `presetName.lowercase().contains(key)`.
     * ترتیب `DISPLAY_ORDER` عمداً حفظ شده تا نتیجه با اندروید یکی باشد.
     */
    fun curveForName(presetName: String): IntArray? {
        val lower = presetName.lowercase()
        DISPLAY_ORDER.forEach { key ->
            if (lower.contains(key)) return CURVES.getValue(key)
        }
        return null
    }

    /**
     * درون‌یابی **خطی در log10(فرکانس)** — همان کاری که اندروید می‌کرد.
     * چون گوش انسان در محور لگاریتمی خطی است، این تنها راه درست است.
     *
     * @param bandFrequencies فرکانس مرکزی هر باند (طول برابر تعداد باند)
     * @param anchorCurve منحنی در ۵ فرکانس لنگر، بر حسب دسی‌بل
     * @return سطوح میلی‌بل برای هر باند، در بازهٔ مجاز
     */
    fun interpolate(bandFrequencies: DoubleArray, anchorCurve: IntArray): IntArray {
        if (bandFrequencies.isEmpty()) return IntArray(0)

        val logAnchors = ANCHOR_FREQUENCIES.map { log10(it.toDouble()) }.toDoubleArray()

        return IntArray(bandFrequencies.size) { i ->
            val f = bandFrequencies[i]
            val db = if (f <= ANCHOR_FREQUENCIES.first()) {
                anchorCurve.first().toDouble()
            } else if (f >= ANCHOR_FREQUENCIES.last()) {
                anchorCurve.last().toDouble()
            } else {
                val logF = log10(f)
                var seg = 0
                while (seg < logAnchors.size - 2 && logF > logAnchors[seg + 1]) seg++
                val x0 = logAnchors[seg]
                val x1 = logAnchors[seg + 1]
                val t = if (x1 == x0) 0.0 else (logF - x0) / (x1 - x0)
                anchorCurve[seg] + t * (anchorCurve[seg + 1] - anchorCurve[seg])
            }
            (db * 100).toInt().coerceIn(DspSpec.MIN_MB, DspSpec.MAX_MB)
        }
    }

    /** فرکانس مرکزی ۵ باند روی محدودهٔ ۲۰Hz..۲۰kHz (لگاریتمیک) */
    fun defaultBandFrequencies(): DoubleArray {
        val lo = 20.0
        val hi = 20000.0
        val ratio = hi / lo
        return DoubleArray(DspSpec.EQ_BANDS) { i ->
            lo * exp(ln(ratio) * i.toDouble() / (DspSpec.EQ_BANDS - 1))
        }
    }

    private fun log10(v: Double): Double = ln(v) / ln(10.0)
}

/**
 * حذف نویز — تقریب باندی، دقیقاً همان فرمول اندروید.
 *
 * صادق باشیم: این **کاهش نویز واقعی نیست**. فقط چند فیلتر باندی است که هوم،
 * نویز فراگیر و وضوح صدای انسان را هدف می‌گیرند. اسمش را هم همان
 * `noise reduction` نگه داشته‌ایم چون در نسخهٔ اندروید هم همین بود.
 */
object NoiseReduction {

    /**
     * @param baseLevels سطوح پایهٔ کاربر (میلی‌بل)
     * @param level شدت ‎0..100‎
     */
    fun apply(baseLevels: List<Int>, level: Int): List<Int> {
        if (level <= 0) return baseLevels
        val k = level / 100f
        val bandFrequencies = EqPresets.defaultBandFrequencies()

        return baseLevels.mapIndexed { i, base ->
            val f = bandFrequencies.getOrElse(i) { 0.0 }
            val cut = cutFor(f, k)
            (base + cut).coerceIn(DspSpec.MIN_MB, DspSpec.MAX_MB)
        }
    }

    /** مقدار کاهش بر حسب میلی‌بل برای یک فرکانس. دقیقاً جدول پرامپت §۱۱.۵. */
    fun cutFor(frequency: Double, k: Float): Int {
        val cutMb = when {
            frequency < 120.0 -> -600f * k          // هوم برق شهری
            frequency in 1000.0..4000.0 -> 150f * k // وضوح صدای انسان
            frequency > 8000.0 -> -1200f * k        // نویز فراگیر (هیس)
            frequency > 5000.0 -> -600f * k
            else -> 0f
        }
        return cutMb.toInt()
    }
}

/** تبدیل قدرت باس ‎0..1000‎ به دسی‌بل، دقیقاً مثل نگاشت پرامپت §۱۱.۴ */
object BassBoost {

    const val MIN_STRENGTH = 0
    const val MAX_STRENGTH = 1000
    const val MAX_DB = 10f

    fun strengthToDb(strength: Int): Float {
        val s = strength.coerceIn(MIN_STRENGTH, MAX_STRENGTH)
        return MAX_DB * s / MAX_STRENGTH
    }

    /** ضریب خطی برای فیلتر شِلف پایین (۱۰۰..۱۵۰ هرتز) */
    fun strengthToLinear(strength: Int): Float = exp(
        ln(10.0) * strengthToDb(strength).toDouble() / 20.0
    ).toFloat()

    fun isActive(strength: Int): Boolean = strength > MIN_STRENGTH
}

/** تبدیل میلی‌بل به ضریب خطی: `10^(mb/2000)` */
fun millibelToLinear(mb: Int): Float = exp(ln(10.0) * mb / 2000.0).toFloat()

/** تبدیل دسی‌بل به ضریب خطی */
fun decibelsToLinear(db: Float): Float = exp(ln(10.0) * db.toDouble() / 20.0).toFloat()

/**
 * مشتق‌گیری مطلق روی نمونه‌های شناور — برای نمودارِ هاله و میکروفون.
 * @return میانگین قدرمطلق هر بلوک، در بازهٔ ‎0..1‎
 */
fun envelopeAbsolute(samples: FloatArray, blockSize: Int = 1024): FloatArray {
    if (samples.isEmpty() || blockSize <= 0) return FloatArray(0)
    val blocks = samples.size / blockSize
    return FloatArray(blocks) { b ->
        var sum = 0f
        val start = b * blockSize
        for (i in start until start + blockSize) sum += abs(samples[i])
        (sum / blockSize).coerceIn(0f, 1f)
    }
}
