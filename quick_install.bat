@echo off
chcp 65001 >nul
echo ========================================
echo 快速安装 AR Hand Tracking
echo ========================================
echo.

:: 检查是否在正确目录
if exist "mediapipe\examples\android\solutions\gradlew.bat" (
    set GRADLE_DIR=mediapipe\examples\android\solutions
) else if exist "gradlew.bat" (
    set GRADLE_DIR=.
) else (
    echo [错误] 找不到 gradlew.bat
    echo 请在 mediapipe 根目录或 solutions 目录运行此脚本
    pause
    exit /b 1
)

cd /d "%GRADLE_DIR%"

echo 正在编译并安装...
call gradlew.bat :ar_hand_tracking:installDebug

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
