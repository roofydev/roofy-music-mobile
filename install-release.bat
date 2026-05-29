@echo off
setlocal

cd /d "%~dp0"

set "JAVA_HOME=C:\roofy-music-projects\.tools\temurin21\jdk-21.0.11+10"
set "PATH=%JAVA_HOME%\bin;%PATH%"

echo.
echo [Roofy Music] Checking connected Android devices...
where adb >nul 2>nul
if errorlevel 1 (
  echo ERROR: adb was not found on PATH.
  echo Install Android platform-tools or open this from an Android Studio terminal.
  exit /b 1
)

adb devices | findstr /R /C:"device$" >nul
if errorlevel 1 (
  echo ERROR: No authorized Android device found.
  echo Enable USB debugging, connect your phone, and accept the RSA prompt.
  adb devices
  exit /b 1
)

echo.
echo [Roofy Music] Building Foss release APK...
echo Note: release uses app.roofymusic.mobile (not the .debug package).
call gradlew.bat :app:assembleFossRelease
if errorlevel 1 (
  echo.
  echo ERROR: Build failed.
  exit /b 1
)

set "APK="
if exist "app\build\outputs\apk\foss\release\app-foss-release.apk" (
  set "APK=app\build\outputs\apk\foss\release\app-foss-release.apk"
) else if exist "app\build\outputs\apk\foss\release\app-foss-release-unsigned.apk" (
  set "APK=app\build\outputs\apk\foss\release\app-foss-release-unsigned.apk"
)

if not defined APK (
  echo ERROR: Could not find a Foss release APK in app\build\outputs\apk\foss\release\
  exit /b 1
)

echo.
echo [Roofy Music] Installing %APK% ...
adb install -r "%APK%"
if errorlevel 1 (
  echo.
  echo ERROR: adb install failed.
  exit /b 1
)

echo.
echo [Roofy Music] Installed successfully.
endlocal
