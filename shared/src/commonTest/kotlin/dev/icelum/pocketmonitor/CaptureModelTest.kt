package dev.icelum.pocketmonitor

import kotlin.test.*

class CaptureModelTest {
    @Test fun savedModeWinsOnlyWhenStillAdvertised() {
        val saved = VideoMode(1920, 1080, 30, VideoEncoding.Mjpeg)
        val standard = VideoMode(1280, 720, 30, VideoEncoding.Mjpeg)
        assertEquals(saved, ModeRecovery().candidates(listOf(standard, saved), saved).first())
        assertEquals(listOf(standard), ModeRecovery().candidates(listOf(standard), saved))
    }

    @Test fun recoveryNeverIncreasesResolutionFpsOrUncompressedBandwidth() {
        val failed = VideoMode(1280, 720, 30, VideoEncoding.Mjpeg)
        val lower = failed.copy(fps = 15)
        val small = failed.copy(width = 640, height = 480)
        val candidates = listOf(failed, lower, small, failed.copy(fps = 60),
            failed.copy(width = 1920, height = 1080), small.copy(encoding = VideoEncoding.Yuy2))
        assertEquals(listOf(lower, small), ModeRecovery().failed(failed).candidates(candidates))
    }

    @Test fun recoveryStopsAfterTwoRestartsAndDoesNotRevisitFailures() {
        val high = VideoMode(1280, 720, 30, VideoEncoding.Mjpeg)
        val middle = high.copy(fps = 15)
        val low = middle.copy(width = 640, height = 480)
        val modes = listOf(high, middle, low, low.copy(fps = 5))
        val recovery = ModeRecovery().failed(high).failed(middle)
        assertEquals(listOf(low, low.copy(fps = 5)), recovery.candidates(modes, high))
        assertTrue(recovery.failed(low).candidates(modes).isEmpty())
    }

    @Test fun uncompressedVideoMayFallBackToAdvertisedMjpeg() {
        val raw = VideoMode(640, 480, 30, VideoEncoding.Yuy2)
        val compressed = raw.copy(encoding = VideoEncoding.Mjpeg)
        assertEquals(listOf(compressed), ModeRecovery().failed(raw).candidates(listOf(raw, compressed)))
        assertTrue(ModeRecovery().failed(raw).candidates(listOf(raw)).isEmpty())
    }
    @Test fun usb2DefaultPrefers720pMjpegAt30() {
        val desired = VideoMode(1280, 720, 30, VideoEncoding.Mjpeg)
        val modes = listOf(VideoMode(1920, 1080, 60, VideoEncoding.Mjpeg),
            VideoMode(1280, 720, 30, VideoEncoding.Yuy2),
            VideoMode(1280, 720, 60, VideoEncoding.Mjpeg), desired)
        assertEquals(desired, preferredModes(modes).first())
    }
    @Test fun unsupportedModesAreNotInvented() {
        val only = VideoMode(640, 480, 15, VideoEncoding.Yuy2)
        assertEquals(listOf(only), preferredModes(listOf(only, only, only.copy(width = 0))))
        assertTrue(preferredModes(emptyList()).isEmpty())
    }
    @Test fun unpluggedOrReplacedSessionCannotPublishFrames() {
        val session = CaptureSession()
        val first = session.begin("usb-a")
        assertTrue(session.accepts(first, "usb-a"))
        session.invalidate()
        assertFalse(session.accepts(first, "usb-a"))
        val second = session.begin("usb-a")
        assertFalse(session.accepts(first, "usb-a"))
        assertFalse(session.accepts(second, "usb-b"))
        assertTrue(session.accepts(second, "usb-a"))
    }
    @Test fun widescreenIsLetterboxedOnPortraitPhone() {
        assertEquals(VideoExtent(400f, 225f), fittedVideoExtent(400f, 300f, 1920, 1080, 0))
    }
    @Test fun rotationFitsVideoWithoutStretching() {
        val size = fittedVideoExtent(300f, 400f, 1920, 1080, 90)
        assertEquals(400f, size.width)
        assertEquals(225f, size.height)
    }
}
