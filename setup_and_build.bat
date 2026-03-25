@echo off
echo ============================================
echo  Flutter APK Build Setup Script
echo ============================================
echo.

:: Check if Flutter is available
where flutter >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo Flutter not found in PATH.
    echo.
    echo Please do the following first:
    echo   1. Download Flutter from: https://storage.googleapis.com/flutter_infra_release/releases/stable/windows/flutter_windows_3.29.2-stable.zip
    echo   2. Extract to D:\flutter
    echo   3. Add D:\flutter\bin to your PATH
    echo   4. Restart this terminal and run this script again
    echo.
    pause
    exit /b 1
)

echo [1/5] Flutter found:
flutter --version
echo.

echo [2/5] Running flutter doctor...
flutter doctor
echo.

echo [3/5] Accepting Android licenses...
flutter doctor --android-licenses
echo.

echo [4/5] Getting dependencies...
cd /d D:\Workspace\keyboard-traslator-app
flutter pub get
echo.

echo [5/5] Building debug APK...
flutter build apk --debug
echo.

if exist "build\app\outputs\flutter-apk\app-debug.apk" (
    echo ============================================
    echo  BUILD SUCCESSFUL!
    echo  APK location: build\app\outputs\flutter-apk\app-debug.apk
    echo ============================================
) else (
    echo Build may have failed. Check the output above.
)
pause
