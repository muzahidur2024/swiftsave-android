# Upload website/downme + website/downloads to tefarab.xyz via FTP.
# Set credentials once (PowerShell session or user env):
#   $env:TEFARAB_FTP_HOST = "ftp.tefarab.xyz"
#   $env:TEFARAB_FTP_USER = "your_cpanel_user"
#   $env:TEFARAB_FTP_PASS = "your_password"
#   $env:TEFARAB_FTP_REMOTE = "/public_html"   # optional; default public_html
#
# Usage:
#   cd swiftsave-android
#   .\scripts\package-website-release.ps1   # build first
#   .\scripts\deploy-website-ftp.ps1

param(
    [string]$FtpHost = $env:TEFARAB_FTP_HOST,
    [string]$FtpUser = $env:TEFARAB_FTP_USER,
    [string]$FtpPass = $env:TEFARAB_FTP_PASS,
    [string]$RemoteBase = $(if ($env:TEFARAB_FTP_REMOTE) { $env:TEFARAB_FTP_REMOTE } else { "/public_html" })
)

$ErrorActionPreference = "Stop"
$RepoRoot = Split-Path $PSScriptRoot -Parent

if (-not $FtpHost -or -not $FtpUser -or -not $FtpPass) {
    throw @"
Missing FTP credentials. Set before running:
  `$env:TEFARAB_FTP_HOST = 'ftp.tefarab.xyz'
  `$env:TEFARAB_FTP_USER = 'your_cpanel_username'
  `$env:TEFARAB_FTP_PASS = 'your_password'
Optional:
  `$env:TEFARAB_FTP_REMOTE = '/public_html'
"@
}

function Upload-FtpFile {
    param(
        [string]$LocalPath,
        [string]$RemotePath
    )
    if (-not (Test-Path $LocalPath)) { throw "Local file not found: $LocalPath" }
    $uri = "ftp://${FtpHost}${RemotePath}"
    Write-Host "Uploading $LocalPath -> $uri"
    $request = [System.Net.FtpWebRequest]::Create($uri)
    $request.Method = [System.Net.WebRequestMethods+Ftp]::UploadFile
    $request.Credentials = New-Object System.Net.NetworkCredential($FtpUser, $FtpPass)
    $request.UseBinary = $true
    $request.UsePassive = $true
    $request.KeepAlive = $false
    $bytes = [System.IO.File]::ReadAllBytes($LocalPath)
    $request.ContentLength = $bytes.Length
    $stream = $request.GetRequestStream()
    try {
        $stream.Write($bytes, 0, $bytes.Length)
    } finally {
        $stream.Close()
    }
    $response = $request.GetResponse()
    $response.Close()
}

function Ensure-FtpDirectory {
    param([string]$RemotePath)
    $uri = "ftp://${FtpHost}${RemotePath}"
    try {
        $request = [System.Net.FtpWebRequest]::Create($uri)
        $request.Method = [System.Net.WebRequestMethods+Ftp]::MakeDirectory
        $request.Credentials = New-Object System.Net.NetworkCredential($FtpUser, $FtpPass)
        $request.UsePassive = $true
        $response = $request.GetResponse()
        $response.Close()
    } catch {
        # Directory may already exist.
    }
}

$indexHtml = Join-Path $RepoRoot "website\downme\index.html"
$releaseInfo = Join-Path $RepoRoot "website\downloads\release-info.json"
if (-not (Test-Path $indexHtml)) { throw "Run package-website-release.ps1 first." }
if (-not (Test-Path $releaseInfo)) { throw "release-info.json missing. Run package-website-release.ps1 first." }

$apkFile = (Get-Content $releaseInfo -Raw | ConvertFrom-Json).apkFile
$apkPath = Join-Path $RepoRoot "website\downloads\$apkFile"
if (-not (Test-Path $apkPath)) { throw "APK not found: $apkPath" }

$remoteBase = $RemoteBase.TrimEnd("/")
Ensure-FtpDirectory "$remoteBase/downme"
Ensure-FtpDirectory "$remoteBase/downloads"

Upload-FtpFile -LocalPath $indexHtml -RemotePath "$remoteBase/downme/index.html"
Upload-FtpFile -LocalPath $releaseInfo -RemotePath "$remoteBase/downloads/release-info.json"
Upload-FtpFile -LocalPath $apkPath -RemotePath "$remoteBase/downloads/$apkFile"

Write-Host ""
Write-Host "Upload complete."
Write-Host "  Page:  https://www.tefarab.xyz/downme/"
Write-Host "  APK:   https://www.tefarab.xyz/downloads/$apkFile"
Write-Host "  Info:  https://www.tefarab.xyz/downloads/release-info.json"
