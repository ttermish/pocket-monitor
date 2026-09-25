package dev.icelum.pocketmonitor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.icelum.pocketmonitor.resources.*
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SettingsPage(preferences: AppPreferences, onPreferences: (AppPreferences) -> Unit,
    appVersion: String, onOpenLicenses: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)
        .padding(bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(stringResource(Res.string.settings_hint), color = MaterialTheme.colorScheme.onSurfaceVariant)
        SettingsSection(stringResource(Res.string.appearance)) {
            Text(stringResource(Res.string.theme_mode), style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeMode.entries.forEach { mode ->
                    FilterChip(selected = preferences.theme == mode,
                        onClick = { onPreferences(preferences.copy(theme = mode)) },
                        modifier = Modifier.testTag("theme_${mode.name.lowercase()}"),
                        label = { Text(when (mode) {
                            ThemeMode.System -> stringResource(Res.string.theme_system)
                            ThemeMode.Light -> stringResource(Res.string.theme_light)
                            ThemeMode.Dark -> stringResource(Res.string.theme_dark)
                        }) })
                }
            }
            Text(stringResource(Res.string.accent_color), style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AccentColor.entries.forEach { accent ->
                    FilterChip(selected = preferences.accent == accent,
                        onClick = { onPreferences(preferences.copy(accent = accent)) },
                        modifier = Modifier.testTag("accent_${accent.name.lowercase()}"),
                        label = { Text(when (accent) {
                            AccentColor.Mint -> stringResource(Res.string.accent_mint)
                            AccentColor.Ocean -> stringResource(Res.string.accent_ocean)
                            AccentColor.Amber -> stringResource(Res.string.accent_amber)
                        }) })
                }
            }
        }
        SettingsSection(stringResource(Res.string.language)) {
            Column(Modifier.selectableGroup()) {
                AppLanguage.entries.forEach { language ->
                    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        .testTag("language_${language.name.lowercase()}")
                        .selectable(preferences.language == language, role = Role.RadioButton,
                            onClick = { onPreferences(preferences.copy(language = language)) }),
                        verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = preferences.language == language, onClick = null)
                        Spacer(Modifier.width(12.dp))
                        Text(when (language) {
                            AppLanguage.System -> stringResource(Res.string.language_system)
                            AppLanguage.English -> "English"
                            AppLanguage.Chinese -> "简体中文"
                        })
                    }
                }
            }
        }
        SettingsSection(stringResource(Res.string.preview_preferences)) {
            PreferenceSwitch(stringResource(Res.string.keep_screen_on), stringResource(Res.string.keep_screen_on_hint),
                preferences.keepScreenOn, "keep_screen_on") { onPreferences(preferences.copy(keepScreenOn = it)) }
            PreferenceSwitch(stringResource(Res.string.show_stats), stringResource(Res.string.show_stats_hint),
                preferences.showFrameStats, "show_frame_stats") { onPreferences(preferences.copy(showFrameStats = it)) }
        }
        SettingsSection(stringResource(Res.string.about)) {
            Text(stringResource(Res.string.app_name), fontWeight = FontWeight.SemiBold)
            if (appVersion.isNotBlank()) Text(stringResource(Res.string.app_version, appVersion),
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(stringResource(Res.string.local_preview), fontSize = 12.sp)
            Text(stringResource(Res.string.no_hardware_claim), fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick = onOpenLicenses, modifier = Modifier.testTag("open_licenses")) {
                Text(stringResource(Res.string.open_source_licenses))
            }
        }
        Text(stringResource(Res.string.saved_automatically), fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}

@Composable
private fun PreferenceSwitch(title: String, hint: String, checked: Boolean, tag: String, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp)
            Text(hint, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onChange, modifier = Modifier.testTag(tag))
    }
}
