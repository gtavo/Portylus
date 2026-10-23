package com.gtavoapps.portylus.storage

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import com.gtavoapps.portylus.explorer.DocumentEntry
import com.gtavoapps.portylus.explorer.FolderLocation
import com.gtavoapps.portylus.explorer.FolderRef
import java.io.File

class DocumentStorage(private val resolver: ContentResolver) {
    fun root(tree: Uri): FolderLocation {
        val uri = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
        val name = displayName(uri) ?: "Carpeta"
        return FolderLocation(tree.toString(), listOf(FolderRef(uri.toString(), name)))
    }

    fun list(location: FolderLocation): List<DocumentEntry> {
        val tree = Uri.parse(location.treeUri)
        val current = Uri.parse(location.current.uri)
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, DocumentsContract.getDocumentId(current))
        val projection = arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME, DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED, DocumentsContract.Document.COLUMN_SIZE)
        // Check the location itself, so an unavailable folder never looks like an empty folder.
        resolver.query(current, arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID), null, null, null)?.use {
            check(it.moveToFirst()) { "La carpeta ya no está disponible." }
        } ?: error("No se puede acceder a la carpeta.")
        return resolver.query(children, projection, null, null, null)?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val typeColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
            val modifiedColumn = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
            val sizeColumn = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
            buildList {
                while (cursor.moveToNext()) {
                    val id = cursor.getString(idColumn)
                    val name = cursor.getString(nameColumn) ?: "Sin nombre"
                    val type = cursor.getString(typeColumn)
                    val directory = type == DocumentsContract.Document.MIME_TYPE_DIR
                    if (directory || type == "application/pdf" || name.endsWith(".pdf", ignoreCase = true)) {
                        add(DocumentEntry(DocumentsContract.buildDocumentUriUsingTree(tree, id).toString(),
                            name, directory, if (modifiedColumn < 0 || cursor.isNull(modifiedColumn)) 0 else cursor.getLong(modifiedColumn),
                            if (directory || sizeColumn < 0 || cursor.isNull(sizeColumn)) null
                            else cursor.getLong(sizeColumn).takeIf { it >= 0 }))
                    }
                }
            }
        } ?: error("El proveedor no ha devuelto el contenido de la carpeta.")
    }

    fun displayName(uri: Uri): String? = resolver.query(uri,
        arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
        if (it.moveToFirst()) it.getColumnIndex(OpenableColumns.DISPLAY_NAME).takeIf { index -> index >= 0 }?.let { index -> it.getString(index) } else null
    }


    fun writeVerified(uri: Uri, prepared: File, expected: String?, cache: File): String {
        val backup = File.createTempFile("portylus-backup-", ".pdf", cache)
        return VerifiedDocumentWrite.write(prepared, backup, expected,
            read = { resolver.openInputStream(uri) ?: error("No se puede leer el destino.") },
            write = { resolver.openOutputStream(uri, "wt") ?: error("No se puede escribir el destino.") })
    }

    fun copyForReading(uri: Uri, destination: File) {
        // Never open the source for writing. No bitmap export or document history.
        resolver.openInputStream(uri)?.use { input ->
            destination.outputStream().use { output -> input.copyTo(output) }
        } ?: error("No se puede leer el PDF.")
        check(destination.length() > 0) { "El archivo está vacío." }
    }
}
