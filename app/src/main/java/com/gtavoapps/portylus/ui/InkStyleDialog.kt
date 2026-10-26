package com.gtavoapps.portylus.ui

import com.gtavoapps.portylus.R
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.unit.dp
import com.gtavoapps.portylus.annotations.AnnotationKind
import com.gtavoapps.portylus.annotations.InkStyle
import com.gtavoapps.portylus.annotations.ToolPresets
import kotlin.math.round
import java.util.Locale

private val palette = listOf(R.string.blue to 0xff2255cc, R.string.black to 0xff222222, R.string.red to 0xffd32f2f,
    R.string.green to 0xff43a047, R.string.purple to 0xff8e44ad, R.string.yellow to 0xffffdf00,
    R.string.orange to 0xfff57c00, R.string.pink to 0xffe754a4, R.string.cyan to 0xff00acc1, R.string.white to 0xffffffff)

@Composable
fun InkStyleDialog(kind: AnnotationKind, presets: ToolPresets, saving: Boolean,
    onDismiss: () -> Unit, onSave: (Int, InkStyle) -> Unit) {
    val strings = androidx.compose.ui.platform.LocalContext.current
    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
    val pen = kind == AnnotationKind.PEN
    var slot by remember(kind) { mutableIntStateOf(presets.selected) }
    val original = presets.slots[slot]
    var width by remember(kind, slot, original) { mutableFloatStateOf(original.width) }
    var hex by remember(kind, slot, original) {
        mutableStateOf("#%06X".format(Locale.ROOT, original.color and 0xffffff))
    }
    val digits = hex.removePrefix("#")
    val color = if (digits.matches(Regex("[0-9A-Fa-f]{6}")))
        digits.toInt(16) or 0xff000000.toInt() else null
    val range = if (pen) 0.5f..8f else 4f..32f
    AlertDialog(onDismissRequest = { if (!saving) onDismiss() },
        title = { Text(if (pen) strings.getString(R.string.pen_styles) else strings.getString(R.string.highlight_styles)) },
        text = {
            Column(Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(3) { index ->
                        FilterChip(selected = slot == index, enabled = !saving,
                            onClick = { slot = index }, label = { Text(strings.getString(R.string.style_number, index + 1)) })
                    }
                }
                Canvas(Modifier.fillMaxWidth().height(56.dp).background(Color.White, RoundedCornerShape(8.dp))
                    .semantics { contentDescription = strings.getString(R.string.style_preview) }) {
                    drawLine(Color(color ?: original.color), Offset(24.dp.toPx(), size.height / 2),
                        Offset(size.width - 24.dp.toPx(), size.height / 2), width.dp.toPx(),
                        cap = StrokeCap.Round, alpha = if (pen) 1f else 72f / 255f)
                }
                Text(strings.getString(R.string.stroke_width, java.text.NumberFormat.getNumberInstance(locale).format(width)))
                Slider(value = width, onValueChange = {
                    width = if (pen) round(it * 2) / 2 else round(it)
                }, valueRange = range, steps = if (pen) 14 else 27, enabled = !saving,
                    modifier = Modifier.semantics { contentDescription = strings.getString(R.string.width_label) })
                palette.chunked(5).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        row.forEach { (name, value) ->
                            val isSelected = color == value.toInt()
                            Box(Modifier.size(44.dp)
                                .background(Color(value), CircleShape)
                                .border(if (isSelected) 3.dp else 1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
                                    CircleShape)
                                .clickable(enabled = !saving, role = Role.RadioButton) {
                                    hex = "#%06X".format(Locale.ROOT, value and 0xffffff)
                                }.semantics { contentDescription = strings.getString(R.string.color_name, strings.getString(name)); selected = isSelected },
                                contentAlignment = Alignment.Center) {
                                if (isSelected) Text("✓", color = if (name in listOf(R.string.white, R.string.yellow, R.string.cyan)) Color.Black else Color.White)
                            }
                        }
                    }
                }
                OutlinedTextField(value = hex, onValueChange = { hex = it.take(7).uppercase(Locale.ROOT) },
                    label = { Text(strings.getString(R.string.color_hex)) }, singleLine = true, isError = color == null,
                    enabled = !saving, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { Button(enabled = !saving && color != null,
            onClick = { color?.let { onSave(slot, InkStyle(it, width)) } }) {
            Text(if (saving) strings.getString(R.string.saving) else strings.getString(R.string.save_use))
        } },
        dismissButton = { TextButton(enabled = !saving, onClick = onDismiss) { Text(strings.getString(R.string.cancel)) } })
}
