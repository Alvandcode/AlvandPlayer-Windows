<#
.SYNOPSIS
    ساخت فایل‌های صوتی واقعی M4A و WMA با رمزگذار داخلی خود ویندوز.

.DESCRIPTION
    چرا این اسکریپت لازم است؟

    برای تست رمزگشایی، فایلِ *واقعی* لازم است — نه فایلی که خودمان بایت‌به‌بایت
    ساخته باشیم، چون آن‌طور خطاهای کتابخانهٔ رمزگشایی پنهان می‌ماند. ولی روی این
    ماشین نه ffmpeg هست و نه هیچ فایل M4A/WMA آماده‌ای.

    راه‌حل: از `Windows.Media.Transcoding.MediaTranscoder` استفاده می‌کنیم که همان
    رمزگذاری است که Windows Media Player استفاده می‌کند. پس خروجی، یک فایل واقعیِ
    کاملاً استاندارد است — دقیقاً همان چیزی که کاربر در میدان دارد.

    ورودی: یک WAV که خودمان می‌سازیم (سینوس + لایهٔ آکارد).
    خروجی: M4A/AAC ۱۹۲kbps و WMA ۱۹۲kbps.

.EXAMPLE
    .\tools\make-test-audio.ps1 -OutDir build\test-audio
#>
[CmdletBinding()]
param(
    [string]$OutDir = '',
    [int]$SampleRate = 44100,
    [int]$DurationSeconds = 6
)

$ErrorActionPreference = 'Stop'
if (-not $OutDir) { $OutDir = Join-Path (Get-Location) 'build\test-audio' }
$OutDir = [IO.Path]::GetFullPath($OutDir)
New-Item -ItemType Directory -Force -Path $OutDir | Out-Null

# ---------- ۱) ساخت WAV منبع ----------

function New-TestWav {
    param([string]$Path, [int]$Rate, [int]$Seconds, [double]$Freq = 440.0)

    $samples = $Rate * $Seconds
    $pcm = New-Object byte[] ($samples * 2)   # ۱۶ بیت مونو

    for ($i = 0; $i -lt $samples; $i++) {
        $t = $i / [double]$Rate
        # سینوس اصلی + هارمونیک دوم تا رمزگذار مجبور به کار واقعی شود
        $v = [math]::Sin(2 * [math]::PI * $Freq * $t) * 0.42 +
             [math]::Sin(2 * [math]::PI * $Freq * 2 * $t) * 0.18
        # پوشِ آرام ابتدا/انتها تا کلیک نداشته باشیم
        $fade = [math]::Min(1.0, [math]::Min($t / 0.05, ($Seconds - $t) / 0.35))
        $s = [int][math]::Round($v * $fade * 32767)
        if ($s -gt 32767) { $s = 32767 } elseif ($s -lt -32768) { $s = -32768 }
        $b = [BitConverter]::GetBytes([int16]$s)
        $pcm[$i * 2] = $b[0]
        $pcm[$i * 2 + 1] = $b[1]
    }

    $ms = New-Object IO.MemoryStream
    $bw = New-Object IO.BinaryWriter($ms)
    $dataLen = $pcm.Length
    $byteRate = $Rate * 2
    $bw.Write([Text.Encoding]::ASCII.GetBytes('RIFF'))
    $bw.Write([int](36 + $dataLen))
    $bw.Write([Text.Encoding]::ASCII.GetBytes('WAVEfmt '))
    $bw.Write([int]16)          # اندازهٔ بلوک fmt
    $bw.Write([int16]1)         # PCM
    $bw.Write([int16]1)         # مونو
    $bw.Write([int]$Rate)
    $bw.Write([int]$byteRate)
    $bw.Write([int16]2)         # block align
    $bw.Write([int16]16)        # bits per sample
    $bw.Write([Text.Encoding]::ASCII.GetBytes('data'))
    $bw.Write([int]$dataLen)
    $bw.Write($pcm)
    $bw.Flush()
    [IO.File]::WriteAllBytes($Path, $ms.ToArray())
    $bw.Dispose(); $ms.Dispose()
}

$wav = Join-Path $OutDir 'source.wav'
New-TestWav -Path $wav -Rate $SampleRate -Seconds $DurationSeconds
Write-Host ("source wav: {0} ({1:N1} KB)" -f $wav, ((Get-Item $wav).Length / 1KB))

# ---------- ۲) راه‌اندازی WinRT در PowerShell 5.1 ----------

Add-Type -AssemblyName System.Runtime.WindowsRuntime | Out-Null

$asTaskGeneric = ([System.WindowsRuntimeSystemExtensions].GetMethods() | Where-Object {
    $_.Name -eq 'AsTask' -and $_.GetParameters().Count -eq 1 -and
    $_.GetParameters()[0].ParameterType.Name -eq 'IAsyncOperation`1'
})[0]

