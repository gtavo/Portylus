package com.gtavoapps.portylus.preferences

import android.content.Context
import androidx.core.content.edit

/** Device defaults remain overridable; orientation never changes the user's input choice. */
class ReaderPreferences(context: Context) {
    private val preferences = context.getSharedPreferences("reader_settings", Context.MODE_PRIVATE)
    val fingerWriting = preferences.getBoolean("finger_writing", context.resources.configuration.smallestScreenWidthDp < 600)
    val keepAwake = preferences.getBoolean("keep_awake", false)
    val appearance = AppAppearance.fromPreference(preferences.getString("appearance", null))

    fun setFingerWriting(value: Boolean) { preferences.edit { putBoolean("finger_writing", value) } }
    fun setKeepAwake(value: Boolean) { preferences.edit { putBoolean("keep_awake", value) } }
    fun setAppearance(value: AppAppearance) { preferences.edit { putString("appearance", value.preferenceValue) } }
}
