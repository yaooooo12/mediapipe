@echo off
chcp 65001 >nul
echo ========================================
echo 测试编译 AR Hand Tracking
echo ========================================
echo.

cd /d "%~dp0"

if exist "ar_hand_tracking\build.gradle" (
    echo ✓ 检测到当前在 solutions 目录
) else if exist "mediapipe\examples\android\solutions\ar_hand_tracking\build.gradle" (
    echo ✓ 检测到当前在 mediapipe 根目录，正在切换...
    cd mediapipe\examples\android\solutions
) else (
    echo [错误] 找不到 ar_hand_tracking 项目
    echo 当前目录: %CD%
    pause
    exit /b 1
)

echo.
echo 当前工作目录: %CD%
echo.
echo ----------------------------------------
echo 开始编译...
echo ----------------------------------------
echo.

gradlew.bat --version
echo.

echo 运行任务: :ar_hand_tracking:assembleDebug
echo.
gradlew.bat :ar_hand_tracking:assembleDebug --info 2>&1 | findstr /V "wrapper problems-report"

echo.
pause
