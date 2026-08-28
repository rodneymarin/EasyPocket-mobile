package com.easypocket.mobile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.easypocket.mobile.data.seed.Seeder
import com.easypocket.mobile.i18n.Language
import com.easypocket.mobile.settings.SettingsRepository
import com.easypocket.mobile.settings.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class AppViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val seeder: Seeder,
) : ViewModel() {
    val themeMode: StateFlow<ThemeMode> = settings.themeModeFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.SYSTEM)
    val language: StateFlow<Language> = settings.languageFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, Language.ENGLISH)

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady

    private val _fatalError = MutableStateFlow<Throwable?>(null)
    val fatalError: StateFlow<Throwable?> = _fatalError

    private val _menuVisible = MutableStateFlow(false)
    val menuVisible: StateFlow<Boolean> = _menuVisible

    private val _refreshTick = MutableStateFlow(0)
    val refreshTick: StateFlow<Int> = _refreshTick.asStateFlow()

    init { init() }

    fun init() {
        _fatalError.value = null
        viewModelScope.launch {
            try {
                seeder.seedIfEmpty()
                _isReady.value = true
            } catch (t: Throwable) { _fatalError.value = t }
        }
    }

    fun openMenu() { _menuVisible.value = true }
    fun closeMenu() { _menuVisible.value = false }
    fun notifyDataChanged() { _refreshTick.value += 1 }
    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { settings.setThemeMode(mode) }
    fun setLanguage(language: Language) = viewModelScope.launch { settings.setLanguage(language) }
    fun resetToSeed() = viewModelScope.launch {
        try { seeder.resetToSeed() } catch (t: Throwable) { _fatalError.value = t }
    }
}
