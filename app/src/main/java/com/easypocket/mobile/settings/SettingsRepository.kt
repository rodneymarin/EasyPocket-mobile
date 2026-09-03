package com.easypocket.mobile.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.easypocket.mobile.i18n.Language
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    private object Keys {
        val LANGUAGE = stringPreferencesKey("language")
        val THEME = stringPreferencesKey("theme")
        val LAST_STORE = stringPreferencesKey("last_store")
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

    suspend fun getLastStore(): String? =
        context.settingsDataStore.data.map { it[Keys.LAST_STORE] }.first()

    suspend fun setLastStore(storeId: String?) {
        context.settingsDataStore.edit { prefs ->
            if (storeId == null) prefs.remove(Keys.LAST_STORE) else prefs[Keys.LAST_STORE] = storeId
        }
    }
}
