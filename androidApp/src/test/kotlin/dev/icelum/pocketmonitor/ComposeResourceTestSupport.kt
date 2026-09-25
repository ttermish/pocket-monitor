package dev.icelum.pocketmonitor

import android.content.ContentProvider
import android.content.Context
import android.content.pm.ProviderInfo
import androidx.test.core.app.ApplicationProvider

/** Robolectric does not automatically start library providers from the merged manifest. */
internal fun initializeComposeResources() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val provider = Class.forName("org.jetbrains.compose.resources.AndroidContextProvider")
        .getDeclaredConstructor().newInstance() as ContentProvider
    provider.attachInfo(context, ProviderInfo().apply { authority = "${context.packageName}.test.resources" })
}
