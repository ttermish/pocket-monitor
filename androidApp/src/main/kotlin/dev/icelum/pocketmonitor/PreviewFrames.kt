package dev.icelum.pocketmonitor

import java.util.concurrent.atomic.AtomicLong

/** A fresh instance per connection prevents a late native callback changing the next session. */
internal class PreviewFrames {
    val count = AtomicLong()
    val lastAt = AtomicLong()
    private val continuousSince = AtomicLong(-1)

    /** True exactly once, allowing immediate first-frame publication without posting every frame. */
    fun record(now: Long): Boolean {
        val previous = lastAt.getAndSet(now)
        if (continuousSince.get() < 0 || now - previous >= 3000) continuousSince.set(now)
        return count.incrementAndGet() == 1L
    }

    fun hasStableVideo(now: Long): Boolean = count.get() >= 3 &&
        continuousSince.get() >= 0 && now - continuousSince.get() >= 3000 && now - lastAt.get() < 1000
}
