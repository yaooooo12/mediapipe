@echo off
echo ========================================
echo 快速安装（跳过清理）
echo ========================================
echo.

echo 编译并安装...
call gradlew.bat :examples:android:solutions:ar_hand_tracking:installDebug

if errorlevel 1 (
    echo.
    echo [错误] 安装失败
    pause
    exit /b 1
)

echo.
echo [成功] 安装完成
echo.
set /p LAUNCH="是否启动应用？(Y/N): "
if /i "%LAUNCH%"=="Y" (
    adb shell am start -n com.google.mediapipe.apps.arhandtracking/.MainActivity
    echo 应用已启动
)

pause
