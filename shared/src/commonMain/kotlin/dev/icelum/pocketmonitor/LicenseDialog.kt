package dev.icelum.pocketmonitor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.icelum.pocketmonitor.resources.*
import org.jetbrains.compose.resources.stringResource

data class LicenseDocument(val name: String, val text: String)

@Composable
fun LicenseDialog(documents: List<LicenseDocument>?, failed: Boolean, onDismiss: () -> Unit) {
    var expanded by remember { mutableStateOf<String?>(null) }
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.open_source_licenses)) },
        text = {
            when {
                failed -> Text(stringResource(Res.string.license_error))
                documents == null -> Text(stringResource(Res.string.license_loading))
                else -> LazyColumn(Modifier.fillMaxWidth().heightIn(max = 500.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(documents, key = { it.name }) { document ->
                        Column {
                            Text(document.name, Modifier.fillMaxWidth().clickable {
                                expanded = if (expanded == document.name) null else document.name
                            }.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.primary)
                            if (expanded == document.name) SelectionContainer {
                                Text(document.text, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.close)) } },
    )
}
