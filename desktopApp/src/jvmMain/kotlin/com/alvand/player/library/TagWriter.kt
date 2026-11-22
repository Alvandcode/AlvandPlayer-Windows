package com.alvand.player.library

import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

/**
 * نویسندهٔ تگ — با همان jaudiotagger که خواندن را انجام می‌دهد.
 *
 * پشتیبانی: ID3v2 (MP3)، VorbisComment (FLAC/Ogg)، MP4 (M4A).
 * قواعد:
 *  - فقط فیلدهای ناشابل هم نوشته می‌شوند (title/artist/album/genre).
 *  - مقدار خالی یعنی «پاک‌کردن فیلد»؛ null یعنی «دست نزن».
 *  - فایل قفل‌شده (در حال پخش روی ویندوز باز است ولی write-share اجازهٔ نوشتن
 *    می‌دهد) — اگر نوشتن شکست بخورد، Exception صادقانه بالا می‌آید تا UI
 *    پیام خطا نشان دهد، نه اینکه ساکت دروغ بگوید.
 *  - **نوشتن اتمیک نیست ولی محافظت‌شده است:** jaudiotagger فایل را درجا
 *    بازنویسی می‌کند (فرمت MP4/Vorbis اجازهٔ tmp+rename راحت نمی‌دهد)، پس قبل
 *    از commit یک کپی `<نام>.alvand-bak` می‌سازیم؛ اگر وسط نوشتن کرش/برق رفت،
 *    کاربر فایل سالم را در بکاپ دارد. بکاپ قبلی همیشه جایگزین می‌شود تا حجم
 *    انباشته نشود.
 *  - همیشه روی Dispatchers.IO اجرا شود.
 */
object TagWriter {

    data class Edit(
        val title: String? = null,
        val artist: String? = null,
        val album: String? = null,
        val genre: String? = null
    ) {
        val isBlank: Boolean
            get() = title == null && artist == null && album == null && genre == null
    }

    sealed interface Result {
        data object Success : Result
        data class Failed(val reason: String) : Result
    }

    fun write(path: Path, edit: Edit): Result {
        if (edit.isBlank) return Result.Success
        return try {
            val file = AudioFileIO.read(path.toFile())
            var tag = file.tag
                ?: runCatching { file.tag ?: file.createDefaultTag() }.getOrNull()
                ?: return Result.Failed("no-tag-support")
            var changed = false

            fun set(key: FieldKey, value: String?) {
                if (value == null) return
                val current = runCatching { tag.getFirst(key) }.getOrNull()?.trim()
                if (current == value) return
                if (value.isBlank()) {
                    runCatching { tag.deleteField(key) }
                } else {
                    tag.setField(key, value)
                }
                changed = true
            }

            set(FieldKey.TITLE, edit.title)
            set(FieldKey.ARTIST, edit.artist)
            set(FieldKey.ALBUM, edit.album)
            set(FieldKey.GENRE, edit.genre)

            if (changed) {
                // محافظت از فایل کاربر: قبل از بازنویسیِ درجای jaudiotagger،
                // نسخهٔ سالم را در بکاپ نگه می‌داریم (بکاپ قبلی جایگزین می‌شود).
                val backup = path.resolveSibling(
                    path.fileName.toString() + ".alvand-bak"
                )
                runCatching {
                    Files.copy(path, backup, StandardCopyOption.REPLACE_EXISTING)
                }
                // jaudiotagger فایل را کامیت می‌کند؛ خطای I/O را صادقانه بالا می‌دهیم
                file.commit()
            }
            Result.Success
        } catch (t: Throwable) {
            Result.Failed(t.message ?: t::class.simpleName.orEmpty())
        }
    }
}
