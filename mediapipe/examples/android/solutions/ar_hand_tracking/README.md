# AR Hand Tracking - MediaPipe Android 应用

## 功能介绍

这是一个基于 MediaPipe 的 Android AR 手部追踪应用，能够：

- 📱 实时调用摄像头识别用户的手部
- ✋ 追踪 21 个手部关键点
- 🔢 在五个手指指尖标注数字 1-5：
  - **1** - 大拇指 (Thumb)
  - **2** - 食指 (Index Finger)
  - **3** - 中指 (Middle Finger)
  - **4** - 无名指 (Ring Finger)
  - **5** - 小指 (Pinky)
- ➡️ 用黄色箭头指向每个手指指尖
- 🎨 显示完整的手部骨架连接线

## 技术特性

- **GPU 加速**: 使用 GPU 进行实时手部检测，保证流畅性能
- **多手支持**: 可同时识别最多 2 只手
- **OpenGL 渲染**: 使用 OpenGL ES 进行高性能图形渲染
- **前置摄像头**: 默认使用前置摄像头，方便自拍模式

## 使用方法

### Android Studio 导入

1. 打开 Android Studio
2. 选择 **File > Open**
3. 导航到 `mediapipe/examples/android/solutions/ar_hand_tracking` 目录
4. 点击 **OK** 导入项目

### 编译和运行

1. 连接 Android 设备或启动模拟器
2. 确保设备运行 Android 5.0 (API 21) 或更高版本
3. 点击 **Run** 按钮 (绿色三角形) 或按 `Shift + F10`
4. 选择目标设备
5. 应用将自动安装并启动

### 应用使用

1. 启动应用后会自动打开摄像头
2. 将手放在摄像头前
3. 应用会自动识别手部并显示：
   - 绿色/红色骨架线（左手/右手）
   - 手部关键点
   - 五个手指指尖上的黄色箭头和数字标注 (1-5)

## 权限要求

应用需要以下权限：
- **CAMERA** - 访问设备摄像头
- **INTERNET** - 用于日志记录
- **ACCESS_NETWORK_STATE** - 检查网络状态

## 系统要求

- **最低 SDK**: Android 5.0 (API 21)
- **目标 SDK**: Android 14 (API 34)
- **编译 SDK**: Android 11 (API 30)
- **推荐设备**: 配备 GPU 的 Android 设备以获得最佳性能

## 项目结构

```
ar_hand_tracking/
├── src/main/
│   ├── java/com/google/mediapipe/examples/arhandtracking/
│   │   ├── MainActivity.java              # 主活动，处理相机输入
│   │   └── ARHandTrackingRenderer.java    # 自定义渲染器，绘制箭头和标签
│   ├── res/
│   │   └── layout/
│   │       └── activity_main.xml          # 简化的全屏布局
│   └── AndroidManifest.xml
├── build.gradle
└── README.md
```

## 技术实现

### 手部追踪

使用 MediaPipe Hands API 进行实时手部检测：
- 21 个手部关键点检测
- 左右手识别
- 3D 世界坐标和 2D 归一化坐标

### 渲染系统

自定义 OpenGL 渲染器 `ARHandTrackingRenderer`：
- 基础几何图形渲染（线条、圆形）
- 箭头绘制（使用三角形）
- 文字标签渲染（使用纹理）
- Alpha 混合支持（文字透明背景）

### 手指指尖映射

```java
HandLandmark.THUMB_TIP (4)        -> 标签 "1"
HandLandmark.INDEX_FINGER_TIP (8)  -> 标签 "2"
HandLandmark.MIDDLE_FINGER_TIP (12) -> 标签 "3"
HandLandmark.RING_FINGER_TIP (16)   -> 标签 "4"
HandLandmark.PINKY_TIP (20)        -> 标签 "5"
```

## 调试日志

应用会在 Logcat 中输出手指指尖的坐标信息：

```
Finger tips - Thumb(1): (0.45, 0.32), Index(2): (0.52, 0.28),
Middle(3): (0.58, 0.25), Ring(4): (0.63, 0.30), Pinky(5): (0.68, 0.35)
```

## 常见问题

### 1. 相机无法启动
- 检查是否授予了相机权限
- 在设置中手动授予权限

### 2. 手部检测不准确
- 确保光线充足
- 将手放在摄像头前约 30-50 厘米处
- 确保手部完整在镜头内

### 3. 性能问题
- 确保设备支持 GPU 加速
- 关闭其他占用资源的应用
- 降低 `maxNumHands` 参数（在 MainActivity.java 中）

## 依赖项

```gradle
implementation 'com.google.mediapipe:solution-core:latest.release'
implementation 'com.google.mediapipe:hands:latest.release'
implementation 'androidx.appcompat:appcompat:1.3.0'
implementation 'com.google.android.material:material:1.3.0'
```

## 许可证

Copyright 2021 The MediaPipe Authors.
Licensed under the Apache License, Version 2.0

## 参考资料

- [MediaPipe Hands](https://google.github.io/mediapipe/solutions/hands.html)
- [MediaPipe Android 文档](https://google.github.io/mediapipe/getting_started/android.html)
- [OpenGL ES](https://developer.android.com/guide/topics/graphics/opengl)

## 作者

基于 MediaPipe Hands 示例修改和扩展
