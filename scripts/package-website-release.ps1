# Builds release APK and prepares website/downloads + updates downme/index.html metadata.
# Run from repo root:  .\scripts\package-website-release.ps1
# Or from workspace root:  cd swiftsave-android; .\scripts\package-website-release.ps1

$ErrorActionPreference = "Stop"
$RepoRoot = Split-Path $PSScriptRoot -Parent
$WorkspaceRoot = Split-Path $RepoRoot -Parent
$GradleRoot = if (Test-Path (Join-Path $WorkspaceRoot "gradlew.bat")) { $WorkspaceRoot } else { $RepoRoot }

$BuildGradle = Join-Path $RepoRoot "app\build.gradle.kts"
if (-not (Test-Path $BuildGradle)) { throw "app/build.gradle.kts not found at $BuildGradle" }

$versionName = "1.0.0"
$versionCode = 1
$gradleText = Get-Content $BuildGradle -Raw
if ($gradleText -match 'versionName\s*=\s*"([^"]+)"') { $versionName = $Matches[1] }
if ($gradleText -match 'versionCode\s*=\s*(\d+)') { $versionCode = [int]$Matches[1] }

$ApkOutName = "DownMe-$versionName-arm64.apk"
$WebsiteDownloads = Join-Path $RepoRoot "website\downloads"
$IndexHtml = Join-Path $RepoRoot "website\downme\index.html"
New-Item -ItemType Directory -Force -Path $WebsiteDownloads | Out-Null

$keystore = Join-Path $RepoRoot "keystore.properties"
if (-not (Test-Path $keystore)) { $keystore = Join-Path $WorkspaceRoot "keystore.properties" }
Write-Host "Building release APK (version $versionName)..."
Push-Location $GradleRoot
try {
    if (Test-Path $keystore) {
        & .\gradlew.bat :app:assembleRelease --no-daemon
    } else {
        & .\gradlew.bat :app:assembleRelease "-PallowDebugReleaseSigning=true" --no-daemon
    }
    if ($LASTEXITCODE -ne 0) { throw "Gradle assembleRelease failed" }
} finally {
    Pop-Location
}

$srcApk = Join-Path $RepoRoot "app\build\outputs\apk\release\app-arm64-v8a-release.apk"
if (-not (Test-Path $srcApk)) {
    $srcApk = Join-Path $GradleRoot "swiftsave-android\app\build\outputs\apk\release\app-arm64-v8a-release.apk"
}
if (-not (Test-Path $srcApk)) { throw "Release APK not found. Expected under app/build/outputs/apk/release/" }

$destApk = Join-Path $WebsiteDownloads $ApkOutName
Copy-Item -Path $srcApk -Destination $destApk -Force
$hash = Get-FileHash -Path $destApk -Algorithm SHA256
$sizeMb = [math]::Round((Get-Item $destApk).Length / 1MB, 1)
$builtAt = (Get-Date).ToUniversalTime().ToString("yyyy-MM-dd HH:mm") + " UTC"

$info = @{
    app          = "DownMe"
    versionCode  = $versionCode
    versionName  = $versionName
    apkFile      = $ApkOutName
    sizeMb       = $sizeMb
    sha256       = $hash.Hash
    builtAt      = $builtAt
    downloadPath = "/downloads/$ApkOutName"
} | ConvertTo-Json
$info | Set-Content -Path (Join-Path $WebsiteDownloads "release-info.json") -Encoding UTF8

if (Test-Path $IndexHtml) {
    $html = Get-Content $IndexHtml -Raw -Encoding UTF8
    $html = $html -replace '(?<=data-version=")[^"]*', $versionName
    $html = $html -replace '(?<=data-apk-file=")[^"]*', $ApkOutName
    $html = $html -replace '(?<=data-sha256=")[^"]*', $hash.Hash
    $html = $html -replace '(?<=data-size-mb=")[^"]*', "$sizeMb"
    $html | Set-Content -Path $IndexHtml -Encoding UTF8 -NoNewline
}

Write-Host ""
Write-Host "Done."
Write-Host "  APK:     $destApk"
Write-Host "  Size:    $sizeMb MB"
Write-Host "  SHA256:  $($hash.Hash)"
Write-Host ""
Write-Host "Upload to tefarab.xyz:"
Write-Host "  1) website/downme/     ->  https://www.tefarab.xyz/downme/"
Write-Host "  2) website/downloads/   ->  https://www.tefarab.xyz/downloads/"
Write-Host "See WEBSITE_DEPLOY.md for your checklist."
