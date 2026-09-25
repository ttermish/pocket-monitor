package dev.icelum.pocketmonitor

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.hardware.usb.*
import android.os.*
import android.util.Log
import android.view.Surface
import androidx.core.content.ContextCompat
import com.serenegiant.usb.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicLong

/** Main-thread commands; USB/native work is serialized on a dedicated Looper. */
class UvcCaptureController(private val context: Context) {
    private val usb = context.getSystemService(UsbManager::class.java)
    private val main = Handler(Looper.getMainLooper())
    private val thread = HandlerThread("UvcPreview").apply { start() }
    private val worker = Handler(thread.looper)
    private val session = CaptureSession()
    private val epoch = AtomicLong()
    private var frames = PreviewFrames()
    private val modeStore = SuccessfulModeStore(context)
    private val mutableState = MutableStateFlow(CaptureState(
        usbHostSupported = context.packageManager.hasSystemFeature(PackageManager.FEATURE_USB_HOST)))
    val state = mutableState.asStateFlow()
    private var foreground = false
    private var destroyed = false
    private var monitor: USBMonitor? = null
    private var surface: Surface? = null
    private var rememberedDevice: Pair<Int, Int>? = null
    private var recovery = ModeRecovery()
    private var modeSaved = false
    private var openedAt = 0L
    private var previousCount = 0L
    private var previousSampleAt = 0L
    // Accessed only on the native worker thread.
    private var camera: UVCCamera? = null
    private var controlBlock: USBMonitor.UsbControlBlock? = null

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            refreshDevices()
            val selected = state.value.selectedDeviceId
            if (selected != null && state.value.devices.none { it.id == selected }) {
                disconnect(CapturePhase.Idle, CaptureMessage.Unplugged)
            }
            reconnectRememberedDevice()
        }
    }

    fun start() {
        if (foreground || destroyed) return
        foreground = true
        ContextCompat.registerReceiver(context, receiver, IntentFilter().apply {
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        }, ContextCompat.RECEIVER_EXPORTED)
        updatePermission()
        refreshDevices()
        if (state.value.phase == CapturePhase.Paused) mutableState.value = state.value.copy(phase = CapturePhase.Idle)
        reconnectRememberedDevice()
        main.post(stats)
    }

    fun stop() {
        if (!foreground) return
        foreground = false
        context.unregisterReceiver(receiver)
        main.removeCallbacks(stats)
        disconnect(CapturePhase.Paused, CaptureMessage.ResumeOnReturn)
    }

    fun destroy() {
        if (destroyed) return
        stop()
        destroyed = true
        worker.post { thread.quitSafely() }
    }

    fun updatePermission() {
        val allowed = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        mutableState.value = state.value.copy(cameraPermission = allowed)
    }

    fun permissionDenied() {
        updatePermission()
        mutableState.value = state.value.copy(phase = CapturePhase.Error,
            message = CaptureMessage.CameraPermissionRequired)
    }

    fun refreshDevices() {
        val devices = usb.deviceList.values.filter(::isVideoDevice).map { device ->
            CaptureDevice(device.deviceName, device.productName?.takeIf { it.isNotBlank() } ?: "",
                device.vendorId, device.productId)
        }.sortedBy { it.id }
        mutableState.value = state.value.copy(devices = devices)
    }

    private fun reconnectRememberedDevice() {
        if (!foreground || !state.value.cameraPermission || surface == null || monitor != null) return
        val key = rememberedDevice ?: return
        val matches = state.value.devices.filter { it.vendorId == key.first && it.productId == key.second }
        // Do not silently choose between two identical cards.
        if (matches.size == 1) connect(matches.single().id)
    }

    fun connect(id: String) {
        beginConnection(id, null, ModeRecovery())
    }

    private fun beginConnection(id: String, requested: VideoMode?, attempts: ModeRecovery) {
        if (!foreground || destroyed) return
        updatePermission()
        if (!state.value.cameraPermission) { permissionDenied(); return }
        val device = usb.deviceList[id]?.takeIf(::isVideoDevice) ?: run {
            refreshDevices()
            mutableState.value = state.value.copy(phase = CapturePhase.Error, message = CaptureMessage.DeviceMissing)
            return
        }
        disconnect(CapturePhase.Idle)
        rememberedDevice = device.vendorId to device.productId
        recovery = attempts
        modeSaved = false
        val preferred = requested ?: modeStore.read(device.vendorId, device.productId)
        val currentFrames = PreviewFrames()
        frames = currentFrames
        val token = session.begin(id)
        epoch.set(token)
        mutableState.value = state.value.copy(phase = CapturePhase.Permission, selectedDeviceId = id,
            activeMode = null, modes = emptyList(), message = if (attempts.failures.isEmpty()) null else CaptureMessage.Recovering,
            recoveryAttempt = attempts.failures.size)
        val currentMonitor = USBMonitor(context, object : USBMonitor.OnDeviceConnectListener {
            private fun current() = foreground && session.accepts(token, id)
            override fun onAttach(device: UsbDevice) { if (current()) refreshDevices() }
            override fun onDetach(device: UsbDevice) {
                if (current() && device.deviceName == id) {
                    disconnect(CapturePhase.Idle, CaptureMessage.Disconnected)
                    refreshDevices()
                }
            }
            override fun onDeviceOpen(device: UsbDevice, block: USBMonitor.UsbControlBlock, createNew: Boolean) {
                if (!current() || device.deviceName != id || state.value.phase != CapturePhase.Permission) return
                mutableState.value = state.value.copy(phase = CapturePhase.Connecting)
                openCamera(token, id, block, surface, preferred, attempts, currentFrames)
            }
            override fun onDeviceClose(device: UsbDevice, block: USBMonitor.UsbControlBlock) {
                if (current() && device.deviceName == id) {
                    disconnect(CapturePhase.Error, CaptureMessage.UsbClosed)
                    refreshDevices()
                }
            }
            override fun onCancel(device: UsbDevice) {
                if (current()) {
                    rememberedDevice = null
                    disconnect(CapturePhase.Error, CaptureMessage.UsbPermissionDenied)
                }
            }
            override fun onError(device: UsbDevice, e: USBMonitor.USBException) {
                if (current()) fail(token, id, CaptureMessage.UsbOpenFailed, e)
            }
        }, main)
        monitor = currentMonitor
        try {
            currentMonitor.register()
            worker.post {
                if (epoch.get() == token) currentMonitor.requestPermission(device)
            }
        } catch (e: Exception) { fail(token, id, CaptureMessage.UsbPermissionFailed, e) }
    }

    fun selectMode(mode: VideoMode) {
        val id = state.value.selectedDeviceId ?: return
        if (mode !in state.value.modes || state.value.busy) return
        beginConnection(id, mode, ModeRecovery())
    }

    fun retry() {
        refreshDevices()
        val selected = state.value.selectedDeviceId
        val device = state.value.devices.find { it.id == selected } ?: state.value.devices.singleOrNull()
        if (device != null) connect(device.id)
    }

    fun userDisconnect() {
        rememberedDevice = null
        disconnect(CapturePhase.Idle, CaptureMessage.Stopped)
    }

    private fun openCamera(token: Long, id: String, block: USBMonitor.UsbControlBlock, output: Surface?,
        preferred: VideoMode?, attempts: ModeRecovery, currentFrames: PreviewFrames) {
        worker.post {
            if (epoch.get() != token) return@post
            var opened: UVCCamera? = null
            var chosen: VideoMode? = null
            var modes = emptyList<VideoMode>()
            try {
                controlBlock = block
                opened = UVCCamera(UVCParam(null, UVCCamera.getRecommendedPlatformQuirks()))
                val result = opened.open(block)
                check(result == 0) { "UVC open returned $result" }
                val sizes = opened.supportedSizeList.orEmpty()
                modes = preferredModes(sizes.flatMap { size ->
                    val encoding = VideoEncoding.entries.find { it.uvcType == size.type } ?: return@flatMap emptyList()
                    (size.fpsList.orEmpty() + size.fps).distinct().filter { it > 0 }.map {
                        VideoMode(size.width, size.height, it, encoding)
                    }
                })
                val candidates = attempts.candidates(modes, preferred)
                for (mode in candidates.distinct()) {
                    if (epoch.get() != token) break
                    try {
                        opened.setPreviewSize(Size(mode.encoding.uvcType, mode.width, mode.height, mode.fps, listOf(mode.fps)))
                        chosen = mode
                        break
                    } catch (e: IllegalArgumentException) { Log.w(TAG, "Rejected mode: $mode", e) }
                }
                check(chosen != null) { "No supported MJPEG/YUY2 preview mode could be negotiated" }
                if (epoch.get() != token) { opened.destroy(true); return@post }
                camera = opened
                val actual = checkNotNull(chosen)
                // Publish the negotiated mode before native callbacks can publish the first frame.
                main.post {
                    if (!session.accepts(token, id)) return@post
                    openedAt = SystemClock.elapsedRealtime()
                    previousSampleAt = openedAt
                    previousCount = 0
                    mutableState.value = state.value.copy(phase = CapturePhase.WaitingForFrames,
                        modes = modes, activeMode = actual)
                }
                opened.setFrameCallback({ _ ->
                    if (epoch.get() == token) {
                        if (currentFrames.record(SystemClock.elapsedRealtime())) main.post {
                            if (session.accepts(token, id)) {
                                mutableState.value = state.value.copy(phase = CapturePhase.Streaming, message = null)
                            }
                        }
                    }
                }, UVCCamera.PIXEL_FORMAT_RAW)
                if (output != null && output.isValid) {
                    output.setFrameRateCompat(actual.fps)
                    opened.setPreviewDisplay(output)
                    opened.startPreview()
                }
            } catch (e: Exception) {
                runCatching { opened?.destroy(true) }
                camera = null
                main.post {
                    if (!session.accepts(token, id)) return@post
                    val failedMode = chosen
                    if (failedMode == null || !tryLowerMode(id, failedMode, modes, attempts))
                        fail(token, id, CaptureMessage.VideoStartFailed, e)
                }
            } catch (e: LinkageError) {
                main.post { fail(token, id, CaptureMessage.NativeUnavailable, e) }
            }
        }
    }

    fun attachSurface(output: Surface) {
        surface = output
        val token = epoch.get()
        worker.post {
            if (epoch.get() == token && output.isValid) camera?.let {
                it.setPreviewDisplay(output)
                it.startPreview()
            }
        }
        reconnectRememberedDevice()
    }

    /** Release TextureView's surface only after native rendering has stopped. */
    fun detachSurface(output: Surface, releaseTexture: () -> Unit) {
        if (surface === output) {
            surface = null
            disconnect(CapturePhase.Paused)
        }
        if (!worker.post { output.release(); releaseTexture() }) {
            // The worker quits only after queued native cleanup has finished.
            output.release()
            releaseTexture()
        }
    }

    private fun disconnect(phase: CapturePhase, message: CaptureMessage? = null) {
        session.invalidate()
        epoch.set(session.generation)
        val oldMonitor = monitor
        monitor = null
        oldMonitor?.unregister()
        worker.post {
            runCatching { camera?.destroy(true) }.onFailure { Log.w(TAG, "Close camera", it) }
            camera = null
            runCatching { controlBlock?.close(true) }
            controlBlock = null
            oldMonitor?.destroy()
        }
        frames = PreviewFrames()
        mutableState.value = state.value.copy(phase = phase, activeMode = null, modes = emptyList(),
            framesPerSecond = 0, message = message, recoveryAttempt = 0)
    }

    private fun fail(token: Long, id: String, message: CaptureMessage, error: Throwable) {
        if (!session.accepts(token, id)) return
        Log.e(TAG, message.name, error)
        disconnect(CapturePhase.Error, message)
    }

    private fun tryLowerMode(id: String, failed: VideoMode, modes: List<VideoMode>, attempts: ModeRecovery): Boolean {
        if (!foreground || surface?.isValid != true) return false
        val next = attempts.failed(failed)
        val candidate = next.candidates(modes).firstOrNull() ?: return false
        Log.i(TAG, "Recovering preview: $failed -> $candidate")
        beginConnection(id, candidate, next)
        return true
    }

    private val stats = object : Runnable {
        override fun run() {
            if (!foreground) return
            val phase = state.value.phase
            if (phase in setOf(CapturePhase.WaitingForFrames, CapturePhase.Streaming, CapturePhase.Stalled)) {
                val now = SystemClock.elapsedRealtime()
                val count = frames.count.get()
                val last = frames.lastAt.get()
                val active = state.value.activeMode
                val id = state.value.selectedDeviceId
                // Only startup failures trigger automatic retries. A later HDMI signal loss must
                // not repeatedly reconnect or overwrite a previously proven format.
                if (count == 0L && now - openedAt >= 8000 && surface?.isValid == true &&
                    active != null && id != null && phase != CapturePhase.Stalled) {
                    if (tryLowerMode(id, active, state.value.modes, recovery)) {
                        main.postDelayed(this, 1000)
                        return
                    }
                }
                val fps = if (previousSampleAt > 0) ((count - previousCount) * 1000 / (now - previousSampleAt).coerceAtLeast(1)).toInt() else 0
                val updatedPhase = when {
                    last > 0 && now - last < 3000 -> CapturePhase.Streaming
                    (count > 0 && now - last >= 3000) || now - openedAt >= 8000 -> CapturePhase.Stalled
                    else -> CapturePhase.WaitingForFrames
                }
                mutableState.value = state.value.copy(phase = updatedPhase, framesPerSecond = fps.coerceAtLeast(0),
                    message = when {
                        updatedPhase == CapturePhase.Stalled -> CaptureMessage.NoFrames
                        updatedPhase == CapturePhase.WaitingForFrames -> state.value.message
                        else -> null
                    })
                if (!modeSaved && frames.hasStableVideo(now) && active != null) {
                    rememberedDevice?.let { (vendor, product) -> modeStore.write(vendor, product, active) }
                    modeSaved = true
                }
                previousCount = count
                previousSampleAt = now
            }
            main.postDelayed(this, 1000)
        }
    }

    companion object {
        private const val TAG = "PocketMonitor"
        fun isVideoDevice(device: UsbDevice): Boolean = device.deviceClass == UsbConstants.USB_CLASS_VIDEO ||
            (0 until device.interfaceCount).any { device.getInterface(it).interfaceClass == UsbConstants.USB_CLASS_VIDEO }
    }
}

private fun Surface.setFrameRateCompat(fps: Int) {
    if (Build.VERSION.SDK_INT >= 30) runCatching { setFrameRate(fps.toFloat(), Surface.FRAME_RATE_COMPATIBILITY_DEFAULT) }
}
