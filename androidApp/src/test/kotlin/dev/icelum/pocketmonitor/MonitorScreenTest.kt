package dev.icelum.pocketmonitor

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w412dp-h892dp-420dpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MonitorScreenTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun idleScreenExplainsWiringAndOffersPermission() {
        var requested = false
        compose.setContent {
            MonitorScreen(CaptureState(), false, {}, { requested = true }, {}, {}, {}, {}, { Box(it) })
        }
        compose.onNodeWithText("等待画面接入").assertIsDisplayed()
        compose.onNodeWithTag("connect").performClick()
        assertTrue(requested)
        compose.onNodeWithTag("help").performClick()
        compose.onNodeWithText("把手机变成显示屏").assertIsDisplayed()
        compose.onNodeWithText("知道了").performClick()
        compose.waitForIdle()
        val output = File("build/reports/screenshots/phone-idle.png").apply { parentFile?.mkdirs() }
        compose.runOnIdle {
            val view = compose.activity.window.decorView
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
            output.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    @Test fun fullScreenCanBeExitedWithoutVideo() {
        compose.setContent {
            var fullscreen by remember { mutableStateOf(false) }
            MonitorScreen(CaptureState(), fullscreen, { fullscreen = it }, {}, {}, {}, {}, {}, { Box(it) })
        }
        compose.onNodeWithContentDescription("全屏").performClick()
        compose.onNodeWithTag("exit_fullscreen").assertIsDisplayed().performClick()
        compose.onNodeWithText("随身屏").assertIsDisplayed()
    }

    @Test fun settingsOnlyOffersAdvertisedModesAndReturnsSelection() {
        val first = VideoMode(1280, 720, 30, VideoEncoding.Mjpeg)
        val second = VideoMode(640, 480, 15, VideoEncoding.Yuy2)
        var chosen: VideoMode? = null
        compose.setContent {
            MonitorScreen(CaptureState(cameraPermission = true, modes = listOf(first, second), activeMode = first),
                false, {}, {}, {}, {}, { chosen = it }, {}, { Box(it) })
        }
        compose.onNodeWithContentDescription("视频设置").performClick()
        compose.onNodeWithText(second.label).performScrollTo().performClick()
        assertEquals(second, chosen)
        compose.onNodeWithText("1920 × 1080 · 60 fps · MJPEG").assertDoesNotExist()
    }
}
