package com.alvand.player.player

import org.jcodec.codecs.aac.AACDecoder
import org.jcodec.containers.mp4.boxes.Box
import org.jcodec.containers.mp4.boxes.MediaHeaderBox
import org.jcodec.containers.mp4.boxes.NodeBox
import org.jcodec.containers.mp4.boxes.SampleDescriptionBox
import org.jcodec.containers.mp4.boxes.SampleEntry
import org.jcodec.containers.mp4.demuxer.AbstractMP4DemuxerTrack
import org.jcodec.containers.mp4.demuxer.MP4Demuxer
import org.jcodec.common.io.FileChannelWrapper
import org.jcodec.common.model.AudioBuffer
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.max

/**
 * رمزگشای M4A (کانتینر MP4 با کدک AAC).
 *
 * ## چرا jcodec و نه چیز دیگر
 *
 * سه مسیر بررسی و آزمایش شد و هر سه مشکلی داشتند که **با تست محتوایی** گرفته شد، نه
 * با تست سادهٔ «صدا آمد؟»:
 *
 * 1. `javasound-aac` روی مسیر Java Sound SPI بافر را با `encoding` اشتباه (`AAC`
 *    به‌جای `PCM_SIGNED`) برمی‌گرداند و `AudioSystem` خطای `Unsupported
 *    conversion` می‌دهد.
 * 2. `javasound-alac` بافر تحویل می‌دهد ولی محتوایش **نویز** است.
 * 3. `de.sfuhrm:jaad` روی فایل‌های واقعیِ ساختهٔ ffmpeg **می‌شکند**:
 *    `error while decoding box … box too large for parent` — چون جعبهٔ `meta`
 *    (متادیتای iTunes) را غلط پارس می‌کند. نسخهٔ `0.9.4` هم API عوض کرده و همین
 *    باگ را دارد.
 *
 * jcodec هم دیمولتی‌پلکسر MP4 خودش را دارد (که `meta` را درست می‌خواند) و هم یک
 * لایهٔ نازک روی همان JAAD برای خودِ AAC. یعنی هر دو کار پوشش داده می‌شود بدون
 * اینکه به کد بومی یا zuberstrosse نیاز باشد. Apache-2.0.
 *
 * ## دو نکتهٔ فنی که با اندازه‌گیری ثابت شد
 *
 * ۱. **خروجی big-endian نیست، little-endian است.** این با مقایسهٔ نمونه‌به‌نمونه
 *    با خروجی ffmpeg ثابت شد (همبستگی ۱٫۰۰۰۰ در حالت little-endian در برابر
 *    ۰٫۰۲ در حالت big-endian). توجه: با نگاه کردن به «فرکانس غالب» می‌شود به
 *    اشتباه افتاد، چون جابه‌جایی بایت هم فرکانس اصلی را حفظ می‌کند.
 *
 * ۲. **AAC یک تأخیر رمزگذار ۱۰۲۴ نمونه‌ای دارد** (priming). بدون کم کردن آن،
 *    خروجی نسبت به اصل فایل ۲۳ میلی‌ثانیه عقب می‌افتد. مقدارش را از
 *    `EditListBox` فایل می‌خوانیم که همان چیزی است که ffmpeg هم به آن تکیه می‌کند.
 *
 * ## محدودیت‌ها (عمدی و صریح)
 *
 * - فقط **AAC** پخش می‌شود. ALAC کدک دیگری است و پشتیبانی نمی‌شود؛ در آن حالت
 *   خطای روشن می‌دهیم نه اینکه صدای خراب پخش کنیم.
 * - فایل DRMدار پشتیبانی نمی‌شود.
 */
