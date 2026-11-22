<#
.SYNOPSIS
    ساخت نصب‌کنندهٔ MSI با WiX، بدون عبور از jpackage.

.DESCRIPTION
    چرا این مسیر؟ `jpackage --type msi` و `--type exe` روی این محیط هر دو با خطای
    «candle.exe … exited with ?? code» می‌شکنند. خودِ WiX سالم است (با اجرای
    مستقیم candle تأیید شد)، ولی `bundle.wxf` تولیدشدهٔ jpackage کامپایل نمی‌شود.

    این اسکریپت همان کار را با ابزارهای خود WiX انجام می‌دهد:

      1. heat   → فهرست فایل‌های app-image را تولید می‌کند
      2. candle → فایل‌های .wxs را به .wixobj کامپایل می‌کند
      3. light  → از .wixobj ها فایل MSI نهایی را می‌سازد

    خروجی: `desktopApp\build\compose\binaries\main\msi\AlvandPlayer-1.0.0.msi`

.EXAMPLE
    .\tools\package-msi.ps1
#>
[CmdletBinding()]
param(
    [string]$ProjectRoot = '',
    [string]$Version = ''
)

$ErrorActionPreference = 'Stop'

if (-not $ProjectRoot) {
    $scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
    $ProjectRoot = Split-Path -Parent $scriptDir
}

# ------------------------------------------------- نسخه: تنها منبعِ حقیقت

# نسخه از `desktopApp/build.gradle.kts` خوانده می‌شود، نه از این اسکریپت. قبلاً
# هر دو جا نوشته شده بود و همین یعنی می‌شد نام MSI یک نسخه باشد ولی داخلش نسخهٔ
# دیگری نصب شود.
$gradleFile = Join-Path $ProjectRoot 'desktopApp/build.gradle.kts'
if (-not $Version) {
    if (-not (Test-Path -LiteralPath $gradleFile)) { throw "not found: $gradleFile" }
    $m = Select-String -Path $gradleFile -Pattern 'packageVersion\s*=\s*"([^"]+)"'
    if (-not $m) { throw "packageVersion not found in $gradleFile" }
    $Version = $m.Matches[0].Groups[1].Value
}

# Windows Installer فقط سه بخش (major.minor.build) می‌پذیرد.
$parts = $Version.Split('.')
if ($parts.Count -gt 3) {
    $Version = ($parts[0..2] -join '.')
    Write-Warning "version truncated to 3 parts for the MSI: $Version"
}
if ($parts.Count -lt 3) {
    # نسخهٔ MSI باید ۳ جزء داشته باشد؛ با «.0» کامل می‌کنیم — بدون ترفند ضرب
    # رشته در عدد (که در پاورشل یعنی تکرار رشته، نه جمع عددی: "1" => "1.01.0")
    $Version = (($parts + @('0') * (3 - $parts.Count)) -join '.')
    Write-Warning "version padded to 3 parts for the MSI: $Version"
}

# ------------------------------------- ProductCode قطعی بر پایهٔ نسخه

# با `Id="*"` در WiX، هر build یک GUID تازه می‌سازد و دو MSI با نسخهٔ یکسان به دو
# محصول متفاوت تبدیل می‌شوند. اینجا یک UUID نسخهٔ ۵ (SHA-1) از UpgradeCode به‌علاوهٔ
# شمارهٔ نسخه می‌سازیم، پس خروجی کاملاً قطعی است:
#   - همان نسخه ⇒ همان کد ⇒ نص�� دوباره درست کار می‌کند
#   - نسخهٔ تازه ⇒ کد تازه ⇒ MajorUpgrade نسخهٔ قبلی را جایگزین می‌کند
$wxsText = Get-Content -LiteralPath (Join-Path $ProjectRoot 'tools/alvand.wxs') -Raw
$ucMatch = [regex]::Match($wxsText, 'UpgradeCode\s*=\s*"([0-9a-fA-F-]+)"')
if (-not $ucMatch.Success) { throw 'UpgradeCode not found in tools/alvand.wxs' }
$upgradeCode = $ucMatch.Groups[1].Value

function New-DeterministicGuid {
    param([string]$Namespace, [string]$Name)
    $nsBytes = [Guid]::Parse($Namespace).ToByteArray()
    $nameBytes = [Text.Encoding]::UTF8.GetBytes($Name)
    $sha = [Security.Cryptography.SHA1]::Create()
    try { $hash = $sha.ComputeHash($nsBytes + $nameBytes) } finally { $sha.Dispose() }

    $b = New-Object byte[] 16
    [Array]::Copy($hash, 0, $b, 0, 16)
    $b[6] = [byte](($b[6] -band 0x0F) -bor 0x50)   # نسخهٔ ۵
    $b[8] = [byte](($b[8] -band 0x3F) -bor 0x80)   # متغیر RFC 4122
    return ([Guid]::new($b)).ToString()
}

$ProductCode = New-DeterministicGuid -Namespace $upgradeCode -Name "AlvandPlayer-$Version"
Write-Host "version:       $Version"
Write-Host "UpgradeCode:   $upgradeCode  (constant, never change)"
Write-Host "ProductCode:   $ProductCode  (derived from version)"

# ---------------------------------------------------------------- مسیرها

$wixDir = Join-Path $ProjectRoot 'build/wix311'
$appImage = Join-Path $ProjectRoot 'desktopApp/build/compose/binaries/main/app/AlvandPlayer'
$outDir = Join-Path $ProjectRoot 'desktopApp/build/compose/binaries/main/msi'
$workDir = Join-Path $ProjectRoot 'build/msi-work'
$mainWxs = Join-Path $ProjectRoot 'tools/alvand.wxs'
$exePath = Join-Path $appImage 'AlvandPlayer.exe'
$iconPath = Join-Path $ProjectRoot 'desktopApp/icons/alvand.ico'

