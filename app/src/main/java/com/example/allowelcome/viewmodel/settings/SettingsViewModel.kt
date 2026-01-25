package com.example.allowelcome.viewmodel.settings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.allowelcome.data.SettingsStore
import com.example.allowelcome.data.TechProfile
import com.example.allowelcome.data.Template
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val TAG = "SettingsViewModel"

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

    // Error events for UI to observe (buffered to avoid missing events)
    private val _errorEvents = MutableSharedFlow<String>(replay = 0, extraBufferCapacity = 1)
    val errorEvents: SharedFlow<String> = _errorEvents.asSharedFlow()

    fun save(profile: TechProfile) {
        viewModelScope.launch {
            try {
                store.saveTechProfile(profile)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save tech profile", e)
                _errorEvents.emit("Failed to save profile: ${e.message}")
            }
        }
    }

    fun saveTemplate(template: Template) {
        viewModelScope.launch {
            try {
                store.saveTemplate(template)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save template", e)
                _errorEvents.emit("Failed to save template: ${e.message}")
            }
        }
    }

    fun setActiveTemplate(templateId: String) {
        viewModelScope.launch {
            try {
                store.setActiveTemplate(templateId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set active template", e)
                _errorEvents.emit("Failed to set active template: ${e.message}")
            }
        }
    }

    fun deleteTemplate(templateId: String) {
        viewModelScope.launch {
            try {
                store.deleteTemplate(templateId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete template", e)
                _errorEvents.emit("Failed to delete template: ${e.message}")
            }
        }
    }

    fun resetTemplate() {
        viewModelScope.launch {
            try {
                store.resetToDefaultTemplate()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to reset template", e)
                _errorEvents.emit("Failed to reset template: ${e.message}")
            }
        }
    }
}
