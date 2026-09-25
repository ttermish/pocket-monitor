# Privacy / 隐私说明

Pocket Monitor previews video locally. The app does not request Internet access and has no analytics, advertising SDK, account system, video uploads, recording or cloud synchronization.

- **Camera permission** allows Android to expose UVC video to the app. **USB permission** grants access to the selected capture card.
- Theme, accent, language and preview preferences are stored in app-private preferences. Working capture formats are stored by USB vendor/product ID, not by device serial number.
- Capture stops when the app leaves the foreground. Android backup is disabled. Uninstalling the app or clearing its storage removes preferences.
- Diagnostic logs can contain capture errors and device details. Logs and screenshots submitted to GitHub are shared by you, outside the app; remove private information first.

This describes the current source. Forks and future versions may behave differently. See [security reporting](../SECURITY.md) for concerns.

## 中文

随身屏在本地预览视频，不申请联网权限，不含统计分析、广告 SDK、账号系统、视频上传、录制或云同步。

- **相机权限**用于 Android 上的 UVC 视频访问；**USB 权限**用于访问所选采集卡。
- 主题、配色、语言和预览偏好保存在应用私有设置中。有效采集格式按 USB 厂商/产品 ID 保存，不使用设备序列号。
- 应用离开前台时停止采集；已禁用 Android 备份。卸载应用或清除存储会删除本地偏好。
- 诊断日志可能包含采集错误和设备信息。你主动提交到 GitHub 的日志或截图不属于 App 内数据传输，请先移除个人信息。

本说明对应当前源码，分支或后续版本可能不同。安全问题请参考[报告方式](../SECURITY.zh-CN.md)。
