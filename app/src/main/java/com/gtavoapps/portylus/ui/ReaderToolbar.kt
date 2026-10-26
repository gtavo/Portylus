package com.gtavoapps.portylus.ui

import com.gtavoapps.portylus.R
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gtavoapps.portylus.annotations.InkStyle

/** Two compact rows keep every primary tool visible on phones with 48 dp touch targets. */
@Composable fun ReaderToolbar(name: String, saving: Boolean, onClose: () -> Unit,
    tools: @Composable (compact: Boolean) -> Unit, actions: @Composable (compact: Boolean) -> Unit) {
    val strings = androidx.compose.ui.platform.LocalContext.current
    Surface(color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))) {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val availableWidth = maxWidth
                val fontScale = androidx.compose.ui.platform.LocalDensity.current.fontScale
                val compact = maxWidth < 600.dp || (fontScale > 1.3f && maxWidth < 1000.dp)
                Column {
                Row(Modifier.fillMaxWidth().height(if (compact) 48.dp else 56.dp).padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    ToolbarAction(strings.getString(R.string.back), enabled = !saving, onClick = onClose) { ActionIcon("back", !saving) }
                    if (compact) Box(Modifier.weight(1f).padding(end = 4.dp)) {
                        Filename(name, Modifier.fillMaxWidth())
                    }
                    else {
                    Filename(name, Modifier.width((availableWidth * 0.18f).coerceIn(100.dp, 240.dp)).padding(end = 8.dp))
                    Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                        Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.CenterVertically) { tools(false) }
                    }
                    VerticalDivider(Modifier.height(24.dp).padding(horizontal = 4.dp),
                        color = MaterialTheme.colorScheme.outlineVariant)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) { actions(compact) }
                }
                if (compact) Row(Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically) { tools(true) }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun Filename(name: String, modifier: Modifier) {
    TooltipBox(positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text(name) } }, state = rememberTooltipState(), modifier = modifier) {
        Text(name, maxLines = 1, overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
    }
}

/** Labels remain available to accessibility, tests and a long-press tooltip. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun ToolbarAction(label: String, selected: Boolean? = null, enabled: Boolean = true,
    onClick: () -> Unit, content: @Composable () -> Unit) {
    TooltipBox(positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text(label) } }, state = rememberTooltipState()) {
        val modifier = Modifier.size(48.dp).semantics { contentDescription = label }
        if (selected == null) IconButton(onClick = onClick, enabled = enabled, modifier = modifier) { content() }
        else IconToggleButton(checked = selected, onCheckedChange = { onClick() }, enabled = enabled,
            modifier = modifier.background(
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else Color.Transparent,
                RoundedCornerShape(12.dp))) {
            val tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            androidx.compose.runtime.CompositionLocalProvider(LocalContentColor provides
                tint.copy(alpha = if (enabled) 1f else 0.38f)) {
                content()
            }
        }
    }
}

@Composable fun StyleBadge(style: InkStyle, slot: Int) {
    val color = Color(style.color)
    Box(Modifier.size(22.dp).background(color, CircleShape), contentAlignment = Alignment.Center) {
        Text((slot + 1).toString(), style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold, color = if (color.luminance() > 0.45f) Color(0xff071a33) else Color.White)
    }
}
