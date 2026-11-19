package com.alvand.player.library

import java.io.File
import java.nio.file.Files
import java.nio.file.Path

/**
 * ریشه‌های پیش‌فرض اسکن خودکار.
 *
 * کاربر خواسته همهٔ درایوها به‌جز درایو C اسکن شوند. ویندوز فهرست مرکزی رسانه
 * ندارد، پس نزدیک‌ترین رفتارِ خودکار همین است: همهٔ ریشه‌های قابل‌دسترس به‌جز
 * درایو سیستم. پوشهٔ Music داخل C عمداً اضافه نمی‌شود چون همان درایو مستثناست.
 */
object DriveRoots {

    fun defaultScanRoots(
        mountedRoots: List<Path> = File.listRoots().map { it.toPath() },
        systemDrive: String? = System.getenv("SystemDrive"),
        windowsDirectory: String? = System.getenv("SystemRoot"),
        isAccessible: (Path) -> Boolean = Files::isDirectory
    ): List<Path> {
        val excluded = systemRoots(systemDrive, windowsDirectory)
        return mountedRoots
            .map { it.toAbsolutePath().normalize() }
            .filterNot { it in excluded }
            .filter { isAccessible(it) }
            .distinct()
    }

    fun systemRoots(systemDrive: String?, windowsDirectory: String?): Set<Path> {
        val roots = HashSet<Path>()
        driveRoot(systemDrive)?.let { roots.add(it) }
        driveRoot(windowsDirectory?.take(2))?.let { roots.add(it) }
        // اگر به هر دلیلی متغیر محیطی خوانده نشد، دست‌کم C را کنار بگذار.
        driveRoot("C:")?.let { roots.add(it) }
        return roots
    }

    private fun driveRoot(drive: String?): Path? {
        val normalized = drive?.trim()?.takeIf { it.length >= 2 } ?: return null
        val letter = normalized.substring(0, 2).uppercase()
        if (!letter[0].isLetter() || letter[1] != ':') return null
        return runCatching { Path.of("$letter\\").toAbsolutePath().normalize() }.getOrNull()
    }
}
