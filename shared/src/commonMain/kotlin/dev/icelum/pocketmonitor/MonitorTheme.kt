package dev.icelum.pocketmonitor

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

fun ThemeMode.isDark(systemDark: Boolean): Boolean = when (this) {
    ThemeMode.System -> systemDark
    ThemeMode.Light -> false
    ThemeMode.Dark -> true
}

@Composable
fun MonitorTheme(preferences: AppPreferences, content: @Composable () -> Unit) {
    val dark = preferences.theme.isDark(isSystemInDarkTheme())
    val primary = when (preferences.accent) {
        AccentColor.Mint -> if (dark) Color(0xFFB8ED82) else Color(0xFF386A17)
        AccentColor.Ocean -> if (dark) Color(0xFF9ACBFF) else Color(0xFF145DA0)
        AccentColor.Amber -> if (dark) Color(0xFFFFD18A) else Color(0xFF805300)
    }
    val container = when (preferences.accent) {
        AccentColor.Mint -> if (dark) Color(0xFF26372E) else Color(0xFFE0ECD8)
        AccentColor.Ocean -> if (dark) Color(0xFF18394F) else Color(0xFFD9EAFE)
        AccentColor.Amber -> if (dark) Color(0xFF493719) else Color(0xFFFFE5B9)
    }
    val onContainer = if (dark) Color(0xFFF0F5F6) else Color(0xFF172126)
    val colors = if (dark) darkColorScheme(
        primary = primary, onPrimary = Color(0xFF101619),
        primaryContainer = container, onPrimaryContainer = onContainer,
        secondaryContainer = container, onSecondaryContainer = onContainer,
        background = Color(0xFF101619), onBackground = Color(0xFFF0F5F6),
        surface = Color(0xFF1B2328), onSurface = Color(0xFFF0F5F6),
        surfaceVariant = Color(0xFF263139), onSurfaceVariant = Color(0xFFB2C0C7),
        outline = Color(0xFF64777F), outlineVariant = Color(0xFF334047),
    ) else lightColorScheme(
        primary = primary, onPrimary = Color.White,
        primaryContainer = container, onPrimaryContainer = onContainer,
        secondaryContainer = container, onSecondaryContainer = onContainer,
        background = Color(0xFFF5F8FA), onBackground = Color(0xFF172126),
        surface = Color.White, onSurface = Color(0xFF172126),
        surfaceVariant = Color(0xFFE8EFF2), onSurfaceVariant = Color(0xFF4E6069),
        outline = Color(0xFF6C7B83), outlineVariant = Color(0xFFCFD9DF),
    )
    MaterialTheme(colorScheme = colors, content = content)
}
