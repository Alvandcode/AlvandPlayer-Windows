package com.alvand.player.system

import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

/**
 * تک‌نمونه‌سازی (Single instance).
 *
 * دو کار می‌کند:
 *  ۱. با قفل‌کردن یک فایل، مالکیت را می‌گیرد؛ اگر نمونهٔ دیگری باز باشد، این اجرا
 *     «مهمان» است. (جاوا کلاس Mutex ندارد، پس از قفل فایل استفاده می‌شود که
 *     بین پردازه‌ها هم کار می‌کند و با پاک‌شدن ناگهانی برنامه خودش آزاد می‌شود.)
 *  ۲. اگر مهمان باشد، آرگومان‌های خط فرمان را روی یک سوکت لوکال به نمونهٔ اصلی
 *     می‌فرستد تا (مثلاً) فایلی که با دوبار کلیک باز شده به همان پنجرهٔ باز برسد،
 *     و بعد خودش می‌بندد.
 *
 * این معادل رفتار intent-filter اندروید (`VIEW`/`SEND`) است.
 */
class SingleInstance private constructor(
    private val channel: FileChannel?,
    private val lock: FileLock?,
    private val socket: ServerSocket?,
    val lockFile: Path,
    private val portFile: Path
) : AutoCloseable {

    val isPrimary: Boolean get() = lock != null && socket != null

    /**
     * اگر نمونهٔ اصلی نیستیم، آرگومان‌ها را به او می‌رسانیم.
     * @return آیا ارسال موفق بود
     */
    fun forwardToPrimary(args: Array<String>): Boolean {
        if (isPrimary) return false
        val port = portFile.readPortSafe() ?: return false
        return runCatching {
            Socket(InetAddress.getLoopbackAddress(), port).use { sock ->
                sock.soTimeout = 2000
                sock.getOutputStream().use { it.write((args.joinToString("\n") + "\n").toByteArray()) }
            }
            true
        }.getOrDefault(false)
    }

    /** فراخوانی هر بار که نمونهٔ اصلی پیام تازه‌ای گرفت (مثلاً فایل جدید) */
    fun startListener(onMessage: (List<String>) -> Unit) {
        val server = socket ?: return
        Thread({
            while (!server.isClosed) {
                runCatching {
                    val text = server.accept().use { c ->
                        c.soTimeout = 2000
                        c.getInputStream().bufferedReader().readText()
                    }
                    val args = text.split("\n").filter { it.isNotBlank() }
                    // حتی اگر آرگومانی نباشد، اجرای دوباره یعنی کاربر می‌خواهد
                    // پنجرهٔ نمونهٔ اصلی را ببیند؛ پس همیشه خبر می‌دهیم.
                    onMessage(args)
                }
            }
        }, "alvand-single-instance").apply { isDaemon = true; start() }
    }

    override fun close() {
        runCatching { socket?.close() }
        runCatching { lock?.release() }
        runCatching { channel?.close() }
        runCatching { Files.deleteIfExists(portFile) }
    }

    companion object {
        private const val PORT_FILE = "instance.port"
        private const val LOCK_FILE = "instance.lock"

        fun acquire(dataDir: Path): SingleInstance {
            Files.createDirectories(dataDir)
            val portFile = dataDir.resolve(PORT_FILE)
            val lockFile = dataDir.resolve(LOCK_FILE)

            val channel = runCatching {
                FileChannel.open(
                    lockFile,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.READ,
                    StandardOpenOption.WRITE
                )
            }.getOrNull() ?: return SingleInstance(null, null, null, lockFile, portFile)

            val lock = runCatching { channel.tryLock() }.getOrNull()
            if (lock == null) return SingleInstance(channel, null, null, lockFile, portFile)

            val server = runCatching { ServerSocket(0, 4, InetAddress.getLoopbackAddress()) }
                .getOrNull()
                ?: return SingleInstance(channel, lock, null, lockFile, portFile)

            runCatching { Files.writeString(portFile, server.localPort.toString()) }
            return SingleInstance(channel, lock, server, lockFile, portFile)
        }

        private fun Path.readPortSafe(): Int? =
            runCatching { Files.readString(this).trim().toInt() }.getOrNull()
    }
}
