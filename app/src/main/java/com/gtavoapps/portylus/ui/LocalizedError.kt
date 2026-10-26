package com.gtavoapps.portylus.ui

import android.content.Context
import com.gtavoapps.portylus.R

/** Translate known engine/storage errors without losing unexpected provider diagnostics. */
fun Context.localizedError(message: String): String = when (message) {
    "La carpeta ya no está disponible." -> getString(R.string.folder_unavailable)
    "No se puede acceder a la carpeta." -> getString(R.string.folder_inaccessible)
    "El proveedor no ha devuelto el contenido de la carpeta." -> getString(R.string.provider_empty)
    "No se puede leer el destino." -> getString(R.string.target_read_error)
    "No se puede escribir el destino." -> getString(R.string.target_write_error)
    "No se puede leer el PDF." -> getString(R.string.pdf_read_error)
    "El archivo está vacío." -> getString(R.string.empty_file)
    "El PDF no permite copiar texto." -> getString(R.string.no_copy_permission)
    "La página tiene unas dimensiones no válidas." -> getString(R.string.invalid_page)
    "Este PDF necesita contraseña; todavía no se admite." -> getString(R.string.password_pdf)
    "El archivo no es un PDF." -> getString(R.string.not_pdf)
    "El PDF no contiene páginas." -> getString(R.string.no_pages)
    "No se puede leer y verificar el destino. Elige Guardar como." -> getString(R.string.verify_target_error)
    "El archivo ha cambiado fuera de Portylus. Usa Guardar como para conservar tus anotaciones." -> getString(R.string.external_change)
    "No se ha podido guardar aquí. El archivo anterior se conserva. Puedes usar Guardar como." -> getString(R.string.save_restored)
    "No se ha podido completar el guardado ni restaurar el destino. Portylus conserva una copia del archivo anterior para recuperarla." -> getString(R.string.save_recovery)
    else -> message
}
