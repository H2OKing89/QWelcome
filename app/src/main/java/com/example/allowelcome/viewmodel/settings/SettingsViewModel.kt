package com.example.allowelcome.viewmodel.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.allowelcome.data.SettingsStore
import com.example.allowelcome.data.TechProfile
import com.example.allowelcome.data.Template
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

    val allTemplates: StateFlow<List<Template>> =
        store.allTemplatesFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val activeTemplate: StateFlow<Template> =
        store.activeTemplateFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = Template(name = "Default", content = store.defaultTemplateContent)
        )

    fun getDefaultTemplateContent(): String = store.defaultTemplateContent

    fun save(profile: TechProfile) {
        viewModelScope.launch { store.saveTechProfile(profile) }
    }

    fun saveTemplate(template: Template) {
        viewModelScope.launch { store.saveTemplate(template) }
    }

    fun setActiveTemplate(templateId: String) {
        viewModelScope.launch { store.setActiveTemplate(templateId) }
    }

    fun deleteTemplate(templateId: String) {
        viewModelScope.launch { store.deleteTemplate(templateId) }
    }

    fun resetTemplate() {
        viewModelScope.launch { store.resetToDefaultTemplate() }
    }
}