foreach ($p in @($wixDir, $appImage, $mainWxs, $exePath, $iconPath)) {
    if (-not (Test-Path -LiteralPath $p)) { throw "not found: $p" }
}
foreach ($tool in @('heat.exe', 'candle.exe', 'light.exe')) {
    if (-not (Test-Path (Join-Path $wixDir $tool))) { throw "WiX tool missing: $tool" }
}

New-Item -ItemType Directory -Force -Path $outDir, $workDir | Out-Null
Get-ChildItem $workDir -File -ErrorAction SilentlyContinue | Remove-Item -Force

Push-Location $workDir
try {
    $files = (Get-ChildItem $appImage -Recurse -File).Count
    $sizeMb = [math]::Round(((Get-ChildItem $appImage -Recurse -File | Measure-Object Length -Sum).Sum / 1MB), 1)
    Write-Host "app-image: $files files, $sizeMb MB"

    # ------------------------------------------------- 1) فهرست فایل‌ها با heat
    Write-Host 'heat: harvesting files...'
    $harvestPath = Join-Path $workDir 'harvest.wxs'
    $harvest = & (Join-Path $wixDir 'heat.exe') dir $appImage `
        -cg AppFiles `
        -dr INSTALLFOLDER `
        -var var.PublishDir `
        -sfrag `
        -srd `
        -sreg `
        -scom `
        -gg `
        -g1 `
        -suid `
        -out $harvestPath 2>&1
    if ($LASTEXITCODE -ne 0) {
        $harvest | Select-Object -First 12 | ForEach-Object { Write-Host "  $_" }
        throw "heat failed with exit code $LASTEXITCODE"
    }
    if (-not (Test-Path $harvestPath)) { throw 'heat produced no harvest.wxs' }
    Write-Host ("  harvest.wxs: {0:N0} bytes" -f (Get-Item $harvestPath).Length)

    # ------------------------------------- 2) قابل‌حمل کردن فایل فهرست
    # خروجی heat خودش یک سند کامل WiX است (پس نباید دوباره در یک <Wix> دیگر
    # پیچیده شود، چون دو declaration داشتن خطای XML می‌دهد). فقط مسیر مطلقِ
    # app-image را به متغیر تبدیل می‌کنیم تا با جابه‌جایی پروژه نشکند.
    $combined = Join-Path $workDir 'product.wxs'
    $harvestText = Get-Content $harvestPath -Raw
    $escaped = $appImage.Replace('\', '\\')
    $harvestText = $harvestText.Replace("`"$escaped`"", '$(var.PublishDir)')
    [System.IO.File]::WriteAllText($combined, $harvestText, (New-Object System.Text.UTF8Encoding($false)))
    Write-Host "combined: product.wxs (from heat, path made relocatable)"

    # ------------------------------------------------------- 3) candle
    # هر فایل جدا کامپایل می‌شود چون candle با یک `-out` پوشه‌ای و مسیر دارای
    # فاصله (مثل «Music Player Windows») در PowerShell به‌هم می‌ریزد.
    Write-Host 'candle: compiling...'
    $candleExe = Join-Path $wixDir 'candle.exe'

    $defs = @(
        "-dPublishDir=$appImage"
        "-dIconPath=$iconPath"
        "-dProductVersion=$Version"
        "-dProductCode=$ProductCode"
        "-dLicensePath=$(Join-Path $ProjectRoot 'tools/license.rtf')"
    )

    $compile = @(
        @{ Source = $mainWxs; Out = (Join-Path $workDir 'alvand.wixobj') }
        @{ Source = $combined; Out = (Join-Path $workDir 'product.wixobj') }
    )

    $wixobj = @()
    foreach ($item in $compile) {
        $args = @('-nologo', '-arch', 'x64', '-ext', 'WixUtilExtension') +
                $defs + @('-out', $item.Out, $item.Source)
        $candleOut = & $candleExe @args 2>&1
        if ($LASTEXITCODE -ne 0) {
            $candleOut | Select-Object -First 20 | ForEach-Object { Write-Host "  $_" }
            throw "candle failed on $(Split-Path $item.Source -Leaf) with exit code $LASTEXITCODE"
        }
        if (-not (Test-Path $item.Out)) { throw "candle produced no output for $($item.Source)" }
        $wixobj += $item.Out
    }
    Write-Host ("  objects: {0}" -f (($wixobj | ForEach-Object { Split-Path $_ -Leaf }) -join ', '))

    # -------------------------------------------------------- 4) light
    Write-Host 'light: linking msi...'
    $msiName = "AlvandPlayer-$Version.msi"
    $msiPath = Join-Path $outDir $msiName
    $lightOut = & (Join-Path $wixDir 'light.exe') -nologo `
        -ext WixUtilExtension `
        -ext WixUIExtension `
        -out $msiPath `
        $wixobj 2>&1
    if ($LASTEXITCODE -ne 0) {
        $lightOut | Select-Object -First 20 | ForEach-Object { Write-Host "  $_" }
        throw "light failed with exit code $LASTEXITCODE"
    }
    if (-not (Test-Path $msiPath)) { throw 'light produced no MSI' }

    $size = [math]::Round((Get-Item $msiPath).Length / 1MB, 1)
    Write-Host ''
    Write-Host "  MSI: $msiPath  ($size MB)"
}
finally {
    Pop-Location
}
