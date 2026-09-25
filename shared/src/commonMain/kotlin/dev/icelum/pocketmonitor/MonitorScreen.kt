package dev.icelum.pocketmonitor

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Ink = Color(0xFF101619)
private val Panel = Color(0xFF1B2328)
private val Muted = Color(0xFF9CAEB6)
private val Lime = Color(0xFFB8ED82)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonitorScreen(
    state: CaptureState,
    fullscreen: Boolean,
    onFullscreen: (Boolean) -> Unit,
    onConnect: (String?) -> Unit,
    onRefresh: () -> Unit,
    onDisconnect: () -> Unit,
    onMode: (VideoMode) -> Unit,
    onAppSettings: () -> Unit,
    preview: @Composable (Modifier) -> Unit,
) {
    var help by rememberSaveable { mutableStateOf(false) }
    var settings by rememberSaveable { mutableStateOf(false) }
    var rotation by rememberSaveable { mutableIntStateOf(0) }
    var zoom by remember { mutableFloatStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    MaterialTheme(colorScheme = darkColorScheme(primary = Lime, onPrimary = Ink,
        background = Ink, surface = Panel, onSurface = Color(0xFFF0F5F6), onSurfaceVariant = Muted)) {
        Surface(Modifier.fillMaxSize(), color = Ink) {
            BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding()) {
                val compact = maxHeight < 500.dp
                Column(Modifier.fillMaxSize().padding(if (fullscreen) 0.dp else 20.dp),
                    verticalArrangement = Arrangement.spacedBy(if (fullscreen) 0.dp else 16.dp)) {
                    if (!fullscreen) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(Lime), contentAlignment = Alignment.Center) {
                                Icon(Icons.Outlined.Monitor, null, tint = Ink)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("随身屏", fontSize = 23.sp, fontWeight = FontWeight.Bold)
                                Text("POCKET MONITOR", color = Muted, fontSize = 10.sp, letterSpacing = 2.sp)
                            }
                            IconButton(onClick = { help = true }, modifier = Modifier.testTag("help")) {
                                Icon(Icons.AutoMirrored.Outlined.HelpOutline, "连接指南")
                            }
                        }
                    }
                    Box(Modifier.fillMaxWidth().then(
                        if (fullscreen || compact) Modifier.weight(1f) else Modifier.aspectRatio(1.3f)
                    ).clip(RoundedCornerShape(if (fullscreen) 0.dp else 22.dp)).background(Color.Black)
                        .border(1.dp, Color(0xFF2C373D), RoundedCornerShape(if (fullscreen) 0.dp else 22.dp))) {
                        // Always keep a real surface mounted, even while an empty-state overlay is visible.
                        BoxWithConstraints(Modifier.fillMaxSize().clip(RoundedCornerShape(0.dp))
                            .pointerInput(rotation, state.activeMode) {
                                detectTransformGestures { _, delta, scale, _ ->
                                    zoom = (zoom * scale).coerceIn(1f, 4f)
                                    pan = if (zoom == 1f) Offset.Zero else Offset(
                                        (pan.x + delta.x).coerceIn(-size.width * (zoom - 1) / 2, size.width * (zoom - 1) / 2),
                                        (pan.y + delta.y).coerceIn(-size.height * (zoom - 1) / 2, size.height * (zoom - 1) / 2))
                                }
                            }.pointerInput(Unit) { detectTapGestures(onDoubleTap = { zoom = 1f; pan = Offset.Zero }) },
                            contentAlignment = Alignment.Center) {
                            val mode = state.activeMode
                            val extent = fittedVideoExtent(maxWidth.value, maxHeight.value,
                                mode?.width ?: 16, mode?.height ?: 9, rotation)
                            preview(Modifier.requiredSize(extent.width.dp, extent.height.dp).graphicsLayer {
                                rotationZ = rotation.toFloat()
                                scaleX = zoom; scaleY = zoom
                                translationX = pan.x; translationY = pan.y
                            })
                        }
                        if (state.phase != CapturePhase.Streaming) {
                            Column(Modifier.fillMaxSize().background(Color(0xFF080D10)).padding(24.dp),
                                verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                                if (state.busy || state.phase == CapturePhase.WaitingForFrames) {
                                    CircularProgressIndicator(Modifier.size(36.dp), color = Lime, strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Outlined.Cable, null, Modifier.size(46.dp), tint = Lime)
                                }
                                Spacer(Modifier.height(16.dp))
                                Text(phaseTitle(state), fontSize = 19.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                                Spacer(Modifier.height(8.dp))
                                Text(state.message ?: phaseHint(state), color = Muted, fontSize = 12.sp,
                                    textAlign = TextAlign.Center, lineHeight = 19.sp)
                            }
                        }
                        Row(Modifier.align(Alignment.TopStart).padding(14.dp).clip(CircleShape)
                            .background(Color(0xC020292E)).padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(Modifier.size(6.dp).background(if (state.phase == CapturePhase.Streaming) Lime else Muted, CircleShape))
                            Text(if (state.phase == CapturePhase.Streaming) "实时画面" else "HDMI IN", fontSize = 10.sp, color = Color.White)
                        }
                        if (state.phase == CapturePhase.Streaming) {
                            Text("${state.framesPerSecond} fps · ${state.activeMode?.width} × ${state.activeMode?.height}",
                                Modifier.align(Alignment.BottomStart).padding(14.dp).background(Color(0xB3101619), CircleShape)
                                    .padding(horizontal = 10.dp, vertical = 5.dp), color = Color.White, fontSize = 11.sp)
                        }
                        if (fullscreen) {
                            FilledTonalIconButton(onClick = { onFullscreen(false) },
                                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).testTag("exit_fullscreen")) {
                                Icon(Icons.Outlined.FullscreenExit, "退出全屏")
                            }
                        }
                    }
                    if (!fullscreen) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            Tool(Icons.Outlined.ZoomOutMap, "复位") { zoom = 1f; pan = Offset.Zero; rotation = 0 }
                            Tool(Icons.Outlined.Rotate90DegreesCcw, "旋转") { rotation = (rotation + 90) % 360; zoom = 1f; pan = Offset.Zero }
                            Tool(Icons.Outlined.Tune, "视频设置") { settings = true }
                            Tool(Icons.Outlined.Fullscreen, "全屏") { onFullscreen(true) }
                        }
                        if (!compact) {
                            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                DevicePanel(state, onConnect, onRefresh, onDisconnect, onAppSettings)
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                                    Step("01", "Mac 输出", "HDMI 接口", Modifier.weight(1f))
                                    Step("02", "视频采集卡", "USB-C 接手机", Modifier.weight(1f))
                                    Step("03", "允许访问", "开始预览", Modifier.weight(1f))
                                }
                                Text("仅在手机本地预览，不录制、不上传。", color = Muted, fontSize = 11.sp,
                                    modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                            }
                        }
                    }
                }
            }
        }
        if (help) AlertDialog(onDismissRequest = { help = false },
            title = { Text("把手机变成显示屏") },
            text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("1. 将 Mac 的 HDMI 输出接到采集卡 HDMI IN。没有 HDMI 口时，使用支持视频输出的 Type-C 转 HDMI 转接器。")
                Text("2. 将采集卡的 USB-C 接到支持 OTG 的 Android 手机；需要时打开手机的 OTG 开关。")
                Text("3. 允许相机权限和 USB 访问，再点击连接。Mac 显示设置选择「镜像」可看到同一桌面；扩展模式会显示第二块桌面。")
                Text("黑屏或彩条：检查 HDMI、Mac 外接显示设置及采集卡供电。收到采集卡的视频帧，不代表 HDMI 输入一定有信号。")
                Text("双指缩放和拖动可查看细节，双击画面复位。此版本仅支持视频预览，不支持键鼠回传、音频或录制。")
                Text("普通 UVC 采集卡的 iPhone 直连接入未实现。", color = Muted)
                Text("视频组件：UVCAndroid 1.0.13（Apache-2.0）；完整第三方许可随 APK 提供。", color = Muted, fontSize = 11.sp)
            } }, confirmButton = { TextButton(onClick = { help = false }) { Text("知道了") } })
        if (settings) ModalBottomSheet(onDismissRequest = { settings = false }, containerColor = Panel) {
            Column(Modifier.fillMaxWidth().fillMaxHeight(0.85f).verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("视频设置", fontSize = 23.sp, fontWeight = FontWeight.Bold)
                Text("采集设备", color = Muted)
                if (state.devices.isEmpty()) Text("尚未发现 UVC 视频设备。请检查 OTG 和连接线。", fontSize = 13.sp)
                state.devices.forEach { device ->
                    OutlinedButton(onClick = { onConnect(device.id) }, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
                        Text(device.name + if (device.id == state.selectedDeviceId) " · 已选择" else "")
                    }
                }
                OutlinedButton(onClick = onRefresh) { Text("刷新设备") }
                HorizontalDivider(color = Color(0xFF334047))
                Text("采集格式", color = Muted)
                Text("先连接采集卡，再选择它支持的格式。USB 2.0 建议先用 720p / 30 fps。", fontSize = 12.sp, color = Muted)
                state.modes.forEach { mode ->
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(if (mode == state.activeMode) Color(0xFF2D3B2C) else Color.Transparent)
                        .clickable(enabled = !state.busy) { onMode(mode) }.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(mode.label, Modifier.weight(1f), fontSize = 13.sp)
                        if (mode == state.activeMode) Icon(Icons.Outlined.Check, "当前格式", tint = Lime)
                    }
                }
                if (state.selectedDevice != null) {
                    HorizontalDivider(color = Color(0xFF334047))
                    val device = state.selectedDevice!!
                    Text("USB ID  ${device.vendorId.toString(16).padStart(4, '0')}:${device.productId.toString(16).padStart(4, '0')}",
                        color = Muted, fontSize = 12.sp)
                }
                TextButton(onClick = { onDisconnect(); settings = false }) { Text("断开连接") }
            }
        }
    }
}

