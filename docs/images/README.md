# 界面截图来源

`phone-idle.png` 来自 `MonitorScreenTest.idleScreenExplainsWiringAndOffersPermission` 对实际 Compose 界面的 Robolectric 渲染，展示 Android 竖屏、未连接采集卡、未授予相机权限的首页。图片未合成视频内容，不作为真实 HDMI 采集验证证据。

重新生成：

```bash
./gradlew :androidApp:testDebugUnitTest --tests 'dev.icelum.pocketmonitor.MonitorScreenTest.idleScreenExplainsWiringAndOffersPermission' --rerun-tasks
```

输出为 `androidApp/build/reports/screenshots/phone-idle.png`。确认文字、布局与当前产品一致后，复制到本目录；不要复制包含私人视频、账号信息或设备序列号的图片。
