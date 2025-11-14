@echo off
echo ========================================
echo AR Hand Tracking - 实时日志查看
echo ========================================
echo.
echo 按 Ctrl+C 停止查看日志
echo.
echo 开始监听日志...
echo.

adb logcat -c
adb logcat | findstr /i "MainActivity ARHandTrackingRenderer mediapipe"

pause