class M4aDecoder private constructor(
    private val channel: FileChannel,
    private val demuxer: MP4Demuxer,
    private val track: AbstractMP4DemuxerTrack,
    private val aac: AACDecoder,
    private val primingSamples: Int,
    sampleRate: Int,
    channels: Int,
    durationSeconds: Double
) : AudioDecoder {

    override val format = AudioFormat(
        sampleRate = sampleRate,
        channels = channels.coerceIn(1, 2),
        bitsPerSample = 16
    )

    override val totalDurationMs: Long = max(0.0, durationSeconds * 1000.0).toLong()

    /** تا اینجا از فایل خوانده‌ایم ولی هنوز به‌خاطر priming دور ریخته‌ایم */
    private var primingLeft = primingSamples

    /** زمانِ خوانده‌شده بر حسب ثانیه، فقط برای محاسبهٔ مدت */
    private var readSeconds = 0.0
    private var exhausted = false

    override fun readBlock(maxFrames: Int): PcmBuffer? {
        if (exhausted) return null

        val targetBytes = max(4096, format.bytesPerSecond * maxFrames.coerceAtLeast(1024) / format.sampleRate)
        val out = ByteArrayOutputStream(targetBytes)
        var any = false

        val perFrameSamples = 1024
        val perFrameBytes = perFrameSamples * format.frameSize

        while (out.size() < targetBytes) {
            val packet = try {
                track.nextFrame()
            } catch (e: Exception) {
                null
            } ?: break

            val decoded: AudioBuffer? = try {
                aac.decodeFrame(packet.data, ByteBuffer.allocate(perFrameBytes * 4 + 1024))
            } catch (e: Exception) {
                // یک فریم خراب نباید کل آهنگ را بکشد؛ مثل رفتار MP3
                null
            }

            if (decoded == null) continue

            val data = decoded.data ?: continue
            val bytes = ByteArray(data.remaining())
            data.duplicate().get(bytes)
            if (bytes.isEmpty()) continue

            readSeconds += perFrameSamples.toDouble() / format.sampleRate

            // دور انداختن priming در ابتدای فایل
            if (primingLeft > 0) {
                val drop = minOf(primingLeft * format.frameSize, bytes.size)
                if (drop >= bytes.size) {
                    primingLeft -= bytes.size / format.frameSize
                    continue
                }
                primingLeft = 0
                out.write(bytes, drop, bytes.size - drop)
                any = true
                continue
            }

            out.write(bytes)
            any = true
        }

        if (!any || out.size() == 0) {
            exhausted = true
            return null
        }
        return PcmBuffer(format, out.toByteArray())
    }

    override fun seekTo(frame: Long) {
        val target = frame.coerceAtLeast(0)
        val seconds = target.toDouble() / format.sampleRate

        // seekPts بر حسب واحد timescale خود فایل است، نه ثانیه
        val pts = (seconds * track.timescale).toLong()
        val ok = try {
            track.canSeek(pts) && track.seekPts(pts)
        } catch (e: Exception) {
            false
        }

        if (ok) {
            exhausted = false
            primingLeft = 0   // وسط فایل دیگر priming نداریم
        } else {
            // اگر کتابخانه seek را نپذیرفت، برگرد به ابتدا؛ بهتر از صدای غلط است
            runCatching { track.seekPts(0L) }
            exhausted = false
            primingLeft = primingSamples
        }
    }

    override fun close() {
        runCatching { demuxer.close() }
        runCatching { channel.close() }
    }

    companion object {

        /** @return دیکودر آماده، یا null اگر فایل M4A نیست یا کدکش AAC نیست */
        fun open(file: File): M4aDecoder? {
            if (!file.isFile) return null
            var channel: FileChannel? = null
            return try {
                channel = FileChannel.open(file.toPath())
                val demuxer = MP4Demuxer.createMP4Demuxer(FileChannelWrapper(channel))

                val track = demuxer.audioTracks
                    .filterIsInstance<AbstractMP4DemuxerTrack>()
                    .firstOrNull()
                    ?: return fail(demuxer, channel)

                val trak = track.box
                val stbl = trak.mdia?.minf?.stbl ?: return fail(demuxer, channel)
                val stsd = stbl.boxes.filterIsInstance<SampleDescriptionBox>().firstOrNull()
                    ?: return fail(demuxer, channel)
                val entry = stsd.boxes.filterIsInstance<SampleEntry>().firstOrNull()
                    ?: return fail(demuxer, channel)

                val dsi = readDecoderSpecificInfo(entry) ?: return fail(demuxer, channel)
                val aac = try {
                    AACDecoder(ByteBuffer.wrap(dsi))
                } catch (e: Throwable) {
                    // ALAC یا هر کدک ناشناخته: عمداً null می‌دهیم
                    return fail(demuxer, channel)
                }

                // تأخیر رمزگذار از EditList فایل می‌آید (کاری که ffmpeg هم می‌کند)
                val mediaTime = trak.edits?.firstOrNull()?.mediaTime ?: 0L
                val timescale = max(1L, track.timescale)
                val priming = ((mediaTime * formatSampleRate(track)) / timescale).toInt()

                val mdhd = trak.mdia?.boxes?.filterIsInstance<MediaHeaderBox>()?.firstOrNull()
                val movieSeconds = if (mdhd != null && mdhd.timescale > 0) {
                    mdhd.duration.toDouble() / mdhd.timescale
                } else {
                    0.0
                }

                M4aDecoder(
                    channel = channel,
                    demuxer = demuxer,
                    track = track,
                    aac = aac,
                    primingSamples = priming,
                    sampleRate = formatSampleRate(track),
                    channels = channelCount(track),
                    durationSeconds = movieSeconds
                )
            } catch (e: Throwable) {
                runCatching { channel?.close() }
                null
            }
        }

        private fun fail(demuxer: MP4Demuxer, channel: FileChannel): M4aDecoder? {
            runCatching { demuxer.close() }
            runCatching { channel.close() }
            return null
        }

        private fun formatSampleRate(track: AbstractMP4DemuxerTrack): Int {
            val metadataRate = runCatching { track.meta?.audioCodecMeta?.sampleRate }.getOrNull()
            // AudioSampleEntry نرخ نمونه را به‌صورت float در بایت‌های 16..19 نگه می‌دارد،
            // ولی jcodec آن را روی timescale ترک می‌گذارد؛ برای AAC این دو برابرند.
            return (metadataRate?.takeIf { it in 8000..192000 }
                ?: track.timescale.toInt()).coerceIn(8000, 192000)
        }

        private fun channelCount(track: AbstractMP4DemuxerTrack): Int {
            val metadataChannels = runCatching { track.meta?.audioCodecMeta?.channelCount }.getOrNull()
            return metadataChannels?.takeIf { it in 1..8 } ?: 2
        }

        /**
         * استخراج `DecoderSpecificInfo` (تگ `0x05`) از جعبهٔ `esds`.
         *
         * ساختار `esds` تودرتوی دارد و پارسر باید هدر هر descriptor را رد کند،
         * وگرنه داده به‌عنوان تگ اشتباه تفسیر می‌شود:
         * - `ES_Descriptor` (0x03): بعد از طول، ۳ بایت هدر (ES_ID دو بایت + یک بایت flags)
         * - `DecoderConfigDescriptor` (0x04): بعد از طول، ۱۳ بایت هدر
         *   (objectType + streamType + bufferSize + maxBitrate + avgBitrate)
         */
        private fun readDecoderSpecificInfo(entry: SampleEntry): ByteArray? {
            for (box in entry.boxes) {
                if (box !is Box.LeafBox) continue
                val data = box.data?.duplicate() ?: continue
                val end = data.position() + data.remaining()
                data.position(data.position() + 4)      // version + flags
                findDsi(data, end)?.let { return it }
            }
            return null
        }

        private fun findDsi(b: ByteBuffer, end: Int): ByteArray? {
            while (b.position() + 2 <= end) {
                val tag = b.get().toInt() and 0xFF
                var len = 0
                var done = false
                for (i in 0 until 4) {
                    if (!b.hasRemaining()) return null
                    val c = b.get().toInt() and 0xFF
                    len = (len shl 7) or (c and 0x7F)
                    if (c and 0x80 == 0) { done = true; break }
                }
                if (!done) return null

                val start = b.position()
                val stop = minOf(start + len, end)

                if (tag == 0x05) {
                    val out = ByteArray(stop - start)
                    b.get(out)
                    return out
                }
                if (tag == 0x03 || tag == 0x04) {
                    val skip = if (tag == 0x03) 3 else 13
                    val inner = b.duplicate()
                    inner.position(start + skip)
                    findDsi(inner, stop)?.let { return it }
                }

                b.position(stop)
                if (len <= 0) return null
            }
            return null
        }
    }
}
