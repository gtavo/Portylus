package com.gtavoapps.portylus.ui

import android.app.LocaleManager
import android.os.LocaleList
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.gtavoapps.portylus.R
import com.gtavoapps.portylus.BuildConfig

@Composable fun SettingsDialog(model: AppViewModel, onInformation: () -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val manager = context.getSystemService(LocaleManager::class.java)
    val language = manager.applicationLocales.toLanguageTags().substringBefore('-').substringBefore(',')
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text(context.getString(R.string.settings)) },
        text = {
            Column(Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState())) {
                Text(context.getString(R.string.language), style = MaterialTheme.typography.titleSmall)
                listOf("" to context.getString(R.string.system_language), "es" to "Español",
                    "en" to "English", "fr" to "Français").forEach { (tag, label) ->
                    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        .clickable(role = Role.RadioButton) {
                            manager.applicationLocales = LocaleList.forLanguageTags(tag)
                        }, verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = language == tag, onClick = null)
                        Text(label, Modifier.padding(start = 12.dp))
                    }
                }
                HorizontalDivider(Modifier.padding(vertical = 12.dp))
                SettingSwitch(context.getString(R.string.finger_writing),
                    context.getString(if (model.fingerWriting) R.string.finger_hint else R.string.stylus_hint),
                    model.fingerWriting, model::updateFingerWriting)
                SettingSwitch(context.getString(R.string.keep_awake), context.getString(R.string.keep_awake_hint),
                    model.keepAwake, model::updateKeepAwake)
                HorizontalDivider(Modifier.padding(vertical = 12.dp))
                if (BuildConfig.SUPPORT_URL.isNotBlank()) {
                    Text(context.getString(R.string.support_message), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    KofiButton()
                }
                TextButton(onClick = onInformation) { Text(context.getString(R.string.privacy_licenses)) }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(context.getString(R.string.close)) } })
}

@Composable private fun SettingSwitch(title: String, hint: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
        .semantics { contentDescription = title }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}
