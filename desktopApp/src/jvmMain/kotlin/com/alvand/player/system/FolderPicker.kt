package com.alvand.player.system

import com.alvand.player.library.LibraryRoots
import com.alvand.player.player.DecoderFactory
import java.io.File
import javax.swing.JFileChooser
import javax.swing.SwingUtilities
import javax.swing.filechooser.FileFilter

/**
 * انتخاب پوشه با دیالوگ دسکتاپ (جایگزین SAF اندروید).
 *
 * از `JFileChooser` در حالت فقط-پوشه استفاده می‌شود، چون `FileDialog` بومی در
 * حالت باز کردن فایل برای انتخاب پوشه قابل‌اعتماد نیست و همین باعث می‌شد دکمهٔ
 * «انتخاب پوشه» عملاً کاری نکند. دیالوگ باید روی نخ EDT باز شود؛ این کلاس خودش
 * آن را مدیریت می‌کند تا صداکننده لازم نباشد نخ را بداند.
 */
object FolderPicker {

    /** @return مسیر انتخاب‌شده، یا null اگر کاربر انصراف داد */
    fun pickDirectory(
        title: String = "Choose music folder",
        dialogs: SwingDialogs = SwingDialogs
    ): File? {
        val chooser = JFileChooser().apply {
            dialogTitle = title
            fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
            isAcceptAllFileFilterUsed = false
            dialogs.initialDirectory()?.let { currentDirectory = it }
        }
        return dialogs.choose(chooser)
    }

    /** @return مسیر انتخاب‌شده، یا null اگر کاربر انصراف داد */
    fun pickAudioFile(
        title: String = "Open audio file",
        dialogs: SwingDialogs = SwingDialogs
    ): File? {
        val chooser = JFileChooser().apply {
            dialogTitle = title
            fileSelectionMode = JFileChooser.FILES_ONLY
            addChoosableFileFilter(SupportedAudioFileFilter())
            fileFilter = SupportedAudioFileFilter()
            dialogs.initialDirectory()?.let { currentDirectory = it }
        }
        return dialogs.choose(chooser)
    }

    /** فقط فایل‌های صوتیِ قابل‌پخشِ موتور، به‌علاوهٔ پوشه‌ها برای ناوبری. */
    class SupportedAudioFileFilter : FileFilter() {
        override fun accept(file: File): Boolean =
            file.isDirectory || DecoderFactory.canDecode(file.absolutePath)

        override fun getDescription(): String {
            val extensions = DecoderFactory.playableExtensions.sorted().joinToString(", ") { "*.$it" }
            return "Audio files ($extensions)"
        }
    }

    /** اجرای دیالوگ Swing؛ جدا شده تا منطق انتخاب مسیر قابل تست/جایگزینی باشد. */
    open class SwingDialogs {
        open fun initialDirectory(): File? =
            LibraryRoots.windowsMusicFolder()?.toFile()?.takeIf { it.isDirectory }

        open fun choose(chooser: JFileChooser): File? = runCatching {
            val selected = if (SwingUtilities.isEventDispatchThread()) {
                chooser.showOpenDialog(null)
            } else {
                var result = JFileChooser.CANCEL_OPTION
                SwingUtilities.invokeAndWait { result = chooser.showOpenDialog(null) }
                result
            }
            if (selected == JFileChooser.APPROVE_OPTION) chooser.selectedFile else null
        }.getOrNull()

        companion object : SwingDialogs()
    }
}

