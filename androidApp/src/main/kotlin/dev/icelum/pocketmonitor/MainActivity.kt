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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
        val preferenceStore = AppPreferencesStore(applicationContext)
        val appVersion = packageManager.getPackageInfo(packageName, 0).versionName.orEmpty()
        setContent {
            val state by capture.state.collectAsStateWithLifecycle()
            var preferences by remember { mutableStateOf(preferenceStore.read()) }
            var tab by rememberSaveable { mutableStateOf(AppTab.Preview) }
            var fullscreen by rememberSaveable { mutableStateOf(false) }
            var showLicenses by rememberSaveable { mutableStateOf(false) }
            var licenseDocuments by remember { mutableStateOf<List<LicenseDocument>?>(null) }
            var licenseFailed by remember { mutableStateOf(false) }
            val dark = preferences.theme.isDark(isSystemInDarkTheme())
            SideEffect {
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !dark
                    isAppearanceLightNavigationBars = !dark
                }
            }
            BackHandler(!fullscreen && tab != AppTab.Preview) { tab = AppTab.Preview }
            BackHandler(fullscreen) { fullscreen = false }
            LaunchedEffect(fullscreen) {
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    if (fullscreen) hide(WindowInsetsCompat.Type.systemBars()) else show(WindowInsetsCompat.Type.systemBars())
                }
            }
            LaunchedEffect(state.phase, preferences.keepScreenOn) {
                if (preferences.keepScreenOn && state.phase in setOf(CapturePhase.Connecting, CapturePhase.WaitingForFrames, CapturePhase.Streaming, CapturePhase.Stalled)) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
            LaunchedEffect(showLicenses) {
                if (showLicenses && licenseDocuments == null) {
                    val result = withContext(Dispatchers.IO) {
                        runCatching {
                            assets.list("licenses").orEmpty().sorted().map { name ->
                                LicenseDocument(name, assets.open("licenses/$name").bufferedReader().use { it.readText() })
                            }.also { check(it.isNotEmpty()) }
                        }
                    }
                    licenseDocuments = result.getOrNull()
                    licenseFailed = result.isFailure
                }
            }
            AppLocale(preferences.language) {
                MonitorScreen(state, fullscreen, { fullscreen = it }, ::requestConnection,
                    capture::refreshDevices, capture::userDisconnect, capture::selectMode,
                    onAppSettings = { startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))) },
                    preview = { modifier -> CapturePreview(modifier, capture) },
                    preferences = preferences,
                    onPreferences = { preferenceStore.write(it); preferences = it },
                    selectedTab = tab, onTab = { tab = it }, appVersion = appVersion,
                    onOpenLicenses = { showLicenses = true })
                if (showLicenses) MonitorTheme(preferences) {
                    LicenseDialog(licenseDocuments, licenseFailed) { showLicenses = false }
                }
            }
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
        val device = connectionDevice(capture.state.value.devices, id)
        if (device != null) capture.connect(device.id)
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