@Composable
private fun DevicePanel(state: CaptureState, onConnect: (String?) -> Unit, onRefresh: () -> Unit,
    onDisconnect: () -> Unit, onAppSettings: () -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Panel).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Usb, null, tint = Lime)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(state.selectedDevice?.name ?: "USB 视频输入", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text(if (state.devices.isEmpty()) "等待接入采集卡" else "发现 ${state.devices.size} 个视频设备", color = Muted, fontSize = 12.sp)
            }
            IconButton(onClick = onRefresh) { Icon(Icons.Outlined.Refresh, "刷新设备", tint = Muted) }
        }
        if (!state.usbHostSupported) Text("此设备未声明 USB Host 支持，需要换用支持 OTG 的手机。", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
        if (state.phase == CapturePhase.Streaming) {
            OutlinedButton(onClick = onDisconnect, modifier = Modifier.fillMaxWidth()) { Text("停止预览") }
        } else {
            Button(onClick = { onConnect(state.selectedDeviceId ?: state.devices.firstOrNull()?.id) },
                enabled = !state.busy && state.usbHostSupported,
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("connect"), shape = RoundedCornerShape(12.dp)) {
                Text(when {
                    state.busy -> "正在连接…"
                    !state.cameraPermission -> "允许视频访问"
                    state.phase == CapturePhase.Error || state.phase == CapturePhase.Stalled -> "重新连接"
                    state.devices.isEmpty() -> "查找采集卡"
                    else -> "连接采集卡"
                }, fontWeight = FontWeight.SemiBold)
            }
        }
        if (!state.cameraPermission) Row(verticalAlignment = Alignment.CenterVertically) {
            Text("读取 USB 视频需要相机权限", color = Muted, fontSize = 11.sp, modifier = Modifier.weight(1f))
            TextButton(onClick = onAppSettings) { Text("系统设置", fontSize = 11.sp) }
        }
    }
}

