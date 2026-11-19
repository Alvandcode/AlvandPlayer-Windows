package com.alvand.player.player

import java.io.File
import java.io.FileOutputStream
import java.nio.file.Path
import kotlin.math.sin

/**
 * ساخت دارایی تست برای FLAC.
 *
 * **چرا رمزگذار داریم:** FLAC بدون فایل نمونه قابل آزمودن نیست، و ساختن یک FLAC
 * معتبر با دست (نوشتن هدر فریم و زیرفریم‌ها بیت‌به‌بیت) پرخطاست — اگر اشتباه
 * کنیم، تست به‌جای کشف باگ، خودش فایل خراب می‌سازد. پس فقط در `jvmTest` یک
 * رمزگذار واقعی اضافه شده تا فایل معتبر تولید شود.
 *
 * برنامهٔ اصلی هیچ وابستگی رمزگذاری ندارد.
 */
object FlacFixtures {

    /**
     * یک فایل FLAC واقعی از یک سیگنال سینوسی می‌سازد (WAV می‌نویسد بعد تبدیل می‌کند).
     *
     * @param freq بسامد پایه — دو فایل با بسامد متفاوت آزمودن «محتوا درست خوانده
     *   می‌شود» را ممکن می‌کند، نه فقط «حافظه پر شد»
     */
    fun write(target: File, seconds: Int = 1, freq: Int = 440, sampleRate: Int = 44100): File {
        val wav = File(target.parentFile, target.name + ".source.wav")
        writeToneWav(wav, seconds, freq, sampleRate)

        val encoder = net.sourceforge.javaflacencoder.FLAC_FileEncoder()
        val status = encoder.encode(wav, target)
        wav.delete()

        // FULL_ENCODE یعنی کل جریان رمزگذاری شد (حالت عادی)؛ OK برای حالت
        // streamable-subset است. هر دو موفقیت‌اند.
        val ok = status == net.sourceforge.javaflacencoder.FLAC_FileEncoder.Status.OK ||
            status == net.sourceforge.javaflacencoder.FLAC_FileEncoder.Status.FULL_ENCODE
        check(ok) { "FLAC encoding failed with $status" }
        // سردر fLaC یعنی واقعاً FLAC نوشته شده، نه یک فایل خالی
        check(target.length() > 0) { "encoder produced an empty file" }
        val magic = target.readBytes().take(4).toByteArray().decodeToString()
        check(magic == "fLaC") { "output is not a FLAC file (magic='$magic')" }
        return target
    }

    fun writeTo(dir: Path, name: String, seconds: Int = 1, freq: Int = 440): File =
        write(File(dir.toFile(), name), seconds, freq)

    /** WAV معتبر ۱۶ بیتی استریو، little-endian */
    private fun writeToneWav(target: File, seconds: Int, freq: Int, sampleRate: Int) {
        val channels = 2
        val frames = sampleRate * seconds
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
            le16(1); le16(channels); le32(sampleRate)
            le32(sampleRate * channels * 2); le16(channels * 2); le16(16)
            out.write("data".toByteArray()); le32(frames * channels * 2)

            // دامنه ۱۲۰۰۰ است تا جابه‌جایی بیت‌ها آشکارا خراب شود
            val block = ByteArray(4096 * channels * 2)
            var written = 0
            var i = 0
            while (written < frames) {
                val n = minOf(block.size / (channels * 2), frames - written)
                var o = 0
                for (s in 0 until n) {
                    val v = (12000 * sin(2 * Math.PI * freq * i / sampleRate)).toInt().toShort()
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
}
