package dev.icelum.pocketmonitor

import android.Manifest
import android.content.Intent
import android.graphics.SurfaceTexture
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Surface
import android.view.TextureView
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle

class MainActivity : ComponentActivity() {
    private lateinit var capture: UvcCaptureController
    private var pendingDevice: String? = null
    private val permission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        capture.updatePermission()
        if (granted) connectGranted(pendingDevice) else capture.permissionDenied()
        pendingDevice = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        capture = UvcCaptureController(applicationContext)
        setContent {
            val state by capture.state.collectAsStateWithLifecycle()
            var fullscreen by rememberSaveable { mutableStateOf(false) }
            BackHandler(fullscreen) { fullscreen = false }
            LaunchedEffect(fullscreen) {
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    if (fullscreen) hide(WindowInsetsCompat.Type.systemBars()) else show(WindowInsetsCompat.Type.systemBars())
                }
            }
            LaunchedEffect(state.phase) {
                if (state.phase in setOf(CapturePhase.Connecting, CapturePhase.WaitingForFrames, CapturePhase.Streaming, CapturePhase.Stalled)) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
            MonitorScreen(state, fullscreen, { fullscreen = it }, ::requestConnection,
                capture::refreshDevices, capture::userDisconnect, capture::selectMode,
                onAppSettings = { startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))) },
                preview = { modifier -> CapturePreview(modifier, capture) })
        }
    }

    private fun requestConnection(id: String?) {
        capture.updatePermission()
        if (!capture.state.value.cameraPermission) {
            pendingDevice = id
            permission.launch(Manifest.permission.CAMERA)
        } else connectGranted(id)
    }

    private fun connectGranted(id: String?) {
        capture.refreshDevices()
        val device = id?.takeIf { selected -> capture.state.value.devices.any { it.id == selected } }
            ?: capture.state.value.devices.firstOrNull()?.id
        if (device != null) capture.connect(device)
    }

    override fun onStart() { super.onStart(); capture.start() }
    override fun onStop() { capture.stop(); super.onStop() }
    override fun onDestroy() { capture.destroy(); super.onDestroy() }
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); setIntent(intent); capture.refreshDevices() }
}

@Composable
private fun CapturePreview(modifier: Modifier, capture: UvcCaptureController) {
    AndroidView(modifier = modifier, factory = { context ->
        TextureView(context).apply {
            isOpaque = true
            surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                private var output: Surface? = null
                override fun onSurfaceTextureAvailable(texture: SurfaceTexture, width: Int, height: Int) {
                    output = Surface(texture).also(capture::attachSurface)
                }
                override fun onSurfaceTextureSizeChanged(texture: SurfaceTexture, width: Int, height: Int) = Unit
                override fun onSurfaceTextureUpdated(texture: SurfaceTexture) = Unit
                override fun onSurfaceTextureDestroyed(texture: SurfaceTexture): Boolean {
                    val old = output
                    output = null
                    if (old != null) {
                        capture.detachSurface(old) { texture.release() }
                        return false
                    }
                    return true
                }
            }
        }
    })
}