@Composable private fun Tool(icon: ImageVector, label: String, action: () -> Unit) {
    Column(Modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = action).padding(horizontal = 14.dp, vertical = 9.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(icon, label, tint = Color(0xFFD4DEE2), modifier = Modifier.size(23.dp))
        Text(label, fontSize = 10.sp, color = Muted)
    }
}

@Composable private fun Step(number: String, title: String, subtitle: String, modifier: Modifier) {
    Column(modifier.padding(vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(number, color = Lime, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Text(title, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        Text(subtitle, fontSize = 10.sp, color = Muted)
    }
}

private fun phaseTitle(state: CaptureState): String = when (state.phase) {
    CapturePhase.Idle -> "等待画面接入"
    CapturePhase.Permission -> "允许 USB 访问"
    CapturePhase.Connecting -> "正在打开采集卡"
    CapturePhase.WaitingForFrames -> "等待视频帧"
    CapturePhase.Streaming -> "实时画面"
    CapturePhase.Stalled -> "视频流暂时中断"
    CapturePhase.Error -> "连接未完成"
    CapturePhase.Paused -> "预览已暂停"
}
private fun phaseHint(state: CaptureState): String = when (state.phase) {
    CapturePhase.Permission -> "请在系统弹窗中允许访问采集卡"
    CapturePhase.Connecting -> "正在协商视频格式"
    CapturePhase.WaitingForFrames -> "USB 已连接，正在等待采集卡输出画面"
    else -> "将电脑的 HDMI 输出接入采集卡\n再把采集卡连接到手机"
}
