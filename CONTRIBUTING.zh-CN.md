# 参与贡献

[English](CONTRIBUTING.md) · **简体中文**

欢迎通过 Issue 和 Pull Request 改善随身屏。交流可使用中文或英文；请先阅读 [README](README.zh-CN.md) 的功能边界和 [AGENTS.md](AGENTS.md) 的开发约定。

## 报告问题

先搜索已有 Issue，并使用对应模板：

- 软件或连接故障：说明预期行为、实际行为和最短复现步骤。
- 采集卡兼容性：提供手机、Android 版本、采集卡型号及 VID/PID、HDMI 输入和实际采集格式，并区分实际画面与卡自生彩条。
- 功能建议：说明使用场景、当前阻碍和期望结果。

日志和截图提交前移除账号、USB 序列号、设备路径中的个人信息及画面隐私。安全漏洞按 [SECURITY.md](SECURITY.zh-CN.md) 私下报告，不在公开 Issue 中提供利用细节。

## 开发流程

1. Fork 仓库并从 `main` 创建主题分支，例如 `fix/usb-reconnect`。仓库私有时需先取得访问权限。
2. 按[开发指南](docs/DEVELOPMENT.md)配置 JDK 17 和 Android SDK，使用仓库自带 Gradle Wrapper。
3. 让一个 PR 聚焦一个问题。保持共享模型与 Android 平台采集层分离，保留连接代次、线程和资源释放约束。
4. 对有行为变化的逻辑补充能复现问题的回归测试，并运行与改动对应的检查。
5. 更新受影响的使用说明，提交 PR，说明行为变化、验证结果和未验证项目。

```bash
./gradlew :shared:jvmTest
./gradlew :androidApp:testDebugUnitTest :androidApp:lintDebug
./gradlew :androidApp:assembleDebug
git diff --check
```

纯文档改动只需检查内容、链接和空白格式。USB/native 变化还需要按[硬件验收清单](docs/VALIDATION.md)验证；没有设备时如实说明。不要把已有验证记录复制成此次测试结果。

## 提交与评审

提交信息可使用 `fix:`、`feat:`、`docs:`、`test:` 或 `build:` 前缀，简短说明改变了什么。PR 应包含问题背景、最终行为、测试结果及关联 Issue；截图仅在有助于评审界面变化时提供。

不提交 APK、构建缓存、`local.properties`、凭据、签名文件和无关格式化变更。新增依赖时说明用途，核对来源与许可证，并更新[第三方声明](THIRD_PARTY_NOTICES.md)。保持上游版权和许可声明，不将他人代码声明为本项目原创。

遵守[社区行为准则](CODE_OF_CONDUCT.md)。讨论保持友善，围绕事实和代码提供反馈，允许不同经验的贡献者提问。维护者按项目范围、可维护性和验证情况评审；不承诺固定处理时限。
