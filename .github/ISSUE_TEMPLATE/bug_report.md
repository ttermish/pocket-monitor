---
name: 故障报告 / Bug report
about: Report reproducible app, connection or display problems / 报告可复现的软件、连接或显示问题
title: "[Bug] "
---

## Problem and reproduction / 问题与复现

Expected behavior, actual behavior and minimal reproduction steps / 预期行为、实际行为，以及最短复现步骤：

1.
2.
3.

## Environment / 环境

- App version / commit and installation source / 版本、提交与安装来源：
- Phone model and Android version / 手机型号与 Android 版本：
- Capture-card model and USB VID:PID / 采集卡型号与 USB VID:PID：
- Adapters and power supply / 转接器与供电方式：
- HDMI source and output format / HDMI 来源和输出规格：
- Selected capture format / App 选择的采集格式：

## Checks already tried / 已做的检查

- Camera and USB access allowed / 是否允许相机和 USB 权限：
- Does another capture app work / 是否能在其他采集应用出画：
- Background/resume and unplug/replug behavior / 前后台切换、拔插后是否可恢复：
- Actual source video, black frames, color bars or no frames / 实际 HDMI 画面、黑屏、彩条或完全无帧：

## Logs or screenshots / 日志或截图

Use `adb logcat -s PocketMonitor USBMonitor UVCCamera` to collect logs. Remove personal information, USB serial numbers and private screen content. Report security vulnerabilities privately using SECURITY.md.

可使用 `adb logcat -s PocketMonitor USBMonitor UVCCamera` 收集日志。请移除个人信息、USB 序列号和屏幕上的账号密码。安全漏洞请按 SECURITY.md 私下报告。
