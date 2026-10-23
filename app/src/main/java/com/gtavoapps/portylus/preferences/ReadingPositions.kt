package com.gtavoapps.portylus.preferences

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import java.security.MessageDigest

/** Private positions only: no names, clear-text URIs or recent-document list. */
class ReadingPositions(context: Context) {
    private val store = context.applicationContext.getSharedPreferences("reading_positions", Context.MODE_PRIVATE)

    fun key(uri: Uri): String {
        // A document selected directly and through a SAF tree must share its position.
        val identity = if (DocumentsContract.isDocumentUri(context, uri)) {
            "${uri.authority}:${DocumentsContract.getDocumentId(uri)}"
        } else uri.normalizeScheme().toString()
        return MessageDigest.getInstance("SHA-256").digest(identity.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }

    private val context = context.applicationContext
    fun read(key: String, pageCount: Int): Int = store.getInt(key, 1).coerceIn(1, pageCount.coerceAtLeast(1))
    fun write(key: String, page: Int) {
        require(page > 0)
        // apply updates memory immediately; Android drains pending writes at lifecycle boundaries.
        store.edit().putInt(key, page).apply()
    }
}
