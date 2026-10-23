package com.gtavoapps.portylus.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.gtavoapps.portylus.explorer.BrowserPreferences
import com.gtavoapps.portylus.explorer.FolderLocation
import com.gtavoapps.portylus.explorer.FolderRef
import com.gtavoapps.portylus.explorer.SortOrder
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject

private val Context.folderDataStore by preferencesDataStore("folders")

class FolderPreferences(context: Context) {
    private val store = context.applicationContext.folderDataStore
    private val key = stringPreferencesKey("browser_v1")

    suspend fun read(): BrowserPreferences {
        val raw = store.data.first()[key] ?: return BrowserPreferences()
        val json = JSONObject(raw)
        return BrowserPreferences(decodeList(json.getJSONArray("roots")),
            decodeList(json.getJSONArray("bookmarks")),
            json.optJSONObject("last")?.let(::decode),
            SortOrder.valueOf(json.getString("sort")))
    }

    suspend fun write(value: BrowserPreferences) {
        val json = JSONObject().put("roots", encodeList(value.roots))
            .put("bookmarks", encodeList(value.bookmarks)).put("sort", value.sort.name)
        value.lastLocation?.let { json.put("last", encode(it)) }
        store.edit { it[key] = json.toString() }
    }

    private fun encode(location: FolderLocation) = JSONObject().put("tree", location.treeUri)
        .put("trail", JSONArray().apply {
            location.trail.forEach { put(JSONObject().put("uri", it.uri).put("name", it.name)) }
        })
    private fun decode(json: JSONObject) = FolderLocation(json.getString("tree"),
        json.getJSONArray("trail").let { array -> (0 until array.length()).map {
            val item = array.getJSONObject(it)
            FolderRef(item.getString("uri"), item.getString("name"))
        } })
    private fun encodeList(items: List<FolderLocation>) = JSONArray().apply { items.forEach { put(encode(it)) } }
    private fun decodeList(array: JSONArray) = (0 until array.length()).map { decode(array.getJSONObject(it)) }
}
