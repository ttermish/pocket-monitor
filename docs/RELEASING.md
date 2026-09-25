# 发行资料与流程

本文供维护者准备源码和 APK 发行使用，步骤不是已完成记录。版本发布说明位于 `docs/releases/`，真实验证结果位于 [VALIDATION.md](VALIDATION.md)。

## 准备版本

1. 核对待发布提交及工作区；确认根目录项目许可证、第三方声明与依赖材料完整。
2. 更新 `androidApp/build.gradle.kts` 的 `versionName` / `versionCode`，同步 README、CHANGELOG 和版本发行说明。
3. 使用[开发指南](DEVELOPMENT.md)中的环境执行共享测试、Android 单元测试、Lint 和 APK 构建。按验证清单执行硬件验收，未完成项目在发行说明中明确保留。
4. 检查最终 APK 的权限、ABI、签名和 native 库对齐；不同签名不能直接覆盖安装。Debug 包必须明确标为开发测试版。

## 附件准备

每个版本的附件应与同一提交和实际构建对应：

- 带版本号的 APK，以及 `SHA256SUMS`。
- 本项目对应源码，包含 Gradle Wrapper、构建配置、项目许可证、第三方声明和构建步骤；排除本地配置、密钥、缓存和构建输出。
- 实际使用的 UVCAndroid AAR、对应源码归档和许可材料。当前固定依赖与上游提交见 [THIRD_PARTY_NOTICES.md](../THIRD_PARTY_NOTICES.md)。

GitHub 自动生成的项目源码归档不包含 Maven 依赖源码。`artifacts/` 被 Git 忽略，本地存在文件不意味着接收 APK 的用户也能获得它们；必须检查实际上传的附件。

UVCAndroid 包含 LGPL-2.1 的 libusb。分发时应保留相关声明，并准备对应库源码和修改、重建及重新链接所需材料，核对最终二进制的实际链接方式。不能仅凭上游下载链接或“库未修改”推定分发资料已齐全。参见 [LGPL-2.1 第 4、6 节](https://www.gnu.org/licenses/old-licenses/lgpl-2.1.en.html)。

若修改 UVCAndroid 或 native 依赖，附上匹配的修改源码、补丁、工具链和重建步骤，并验证替换库后可重新构建安装。现有 App 构建步骤只验证使用预构建 AAR；不应描述为已完成上游 native 源码重建验证。

## 发布和复核

按用户授权创建版本标签和 GitHub Release，上传经过校验的附件。下载上传后的文件重新检查 SHA-256、附件名称、源码对应关系和发行说明链接。创建 Release、公开仓库与推送代码分别处理，不把其中一项完成写成其他项也已完成。

第三方版本或文件变化时，更新来源、哈希和许可证。不要复用旧版本的校验清单，也不要在发行说明中声称未经实际执行的测试、硬件兼容性或 native 重建已经通过。
