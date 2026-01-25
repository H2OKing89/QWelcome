package com.example.allowelcome.viewmodel.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.allowelcome.data.SettingsStore
import com.example.allowelcome.data.TechProfile
import com.example.allowelcome.data.TemplateSettings
import com.example.allowelcome.data.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val store: SettingsStore
) : ViewModel() {

    val techProfile: StateFlow<TechProfile> =
        store.techProfileFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = TechProfile()
        )

    val templateSettings: StateFlow<TemplateSettings> =
        store.templateSettingsFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = TemplateSettings()
        )

    val themeMode: StateFlow<ThemeMode> =
        store.themeModeFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ThemeMode.SYSTEM
        )

    fun getDefaultTemplate(): String = store.defaultTemplate

    fun save(profile: TechProfile) {
        viewModelScope.launch { store.saveTechProfile(profile) }
    }

    fun saveTemplate(settings: TemplateSettings) {
        viewModelScope.launch { store.saveTemplateSettings(settings) }
    }

    fun saveThemeMode(mode: ThemeMode) {
        viewModelScope.launch { store.saveThemeMode(mode) }
    }

    fun resetTemplate() {
        viewModelScope.launch { store.resetToDefaultTemplate() }
    }
}
