<#
.SYNOPSIS
    تولید فایل آیکون‌های پخش از روی سورس رسمی Material Icons.

.DESCRIPTION
    چرا این اسکریپت؟ بستهٔ `material-icons-extended` **۳۶ مگابایت** است در حالی که
    پروژه فقط ۷ آیکون از آن استفاده می‌کند — یعنی ۲۲٪ کل حجم بستهٔ نهایی.

    این اسکریپت path هر آیکون را **عیناً از سورس رسمی** (کتابخانهٔ Material Icons
    از Android Open Source Project، لایسنس Apache-2.0) بیرون می‌کشد و در یک فایل
    واحد داخل پروژه می‌گذارد. نتیجه: آیکون‌ها **پیکسل‌به‌پیکسل** یکسان می‌مانند
    و وابستگی ۳۶ مگابایتی حذف می‌شود.

    باقی می‌ماند فقط `material-icons-core` (۰٫۸۴ مگابایت) چون توابع
    `materialIcon`/`materialPath` از آن می‌آیند.

.EXAMPLE
    .\tools\generate-transport-icons.ps1
#>
[CmdletBinding()]
param(
    [string]$ProjectRoot = '',
    [string]$ExtractRoot = "$env:TEMP\alvand-icons",
    [string]$MaterialVersion = '1.7.3'
)

$ErrorActionPreference = 'Stop'

if (-not $ProjectRoot) {
    $ProjectRoot = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
}

# آیکون‌هایی که پروژه استفاده می‌کند و از کدام artifact می‌آیند
$wanted = [ordered]@{
    'Pause'       = 'extended'
    'PlayArrow'   = 'core'
    'Shuffle'     = 'extended'
    'Repeat'      = 'extended'
    'RepeatOne'   = 'extended'
    'SkipNext'    = 'extended'
    'SkipPrevious'= 'extended'
    'Favorite'    = 'core'
    'FavoriteBorder' = 'core'
    # shell ناوبری دسکتاپ (بخش‌بخش از روی طرح کاربر)
    'Home'        = 'core'
    'Settings'    = 'core'
    'Search'      = 'core'
    'KeyboardArrowRight' = 'core'
    'LibraryMusic' = 'extended'
    'QueueMusic'  = 'extended'
    'Explore'     = 'extended'
    'Radio'       = 'extended'
    'Equalizer'   = 'extended'
    'GraphicEq'   = 'extended'
    'Tune'        = 'extended'
    'Lyrics'      = 'extended'
    'KeyboardArrowUp' = 'core'
    'KeyboardArrowDown' = 'core'
}

