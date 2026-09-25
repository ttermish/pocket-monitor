package dev.icelum.pocketmonitor

import android.content.res.Configuration
import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import java.util.Locale

/** Use the device locale for System, even after an in-app language override. */
@Composable
internal fun AppLocale(language: AppLanguage, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val locale = language.languageTag?.let(Locale::forLanguageTag)
        ?: Resources.getSystem().configuration.locales[0]
    val localized = remember(configuration, locale) {
        Configuration(configuration).apply { setLocale(locale) }
    }
    val localizedContext = remember(context, localized) { context.createConfigurationContext(localized) }
    // Compose resources also uses the Java locale for non-composable resource access.
    SideEffect { Locale.setDefault(locale) }
    val direction = if (localized.layoutDirection == android.view.View.LAYOUT_DIRECTION_RTL)
        LayoutDirection.Rtl else LayoutDirection.Ltr
    CompositionLocalProvider(LocalContext provides localizedContext, LocalConfiguration provides localized,
        LocalLayoutDirection provides direction) {
        content()
    }
}
