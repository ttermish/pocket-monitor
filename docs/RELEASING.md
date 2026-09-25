# 发行资料与流程

本文供维护者准备源码和 APK 发行使用，步骤不是已完成记录。版本发布说明位于 `docs/releases/`，真实验证结果位于 [VALIDATION.md](VALIDATION.md)。

## GitHub Actions

| 工作流 | 触发方式 | 结果 |
| --- | --- | --- |
| `Android CI` | 推送 `main`、向 `main` 提 PR | 共享测试、Android 单元测试、Debug Lint；分支构建上传 Debug APK，报告保留 14 天，不发布 Release |
| `Android Release` | 推送 `v*` 标签 | 标签必须匹配 Android versionName，提交必须属于 `main` 历史；构建通过后创建 GitHub Release并上传附件 |

签名发布只接受标签触发，不提供从 `main` 手动运行的入口。完成后从 GitHub Release 下载 `pocket-monitor-<版本>-release.apk` 安装；AAB 用于后续商店上传，不能直接安装。整套附件也上传为 Actions artifact，保留 30 天。

版本发行说明顶部应放置带明确版本的 APK 下载链接；标签推送后、签名工作流完成前，GitHub 标签页可能只有自动生成的 Source code，不能把它视为已发布安装包。若工作流失败，修复后重跑对应失败任务并检查附件，不能用新的标签覆盖同版本。

标签发行前，先更新版本号和 `docs/releases/v<版本>.md`，提交并推送 `main`，再创建新标签。不要移动已经发布或用于其他构建的旧标签。仓库仍为私有时，Actions 产物及 Release 也需要仓库访问权限。

## 签名 Environment

`release` GitHub Environment 仅允许 `v*` 标签。CI / PR 检查不读取签名信息。Environment 中配置：

| 名称 | 类型 | 内容 |
| --- | --- | --- |
| `ANDROID_KEYSTORE_BASE64` | Secret | Release keystore 的单行 Base64 内容 |
| `ANDROID_KEYSTORE_PASSWORD` | Secret | keystore 密码 |
| `ANDROID_KEY_ALIAS` | Secret | 签名密钥别名 |
| `ANDROID_KEY_PASSWORD` | Secret | 密钥密码 |
| `ANDROID_SIGNING_CERT_SHA256` | Variable | 签名证书 SHA-256，小写十六进制、无冒号 |

工作流将 keystore 临时还原到 runner 的临时目录，以 `ANDROID_KEYSTORE_PATH` 传给 Gradle；构建结束后清理。密码仅在签名构建步骤作为环境变量注入，Release 任务不保存 Gradle 缓存。缺少任何签名配置即失败，不回退为 Debug 签名或未签名产物。

发布前验证 APK 签名与 Environment 中的证书指纹一致、AAB 签名可验证、APK 满足 16 KB ZIP 对齐，并核对附件 SHA-256。ZIP 对齐不等同于完成 native ELF 对齐或真实设备兼容性验收。

签名材料需另行保留私密备份，GitHub Secrets 不提供明文取回功能。后续升级应沿用同一密钥；不要把 keystore、密码或备份放进仓库和发布附件。Release 签名与此前 Debug 签名不同，不能覆盖安装旧 Debug 包；卸载旧包会清除本地设置。

## 准备版本

1. 核对待发布提交及工作区；确认根目录项目许可证、第三方声明与依赖材料完整。
2. 更新 `androidApp/build.gradle.kts` 的 `versionName` / `versionCode`，同步 README、CHANGELOG 和版本发行说明。
3. 使用[开发指南](DEVELOPMENT.md)中的环境执行共享测试、Android 单元测试、Lint 和 APK 构建。按验证清单执行硬件验收，未完成项目在发行说明中明确保留。
4. 检查最终 APK 的权限、ABI、签名和 native 库对齐；不同签名不能直接覆盖安装。Debug 包必须明确标为开发测试版。

## 附件准备

每个版本的附件应与同一提交和实际构建对应：

- 带版本号的签名 APK、AAB，以及 `SHA256SUMS` 和 `BUILD_INFO.json`（记录版本、提交与证书指纹）。
- 本项目对应源码，包含 Gradle Wrapper、构建配置、项目许可证、第三方声明和构建步骤；排除本地配置、密钥、缓存和构建输出。
- 实际使用的 UVCAndroid AAR、对应源码归档和许可材料。当前固定依赖与上游提交见 [THIRD_PARTY_NOTICES.md](../THIRD_PARTY_NOTICES.md)。

GitHub 自动生成的项目源码归档不包含 Maven 依赖源码。`artifacts/` 被 Git 忽略，本地存在文件不意味着接收 APK 的用户也能获得它们；必须检查实际上传的附件。

`scripts/package_release.py` 从实际 APK metadata 读取版本，生成对应提交的源码 ZIP，并下载、校验固定版本的 UVCAndroid AAR 和源码归档。所有输出写入 `artifacts/release/`，要求目录为空以避免混入旧版本。GitHub 工作流会自动执行该脚本并上传整套附件。

UVCAndroid 包含 LGPL-2.1 的 libusb。分发时应保留相关声明，并准备对应库源码和修改、重建及重新链接所需材料，核对最终二进制的实际链接方式。不能仅凭上游下载链接或“库未修改”推定分发资料已齐全。参见 [LGPL-2.1 第 4、6 节](https://www.gnu.org/licenses/old-licenses/lgpl-2.1.en.html)。

若修改 UVCAndroid 或 native 依赖，附上匹配的修改源码、补丁、工具链和重建步骤，并验证替换库后可重新构建安装。现有 App 构建步骤只验证使用预构建 AAR；不应描述为已完成上游 native 源码重建验证。

## 发布和复核

按用户授权创建版本标签和 GitHub Release，上传经过校验的附件。下载上传后的文件重新检查 SHA-256、附件名称、源码对应关系和发行说明链接。创建 Release、公开仓库与推送代码分别处理，不把其中一项完成写成其他项也已完成。

第三方版本或文件变化时，更新来源、哈希和许可证。不要复用旧版本的校验清单，也不要在发行说明中声称未经实际执行的测试、硬件兼容性或 native 重建已经通过。
