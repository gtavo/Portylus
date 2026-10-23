package com.gtavoapps.portylus.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.gtavoapps.portylus.annotations.InkPreferences
import com.gtavoapps.portylus.annotations.InkStyle
import com.gtavoapps.portylus.annotations.ToolPresets
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject

private val Context.inkStyleDataStore by preferencesDataStore("ink_styles")

/** Separate from folder permissions/bookmarks; contains exactly six presets and two selections. */
class InkStylePreferences(context: Context) {
    private val store = context.applicationContext.inkStyleDataStore
    private val key = stringPreferencesKey("styles_v1")

    suspend fun read(): InkPreferences {
        val raw = store.data.first()[key] ?: return InkPreferences()
        val json = JSONObject(raw)
        return InkPreferences(decode(json.getJSONObject("pen")), decode(json.getJSONObject("highlighter")))
    }

    suspend fun write(value: InkPreferences) {
        val raw = JSONObject().put("pen", encode(value.pen)).put("highlighter", encode(value.highlighter)).toString()
        store.edit { it[key] = raw }
    }

    private fun encode(value: ToolPresets) = JSONObject().put("selected", value.selected)
        .put("slots", JSONArray().apply {
            value.slots.forEach { put(JSONObject().put("color", it.color).put("width", it.width.toDouble())) }
        })

    private fun decode(json: JSONObject): ToolPresets {
        val slots = json.getJSONArray("slots")
        return ToolPresets((0 until slots.length()).map { index ->
            val slot = slots.getJSONObject(index)
            InkStyle(slot.getInt("color"), slot.getDouble("width").toFloat())
        }, json.getInt("selected"))
    }
}
