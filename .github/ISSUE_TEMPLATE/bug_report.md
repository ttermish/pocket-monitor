---
name: 故障报告 / Bug report
about: 报告可复现的软件、连接或显示问题
title: "[Bug] "
---

## 问题与复现

预期行为、实际行为，以及最短复现步骤：

1.
2.
3.

## 环境

- App 版本 / 提交、安装来源：
- 手机型号 / Android 版本：
- 采集卡型号 / USB VID:PID：
- 转接器、供电方式：
- HDMI 来源和输出规格：
- App 选择的采集格式：

## 已做的检查

- 是否允许相机和 USB 权限：
- 是否能在其他采集应用出画：
- 前后台切换、拔插后是否可恢复：
- 是实际 HDMI 画面、黑屏、彩条，还是完全没有视频帧：

## 日志或截图

可使用 `adb logcat -s PocketMonitor USBMonitor UVCCamera` 收集日志。请移除个人信息、USB 序列号和屏幕上的账号密码。安全漏洞请按 SECURITY.md 私下报告。
