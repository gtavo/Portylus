package com.gtavoapps.portylus.ui

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
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Goma") }, text = {
        Column {
            Text("Tipo de borrado")
            FilterChip(selected = options.mode == EraserMode.PARTIAL,
                onClick = { onChange(options.copy(mode = EraserMode.PARTIAL)) }, label = { Text("Parcial") })
            FilterChip(selected = options.mode == EraserMode.WHOLE,
                onClick = { onChange(options.copy(mode = EraserMode.WHOLE)) }, label = { Text("Trazo completo") })
            Text("Borrar solo")
            FilterChip(selected = options.filter == AnnotationKind.PEN,
                onClick = { onChange(options.copy(filter = AnnotationKind.PEN)) }, label = { Text("Escritura") })
            FilterChip(selected = options.filter == AnnotationKind.HIGHLIGHTER,
                onClick = { onChange(options.copy(filter = AnnotationKind.HIGHLIGHTER)) }, label = { Text("Resaltados") })
        }
    }, confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } })
}
