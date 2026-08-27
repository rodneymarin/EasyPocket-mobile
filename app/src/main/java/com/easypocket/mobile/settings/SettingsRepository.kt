package com.easypocket.mobile.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.easypocket.mobile.i18n.Language
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    private object Keys {
        val LANGUAGE = stringPreferencesKey("language")
        val THEME = stringPreferencesKey("theme")
    }

    val languageFlow: Flow<Language> =
        context.settingsDataStore.data.map { p -> Language.fromCode(p[Keys.LANGUAGE]) }

    val themeModeFlow: Flow<ThemeMode> =
        context.settingsDataStore.data.map { p -> ThemeMode.fromName(p[Keys.THEME]) }

    suspend fun setLanguage(language: Language) {
        context.settingsDataStore.edit { it[Keys.LANGUAGE] = language.code }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.settingsDataStore.edit { it[Keys.THEME] = mode.name }
    }
}
