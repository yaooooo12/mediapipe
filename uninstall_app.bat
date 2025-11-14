@echo off
echo ========================================
echo 卸载 AR Hand Tracking 应用
echo ========================================
echo.

adb uninstall com.google.mediapipe.apps.arhandtracking

if errorlevel 1 (
    echo [错误] 卸载失败
) else (
    echo [成功] 应用已卸载
)

echo.
pause
