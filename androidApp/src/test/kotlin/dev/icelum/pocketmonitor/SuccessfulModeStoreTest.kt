package dev.icelum.pocketmonitor

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SuccessfulModeStoreTest {
    @Test fun remembersAcrossStoreInstancesAndSeparatesCardModels() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val mode = VideoMode(1280, 720, 30, VideoEncoding.Mjpeg)
        SuccessfulModeStore(context).write(1, 2, mode)
        val reopened = SuccessfulModeStore(context)
        assertEquals(mode, reopened.read(1, 2))
        assertNull(reopened.read(1, 3))
        assertNull(reopened.read(2, 2))
    }

    @Test fun malformedPreferenceIsIgnored() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val preferences = context.getSharedPreferences("successful_video_modes", Context.MODE_PRIVATE)
        for (invalid in listOf("bad", "0:720:30:Mjpeg", "1280:720:30:Unknown", "x:720:30:Mjpeg")) {
            preferences.edit().putString("1:2", invalid).commit()
            assertNull(SuccessfulModeStore(context).read(1, 2))
        }
    }
}
