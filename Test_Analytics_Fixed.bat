@echo off
echo Testing BSNL Tracker Analytics Functionality
echo ============================================
echo.
echo Compiling the application...
cd /d "c:\Users\Adithya Bhaskar\Desktop\BSNL Tracker"
javac -cp "lib\jcalendar-1.4.jar;." -d target\classes src\main\java\com\login\*.java src\main\java\com\login\ui\*.java src\main\java\com\login\service\*.java src\main\java\com\login\model\*.java

if %errorlevel% neq 0 (
    echo Compilation failed!
    pause
    exit /b 1
)

echo Compilation successful!
echo.
echo Starting BSNL Tracker...
echo.
echo Instructions for testing:
echo 1. Login with: admin / admin123
echo 2. Go to Analytics tab
echo 3. Try changing filters (Network, Vendor, Quarter)
echo 4. Verify no infinite loops or duplicate filter panels
echo 5. Check that filtering updates data correctly
echo.
echo Press any key to start the application...
pause > nul

java -cp "target\classes;lib\jcalendar-1.4.jar" com.login.LoginApp_Fixed

echo.
echo Application closed.
pause
