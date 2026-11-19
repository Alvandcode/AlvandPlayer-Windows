/**
 * ماژول desktopApp — رابط کاربری Compose Multiplatform و یکپارچگی با ویندوز.
 *
 * نکته دربارهٔ وابستگی‌های Compose:
 * از CMP 1.10 به بعد، آثار دسکتاپ (`*-desktop`) فقط stub هستند و کلاس‌های واقعی
 * androidx را Gradle از «مخزن گوگل» وارد می‌کند. پس `google()` MUST remain در
 * dependencyResolutionManagement — حذفش یعنی «Unresolved reference 'runtime'».
 */

import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    jvm()

    sourceSets {
        jvmMain.dependencies {
            // ماژول shared — منبع یگانهٔ منطق خالص. قبلاً همان فایل‌ها داخل
            // desktopApp هم کپی شده بودند و بیلد کپی‌ها را می‌گرفت؛ این
            // دوقلویی هر لحظه می‌توانست واگرا شود (ویدئو: playback move).
            implementation(project(":shared"))

            implementation(compose.desktop.currentOs)
            implementation(compose.material3)
            // آیکون‌های متریال برای کنترل‌های پخش
            implementation(libs.material.icons.core)
            implementation(libs.kotlinx.coroutines.swing)

            // خواندن تگ‌های ID3v1/ID3v2, MP4, Vorbis, FLAC, APE, Ogg و کاور داخل فایل
            implementation(libs.jaudiotagger)
            // Room روی دسکتاپ وجود ندارد؛ دیتابیس SQLite با JDBC خام
            implementation(libs.sqlite.jdbc)

            // رمزگشای MP3 خالص جاوا — بدون ffmpeg بومی و بدون زیرپروسه
            implementation(libs.jlayer)
            // FLAC و Ogg Vorbis از راه SPI جاوسوند خوانده می‌شوند
            implementation(libs.jflac)
            implementation(libs.vorbis.support)

            // M4A: دیمولتی‌پلکسر MP4 + رمزگشای AAC، خالص جاوا (Apache-2.0).
            // ALAC جدا لازم است چون کدک دیگری است و داخل همان پسوند m4a می‌آید.
            implementation(libs.jcodec)
            implementation(libs.javasound.aac)
            implementation(libs.javasound.alac)

            // WMA هنوز رمزگشا ندارد: کدک اختصاصی مایکروسافت است و هیچ
            // پیاده‌سازی خالص‌جاوایی برایش وجود ندارد. تنها راه‌ها ffmpeg
            // (۳۰+ مگابایت، LGPL) یا JNI روی Media Foundation است.
            // دلیل در docs/PORT-DIFFERENCES.md بخش ۱۶ آمده است.

            // تست‌های این ماژول از kotlin.test و coroutines-test استفاده می‌کنند
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)

            // کلیدهای مدیا: RegisterHotKey/GetMessage از user32 ویندوز.
            // JNA خالص جاوا است و هیچ باینری با برنامه توزیع نمی‌شود —
            // فایل jna.jar کوچک است و بارگذاری کتابخانهٔ بومی در زمان اجرا انجام می‌شود.
            implementation(libs.jna)
            implementation(libs.jna.platform)
        }

        jvmTest.dependencies {
            // تست‌های FLAC با رمزگذار خالص‌جاوایی فایل واقعی می‌سازند
            implementation(libs.flac.encoder)
        }
    }
}

// فقط AAR-onlyهای اندرویدی exclude می‌شوند؛ این‌ها jar ندارند و در JVM
// resolve نمی‌شوند (core-common و annotation فقط AAR منتشر شده‌اند).
// بقیهٔ androidx (مثل lifecycle) jar دارند و در زمان اجرا لازم‌اند —
// exclude کردنشان NoClassDefFoundError می‌دهد (LifecycleOwner و ...).
configurations.all {
    resolutionStrategy {
        exclude(group = "androidx.arch.core", module = "core-common")
        exclude(group = "androidx.annotation", module = "annotation-jvm")
        exclude(group = "androidx.annotation", module = "annotation")
    }
}
compose.desktop {
    application {
        mainClass = "com.alvand.player.MainKt"

        nativeDistributions {
            // فقط فرمت‌های ویندوز: MSI و بوت‌استرپر EXE
            targetFormats(TargetFormat.Msi, TargetFormat.Exe)
            packageName = "AlvandPlayer"
            // تنها منبعِ حقیقتِ شمارهٔ نسخه. tools/package-msi.ps1 همین را می‌خواند،
            // پس نام فایل MSI و نسخهٔ داخل آن هیچ‌وقت اختلاف پیدا نمی‌کنند.
            packageVersion = "1.1.0"
            description = "Alvand Player"
            vendor = "Alvand"

            windows {
                iconFile.set(file("icons/alvand.ico"))
                menu = true
                menuGroup = "Alvand Player"
                // این UUID ارتقا نصب را ثابت نگه می‌دارد تا نصب جدید، قبلی را جایگزین کند
                upgradeUuid = "8f0a2c14-6b3d-4f77-9d21-1c5e7a9b0d33"
            }
        }
    }
}
