# Contributing

**English** · [简体中文](CONTRIBUTING.zh-CN.md)

Issues and pull requests are welcome in English or Chinese. Read the product scope in [README](README.md) and the [development conventions (中文)](AGENTS.md) before changing code.

## Report a problem

Search existing issues and choose the appropriate template:

- Bugs: expected behavior, actual behavior and minimal reproduction steps.
- Hardware compatibility: phone and Android version, capture-card model and VID/PID, HDMI input and actual capture format. Distinguish real source video from a card-generated no-signal picture.
- Feature requests: the use case, current obstacle and desired outcome.

Remove personal information, USB serial numbers and private screen content from logs and screenshots. Report vulnerabilities through [SECURITY.md](SECURITY.md), without publishing exploit details in an issue.

## Development workflow

1. Fork the repository and create a focused branch from `main`, such as `fix/usb-reconnect`.
2. Follow the [build guide](docs/DEVELOPMENT.en.md) to configure JDK 17 and the Android SDK. Use the bundled Gradle Wrapper.
3. Keep each PR focused on one problem. Separate shared models from Android capture code, preserving session invalidation, threading and resource cleanup.
4. Add regression coverage for behavior changes and run the relevant checks.
5. Update affected documentation and open a PR describing the resulting behavior, verification and untested cases.

```bash
./gradlew :shared:jvmTest
./gradlew :androidApp:testDebugUnitTest :androidApp:lintDebug
./gradlew :androidApp:assembleDebug
python3 scripts/check_project.py
git diff --check
```

Documentation-only changes need content, link and whitespace checks. USB/native changes also need the [hardware checks (中文)](docs/VALIDATION.md). State when hardware is unavailable; do not present historical validation as checks performed for the current change.

## Commits and review

Use concise commit subjects such as `fix:`, `feat:`, `docs:`, `test:` or `build:`. Include the problem, final behavior, test results and related issues in the PR. Add screenshots when useful for reviewing a UI change.

Do not commit APKs, build caches, local SDK settings, credentials, signing files or unrelated formatting. Explain new dependencies, check their origin and license, and update [third-party notices](THIRD_PARTY_NOTICES.md). Preserve upstream attribution rather than claiming third-party work as original project code.

Follow the [code of conduct](CODE_OF_CONDUCT.md). Maintainers review changes for scope, maintainability and verification; there is no guaranteed review turnaround.

For UI text and screenshot updates, follow the [translation guide](docs/TRANSLATING.md).
