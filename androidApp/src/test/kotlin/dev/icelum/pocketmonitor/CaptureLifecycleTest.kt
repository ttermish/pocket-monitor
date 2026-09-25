package dev.icelum.pocketmonitor

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CaptureLifecycleTest {
    @Before fun initializeResources() = initializeComposeResources()
    @Test fun foregroundBackgroundCycleWithoutUsbDoesNotOpenCamera() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val capture = UvcCaptureController(context)
        capture.start()
        assertTrue(capture.state.value.devices.isEmpty())
        assertEquals(CapturePhase.Idle, capture.state.value.phase)
        capture.stop()
        assertEquals(CapturePhase.Paused, capture.state.value.phase)
        capture.start()
        assertEquals(CapturePhase.Idle, capture.state.value.phase)
        capture.destroy()
        capture.destroy()
    }

    @Test fun permissionDenialIsRecoverableAndDoesNotStartUsb() {
        val capture = UvcCaptureController(ApplicationProvider.getApplicationContext())
        capture.start()
        capture.permissionDenied()
        assertFalse(capture.state.value.cameraPermission)
        assertEquals(CapturePhase.Error, capture.state.value.phase)
        capture.retry()
        assertEquals(CapturePhase.Error, capture.state.value.phase)
        capture.destroy()
    }

    @Test fun actualActivityCanCreateStopAndDestroy() {
        val activity = Robolectric.buildActivity(MainActivity::class.java).setup()
        shadowOf(Looper.getMainLooper()).idle()
        activity.pause().stop().destroy()
    }

    @Test fun packagedAppDoesNotRequestAudioStorageOrNetwork() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val permissions = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
            .requestedPermissions.orEmpty().toSet()
        assertTrue(Manifest.permission.CAMERA in permissions)
        assertFalse(Manifest.permission.RECORD_AUDIO in permissions)
        assertFalse(Manifest.permission.INTERNET in permissions)
        assertFalse(Manifest.permission.MANAGE_EXTERNAL_STORAGE in permissions)
    }
}
