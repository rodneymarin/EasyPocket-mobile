package com.easypocket.mobile.i18n

enum class Language(val code: String) {
    ENGLISH("en"),
    SPANISH("es");

    companion object {
        fun fromCode(code: String?): Language =
            entries.firstOrNull { it.code == code } ?: ENGLISH
    }
}
