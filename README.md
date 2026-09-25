# 随身屏 · Pocket Monitor

Kotlin Multiplatform + Compose Multiplatform 编写的便携视频监视器。第一版是 **Android 8.0+ App**：用 UVC 采集卡，把 Mac / NAS 的 HDMI 输出显示在手机上。电脑无需安装发送端程序。

当前可构建、安装的目标只有 Android。`shared` 提供跨平台界面、数据模型、格式排序和预览几何计算；JVM 目标用于运行共享测试，**不是 Mac 客户端**。尚未提供 iPhone App；不能把普通 UVC 采集卡直连 iPhone 当作已支持功能。

## 安装与连接

开发测试 APK：`artifacts/pocket-monitor-0.1.1-debug.apk`。这是 Debug 签名包，不是商店发行包。源码仓库不保存生成的 APK，已发布版本从仓库 Releases 下载。

```text
Mac HDMI 输出 ── HDMI 线 ── 采集卡 HDMI IN
                                  │
                              USB-C / OTG
                                  │
                             Android 手机
```

Mac 只有 Type-C 时，在 Mac 侧增加支持 DP 视频输出的 Type-C 转 HDMI 转接器。USB-C 接口外形本身不代表支持视频输入。

1. 在 Android 手机上安装 APK，连接支持 UVC 的 HDMI 采集卡；部分手机需要在设置里开启 OTG。
2. 在应用中点击「允许视频访问」，允许相机权限；点击「连接采集卡」，允许系统的 USB 访问弹窗。
3. 在 Mac「系统设置 → 显示器」里选择镜像已有屏幕，或将窗口移到新出现的扩展显示器。先用 1920×1080、60Hz 的 HDMI 输出测试，避免输出超出采集卡能力。
4. 应用默认从设备报告的模式中优先选择 720p/30 MJPEG；「视频设置」中可切换采集格式。持续收到视频帧至少 3 秒后，按采集卡 VID/PID 保存格式，下次优先使用；同型号卡共用此设置。输入帧率与采集帧率不是同一项。
5. 支持全屏、每次旋转 90°、双指缩放和拖动；双击画面或点击「复位」恢复显示。

视频采集只在前台运行，切到后台会关闭采集、释放 USB；回到前台或同型号设备重新接入时尝试恢复。多张相同型号采集卡不会自动选取，需手动选择。第一版不包含音频、录制、截图、键盘或鼠标回传。

## 黑屏、彩条与权限

- **未发现设备**：检查手机 OTG、数据线方向、供电，以及卡是否为标准 UVC 设备。普通 Type-C → HDMI 投屏线不是采集卡。
- **权限拒绝**：再次点击授权；若系统不再弹窗，点击「系统设置」开启相机权限，然后重新连接。
- **无法打开**：关闭其他正在使用采集卡的应用，拔插后重试。
- **等待视频帧 / 视频流中断**：启动 8 秒仍无视频帧时，最多自动降级重连两次，只选设备报告的较低规格。启动抛出异常也会尝试降级。没有可用的更低规格时停止；已经出画后发生断流不会自动反复重连。请检查 HDMI 输入、供电，或手动切换格式。
- **显示彩条、黑屏或无信号提示**：部分卡会自行生成这些画面，App 仍会收到有效视频帧。App 的「实时画面」与 fps 指采集流，不保证 HDMI 输入已锁定，也不宣称检测到电脑的实际画面。
- 加密的 HDCP 视频不属于支持范围。Boot/BIOS/恢复模式是否有画面也取决于电脑的视频输出与采集卡支持的时序。

## 本地开发

需要 JDK 17、Android SDK 35 / Build Tools 35.0.0。设置 `ANDROID_HOME`，或在不提交的 `local.properties` 中填写 `sdk.dir`。

```bash
./gradlew :androidApp:assembleDebug
./gradlew :shared:jvmTest :androidApp:testDebugUnitTest :androidApp:lintDebug
adb install -r androidApp/build/outputs/apk/debug/androidApp-debug.apk
```

版本固定：Kotlin 2.1.21、Compose Multiplatform 1.8.1、AGP 8.9.2、Gradle 8.14.2、UVCAndroid 1.0.13。构建依赖来自 Google Maven、Maven Central 和 Gradle Plugin Portal。

目录：

- `shared/src/commonMain`：Compose 界面、连接状态、设备/格式模型、模式选择与视频比例计算。
- `androidApp/src/main`：权限、USB 设备识别、原生 UVC 采集、TextureView 与生命周期管理。
- `shared/src/commonTest`：共享逻辑测试。
- `androidApp/src/test`：Robolectric 生命周期、权限边界和 Compose 交互测试。
- `docs/VALIDATION.md`：已完成的验证与真机验收步骤。
- `THIRD_PARTY_NOTICES.md`、APK 内 `assets/licenses/`：第三方组件及许可证。

USB 和 native 操作在独立 HandlerThread 上串行执行；连接代次用于丢弃旧授权/打开/帧回调。原生渲染停止后才释放 Surface。视频直接渲染到 TextureView，不逐帧转成 Compose Bitmap；帧回调只用于统计，不保存视频数据。

## 平台边界

Android 实机、手机供电和具体采集卡仍需配对验证。自动化测试不替代 HDMI → 采集卡 → 手机的真实视频验证。

iPhone 后续需要可实际接入的视频来源（例如协议公开的网络采集设备），再添加对应接收端。KMP 只能共享代码，不能增加手机缺少的硬件输入或系统 API。
