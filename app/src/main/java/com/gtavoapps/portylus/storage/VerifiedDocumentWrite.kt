package com.gtavoapps.portylus.storage

import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest

class DocumentWriteFailure(message: String, val recovery: File? = null, cause: Throwable? = null) :
    Exception(message, cause)

/** Read back byte-for-byte, detect external changes, and restore the exact previous bytes on failure. */
object VerifiedDocumentWrite {
    fun hash(file: File) = file.inputStream().use(::hash)
    fun hash(input: InputStream): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = ByteArray(64 * 1024)
        while (true) {
            val count = input.read(bytes)
            if (count < 0) break
            digest.update(bytes, 0, count)
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    fun write(prepared: File, backup: File, expected: String?,
        read: () -> InputStream, write: () -> OutputStream): String {
        // A target that cannot be read cannot be backed up or verified: leave it untouched.
        try { read().use { input -> backup.outputStream().use(input::copyTo) } }
        catch (error: Exception) {
            backup.delete()
            throw DocumentWriteFailure("No se puede leer y verificar el destino. Elige Guardar como.", cause = error)
        }
        val previous = hash(backup)
        if (expected != null && previous != expected) {
            backup.delete()
            throw DocumentWriteFailure("El archivo ha cambiado fuera de Portylus. Usa Guardar como para conservar tus anotaciones.")
        }
        val result = hash(prepared)
        try {
            write().use { output -> prepared.inputStream().use { it.copyTo(output) } }
            check(read().use(::hash) == result) { "El proveedor no ha conservado todos los bytes." }
            backup.delete()
            return result
        } catch (error: Exception) {
            val restored = runCatching {
                if (!runCatching { read().use(::hash) == previous }.getOrDefault(false)) {
                    write().use { output -> backup.inputStream().use { it.copyTo(output) } }
                    check(read().use(::hash) == previous)
                }
            }.isSuccess
            if (restored) backup.delete()
            throw DocumentWriteFailure(
                if (restored) "No se ha podido guardar aquí. El archivo anterior se conserva. Puedes usar Guardar como."
                else "No se ha podido completar el guardado ni restaurar el destino. Portylus conserva una copia del archivo anterior para recuperarla.",
                recovery = backup.takeUnless { restored }, cause = error)
        }
    }
}
