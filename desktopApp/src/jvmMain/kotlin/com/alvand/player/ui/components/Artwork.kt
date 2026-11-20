package com.alvand.player.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.decodeToImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Collections

/**
 * خواندن کاورِ داخل فایل صوتی.
 *
 * کاور از خود فایل استخراج می‌شود (`jaudiotagger`) و **هیچ درخواست شبکه‌ای**
 * داده نمی‌شود — برنامه هیچ‌وقت چیزی جمع‌آوری نمی‌کند.
 *
 * چرا کش لازم است: هر بار که کاربر آهنگ عوض می‌کند، کل صف دوباره رندر می‌شود.
 * بدون کش، برای هر کارت فایل از دیسک دوباره خوانده و دوباره رمزگشایی می‌شد که در
 * یک صف ۲۰۰ آهنگی یعنی صدها بار خواندن فایل در یک ثانیه.
 */
object ArtworkCache {

    private val cache = Collections.synchronizedMap(
        object : LinkedHashMap<String, ImageBitmap>(32, 0.75f, true) {
            override fun removeEldestEntry(
                eldest: MutableMap.MutableEntry<String, ImageBitmap>?
            ): Boolean = size > MAX_ENTRIES
        }
    )

    /** ۲۴ کاور؛ کاورها معمولاً ۳۰۰ تا ۵۰۰ پیکسل‌اند ولی سقف حافظه را باز هم محدود نگه می‌دارد */
    private const val MAX_ENTRIES = 24

    fun get(uri: String): ImageBitmap? = cache[uri]

    fun put(uri: String, image: ImageBitmap) {
        cache[uri] = image
    }

    fun clear() = cache.clear()

    /**
     * رمزگشایی بایت‌های تصویر به `ImageBitmap`.
     *
     * از `decodeToImageBitmap` استفاده می‌شود که API رسمی Compose است و روی
     * دسکتاپ به skia می‌رسد. عمداً کوچک‌سازی دستی انجام نمی‌دهیم چون راه استاندارد
     * و قابل اتکایی برای آن در Compose Multiplatform وجود ندارد؛ به‌جایش تعداد
     * ورودی‌های کش محدود شده تا حافظه کنترل‌شده بماند. `Image` خودش موقع نمایش
     * با `ContentScale.Crop` مقیاس می‌گیرد.
     */
    fun decode(bytes: ByteArray): ImageBitmap? =
        runCatching { bytes.decodeToImageBitmap() }.getOrNull()
}

/** خواندن کاور یک فایل صوتی؛ null یعنی فایل کاور ندارد یا خراب است. */
suspend fun loadArtwork(uri: String): ImageBitmap? {
    if (uri.isBlank()) return null
    ArtworkCache.get(uri)?.let { return it }

    return withContext(Dispatchers.IO) {
        val file = File(uri)
        if (!file.isFile) return@withContext null

        // اول کاورِ داخل خود فایل، بعد تصویرِ کنار فایل
        val embedded = runCatching {
            // در jaudiotagger ۳ کاور روی `Tag` است، نه روی خود `AudioFile`
            org.jaudiotagger.audio.AudioFileIO.read(file)
                .tag?.firstArtwork?.binaryData
        }.getOrNull()

        val bytes = embedded ?: runCatching {
            sidecarCandidates(file).firstOrNull()?.readBytes()
        }.getOrNull() ?: return@withContext null

        ArtworkCache.decode(bytes)?.also { ArtworkCache.put(uri, it) }
    }
}

/**
 * نام‌های متداول تصویرِ کنار فایل.
 *
 * خیلی از کتابخانه‌های موسیقی کاور را داخل تگ نمی‌گذارند و به‌جایش یک تصویر با
 * نام شناخته‌شده کنار آهنگ‌ها می‌گذارند. پشتیبانی از این حالت یعنی کاربر کاور
 * می‌بیند حتی وقتی فایل صوتی تگ تصویری ندارد.
 */
private val SIDECAR_NAMES = listOf("cover", "folder", "front", "album", "albumart", "thumb")
private val SIDECAR_EXTS = listOf(".jpg", ".jpeg", ".png", ".webp")

private fun sidecarCandidates(file: File): List<File> {
    val dir = file.parentFile ?: return emptyList()
    val base = file.nameWithoutExtension
    val out = ArrayList<File>(SIDECAR_NAMES.size * SIDECAR_EXTS.size + SIDECAR_EXTS.size)
    for (name in SIDECAR_NAMES) {
        for (ext in SIDECAR_EXTS) out += File(dir, name + ext)
    }
    // هم‌نامِ خودِ آهنگ، مثل ‎01 - Artist - Title.jpg‎ کنار ‎.mp3‎
    for (ext in SIDECAR_EXTS) out += File(dir, base + ext)
    return out.filter { it.isFile && it.length() > 0L }
}

/**
 * کاورِ آمادهٔ یک آیتم، یا null تا وقتی خوانده نشده.
 *
 * کلید برابر `uri` است پس با چرخاندن صف، کاورها دوباره خوانده نمی‌شوند.
 */
@Composable
fun rememberArtwork(uri: String?): State<ImageBitmap?> = produceState<ImageBitmap?>(
    initialValue = uri?.let { ArtworkCache.get(it) },
    key1 = uri
) {
    val target = uri
    if (target.isNullOrBlank()) {
        value = null
        return@produceState
    }
    // نکته: این پروڈوسر وقتی uri عوض می‌شود دوباره اجرا می‌شود، ولی «value»
    // هنوز کاور آهنگ قبلی را دارد. اگر شرط را «value == null» بگذاریم،
    // کاورِ قبلی برای همیشه می‌ماند و همهٔ آهنگ‌ها کاورِ آهنگِ اول را نشان
    // می‌دادند. برعکسش هم خطرناک است: اگر فقط «value !== cached» چک کنیم،
    // حالتِ «هردو نال» (آهنگِ هنوز کش‌نشده) از چنگمان درمی‌رود و کاور
    // هیچ‌وقت بارگذاری نمی‌شود. پس فقط وقتی می‌پریم که مقدارِ فعلی واقعاً
    // همان کاورِ کش‌شدهٔ مقصد باشد؛ در هر حالت دیگری دوباره می‌خوانیم.
    val cached = ArtworkCache.get(target)
    if (!(cached != null && value === cached)) {
        value = cached ?: loadArtwork(target)
    }
}
