# AR Hand Tracking 构建脚本使用指南

## 📦 Windows BAT 脚本说明

为了方便在 Windows 上构建和安装 AR Hand Tracking 应用，我提供了以下脚本：

---

## 🚀 主要脚本

### 1. `build_ar_hand_tracking.bat` - 完整构建流程 ⭐推荐

**功能**：完成所有构建和安装步骤

**使用方法**：
```cmd
# 在 MediaPipe 根目录下运行
build_ar_hand_tracking.bat
```

**执行步骤**：
1. ✅ 检查 Gradle 环境
2. 🧹 清理旧的构建文件
3. 🔨 编译 Debug APK
4. 📱 检查设备连接
5. 📲 安装应用到设备
6. ❓ 询问是否启动应用

**预计时间**：首次 3-5 分钟，后续 1-2 分钟

---

### 2. `quick_install.bat` - 快速安装

**功能**：跳过清理步骤，直接编译和安装（适合快速迭代）

**使用方法**：
```cmd
quick_install.bat
```

**适用场景**：
- 代码小改动后快速测试
- 不需要完整清理构建

**预计时间**：30秒 - 1分钟

---

### 3. `view_logs.bat` - 查看应用日志

**功能**：实时查看应用运行日志

**使用方法**：
```cmd
# 启动应用后运行
view_logs.bat
```

**显示内容**：
- MainActivity 日志
- ARHandTrackingRenderer 日志
- MediaPipe 相关日志

**停止查看**：按 `Ctrl+C`

---

### 4. `check_device.bat` - 检查设备状态

**功能**：检查 Android 设备和应用状态

**使用方法**：
```cmd
check_device.bat
```

**显示信息**：
- 已连接的设备列表
- 设备型号和 Android 版本
- 应用是否已安装
- 应用版本信息

---

### 5. `uninstall_app.bat` - 卸载应用

**功能**：从设备卸载应用

**使用方法**：
```cmd
uninstall_app.bat
```

---

## 🔧 使用前准备

### 1. 配置 Android SDK 环境变量

确保 `adb` 命令可用，需要将以下路径添加到系统 PATH：

```
C:\Users\你的用户名\AppData\Local\Android\Sdk\platform-tools
```

**验证方法**：
```cmd
adb version
```

如果显示版本号，说明配置成功。

### 2. 启用 USB 调试

在 Android 设备上：
1. 进入 **设置** → **关于手机**
2. 连续点击 **版本号** 7次，启用开发者选项
3. 进入 **设置** → **开发者选项**
4. 启用 **USB 调试**

### 3. 连接设备

1. 用 USB 数据线连接设备和电脑
2. 在设备上允许 USB 调试授权
3. 运行 `check_device.bat` 验证连接

---

## 📋 完整工作流程示例

### 首次构建和安装

```cmd
# 1. 进入项目目录
cd C:\path\to\mediapipe

# 2. 检查设备
check_device.bat

# 3. 完整构建
build_ar_hand_tracking.bat
```

### 代码修改后快速测试

```cmd
# 1. 快速安装
quick_install.bat

# 2. 查看日志
view_logs.bat
```

### 调试问题

```cmd
# 1. 检查设备状态
check_device.bat

# 2. 查看实时日志
view_logs.bat

# 3. 如果需要，先卸载旧版本
uninstall_app.bat

# 4. 重新完整构建
build_ar_hand_tracking.bat
```

---

## ⚠️ 常见问题

### Q1: "adb 未找到"

**解决方法**：
1. 打开 Android Studio
2. 等待完全启动（会自动配置环境）
3. 或手动添加 platform-tools 到 PATH

### Q2: "没有检测到 Android 设备"

**检查项**：
- [ ] USB 线是否连接
- [ ] USB 调试是否启用
- [ ] 在设备上是否允许了授权
- [ ] 尝试重新插拔 USB
- [ ] 运行 `adb devices` 手动检查

### Q3: "编译失败"

**可能原因**：
1. **首次构建**：Gradle 正在下载依赖，需要等待
2. **网络问题**：检查网络连接，可能需要配置 Gradle 代理
3. **SDK 版本**：确保安装了 API 30 和 API 34 的 SDK

**解决方法**：
```cmd
# 在 Android Studio 中
# Tools → SDK Manager → 安装 Android 11.0 (API 30) 和 Android 14 (API 34)
```

### Q4: "安装失败 - INSTALL_FAILED_UPDATE_INCOMPATIBLE"

**原因**：已安装的应用签名不匹配

**解决方法**：
```cmd
# 先卸载旧版本
uninstall_app.bat

# 再重新安装
build_ar_hand_tracking.bat
```

---

## 🎯 脚本执行顺序建议

**第一次使用**：
```
check_device.bat → build_ar_hand_tracking.bat → view_logs.bat
```

**日常开发**：
```
quick_install.bat → view_logs.bat
```

**遇到问题**：
```
check_device.bat → uninstall_app.bat → build_ar_hand_tracking.bat
```

---

## 📁 生成的文件位置

构建成功后，APK 文件在：
```
mediapipe\examples\android\solutions\ar_hand_tracking\build\outputs\apk\debug\ar_hand_tracking-debug.apk
```

可以直接复制这个 APK 分享给其他设备安装。

---

## 💡 提示

1. **首次构建**需要下载 Gradle 依赖和 MediaPipe 模型，可能需要 5-10 分钟
2. **后续构建**通常只需要 1-2 分钟
3. **修改代码后**使用 `quick_install.bat` 可以更快
4. **查看日志**能帮助调试手部追踪问题
5. **保持 USB 连接**期间，确保设备不会休眠

---

## 🔗 相关文档

- [Android Studio 官方文档](https://developer.android.com/studio)
- [ADB 使用指南](https://developer.android.com/studio/command-line/adb)
- [MediaPipe Hands 文档](https://google.github.io/mediapipe/solutions/hands.html)
- [应用详细说明](mediapipe/examples/android/solutions/ar_hand_tracking/README.md)

---

**祝你构建顺利！** 🎉

如有问题，请查看日志输出或在 Android Studio 的 Build 窗口查看详细错误信息。
