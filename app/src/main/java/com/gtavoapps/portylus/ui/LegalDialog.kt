package com.gtavoapps.portylus.ui

import android.content.Intent
import androidx.core.net.toUri
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.gtavoapps.portylus.BuildConfig
import com.gtavoapps.portylus.R

@Composable fun LegalDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val strings = context
    val language = androidx.compose.ui.platform.LocalConfiguration.current.locales[0].language
    var selected by rememberSaveable { mutableStateOf("about.txt") }
    val text = remember(selected) {
        context.assets.open("legal/$selected").bufferedReader().use { it.readText() }
    }
    var linkError by remember { mutableStateOf(false) }
    fun open(url: String) {
        linkError = false
        try { context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri())) }
        catch (_: android.content.ActivityNotFoundException) { linkError = true }
    }
    val sections = listOf("about.txt" to strings.getString(R.string.about), "privacy.txt" to strings.getString(R.string.privacy),
        "notices.txt" to strings.getString(R.string.licenses), "support.txt" to strings.getString(R.string.support))
    AlertDialog(onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Image(painterResource(R.drawable.portylus_symbol), contentDescription = strings.getString(R.string.app_logo),
                    modifier = Modifier.size(32.dp))
                Column {
                    Text("Portylus ${BuildConfig.VERSION_NAME}")
                    Text(BuildConfig.DEVELOPER_NAME, style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary)
                }
            }
        },
        text = {
            Column {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                    sections.forEach { (file, label) ->
                        val active = selected == file || (file == "notices.txt" && selected == "AGPL-3.0.txt")
                        TextButton(onClick = { selected = file; linkError = false },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                            colors = ButtonDefaults.textButtonColors(
                            contentColor = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)) {
                            Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1)
                        }
                    }
                }
                HorizontalDivider()
                key(selected) {
                    Column(Modifier.weight(1f, fill = false).heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState()).padding(vertical = 12.dp)) {
                        if (selected == "about.txt" || selected == "support.txt") {
                            Image(painterResource(R.drawable.gtavo_apps), contentDescription = "gtavo.apps",
                                modifier = Modifier.size(40.dp).align(Alignment.CenterHorizontally))
                            Spacer(Modifier.height(12.dp))
                        }
                        if (language != "es") {
                            Text(strings.getString(R.string.legal_original), style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary)
                            Spacer(Modifier.height(8.dp))
                        }
                        SelectionContainer { Text(text) }
                    }
                }
                HorizontalDivider()
                FlowRow(Modifier.fillMaxWidth()) {
                    when (selected) {
                        "about.txt" -> {
                            TextButton(onClick = { open(BuildConfig.PROJECT_URL) }) { Text(strings.getString(R.string.project_web)) }
                        }
                        "privacy.txt" -> TextButton(onClick = { open(BuildConfig.PRIVACY_URL) }) { Text(strings.getString(R.string.web_policy)) }
                        "support.txt" -> {
                            TextButton(onClick = { selected = "privacy.txt" }) { Text(strings.getString(R.string.privacy)) }
                        }
                        else -> {
                            TextButton(onClick = { selected = if (selected == "AGPL-3.0.txt") "notices.txt" else "AGPL-3.0.txt" }) {
                                Text(if (selected == "AGPL-3.0.txt") strings.getString(R.string.third_party_notices) else strings.getString(R.string.read_agpl))
                            }
                            if (BuildConfig.SOURCE_URL.isNotBlank()) TextButton(onClick = { open(BuildConfig.SOURCE_URL) }) { Text(strings.getString(R.string.version_source)) }
                            else TextButton(onClick = { open(BuildConfig.PROJECT_URL.trimEnd('/') + "/codigo/") }) { Text(strings.getString(R.string.source_publication)) }
                        }
                    }
                }
                if (linkError) Text(strings.getString(R.string.no_link_handler))
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(strings.getString(R.string.close)) } })
}
