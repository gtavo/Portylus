package com.gtavoapps.portylus.ui

import android.app.LocaleManager
import android.os.LocaleList
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.unit.dp
import com.gtavoapps.portylus.R
import com.gtavoapps.portylus.BuildConfig
import com.gtavoapps.portylus.preferences.AppAppearance

@Composable fun SettingsDialog(model: AppViewModel, onInformation: () -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val manager = context.getSystemService(LocaleManager::class.java)
    val language = manager.applicationLocales.toLanguageTags().substringBefore('-').substringBefore(',')
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text(context.getString(R.string.settings)) },
        text = {
            Column(Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState())) {
                AppearancePicker(model.appearance, model::updateAppearance)
                Spacer(Modifier.height(12.dp))
                SettingsChoice(context.getString(R.string.language), context.getString(R.string.language_choose),
                    language, listOf("" to context.getString(R.string.system_language), "es" to "Español",
                        "en" to "English", "fr" to "Français")) { tag ->
                    manager.applicationLocales = LocaleList.forLanguageTags(tag)
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

@Composable private fun AppearancePicker(appearance: AppAppearance, onChange: (AppAppearance) -> Unit) {
    val strings = LocalContext.current
    fun label(value: AppAppearance) = strings.getString(when (value) {
        AppAppearance.SYSTEM -> R.string.appearance_system
        AppAppearance.LIGHT -> R.string.appearance_light
        AppAppearance.DARK -> R.string.appearance_dark
    })
    SettingsChoice(strings.getString(R.string.appearance), strings.getString(R.string.appearance_choose),
        appearance, AppAppearance.entries.map { it to label(it) }, onChange)
}

@Composable private fun <T> SettingsChoice(title: String, description: String, value: T,
    options: List<Pair<T, String>>, onChange: (T) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column {
    Text(title, style = MaterialTheme.typography.titleSmall)
    Box(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        OutlinedButton(onClick = { expanded = true }, shape = MaterialTheme.shapes.small,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                .semantics { contentDescription = description }) {
            Text(options.firstOrNull { it.first == value }?.second ?: options.first().second, Modifier.weight(1f))
            ActionIcon("down", tint = LocalContentColor.current)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (option, label) ->
                DropdownMenuItem(text = { Text(label) },
                    leadingIcon = { RadioButton(selected = value == option, onClick = null) },
                    onClick = { expanded = false; onChange(option) },
                    modifier = Modifier.heightIn(min = 48.dp).semantics { selected = value == option })
            }
        }
    }
    }
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