[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12

function Get-IconSources {
    param([string]$Artifact)

    $dir = Join-Path $ExtractRoot $Artifact
    if (Test-Path $dir) { return $dir }

    $group = if ($Artifact -eq 'core') {
        'org/jetbrains/compose/material/material-icons-core-desktop'
    } else {
        'org/jetbrains/compose/material/material-icons-extended-desktop'
    }
    $name = "material-icons-$Artifact-desktop"
    $url = "https://repo1.maven.org/maven2/$group/$MaterialVersion/$name-$MaterialVersion-sources.jar"
    $jar = Join-Path $ExtractRoot "$name-sources.jar"

    New-Item -ItemType Directory -Force -Path $ExtractRoot, $dir | Out-Null
    Write-Host "downloading $(Split-Path $url -Leaf)..."
    Invoke-WebRequest -Uri $url -OutFile $jar -TimeoutSec 300 -UseBasicParsing
    Write-Host ("  {0:N1} MB" -f ((Get-Item $jar).Length / 1MB))

    Push-Location $dir
    try { & "$env:JAVA_HOME\bin\jar.exe" xf $jar } finally { Pop-Location }
    return $dir
}

function Get-PathBody {
    <# متن داخل materialPath { ... } را برمی‌گرداند #>
    param([string]$Path)
    $text = Get-Content -LiteralPath $Path -Raw
    $m = [regex]::Match($text, 'materialPath\s*\{(?<body>.*?)\n\s{8}\}\r?\n\s{4}\}', 'Singleline')
    if (-not $m.Success) {
        $m = [regex]::Match($text, 'materialPath\s*\{(?<body>.*?)\}', 'Singleline')
    }
    if (-not $m.Success) { throw "no materialPath block found in $Path" }
    return $m.Groups['body'].Value.TrimEnd()
}

$roots = @{
    extended = Get-IconSources 'extended'
    core     = Get-IconSources 'core'
}

$sb = New-Object System.Text.StringBuilder

$header = @'
package com.alvand.player.ui.icons

/*
 * آیکون‌های کنترل پخش.
 *
 * ## چرا این‌ها اینجا هستند و نه از کتابخانه
 *
 * بستهٔ `material-icons-extended` **۳۶ مگابایت** است، در حالی که کل برنامه فقط
 * ۷ آیکون از آن می‌گیرد — یعنی حدود ۲۲٪ حجم بستهٔ نهایی صرف چند آیکون می‌شد.
 *
 * این pathها **عیناً و بدون هیچ تغییری** از سورس رسمی Material Icons کپی شده‌اند
 * (Android Open Source Project، لایسنس Apache-2.0). یعنی شکل و ضخامت و اندازهٔ
 * آیکون‌ها روی صفحه **دقیقاً همان** چیزی است که در نسخهٔ قبلی دیده می‌شد؛ فقط
 * دیگر کل آن کتابخانه همراه برنامه توزیع نمی‌شود.
 *
 * اگر خواستید آیکون‌ها را عوض کنید، همین فایل تنها جایی است که لازم است تغییر
 * کند. برای ساخت دوباره‌اش از روی سورس رسمی:
 *
 *     .\tools\generate-transport-icons.ps1
 *
 * ## لایسنس
 *
 * Apache License 2.0 — متن کامل در LICENSE-APACHE-2.0.txt کنار همین فایل.
 */
'@

[void]$sb.AppendLine($header)
[void]$sb.AppendLine()
[void]$sb.AppendLine('import androidx.compose.material.icons.Icons')
[void]$sb.AppendLine('import androidx.compose.material.icons.materialIcon')
[void]$sb.AppendLine('import androidx.compose.material.icons.materialPath')
[void]$sb.AppendLine('import androidx.compose.ui.graphics.vector.ImageVector')
[void]$sb.AppendLine()

foreach ($name in $wanted.Keys) {
    $src = Get-ChildItem $roots[$wanted[$name]] -Recurse -Filter "$name.kt" -ErrorAction SilentlyContinue |
        Where-Object { $_.FullName -match 'icons[\\/]filled' } | Select-Object -First 1
    if (-not $src) { throw "source for '$name' not found in $($wanted[$name])" }

    $body = Get-PathBody $src.FullName
    $field = '_' + $name.ToLowerInvariant()

    [void]$sb.AppendLine("/**")
    [void]$sb.AppendLine(" * آیکون $name از Material Icons (Filled)، کپی‌شده از سورس رسمی.")
    [void]$sb.AppendLine(" */")
    [void]$sb.AppendLine("public val Icons.Filled.${name}: ImageVector")
    [void]$sb.AppendLine("    get() {")
    [void]$sb.AppendLine("        if ($field != null) {")
    [void]$sb.AppendLine("            return $field!!")
    [void]$sb.AppendLine("        }")
    [void]$sb.AppendLine("        $field = materialIcon(name = `"Filled.$name`") {")
    [void]$sb.AppendLine("            materialPath {")
    # بدنهٔ path را با تورفتگی همان سبک مرجع می‌نویسیم
    foreach ($line in ($body -split "`r?`n")) {
        $trimmed = $line.Trim()
        if ($trimmed) { [void]$sb.AppendLine('                ' + $trimmed) }
    }
    [void]$sb.AppendLine("            }")
    [void]$sb.AppendLine("        }")
    [void]$sb.AppendLine("        return $field!!")
    [void]$sb.AppendLine("    }")
    [void]$sb.AppendLine()
    [void]$sb.AppendLine("private var ${field}: ImageVector? = null")
    [void]$sb.AppendLine()
}

$outDir = Join-Path $ProjectRoot 'desktopApp/src/jvmMain/kotlin/com/alvand/player/ui/icons'
New-Item -ItemType Directory -Force -Path $outDir | Out-Null
$outFile = Join-Path $outDir 'TransportIcons.kt'
[System.IO.File]::WriteAllText($outFile, $sb.ToString(), (New-Object System.Text.UTF8Encoding($false)))

Write-Host ''
Write-Host ("wrote {0} ({1:N0} bytes)" -f $outFile, (Get-Item $outFile).Length)
Write-Host ("icons: {0}" -f (($wanted.Keys) -join ', '))
