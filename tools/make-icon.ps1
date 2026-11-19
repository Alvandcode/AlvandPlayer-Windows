<#
.SYNOPSIS
    ساخت آیکون نرم‌افزار از یک تصویر PNG مربوط.

.DESCRIPTION
    کارهایی که این اسکریپت انجام می‌دهد:

    1. ناحیهٔ واقعی تصویر را با آلفا پیدا می‌کند تا حاشیهٔ شفاف بی‌دلیل وارد آیکون
       نشود (وگرنه آیکون داخل کادرش کوچک به‌نظر می‌رسد).
    2. محتوا را با کمی فاصله داخل یک مربع وسط‌چین می‌کند تا هالهٔ نرمِ لبه‌ها بریده
       نشود.
    3. با **پیش‌ضرب کردن آلفا** قبل از مقیاس‌دهی و **حذف پیش‌ضرب** بعد از آن
       کوچک می‌کند.

    نکتهٔ مهم دربارهٔ مورد ۳: اگر روی دادهٔ معمولی کوچک کنیم، رنگِ پیکسل‌های
    کاملاً شفاف (که معمولاً سیاه‌اند) به لبهٔ تصویر نشت می‌کند و یک **هالهٔ تیره**
    دور آیکون می‌افتد. برای آیکونی که روی نوار وظیفهٔ ویندوز می‌نشیند، این خیلی
    بد دیده می‌شود.

    خودِ مقیاس‌دهی را `Graphics2D` با درون‌یابی دوخطی باکیفیت بالا انجام می‌دهد؛
    حلقهٔ پیکسل‌به‌پیکسل دستی در PowerShell هم کند است هم مستعد خطا.

    خروجی:
      - desktopApp/icons/alvand.ico با چند اندازه (نصب‌کننده و Explorer)
      - desktopApp/src/jvmMain/resources/icons/alvand-<size>.png (پنجره و نوار وظیفه)

.EXAMPLE
    .\tools\make-icon.ps1 -Source "C:\path\to\logo.png"