$asTaskAction = ([System.WindowsRuntimeSystemExtensions].GetMethods() | Where-Object {
    $_.Name -eq 'AsTask' -and $_.GetParameters().Count -eq 1 -and
    $_.GetParameters()[0].ParameterType.Name -eq 'IAsyncAction'
})[0]

function Await($op, $type) {
    $task = $asTaskGeneric.MakeGenericMethod($type).Invoke($null, @($op))
    $task.Wait(-1) | Out-Null
    $task.Result
}

function AwaitAction($action) {
    $task = $asTaskAction.Invoke($null, @($action))
    $task.Wait(-1) | Out-Null
}

# کلاس‌های WinRT را با بارگذاری نوع فعال می‌کنیم
$null = [Windows.Media.Transcoding.MediaTranscoder, Windows.Media.Transcoding, ContentType = WindowsRuntime]
$null = [Windows.Storage.StorageFile, Windows.Storage, ContentType = WindowsRuntime]
$null = [Windows.Storage.Streams.IRandomAccessStream, Windows.Storage.Streams, ContentType = WindowsRuntime]
$null = [Windows.Media.Transcodes.MediaEncodingProfile, Windows.Media.Transcodes, ContentType = WindowsRuntime]
$null = [Windows.Media.Transcoding.AudioEncoderEncodingProfile, Windows.Media.Transcoding, ContentType = WindowsRuntime]

function Convert-WithWindows {
    <# ساخت خروجی با رمزگذار داخلی ویندوز. profile_name یا 'aac' است یا 'wma' #>
    param(
        [string]$InPath,
        [string]$OutPath,
        [ValidateSet('aac', 'wma')][string]$Kind,
        [int]$Bitrate = 192000
    )

    $inFile = Await ([Windows.Storage.StorageFile]::GetFileFromPathAsync($InPath)) ([Windows.Storage.StorageFile])
    $outFile = Await ([Windows.Storage.StorageFile]::CreateFileAsync(
        $OutPath,
        [Windows.Storage.CreationCollisionOption]::ReplaceExisting)) ([Windows.Storage.StorageFile])
    $stream = Await ($outFile.OpenAsync([Windows.Storage.FileAccessMode]::ReadWrite)) ([Windows.Storage.Streams.IRandomAccessStream])

    $profile = if ($Kind -eq 'aac') {
        [Windows.Media.Transcodes.MediaEncodingProfile]::CreateM4a(
            [Windows.Media.Transcoding.AudioEncoderEncodingProfile]::CreateAac(
                [Windows.Media.Transcoding.AudioEncoderEncodingFormat]::Auto, $Bitrate, 2))
    } else {
        [Windows.Media.Transcodes.MediaEncodingProfile]::CreateWma(
            [Windows.Media.Transcoding.AudioEncoderEncodingProfile]::CreateWma(
                [Windows.Media.Transcoding.AudioEncoderEncodingFormat]::Auto, $Bitrate))
    }

    $t = New-Object Windows.Media.Transcoding.MediaTranscoder
    $t.OutputStream = $stream

    $opts = New-Object Windows.Media.Transcoding.MediaTranscodeOptions
    $opts.OutputFileFormat = $profile

    $prep = Await ($t.PrepareFileTranscodeAsync($inFile, $opts)) ([Windows.Media.Transcoding.PrepareTranscodeResult])
    if ($prep.Status -ne [Windows.Media.Transcoding.TranscodeFailureReason]::None) {
        throw "PrepareFileTranscodeAsync failed: $($prep.Status)"
    }

    AwaitAction ($prep.TranscodeAsync())
    $stream.Dispose()
}

# ---------- ۳) تبدیل ----------

foreach ($kind in @('aac', 'wma')) {
    $ext = if ($kind -eq 'aac') { 'm4a' } else { 'wma' }
    $out = Join-Path $OutDir "sample.$ext"
    try {
        Convert-WithWindows -InPath $wav -OutPath $out -Kind $kind
        $len = (Get-Item $out).Length
        if ($len -lt 2000) { throw "output suspiciously small: $len bytes" }
        Write-Host ("  {0,-5} -> {1}  ({2:N1} KB)" -f $kind, $out, ($len / 1KB))
    } catch {
        Write-Warning "  $kind failed: $($_.Exception.Message)"
        if (Test-Path $out) { Remove-Item $out -Force }
    }
}

Write-Host ''
Write-Host "fixtures in $OutDir"
Get-ChildItem $OutDir | ForEach-Object { "  {0,-14} {1,8:N1} KB" -f $_.Name, ($_.Length / 1KB) }
