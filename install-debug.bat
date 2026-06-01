@echo off
setlocal

cd /d "%~dp0"

set "JAVA_HOME=C:\roofy-music-projects\.tools\temurin21\jdk-21.0.11+10"
set "PATH=%JAVA_HOME%\bin;%PATH%"

if not exist "%JAVA_HOME%\bin\jlink.exe" (
  echo ERROR: JDK not found at %JAVA_HOME%
  echo Install Temurin 21 there or update JAVA_HOME in this script.
  exit /b 1
)

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
echo [Roofy Music] Stopping old Gradle daemons (wrong JDK can break the build)...
call gradlew.bat --stop >nul 2>nul

echo.
echo [Roofy Music] Building and installing Foss debug APK...
call gradlew.bat :app:installFossDebug --no-configuration-cache
if errorlevel 1 (
  echo.
  echo ERROR: Build or install failed.
  exit /b 1
)

echo.
echo [Roofy Music] Installed successfully.
endlocal
