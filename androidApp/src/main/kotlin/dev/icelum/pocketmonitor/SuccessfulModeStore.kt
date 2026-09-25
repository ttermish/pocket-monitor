package dev.icelum.pocketmonitor

import android.content.Context

/** VID/PID identifies a card model, not a physical unit; no USB serial permission needed. */
internal class SuccessfulModeStore(context: Context) {
    private val preferences = context.getSharedPreferences("successful_video_modes", Context.MODE_PRIVATE)

    fun read(vendorId: Int, productId: Int): VideoMode? = runCatching {
        val parts = preferences.getString("$vendorId:$productId", null)?.split(":") ?: return null
        if (parts.size != 4) return null
        VideoMode(parts[0].toInt(), parts[1].toInt(), parts[2].toInt(), VideoEncoding.valueOf(parts[3]))
            .takeIf { it.width > 0 && it.height > 0 && it.fps > 0 }
    }.getOrNull()

    fun write(vendorId: Int, productId: Int, mode: VideoMode) {
        preferences.edit().putString("$vendorId:$productId",
            "${mode.width}:${mode.height}:${mode.fps}:${mode.encoding.name}").apply()
    }
}
