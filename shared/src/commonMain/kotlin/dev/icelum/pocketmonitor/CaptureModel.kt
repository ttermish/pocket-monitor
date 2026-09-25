package dev.icelum.pocketmonitor

import kotlin.math.abs

enum class CapturePhase { Idle, Permission, Connecting, WaitingForFrames, Streaming, Stalled, Error, Paused }
enum class VideoEncoding(val title: String, val uvcType: Int) { Mjpeg("MJPEG", 7), Yuy2("YUY2", 5) }

data class CaptureDevice(val id: String, val name: String, val vendorId: Int, val productId: Int)
data class VideoMode(val width: Int, val height: Int, val fps: Int, val encoding: VideoEncoding) {
    val label: String get() = "${width} × $height · $fps fps · ${encoding.title}"
}
data class CaptureState(
    val phase: CapturePhase = CapturePhase.Idle,
    val devices: List<CaptureDevice> = emptyList(),
    val selectedDeviceId: String? = null,
    val modes: List<VideoMode> = emptyList(),
    val activeMode: VideoMode? = null,
    val framesPerSecond: Int = 0,
    val cameraPermission: Boolean = false,
    val usbHostSupported: Boolean = true,
    val message: String? = null,
) {
    val selectedDevice: CaptureDevice? get() = devices.find { it.id == selectedDeviceId }
    val busy: Boolean get() = phase == CapturePhase.Permission || phase == CapturePhase.Connecting
}

/** Prefer 720p/30 MJPEG to keep USB 2.0 bandwidth and phone decoding practical. */
fun preferredModes(modes: List<VideoMode>): List<VideoMode> = modes
    .filter { it.width > 0 && it.height > 0 && it.fps > 0 }
    .distinct()
    .sortedWith(compareBy<VideoMode>(
        { if (it.encoding == VideoEncoding.Mjpeg) 0 else 1 },
        { abs(it.width.toLong() * it.height - 1280L * 720) },
        { abs(it.fps - 30) },
        { it.fps },
    ))

/** Bounded recovery; never increase pixel rate or switch MJPEG to uncompressed video. */
data class ModeRecovery(val failures: List<VideoMode> = emptyList()) {
    fun candidates(advertised: List<VideoMode>, preferred: VideoMode? = null): List<VideoMode> {
        if (failures.size >= 3) return emptyList()
        val modes = preferredModes(advertised)
        val failed = failures.lastOrNull() ?: return (listOfNotNull(preferred?.takeIf { it in modes }) + modes).distinct()
        return modes.filter { candidate ->
            candidate !in failures &&
                candidate.width.toLong() * candidate.height <= failed.width.toLong() * failed.height &&
                candidate.fps <= failed.fps &&
                (candidate.encoding == failed.encoding || candidate.encoding == VideoEncoding.Mjpeg) &&
                (candidate.width.toLong() * candidate.height < failed.width.toLong() * failed.height ||
                    candidate.fps < failed.fps || candidate.encoding != failed.encoding)
        }
    }

    fun failed(mode: VideoMode) = copy(failures = failures + mode)
}

/** A generation invalidates USB/worker callbacks after detach, stop or another connection. */
class CaptureSession {
    var generation: Long = 0
        private set
    var deviceId: String? = null
        private set
    fun begin(id: String): Long { generation++; deviceId = id; return generation }
    fun invalidate() { generation++; deviceId = null }
    fun accepts(token: Long, id: String): Boolean = generation == token && deviceId == id
}

data class VideoExtent(val width: Float, val height: Float)

/** Size the unrotated video so its rotated bounds fit inside the viewport. */
fun fittedVideoExtent(viewWidth: Float, viewHeight: Float, sourceWidth: Int, sourceHeight: Int, rotation: Int): VideoExtent {
    if (viewWidth <= 0 || viewHeight <= 0 || sourceWidth <= 0 || sourceHeight <= 0) return VideoExtent(0f, 0f)
    val sideways = ((rotation % 180) + 180) % 180 == 90
    val boundWidth = if (sideways) sourceHeight else sourceWidth
    val boundHeight = if (sideways) sourceWidth else sourceHeight
    val scale = minOf(viewWidth / boundWidth, viewHeight / boundHeight)
    return VideoExtent(sourceWidth * scale, sourceHeight * scale)
}
