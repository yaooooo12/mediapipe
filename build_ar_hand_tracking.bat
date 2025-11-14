@echo off
echo ========================================
echo AR Hand Tracking - Build and Install
echo ========================================
echo.

:: 设置项目路径
set PROJECT_DIR=%~dp0
set APP_MODULE=:examples:android:solutions:ar_hand_tracking

:: 颜色输出（如果支持）
set GREEN=[92m
set RED=[91m
set YELLOW=[93m
set NC=[0m

echo %YELLOW%[1/5] 检查 Gradle 环境...%NC%
if not exist "%PROJECT_DIR%gradlew.bat" (
    echo %RED%错误: gradlew.bat 不存在！%NC%
    echo 请确保在 MediaPipe 项目根目录运行此脚本
    pause
    exit /b 1
)

echo %GREEN%✓ Gradle 环境正常%NC%
echo.

echo %YELLOW%[2/5] 清理旧的构建文件...%NC%
call gradlew.bat %APP_MODULE%:clean
if errorlevel 1 (
    echo %RED%清理失败！%NC%
    pause
    exit /b 1
)
echo %GREEN%✓ 清理完成%NC%
echo.

echo %YELLOW%[3/5] 编译 Debug APK...%NC%
echo 这可能需要几分钟，请耐心等待...
call gradlew.bat %APP_MODULE%:assembleDebug
if errorlevel 1 (
    echo %RED%编译失败！请检查错误信息%NC%
    pause
    exit /b 1
)
echo %GREEN%✓ 编译成功%NC%
echo.

echo %YELLOW%[4/5] 检查 Android 设备连接...%NC%
adb devices > nul 2>&1
if errorlevel 1 (
    echo %RED%错误: adb 未找到！%NC%
    echo 请确保 Android SDK platform-tools 在系统 PATH 中
    echo 或者打开 Android Studio，它会自动配置 adb
    pause
    exit /b 1
)

adb devices | findstr "device$" > nul
if errorlevel 1 (
    echo %RED%错误: 没有检测到 Android 设备！%NC%
    echo.
    echo 请确保：
    echo 1. USB 调试已启用
    echo 2. 设备已通过 USB 连接
    echo 3. 在设备上允许了 USB 调试授权
    echo.
    echo 当前设备列表：
    adb devices
    pause
    exit /b 1
)

echo %GREEN%✓ 设备已连接%NC%
adb devices
echo.

echo %YELLOW%[5/5] 安装应用到设备...%NC%
call gradlew.bat %APP_MODULE%:installDebug
if errorlevel 1 (
    echo %RED%安装失败！%NC%
    pause
    exit /b 1
)
echo %GREEN%✓ 安装成功%NC%
echo.

echo %GREEN%========================================%NC%
echo %GREEN%构建和安装完成！%NC%
echo %GREEN%========================================%NC%
echo.
echo APK 位置：
echo mediapipe\examples\android\solutions\ar_hand_tracking\build\outputs\apk\debug\
echo.

:: 询问是否启动应用
set /p LAUNCH="是否立即启动应用？(Y/N): "
if /i "%LAUNCH%"=="Y" (
    echo.
    echo 正在启动应用...
    adb shell am start -n com.google.mediapipe.apps.arhandtracking/.MainActivity
    if errorlevel 1 (
        echo %RED%启动失败%NC%
    ) else (
        echo %GREEN%✓ 应用已启动%NC%
    )
)

echo.
echo 按任意键退出...
pause > nul
