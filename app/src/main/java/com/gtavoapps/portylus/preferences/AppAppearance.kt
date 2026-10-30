package com.gtavoapps.portylus.preferences

/** Stable values, independent of translated labels and enum order. */
enum class AppAppearance(val preferenceValue: String) {
    SYSTEM("system"), LIGHT("light"), DARK("dark");

    fun usesDarkTheme(systemDark: Boolean): Boolean = when (this) {
        SYSTEM -> systemDark
        LIGHT -> false
        DARK -> true
    }

    companion object {
        fun fromPreference(value: String?): AppAppearance =
            entries.firstOrNull { it.preferenceValue == value } ?: SYSTEM
    }
}
