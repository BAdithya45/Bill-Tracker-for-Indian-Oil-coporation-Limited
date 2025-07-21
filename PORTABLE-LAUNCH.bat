@echo off
title BSNL Tracker - Portable Launch
color 0A
echo.
echo ========================================
echo     BSNL TRACKER - PORTABLE LAUNCHER
echo ========================================
echo.

cd /d "%~dp0"

REM Check Java installation first
echo 🔍 Checking Java installation...
java -version >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ❌ ERROR: Java is NOT installed on this PC!
    echo.
    echo 📋 TO FIX THIS:
    echo 1. Download Java from: https://adoptium.net/
    echo 2. Install Java 8 or higher
    echo 3. Restart this script
    echo.
    echo OR download portable Java and extract to 'java' folder here
    echo.
    pause
    exit /b 1
)
echo ✅ Java is installed

REM Check for WAR file in multiple locations
set "APP_PATH="
if exist "target\bill.war" (
    set "APP_PATH=target\bill.war"
    echo ✅ Found WAR file in: target\
) else if exist "app\bill.war" (
    set "APP_PATH=app\bill.war"
    echo ✅ Found WAR file in: app\
) else if exist "bill.war" (
    set "APP_PATH=bill.war"
    echo ✅ Found WAR file in: current directory
) else (
    echo.
    echo ❌ ERROR: Application file not found!
    echo.
    echo 📋 EXPECTED LOCATIONS:
    echo   • target\bill.war
    echo   • app\bill.war  
    echo   • bill.war
    echo.
    echo 💡 SOLUTION: Copy the WAR file to one of these locations
    echo.
    pause
    exit /b 1
)

REM Create data directories if they don't exist
if not exist "data" mkdir data
if not exist "data\pdfs" mkdir data\pdfs
if not exist "data\pdfs\shared" mkdir data\pdfs\shared

REM Kill any existing Java processes
echo 🧹 Cleaning up existing processes...
taskkill /F /IM java.exe >nul 2>&1

REM Show startup info
echo.
echo 🚀 Starting BSNL Tracker...
echo 📁 Using: %APP_PATH%
echo 🌐 Port: 8081
echo.

REM Start application in background
start /B /MIN java -jar "%APP_PATH%"

REM Wait for startup with progress
echo ⏳ Starting application...
for /L %%i in (1,1,15) do (
    echo|set /p="."
    timeout /t 1 /nobreak >nul
)
echo.

REM Test if application is running
echo 🔍 Testing connection...
powershell -Command "try { $response = Invoke-WebRequest -Uri 'http://localhost:8081' -TimeoutSec 5 -UseBasicParsing; exit 0 } catch { exit 1 }" >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo ✅ Application is responding
) else (
    echo ⚠️  Application may still be starting...
)

REM Open browser
echo 🌐 Opening browser...
start http://localhost:8081/

REM Success message
cls
echo.
echo.
echo ✅ Application: STARTED
echo ✅ Browser: OPENED  
echo ✅ URL: http://localhost:8081/
echo.
echo 👤 Default Login:
echo    Username: admin
echo    Password: admin
echo.
echo 📍 Running from: %APP_PATH%
echo 💾 Data stored in: data\ folder
echo.
echo ========================================
echo.
echo 💡 TIP: You can safely close this window
echo 🛑 To stop: Run STOP-APP.bat or kill Java process
echo.
pause
