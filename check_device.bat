@echo off
echo ========================================
echo 检查 Android 设备状态
echo ========================================
echo.

echo [1] 已连接的设备：
adb devices
echo.

echo [2] 设备信息：
adb shell getprop ro.product.model
adb shell getprop ro.build.version.release
echo.

echo [3] 已安装的应用：
adb shell pm list packages | findstr arhandtracking
if errorlevel 1 (
    echo AR Hand Tracking 未安装
) else (
    echo AR Hand Tracking 已安装
    echo.
    echo [4] 应用详细信息：
    adb shell dumpsys package com.google.mediapipe.apps.arhandtracking | findstr "versionName versionCode"
)

echo.
pause
