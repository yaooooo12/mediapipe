@echo off
chcp 65001 >nul
echo ========================================
echo 修复 Gradle 版本兼容性问题
echo ========================================
echo.

:: 找到 solutions 目录
if exist "gradle\wrapper\gradle-wrapper.properties" (
    set WRAPPER_FILE=gradle\wrapper\gradle-wrapper.properties
) else if exist "mediapipe\examples\android\solutions\gradle\wrapper\gradle-wrapper.properties" (
    set WRAPPER_FILE=mediapipe\examples\android\solutions\gradle\wrapper\gradle-wrapper.properties
) else (
    echo [错误] 找不到 gradle-wrapper.properties
    pause
    exit /b 1
)

echo 当前 Gradle wrapper 配置: %WRAPPER_FILE%
echo.

echo 问题说明：
echo - 当前 Gradle 版本: 8.14.3
echo - 项目 Android Gradle Plugin: 4.2.0
echo - 不兼容！需要降级到 Gradle 6.7.1
echo.

echo 正在备份原文件...
copy "%WRAPPER_FILE%" "%WRAPPER_FILE%.backup" >nul
echo ✓ 备份完成: %WRAPPER_FILE%.backup
echo.

echo 正在修改 Gradle 版本为 6.7.1...
(
echo distributionBase=GRADLE_USER_HOME
echo distributionPath=wrapper/dists
echo distributionUrl=https\://services.gradle.org/distributions/gradle-6.7.1-bin.zip
echo networkTimeout=10000
echo validateDistributionUrl=true
echo zipStoreBase=GRADLE_USER_HOME
echo zipStorePath=wrapper/dists
) > "%WRAPPER_FILE%"

echo ✓ 修改完成
echo.

echo 新配置内容：
type "%WRAPPER_FILE%"
echo.

echo ========================================
echo ✓ 修复完成！
echo ========================================
echo.
echo Gradle 版本已降级到 6.7.1（兼容 AGP 4.2.0）
echo.
echo 下一步：重新运行编译命令
echo   .\gradlew.bat :ar_hand_tracking:assembleDebug
echo.
pause
