package dev.icelum.pocketmonitor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.icelum.pocketmonitor.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ConnectionGuide(onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.guide_title)) },
        text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(stringResource(Res.string.guide_source))
            Text(stringResource(Res.string.guide_phone))
            Text(stringResource(Res.string.guide_permissions))
            Text(stringResource(Res.string.guide_signal))
            Text(stringResource(Res.string.guide_gestures))
            Text(stringResource(Res.string.guide_platforms), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(stringResource(Res.string.guide_licenses), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
        } }, confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.got_it)) } })
}
