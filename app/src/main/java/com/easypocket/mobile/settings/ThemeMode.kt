package com.easypocket.mobile.settings

enum class ThemeMode {
    LIGHT, DARK, SYSTEM;

    companion object {
        fun fromName(name: String?): ThemeMode =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: SYSTEM
    }
}
