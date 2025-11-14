@echo off
chcp 65001 >nul
echo ========================================
echo AR Hand Tracking - Build and Install
echo ========================================
echo.

:: 设置颜色
set GREEN=[92m
set RED=[91m
set YELLOW=[93m
set NC=[0m

echo %YELLOW%[1/6] 检查项目目录...%NC%

:: 检查是否在 mediapipe 根目录
if exist "mediapipe\examples\android\solutions\gradlew.bat" (
    echo %GREEN%✓ 检测到 MediaPipe 项目根目录%NC%
    set GRADLE_DIR=mediapipe\examples\android\solutions
    set IN_ROOT=1
    goto :gradle_found
)

:: 检查是否在 solutions 目录
if exist "gradlew.bat" (
    if exist "ar_hand_tracking\build.gradle" (
        echo %GREEN%✓ 检测到 solutions 目录%NC%
        set GRADLE_DIR=.
        set IN_ROOT=0
        goto :gradle_found
    )
)

:: 未找到正确目录
echo %RED%错误: 找不到 gradlew.bat 文件！%NC%
echo.
echo 请将此脚本放在以下任一位置运行：
echo 1. MediaPipe 项目根目录（推荐）
echo    例如: C:\Users\Temp\AndroidStudioProjects\mediapipe\
echo.
echo 2. solutions 目录
echo    例如: C:\Users\Temp\AndroidStudioProjects\mediapipe\mediapipe\examples\android\solutions\
echo.
echo 当前目录: %CD%
echo.
pause
exit /b 1

:gradle_found
echo 工作目录: %CD%
echo Gradle 目录: %GRADLE_DIR%
echo.

echo %YELLOW%[2/6] 检查 ADB 工具...%NC%
where adb >nul 2>&1
if errorlevel 1 (
    echo %RED%警告: adb 未在 PATH 中找到%NC%
    echo 尝试使用 Android Studio 的 adb...

    :: 尝试常见的 Android SDK 位置
    set ADB_PATH=
    if exist "%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe" (
        set ADB_PATH=%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe
    ) else if exist "%USERPROFILE%\AppData\Local\Android\Sdk\platform-tools\adb.exe" (
        set ADB_PATH=%USERPROFILE%\AppData\Local\Android\Sdk\platform-tools\adb.exe
    )

    if defined ADB_PATH (
        echo %GREEN%✓ 找到 adb: %ADB_PATH%%NC%
        set "PATH=%PATH%;%ADB_PATH%\.."
    ) else (
        echo %RED%错误: 找不到 adb 工具！%NC%
        echo 请打开 Android Studio 或手动安装 Android SDK
        pause
        exit /b 1
    )
) else (
    echo %GREEN%✓ adb 工具已就绪%NC%
)
echo.

echo %YELLOW%[3/6] 清理旧的构建文件...%NC%
cd /d "%GRADLE_DIR%"
call gradlew.bat :ar_hand_tracking:clean
if errorlevel 1 (
    echo %RED%清理失败！%NC%
    pause
    exit /b 1
)
echo %GREEN%✓ 清理完成%NC%
echo.

echo %YELLOW%[4/6] 编译 Debug APK...%NC%
echo 这可能需要几分钟，首次编译会下载依赖...
call gradlew.bat :ar_hand_tracking:assembleDebug
if errorlevel 1 (
    echo %RED%编译失败！请检查上方的错误信息%NC%
    echo.
    echo 常见问题：
    echo - 首次编译需要下载依赖，请确保网络连接正常
    echo - 检查是否安装了 Android SDK API 30 和 API 34
    echo - 在 Android Studio 中打开项目可能会自动解决依赖问题
    pause
    exit /b 1
)
echo %GREEN%✓ 编译成功%NC%
echo.

echo %YELLOW%[5/6] 检查 Android 设备连接...%NC%
adb devices | findstr "device$" >nul
if errorlevel 1 (
    echo %RED%错误: 没有检测到 Android 设备！%NC%
    echo.
    echo 请确保：
    echo 1. USB 调试已启用（设置 → 开发者选项 → USB 调试）
    echo 2. 设备已通过 USB 连接到电脑
    echo 3. 在设备上允许了 USB 调试授权
    echo.
    echo 当前设备列表：
    adb devices
    echo.
    pause
    exit /b 1
)

echo %GREEN%✓ 设备已连接%NC%
echo 已连接的设备：
adb devices
echo.

echo %YELLOW%[6/6] 安装应用到设备...%NC%
call gradlew.bat :ar_hand_tracking:installDebug
if errorlevel 1 (
    echo %RED%安装失败！%NC%
    echo.
    echo 可能的解决方法：
    echo 1. 卸载设备上的旧版本应用
    echo 2. 检查设备存储空间是否充足
    echo 3. 确保设备允许安装此应用
    pause
    exit /b 1
)
echo %GREEN%✓ 安装成功%NC%
echo.

echo %GREEN%========================================%NC%
echo %GREEN%     构建和安装完成！%NC%
echo %GREEN%========================================%NC%
echo.
echo APK 位置：
echo ar_hand_tracking\build\outputs\apk\debug\ar_hand_tracking-debug.apk
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
        echo %GREEN%✓ 应用已启动，请在设备上查看%NC%
    )
)

echo.
echo 按任意键退出...
pause >nul
