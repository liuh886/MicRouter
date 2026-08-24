# One-command debug APK build
# Usage: .\build-apk.ps1   (from android-app folder)
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.20.8-hotspot"
& "C:\Gradle\gradle-8.7\bin\gradle.bat" assembleDebug --no-daemon $args
Write-Host ""
Write-Host "APK: $PSScriptRoot\app\build\outputs\apk\debug\app-debug.apk"
Write-Host "Install: & `$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe install -r app\build\outputs\apk\debug\app-debug.apk"
