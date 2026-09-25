# 验证记录

版本：0.1.1（Debug）。日期：2026-09-25。

## 自动化验证

- Android Debug APK 构建。
- 9 项 KMP/JVM 共享逻辑测试：默认模式排序、已保存格式的有效性、降级范围与重试上限、只使用设备支持的模式、过期连接回调失效、保持比例与旋转计算。
- 12 项 Android JVM / Robolectric / Compose 测试：首帧通知、连续视频帧判定、旧帧隔离、格式持久化与损坏设置处理，以及授权入口、连接指南、全屏退出、模式选择、生命周期和权限边界。
- Android Lint：无错误；固定依赖版本和 target SDK 存在更新提示。
- 检查实际 APK 的权限、ABI、签名及 native 库对齐。
- 已查看自动化渲染的未连接界面截图，未使用假视频表示实际采集成功。

测试报告在 `shared/build/reports/tests/jvmTest/`、`androidApp/build/reports/tests/testDebugUnitTest/` 和 `androidApp/build/reports/lint-results-debug.html`。

## Release 工作流本地验证（2026-09-25）

- Debug 构建、共享测试和 Android 单元测试任务通过（既有测试输出为 up-to-date），Debug Lint 通过。
- 专用签名的 Release APK / AAB 构建与 Release Lint 通过。
- APK 签名证书与新生成的 Release 证书 SHA-256 一致；`apksigner verify`、`jarsigner -verify` 和 `zipalign -c -P 16 4` 通过。此处 ZIP 对齐不代表新增 native ELF 对齐或真机验收结论。
- 缺少签名环境变量时 `validateReleaseSigning` 正确失败；打包脚本拒绝与 APK 版本不一致的标签。
- 发行资料打包及 SHA-256 复核通过，项目源码 ZIP 未包含 keystore、凭据、本机 SDK 配置或构建产物。
- CI 与 Release YAML 通过 actionlint 检查，所引用 Actions 固定到提交 SHA。远程运行结果以 GitHub Actions 记录为准。

## 0.1.3 页面、主题与语言验证（2026-09-25）

- 11 项共享逻辑测试、18 项 Android/Robolectric/Compose 测试通过，无失败或跳过；Debug 构建与 Lint 通过。
- 新增多卡明确选择、过期设备选择不回退、主题模式、设置持久化及损坏值回退测试。
- 在实际 Compose UI 中切换页面、主题及中英语言，验证 Android View 只创建一次；隐藏预览控件不出现在无障碍语义树中。测试不等于真实 TextureView/USB 已通过硬件验证。
- 中英文界面立即切换，选择「跟随系统语言」恢复设备语言；明暗模式、配色、亮屏与帧率显示偏好可保存。
- 已生成并检查中英文预览、设备和设置页截图，均展示未连接采集卡的真实 UI。
- 101 个中英文字符串键与编号占位符一致；本地文档目标和 GitHub Actions 工作流静态检查通过。
- Release APK/AAB 构建与 Lint 通过；APK 签名指纹与既有 Release 一致，签名和 ZIP 对齐检查通过。AAB 禁用语言拆包，保证应用内离线切换语言；包内包含两种语言资源和 9 份许可/声明文件。

## 未完成的硬件验证

当前开发环境没有 ADB 真机和 USB 采集卡。没有验证 Mac HDMI 实际输入、特定绿联型号、真实帧率/延迟、手机 USB 供电、拔插或 native USB 视频稳定性。Robolectric 测试不会执行真实 libuvc 传输。

待用户设备到位后执行：

1. 记录 Android 型号/版本、采集卡型号及 USB VID:PID。
2. 首次连接拒绝相机权限，再允许；拒绝 USB 权限，再连接。不可崩溃或重复弹窗。
3. Mac 输出 1080p60，手机以默认 720p30 开始采集；必须实际看到 Mac 桌面和移动窗口。
4. 如设备报告 1080p30 / 1080p60，在设置中逐个验证；标称能力不等于手机最终能稳定运行的能力。
5. 横竖屏、全屏退出、旋转和双指缩放：画面不应被拉伸。
6. 拔插采集卡、切换到后台再回来、锁屏再解锁，重复至少 5 次；检查连接恢复、资源释放与供电。
7. HDMI 拔出时观察卡输出黑屏/彩条还是停止发帧，确认 App 提示与实际传输状态相符。
8. 持续预览 15 分钟，记录卡/手机发热和帧率。观察重连后是否出现设备忙。
9. 手动选择不同格式，出画至少 3 秒，彻底关闭再打开 App；确认恢复该格式。更换不同 VID/PID 的卡，不应套用原卡的设置。
10. 用能复现启动无帧的卡/模式验证：8 秒后尝试降级，最多两次；拒绝权限、拔出、切后台应取消旧尝试。卡自行输出黑屏/彩条仍属于有视频帧，不会触发无帧降级。
11. 预览中反复切换三个标签、明暗模式、配色和语言；检查实际视频无异常中断。关闭亮屏开关后确认屏幕遵循系统超时；切换帧率显示并重启验证偏好保存。
12. 观察首帧到达时等待提示是否及时消失，避免额外等待一次每秒状态刷新。此处不等于实际 HDMI 采集延迟已测量或降低。

收集开发日志：`adb logcat -s PocketMonitor USBMonitor UVCCamera`。回报问题时附手机型号、Android 版本、采集卡 USB ID、选中的视频格式和复现步骤，不要提供屏幕上的账号密码。
