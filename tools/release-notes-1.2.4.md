# Alvand Player 1.2.4 — Windows

## Audit fixes / رفع ایرادهای ممیزی

### English

- **MIT LICENSE file added to the repository** — GitHub now detects the license
- **Fixed: race in queue removal** — two rapid queue-row removals could remove the wrong row (index is now read inside the control lock)
- **TagWriter now keeps a backup** — before editing tags in place, the original file is copied to `<name>.alvand-bak`; a crash mid-write no longer destroys the user's music file
- **Clearing the queue now persists** — closing the app after clearing no longer resurrects the old queue on next launch
- **Window size & position are remembered** across launches (defaults to 1040×820 centered when unset)
- **Version is single-source again** — `packageVersion`, the lyrics User-Agent and the MSI pipeline all read/write 1.2.4; a fresh `package-msi.ps1` run can no longer produce a mismatched version
- **Fixed: MSI version padding bug** — a single-part version like `1` used to become the malformed `1.01.0`; it is now correctly padded to `1.0.0`
- 342 tests green

### فارسی

<div dir="rtl">

- **فایل لایسنس MIT به ریپو اضافه شد** — گیت‌هاب حالا لایسنس را تشخیص می‌دهد
- **رفع باگ مسابقه در حذف صف** — دو حذفِ سریعِ پشت‌سرهم می‌توانست ردیفِ اشتباه را حذف کند
- **TagWriter حالا بکاپ نگه می‌دارد** — قبل از ویرایش تگ درجا، نسخهٔ اصلی در `<نام>.alvand-bak` کپی می‌شود؛ کرش وسط نوشتن دیگر فایل موزیک کاربر را خراب نمی‌کند
- **پاک کردن صف حالا می‌ماند** — بستن برنامه بعد از پاک کردن صف دیگر صف قدیمی را برنمی‌گرداند
- **اندازه و موقعیت پنجره بین اجراها ذخیره می‌شود**
- **نسخه دوباره تک‌منبعی شد** — نسخهٔ MSI، User-Agent و خط ساخت همگی 1.2.4
- **رفع باگ padding نسخه در MSI** — نسخهٔ `"1"` دیگر به `"1.01.0"` خراب نمی‌شود

</div>

## Install / نصب

Download `AlvandPlayer-1.2.4-setup.msi` below, double-click, done.
Requires **Windows 10/11 (x64)** — no other dependencies.

فایل MSI زیر را دانلود و دابل‌کلیک کنید. فقط ویندوز ۱۰/۱۱ (۶۴ بیتی) لازم است.

---

⭐ If you like the player, please star the repo and join the Telegram channel: **https://t.me/a_c_official**

<div dir="rtl">

⭐ اگر پلیر را دوست داشتید، به ریپو ستاره بدهید و عضو کانال تلگرام شوید: **https://t.me/a_c_official**

</div>
