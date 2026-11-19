/**
 * تنظیمات ریشه پروژه Alvand Player نسخه ویندوز.
 *
 * نکتهٔ محیطی: روی این ماشین فایل سراسری `~/.gradle/init.d/00-mirrors-only.gradle`
 * آینه‌های `maven.aliyun.com` را اولِ فهرست مخازن می‌گذارد. آن آینه گاهی قطع می‌شود و
 * آن‌وقت Gradle هر بار تا timeout (۱۲۰ ثانیه × چند تلاش) معطل می‌ماند. برای اینکه ساخت
 * پروژه قابل‌اعتماد باشد، همان آینه را از فهرست حذف می‌کنیم — فقط داخل همین پروژه، بدون
 * دست‌زدن به تنظیمات سراسری سیستم.
 *
 * ترتیب مخازن: Maven Central اول (تقریباً همه‌چیز را دارد و پایدار است)، بعد مخزن گوگل
 * برای کتابخانه‌های AndroidX که Compose Multiplatform در لایهٔ مشترک به آن‌ها تکیه می‌کند.
 */
pluginManagement {
    repositories {
        mavenCentral()
        google()
        gradlePluginPortal()
    }
}

import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.artifacts.repositories.MavenArtifactRepository

fun RepositoryHandler.dropUnreachableMirrors() {
    val dead = filter { repo ->
        val url = (repo as? MavenArtifactRepository)?.url
        url != null && url.host.contains("aliyun")
    }
    removeAll(dead.toList())
}

// قلاب init.d بعد از قلاب خودمان اجرا می‌شود، پس فرصت داریم مخازن مرده را پاک کنیم.
gradle.settingsEvaluated {
    pluginManagement.repositories.dropUnreachableMirrors()
}

dependencyResolutionManagement {
    repositories {
        // آینهٔ گوگل فقط برای گروه androidx — در این شبکه dl.google.com در دسترس
        // نیست و بدون این، کل androidx.* resolve نمی‌شود. فیلتر محتوایی ensures
        // این آینه فقط برای androidx consulted می‌شود و بقیه را کند نمی‌کند.
        maven {
            url = uri("https://maven.aliyun.com/repository/google")
            content {
                includeGroupByRegex("androidx\\..*")
            }
        }
        mavenCentral()
        google()
    }
}

rootProject.name = "Alvand-Player-Windows"

// shared : منطق خالص و قابل تست بدون UI (بازآزمايی در هر jvmTest)
include(":shared")
include(":desktopApp")

gradle.allprojects {
    buildscript.repositories.dropUnreachableMirrors()
    repositories.dropUnreachableMirrors()
}