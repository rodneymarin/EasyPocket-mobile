package com.easypocket.mobile.i18n

import androidx.compose.runtime.staticCompositionLocalOf

enum class Language(val code: String) {
    ENGLISH("en"),
    SPANISH("es");

    companion object {
        fun fromCode(code: String?): Language =
            entries.firstOrNull { it.code == code } ?: ENGLISH
    }
}

val LocalLanguage = staticCompositionLocalOf { Language.ENGLISH }