#>
[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [string]$Source,

    [string]$ProjectRoot = '',

    # اندازه‌هایی که به‌صورت PNG جدا نوشته می‌شوند (پنجره و نوار وظیفه)
    [int[]]$PngSizes = @(16, 24, 32, 48, 64, 128, 256),

    # اندازه‌هایی که داخل فایل .ico می‌روند (ویندوز ۲۰ و ۴۰ هم استفاده می‌کند)
    [int[]]$IcoSizes = @(16, 20, 24, 32, 40, 48, 64, 128, 256),

    # نسبت اندازهٔ محتوا به کادر؛ کمی کمتر از ۱ تا هالهٔ لبه‌ها بریده نشود
    [double]$ContentScale = 0.97
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

# ریشهٔ پروژه: یک پوشه بالاتر از tools/
# ($PSScriptRoot داخل بلوک param همیشه در دسترس نیست، پس از مسیر اسکریپت می‌خوانیم)
if (-not $ProjectRoot) {
    $scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
    $ProjectRoot = Split-Path -Parent $scriptDir
}

$icoDir = Join-Path $ProjectRoot 'desktopApp/icons'
$pngDir = Join-Path $ProjectRoot 'desktopApp/src/jvmMain/resources/icons'
New-Item -ItemType Directory -Force -Path $icoDir, $pngDir | Out-Null

if (-not (Test-Path -LiteralPath $Source)) {
    throw "Source not found: $Source"
}

# ---------------------------------------------------------------- خواندن منبع

$src = New-Object System.Drawing.Bitmap($Source)
try {
    $sw = $src.Width
    $sh = $src.Height

    # کادر آلفا را با LockBits پیدا می‌کنیم (GetPixel خیلی کند است)
    $locked = $src.LockBits(
        (New-Object System.Drawing.Rectangle(0, 0, $sw, $sh)),
        [System.Drawing.Imaging.ImageLockMode]::ReadOnly,
        [System.Drawing.Imaging.PixelFormat]::Format32bppArgb
    )
    $stride = $locked.Stride
    $raw = New-Object byte[] ($stride * $sh)
    [System.Runtime.InteropServices.Marshal]::Copy($locked.Scan0, $raw, 0, $raw.Length)
    $src.UnlockBits($locked)

    $minX = $sw; $maxX = -1; $minY = $sh; $maxY = -1
    for ($y = 0; $y -lt $sh; $y++) {
        $rowStart = $y * $stride
        for ($x = 0; $x -lt $sw; $x++) {
            if ($raw[$rowStart + $x * 4 + 3] -gt 8) {
                if ($x -lt $minX) { $minX = $x }
                if ($x -gt $maxX) { $maxX = $x }
                if ($y -lt $minY) { $minY = $y }
                if ($y -gt $maxY) { $maxY = $y }
            }
        }
    }
    if ($maxX -lt 0) { throw 'the source image is fully transparent' }

    $cw = $maxX - $minX + 1
    $ch = $maxY - $minY + 1
    Write-Host ("content area: {0}x{1} at ({2},{3}) inside {4}x{5}" -f $cw, $ch, $minX, $minY, $sw, $sh)

    # ------------------------------ بیت‌مپِ پیش‌ضرب‌شده از ناحیهٔ محتوا

    $crop = New-Object System.Drawing.Bitmap($cw, $ch, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $cLock = $crop.LockBits(
        (New-Object System.Drawing.Rectangle(0, 0, $cw, $ch)),
        [System.Drawing.Imaging.ImageLockMode]::WriteOnly,
        [System.Drawing.Imaging.PixelFormat]::Format32bppArgb
    )
    $cropStride = $cLock.Stride
    $premul = New-Object byte[] ($cropStride * $ch)
    for ($y = 0; $y -lt $ch; $y++) {
        $sRow = ($minY + $y) * $stride
        $dRow = $y * $cropStride
        for ($x = 0; $x -lt $cw; $x++) {
            $s = $sRow + ($minX + $x) * 4
            $d = $dRow + $x * 4
            $a = $raw[$s + 3]
            # پیش‌ضرب: هر کانال رنگ در آلفا ضرب می‌شود
            $premul[$d]     = [byte](($raw[$s + 2] * $a) / 255)   # R
            $premul[$d + 1] = [byte](($raw[$s + 1] * $a) / 255)   # G
            $premul[$d + 2] = [byte](($raw[$s] * $a) / 255)       # B
            $premul[$d + 3] = $a
        }
    }
    [System.Runtime.InteropServices.Marshal]::Copy($premul, 0, $cLock.Scan0, $premul.Length)
    $crop.UnlockBits($cLock)

    # -------------------------------------------------------- مقیاس‌دهی

    $cache = @{}

    function Get-Scaled {
        param([int]$Size)

        if ($cache.ContainsKey($Size)) { return $cache[$Size] }

        $dst = New-Object System.Drawing.Bitmap($Size, $Size, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
        $g = [System.Drawing.Graphics]::FromImage($dst)
        $g.Clear([System.Drawing.Color]::Transparent)
        $g.CompositingMode = [System.Drawing.Drawing2D.CompositingMode]::SourceCopy
        $g.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality
        $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
        $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
        $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality

        $long = [double][math]::Max($cw, $ch)
        $target = $Size * $ContentScale
        $rw = $cw / $long * $target
        $rh = $ch / $long * $target
        $dx = [float](($Size - $rw) / 2)
        $dy = [float](($Size - $rh) / 2)

        $g.DrawImage($crop, $dx, $dy, [float]$rw, [float]$rh)
        $g.Dispose()

        # ------------- حذف پیش‌ضرب و نوشتن PNG
        $dLock = $dst.LockBits(
            (New-Object System.Drawing.Rectangle(0, 0, $Size, $Size)),
            [System.Drawing.Imaging.ImageLockMode]::ReadOnly,
            [System.Drawing.Imaging.PixelFormat]::Format32bppArgb
        )
        $dStride = $dLock.Stride
        $dBytes = New-Object byte[] ($dStride * $Size)
        [System.Runtime.InteropServices.Marshal]::Copy($dLock.Scan0, $dBytes, 0, $dBytes.Length)
        $dst.UnlockBits($dLock)

        $final = New-Object System.Drawing.Bitmap($Size, $Size, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
        $fLock = $final.LockBits(
            (New-Object System.Drawing.Rectangle(0, 0, $Size, $Size)),
            [System.Drawing.Imaging.ImageLockMode]::WriteOnly,
            [System.Drawing.Imaging.PixelFormat]::Format32bppArgb
        )
        $fStride = $fLock.Stride
        $fBytes = New-Object byte[] ($fStride * $Size)
        for ($y = 0; $y -lt $Size; $y++) {
            $sRow = $y * $dStride
            $oRow = $y * $fStride
            for ($x = 0; $x -lt $Size; $x++) {
                $s = $sRow + $x * 4
                $o = $oRow + $x * 4
                $a = $dBytes[$s + 3]
                if ($a -eq 0) {
                    $fBytes[$o] = 0; $fBytes[$o + 1] = 0; $fBytes[$o + 2] = 0; $fBytes[$o + 3] = 0
                } else {
                    $fBytes[$o]     = $dBytes[$s + 2]   # B
                    $fBytes[$o + 1] = $dBytes[$s + 1]   # G
                    $fBytes[$o + 2] = $dBytes[$s]       # R
                    $fBytes[$o + 3] = $a
                }
            }
        }
        [System.Runtime.InteropServices.Marshal]::Copy($fBytes, 0, $fLock.Scan0, $fBytes.Length)
        $final.UnlockBits($fLock)

        $dst.Dispose()
        $cache[$Size] = $final
        return $final
    }

    function Save-Png {
        param([int]$Size, [string]$Path)
        $bmp = Get-Scaled $Size
        $bmp.Save($Path, [System.Drawing.Imaging.ImageFormat]::Png)
    }

    # همچنین RGBA خام برای نوشتن ICO
    function Get-RgbaBytes {
        param([int]$Size)
        $bmp = Get-Scaled $Size
        $lock = $bmp.LockBits(
            (New-Object System.Drawing.Rectangle(0, 0, $Size, $Size)),
            [System.Drawing.Imaging.ImageLockMode]::ReadOnly,
            [System.Drawing.Imaging.PixelFormat]::Format32bppArgb
        )
        $stride = $lock.Stride
        $bytes = New-Object byte[] ($stride * $Size)
        [System.Runtime.InteropServices.Marshal]::Copy($lock.Scan0, $bytes, 0, $bytes.Length)
        $bmp.UnlockBits($lock)

        $out = New-Object byte[] ($Size * $Size * 4)
        for ($y = 0; $y -lt $Size; $y++) {
            $sRow = $y * $stride
            for ($x = 0; $x -lt $Size; $x++) {
                $s = $sRow + $x * 4
                $o = ($y * $Size + $x) * 4
                $out[$o]     = $bytes[$s + 2]   # R
                $out[$o + 1] = $bytes[$s + 1]   # G
                $out[$o + 2] = $bytes[$s]       # B
                $out[$o + 3] = $bytes[$s + 3]   # A
            }
        }
        return , $out
    }

    # ------------------------------------------------------------- نوشتن PNG

    foreach ($s in ($PngSizes | Sort-Object)) {
        $p = Join-Path $pngDir "alvand-$s.png"
        Save-Png $s $p
        Write-Host ("  png  {0,4}px -> {1}  ({2} bytes)" -f $s, (Split-Path -Leaf $p), (Get-Item $p).Length)
    }

    # ------------------------------------------------------------- نوشتن ICO

    # فایل .ico دستی نوشته می‌شود چون .NET راه ساده‌ای برای چند اندازه ندارد.
    # قالب کلاسیک: هدر ۶ بایتی + رکورد ۱۶ بایتی برای هر اندازه + دادهٔ
    # ۳۲ بیتی BGRA به‌همراه ماسک AND.
    $entries = @()
    foreach ($s in ($IcoSizes | Sort-Object)) {
        $data = Get-RgbaBytes $s

        $xor = New-Object byte[] ($s * $s * 4)
        $andStride = [int]([math]::Ceiling($s / 32.0) * 4)
        $and = New-Object byte[] ($andStride * $s)

        # دادهٔ ICO از پایین به بالا ذخیره می‌شود
        for ($y = 0; $y -lt $s; $y++) {
            $dy = $s - 1 - $y
            for ($x = 0; $x -lt $s; $x++) {
                $si = ($y * $s + $x) * 4
                $di = ($dy * $s + $x) * 4
                $xor[$di]     = $data[$si + 2]   # B
                $xor[$di + 1] = $data[$si + 1]   # G
                $xor[$di + 2] = $data[$si]       # R
                $xor[$di + 3] = $data[$si + 3]   # A
            }
        }

        # ماسک AND: بیت ۱ یعنی پیکسل کاملاً شفاف است
        for ($y = 0; $y -lt $s; $y++) {
            $row = $y * $andStride
            for ($x = 0; $x -lt $s; $x++) {
                $a = $data[($y * $s + $x) * 4 + 3]
                if ($a -lt 128) {
                    $byte = $x -shr 5
                    $bit = 7 - ($x -band 7)
                    $and[$row + $byte] = $and[$row + $byte] -bor (1 -shl $bit)
                }
            }
        }

        $dib = New-Object System.IO.MemoryStream
        $bw = New-Object System.IO.BinaryWriter($dib)
        $bw.Write([int]40)          # biSize
        $bw.Write([int]$s)           # biWidth
        $bw.Write([int]($s * 2))     # biHeight: XOR و AND روی هم
        $bw.Write([int16]1)          # biPlanes
        $bw.Write([int16]32)         # biBitCount
        $bw.Write([int]0)            # biCompression = BI_RGB
        $bw.Write([int]($xor.Length + $and.Length))
        $bw.Write([int]0); $bw.Write([int]0); $bw.Write([int]0); $bw.Write([int]0)
        $bw.Write($xor)
        $bw.Write($and)
        $bw.Flush()
        $entries += , [pscustomobject]@{ Size = $s; Data = $dib.ToArray() }
        $dib.Dispose()
    }

    $ms = New-Object System.IO.MemoryStream
    $w = New-Object System.IO.BinaryWriter($ms)
    $w.Write([int16]0)   # reserved
    $w.Write([int16]1)   # type = icon
    $w.Write([int16]$entries.Count)

    $offset = 6 + 16 * $entries.Count
    foreach ($e in $entries) {
        $dim = if ($e.Size -ge 256) { [byte]0 } else { [byte]$e.Size }
        $w.Write($dim)                                  # biWidth (0 یعنی ۲۵۶)
        $w.Write($dim)                                  # biHeight
        $w.Write([byte]0)                               # رنگ‌ها
        $w.Write([byte]0)                               # رزرو
        $w.Write([int16]1)                              # planes
        $w.Write([int16]32)                             # bitCount
        $w.Write([int]$e.Data.Length)
        $w.Write([int]$offset)
        $offset += $e.Data.Length
    }
    foreach ($e in $entries) { $w.Write($e.Data) }
    $w.Flush()

    $icoPath = Join-Path $icoDir 'alvand.ico'
    [System.IO.File]::WriteAllBytes($icoPath, $ms.ToArray())
    $w.Dispose(); $ms.Dispose()

    Write-Host ("  ico  {0} sizes -> {1}  ({2} bytes)" -f $IcoSizes.Count, (Split-Path -Leaf $icoPath), (Get-Item $icoPath).Length)

    foreach ($b in $cache.Values) { $b.Dispose() }
    $crop.Dispose()
    Write-Host 'done'
}
finally {
    $src.Dispose()
}
