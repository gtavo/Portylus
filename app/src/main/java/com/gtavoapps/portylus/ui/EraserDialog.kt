package com.gtavoapps.portylus.ui

import com.gtavoapps.portylus.R
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.gtavoapps.portylus.annotations.AnnotationKind
import com.gtavoapps.portylus.annotations.EraserMode
import com.gtavoapps.portylus.annotations.EraserOptions

@Composable
fun EraserDialog(options: EraserOptions, onChange: (EraserOptions) -> Unit, onDismiss: () -> Unit) {
    val strings = androidx.compose.ui.platform.LocalContext.current
    AlertDialog(onDismissRequest = onDismiss, title = { Text(strings.getString(R.string.eraser)) }, text = {
        Column {
            Text(strings.getString(R.string.erase_type))
            FilterChip(selected = options.mode == EraserMode.PARTIAL,
                onClick = { onChange(options.copy(mode = EraserMode.PARTIAL)) }, label = { Text(strings.getString(R.string.partial)) })
            FilterChip(selected = options.mode == EraserMode.WHOLE,
                onClick = { onChange(options.copy(mode = EraserMode.WHOLE)) }, label = { Text(strings.getString(R.string.whole_stroke)) })
            Text(strings.getString(R.string.erase_only))
            FilterChip(selected = options.filter == AnnotationKind.PEN,
                onClick = { onChange(options.copy(filter = AnnotationKind.PEN)) }, label = { Text(strings.getString(R.string.handwriting)) })
            FilterChip(selected = options.filter == AnnotationKind.HIGHLIGHTER,
                onClick = { onChange(options.copy(filter = AnnotationKind.HIGHLIGHTER)) }, label = { Text(strings.getString(R.string.highlights)) })
        }
    }, confirmButton = { TextButton(onClick = onDismiss) { Text(strings.getString(R.string.close)) } })
}
