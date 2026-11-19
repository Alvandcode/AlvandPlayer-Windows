# راهنمای ساخت (BUILD)

## پیش‌نیازها

| ابزار | نسخه | توضیح |
|---|---|---|
| JDK | **17** (Temurin) | نسخهٔ ۱۷ یا بالاتر. روی این ماشین: `C:\src\jdk17` |
| Gradle | ندارید | خودِ پروژه wrapper دارد؛ Gradle جدا لازم نیست |
| ویندوز | 10 نسخهٔ 1809+ یا 11 | فقط x64 (ARM64 در milestone 9) |

هیچ Android Studio و هیچ Android SDK لازم نیست. Gradle، Kotlin و همهٔ کتابخانه‌ها
خودکار دانلود می‌شوند.

## فرمان‌ها

همهٔ فرمان‌ها از ریشهٔ پروژه (همین پوشه) اجرا می‌شوند:

```powershell
# اجرای برنامه
.\gradlew.bat :desktopApp:run

# تست‌ها (بدون نیاز به نمایشگر)
.\gradlew.bat jvmTest

# کامپایل همه‌چیز + تست‌ها
.\gradlew.bat build

# بستهٔ نصب‌کننده (MSI و EXE) — نیاز به WiX v4 / jpackage
.\gradlew.bat :desktopApp:packageDistributionForCurrentOS

# ZIP قابل‌حمل
.\gradlew.bat :desktopApp:packageZip
```

روی لینوکس/macOS به‌جای `gradlew.bat` از `./gradlew` استفاده کنید.

## ساختار پروژه

```
Alvand-Player-Windows/
├── shared/          منطق خالص و قابل تست (KMP، فقط jvm فعلاً) — بدون ویندوز و بدون UI
│   ├── src/commonMain/    مدل، کتابخانه، لیریک، DSP، تم
│   └── src/commonTest/    تست‌های پورت‌شده از اندروید
├── desktopApp/      رابط کاربری Compose Multiplatform + یکپارچگی با ویندوز
│   ├── src/jvmMain/
│   └── icons/       آیکون .ico برای بسته‌بندی
├── tools/           اسکریپت‌های کمکی (ساخت آیکون)
└── docs/            مستندات
```

مرز `shared` و `desktopApp` **اجباری** است: به همین دلیل است که می‌شود منطق را بدون
نمایشگر تست کرد و بعداً همان کد را روی Android هم استفاده کرد.

## تنظیمات Gradle و شبکه

`settings.gradle.kts` مخازن را این‌طور تنظیم می‌کند:

1. `mavenCentral()` — تقریباً همه‌چیز
2. `google()` — کتابخانه‌های AndroidX که لایهٔ مشترک Compose لازم دارد
3. `gradlePluginPortal()` — فقط برای افزونه‌ها

همچنین آینه‌های `maven.aliyun.com` که فایل سراسری `~/.gradle/init.d` اضافه می‌کند
حذف می‌شوند، چون روی بعضی شبکه‌ها قطع می‌شود و هر resolve را چند دقیقه معطل می‌کند.
اگر شبکهٔ شما به گوگل دسترسی ندارد، باید یک آینهٔ سالم جای `google()` بگذارید.

## داده‌های اپ

| چیز | مسیر |
|---|---|
| تنظیمات | `%APPDATA%\Alvand\alvand_settings.properties` |
| کرش‌لاگ | `%APPDATA%\Alvand\crashes\` (در milestone 2) |
| متن آهنگ | `%APPDATA%\Alvand\lyrics\` (در milestone 6) |
| دیتابیس | `%APPDATA%\Alvand\alvand.db` (در milestone 2) |

برای پاک کردن کامل، پوشهٔ `%APPDATA%\Alvand` را حذف کنید.
