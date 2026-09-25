# Translations / 翻译

The app uses Compose Multiplatform string resources. English defaults are in `shared/src/commonMain/composeResources/values/strings.xml`; Simplified Chinese is in `values-zh/strings.xml`. Android launcher labels are in `androidApp/src/main/res/values*/strings.xml`.

Keep resource keys and numbered placeholders such as `%1$d` and `%1$s` identical across languages. Put user-visible errors in resources and map them from `CaptureMessage`; do not store translated messages in the capture controller. Keep hardware limitations and troubleshooting advice accurate.

Run `python3 scripts/check_project.py` for key/placeholder parity and local documentation targets. UI changes also require `./gradlew :androidApp:testDebugUnitTest :androidApp:lintDebug`. The `AppSettingsTest` screenshot test renders the actual Preview, Devices and Settings pages in both languages to `androidApp/build/reports/screenshots/`; inspect them before copying them to `docs/images/`.

To add a language, supply a `values-<language>/strings.xml`, update `AppLanguage` and the settings labels, and test runtime switching and fallback. The language picker uses native language names so users can recover after choosing an unfamiliar language.

## 中文

应用使用 Compose Multiplatform 字符串资源。默认英文位于 `shared/src/commonMain/composeResources/values/strings.xml`，简体中文位于 `values-zh/strings.xml`；Android 启动器名称位于 `androidApp/src/main/res/values*/strings.xml`。

各语言保持相同资源键及 `%1$d`、`%1$s` 等编号占位符。用户可见错误放入资源，通过 `CaptureMessage` 映射，不在采集控制器保存翻译文本。硬件限制和排障说明需与实际能力一致。

运行 `python3 scripts/check_project.py` 检查翻译和本地文档链接；UI 变更同时运行 Android 测试和 Lint。`AppSettingsTest` 会在 `androidApp/build/reports/screenshots/` 生成两种语言的预览、设备和设置页截图，检查后再复制到 `docs/images/`。

新增语言时，添加 `values-<语言>/strings.xml`，更新 `AppLanguage` 与设置选项，并测试即时切换和回退。语言选项使用各语言自己的名称，方便用户切回。
