# StorageIQ - Automated Release Bundle Generator
# Usage: .\build-release.ps1

Write-Host "=============================================" -ForegroundColor Cyan
Write-Host " StorageIQ - Building Production App Bundle  " -ForegroundColor Cyan
Write-Host "=============================================" -ForegroundColor Cyan

# 1. Check for local.properties or ANDROID_HOME
if (-not (Test-Path ".\local.properties")) {
    if ($env:ANDROID_HOME) {
        $cleanPath = $env:ANDROID_HOME.Replace('\', '\\')
        "sdk.dir=$cleanPath" | Out-File -FilePath ".\local.properties" -Encoding ascii
        Write-Host "[OK] Generated local.properties from ANDROID_HOME: $env:ANDROID_HOME" -ForegroundColor Green
    } elseif (Test-Path "C:\Android") {
        "sdk.dir=C\:\\Android" | Out-File -FilePath ".\local.properties" -Encoding ascii
        Write-Host "[OK] Generated local.properties pointing to C:\Android" -ForegroundColor Green
    } else {
        Write-Host "[!] Warning: No Android SDK detected. Please ensure C:\Android exists or set ANDROID_HOME." -ForegroundColor Yellow
    }
}

# 2. Check for keystore.properties
if (-not (Test-Path ".\keystore.properties")) {
    Write-Host "[!] Note: keystore.properties not found. Release bundle will be unsigned." -ForegroundColor Yellow
    Write-Host "    To sign automatically, copy keystore.properties.example -> keystore.properties" -ForegroundColor Yellow
} else {
    Write-Host "[OK] keystore.properties found. Bundle will be digitally signed for Google Play." -ForegroundColor Green
}

# 3. Execute Gradle bundleRelease
Write-Host ""
Write-Host "--> Starting Gradle bundleRelease..." -ForegroundColor Cyan
cmd.exe /c "gradlew.bat bundleRelease"

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "============================================================" -ForegroundColor Green
    Write-Host " [SUCCESS] Release Bundle Created Successfully!            " -ForegroundColor Green
    Write-Host "============================================================" -ForegroundColor Green
    $bundlePath = ".\app\build\outputs\bundle\release\app-release.aab"
    if (Test-Path $bundlePath) {
        $fileInfo = Get-Item $bundlePath
        $sizeMB = [math]::Round($fileInfo.Length / 1MB, 2)
        Write-Host "Location: $($fileInfo.FullName)" -ForegroundColor White
        Write-Host "Size: $sizeMB MB" -ForegroundColor White
        Write-Host ""
        Write-Host "You can now upload this .aab file directly to Google Play Console!" -ForegroundColor Cyan
    }
} else {
    Write-Host ""
    Write-Host "[ERROR] Build failed with exit code $LASTEXITCODE. Please check the logs above." -ForegroundColor Red
}
