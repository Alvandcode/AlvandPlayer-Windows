/**
 * ماژول shared — منطق خالص و قابل تست بدون UI (تنها JVM)
 */

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    jvm()

    sourceSets {
        jvmMain.dependencies {
            // Compose dependencies needed for theme files
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.runtime)

            // کتابخانه‌های مشترک
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)

            // مدل‌های داده، تنظیمات، منطق پلیر
        }

        commonTest.dependencies {
            // همهٔ تست‌های common از kotlin.test استفاده می‌کنند؛ بدون این
            // وابستگی کل دستهٔ تست compile نمی‌شود (بسیاری از ادعاهای
            // «۳۳۱ تست» در README بدون این قابل اجرا نبود).
            implementation(kotlin("test"))
        }
    }
}