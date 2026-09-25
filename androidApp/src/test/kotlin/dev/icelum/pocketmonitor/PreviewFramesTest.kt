package dev.icelum.pocketmonitor

import org.junit.Assert.*
import org.junit.Test

class PreviewFramesTest {
    @Test fun firstFrameSignalsImmediatelyAndOnlyOnce() {
        val frames = PreviewFrames()
        assertTrue(frames.record(0))
        assertFalse(frames.record(33))
        assertFalse(frames.record(66))
        assertEquals(3L, frames.count.get())
        assertEquals(66L, frames.lastAt.get())
    }

    @Test fun stableModeRequiresSustainedRecentFrames() {
        val frames = PreviewFrames()
        frames.record(0)
        assertFalse(frames.hasStableVideo(3000))
        frames.record(1000)
        frames.record(2000)
        assertFalse(frames.hasStableVideo(2000))
        frames.record(3000)
        assertTrue(frames.hasStableVideo(3000))
        assertFalse(frames.hasStableVideo(6000))
        frames.record(6000)
        assertFalse(frames.hasStableVideo(6000))
    }

    @Test fun oldFramesCannotAffectNewConnection() {
        val old = PreviewFrames()
        val current = PreviewFrames()
        old.record(1234)
        assertEquals(0L, current.count.get())
        assertFalse(current.hasStableVideo(5000))
        assertTrue(current.record(5000))
    }
}
