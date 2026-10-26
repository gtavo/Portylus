package com.gtavoapps.portylus.ui

import android.content.Intent
import androidx.core.net.toUri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.gtavoapps.portylus.BuildConfig
import com.gtavoapps.portylus.R

/** Official Ko-fi cup; ordinary external link, no payment SDK or network permission. */
@Composable fun KofiButton() {
    if (BuildConfig.SUPPORT_URL.isBlank()) return
    val context = LocalContext.current
    var failed by remember { mutableStateOf(false) }
    TextButton(onClick = {
        try { context.startActivity(Intent(Intent.ACTION_VIEW, BuildConfig.SUPPORT_URL.toUri())) }
        catch (_: android.content.ActivityNotFoundException) { failed = true }
    }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
        modifier = Modifier.heightIn(min = 48.dp).semantics { contentDescription = context.getString(R.string.support_kofi) }) {
        Image(painterResource(R.drawable.kofi_symbol), contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(6.dp))
        Text(context.getString(R.string.support_kofi), style = MaterialTheme.typography.labelLarge,
            maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
    }
    if (failed) AlertDialog(onDismissRequest = { failed = false },
        text = { Text(context.getString(R.string.no_link_handler)) },
        confirmButton = { TextButton(onClick = { failed = false }) { Text(context.getString(R.string.close)) } })
}
