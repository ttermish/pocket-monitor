package dev.icelum.pocketmonitor

import android.app.Application
import com.serenegiant.utils.UVCUtils

class MonitorApplication : Application() {
    override fun onCreate() { super.onCreate(); UVCUtils.init(this) }
}
