package com.alvand.player.ui

import java.awt.Color
import java.awt.GradientPaint
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Path
import javax.imageio.ImageIO

/**
 * ساخت دارایی‌های تست برای بازبینی چشمی صفحهٔ پلیر.
 *
 * **چرا دارایی می‌سازیم و از فایل واقعی استفاده نمی‌کنیم:** برای دیدن کارت‌های کاور
 * و پس‌زمینه، لازم است تصویر واقعی داشته باشیم. هیچ فایل صوتی با کاورِ داخلی در
 * این مخزن وجود ندارد و کپی کردن موسیقیِ کاربر هم درست نیست. پس خودمان می‌سازیم:
 * کاملاً قطعی، آفلاین، بدون مسئلهٔ لایسنس.
 *
 * هر دارایی یک WAV واقعیِ یک‌ثانیه‌ای (۴۴۰ هرتز) به‌علاوهٔ یک PNG هم‌نام است. کاور
 * از راه «تصویر کنار فایل» خوانده می‌شود، پس این دقیقاً همان مسیری است که
 * کتابخانهٔ واقعیِ کاربر هم ممکن است داشته باشد.
 */
object PlayerAssets {

    data class Track(
        val wav: File,
        val cover: File,
        val title: String,
        val artist: String
    )

    private val COVERS = listOf(
        Triple(Color(24, 62, 168), Color(196, 74, 148), "Dreamy Night"),
        Triple(Color(16, 122, 108), Color(226, 168, 58), "Midnight Drive"),
        Triple(Color(88, 28, 118), Color(64, 156, 214), "Paper Boats")
    )

    private val ARTISTS = listOf("Alvand", "Alvand", "Alvand")

    /**
     * سه آهنگ کوتاه می‌سازد و مسیرشان را برمی‌گرداند.
     *
     * @param dir پوشهٔ مقصد؛ خودش ساخته می‌شود
     */
    fun create(dir: Path): List<Track> {
        dir.toFile().mkdirs()
        return COVERS.mapIndexed { i, (from, to, title) ->
            val stem = "track${i + 1}"
            val wav = File(dir.toFile(), "$stem.wav")
            val png = File(dir.toFile(), "$stem.png")
            writeToneWav(wav, seconds = 1, freq = 440 + i * 110)
            writeCoverPng(png, from, to, seed = i)
            Track(wav, png, title, ARTISTS[i])
        }
    }

    /** WAV معتبر ۱۶ بیتی استریو — همان چیزی که `RawAudioDecoder` می‌خواند */
    private fun writeToneWav(target: File, seconds: Int, freq: Int) {
        val rate = 44100
        val channels = 2
        val frames = rate * seconds
        FileOutputStream(target).use { out ->
            fun le16(v: Int) { out.write(v and 0xFF); out.write((v shr 8) and 0xFF) }
            fun le32(v: Int) {
                out.write(v and 0xFF); out.write((v shr 8) and 0xFF)
                out.write((v shr 16) and 0xFF); out.write((v shr 24) and 0xFF)
            }
            out.write("RIFF".toByteArray())
            le32(36 + frames * channels * 2)
            out.write("WAVE".toByteArray())
            out.write("fmt ".toByteArray()); le32(16)
            le16(1); le16(channels); le32(rate)
            le32(rate * channels * 2); le16(channels * 2); le16(16)
            out.write("data".toByteArray()); le32(frames * channels * 2)

            val block = ByteArray(4096 * channels * 2)
            var written = 0
            var i = 0
            while (written < frames) {
                val n = minOf(block.size / (channels * 2), frames - written)
                var o = 0
                for (s in 0 until n) {
                    val v = (9000 * kotlin.math.sin(2 * Math.PI * freq * i / rate)).toInt().toShort()
                    repeat(channels) {
                        block[o++] = (v.toInt() and 0xFF).toByte()
                        block[o++] = ((v.toInt() shr 8) and 0xFF).toByte()
                    }
                    i++
                }
                out.write(block, 0, n * channels * 2)
                written += n
            }
        }
    }

    /** کاور مربعی با گرادیان و شکل ساده — به‌قدری متمایز که در عکس معلوم باشد */
    private fun writeCoverPng(target: File, from: Color, to: Color, seed: Int) {
        val size = 480
        val img = BufferedImage(size, size, BufferedImage.TYPE_INT_RGB)
        val g = img.createGraphics()
        g.setRenderingHint(
            RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON
        )
        g.paint = GradientPaint(0f, 0f, from, size.toFloat(), size.toFloat(), to)
        g.fillRect(0, 0, size, size)

        // چند شکل هندسی تا کاورها از هم تشخیص داده شوند
        g.color = Color(255, 255, 255, 90)
        when (seed % 3) {
            0 -> g.fillOval(size / 5, size / 5, size * 3 / 5, size * 3 / 5)
            1 -> {
                g.fillPolygon(
                    intArrayOf(size / 2, size / 6, size * 5 / 6),
                    intArrayOf(size / 6, size * 4 / 5, size * 4 / 5),
                    3
                )
            }
            else -> g.fillRoundRect(size / 6, size / 6, size * 2 / 3, size * 2 / 3, 60, 60)
        }
        g.color = Color(255, 255, 255, 150)
        g.fillRect(size / 2 - 6, size / 12, 12, size / 6)
        g.dispose()

        FileOutputStream(target).use { ImageIO.write(img, "png", it) }
    }
}
