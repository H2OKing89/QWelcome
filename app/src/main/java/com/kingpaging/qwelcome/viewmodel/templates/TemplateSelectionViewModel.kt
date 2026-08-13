package com.kingpaging.qwelcome.viewmodel.templates

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kingpaging.qwelcome.R
import com.kingpaging.qwelcome.data.DEFAULT_TEMPLATE_ID
import com.kingpaging.qwelcome.data.SettingsStore
import com.kingpaging.qwelcome.data.Template
import com.kingpaging.qwelcome.data.TemplateSelectionResult
import com.kingpaging.qwelcome.util.ResourceProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException

private const val TAG = "TemplateSelectionViewModel"

data class TemplateSelectionUiState(
    val templates: List<Template> = emptyList(),
    val activeTemplateId: String = DEFAULT_TEMPLATE_ID,
    val isLoading: Boolean = true,
)

sealed interface TemplateSelectionEvent {
    data class Error(
        val message: String,
    ) : TemplateSelectionEvent

    data class SelectionBlocked(
        val template: Template,
        val missingPlaceholders: List<String>,
    ) : TemplateSelectionEvent
}

class TemplateSelectionViewModel(
    private val settingsStore: SettingsStore,
    private val resourceProvider: ResourceProvider,
) : ViewModel() {
    private val _uiState = MutableStateFlow(TemplateSelectionUiState())
    val uiState: StateFlow<TemplateSelectionUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<TemplateSelectionEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<TemplateSelectionEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            combine(
                settingsStore.allTemplatesFlow,
                settingsStore.activeTemplateIdFlow,
            ) { templates, activeTemplateId ->
                TemplateSelectionUiState(
                    templates = templates,
                    activeTemplateId = activeTemplateId,
                    isLoading = false,
                )
            }.collect { state ->
                _uiState.update { state }
            }
        }
    }

    fun selectTemplate(templateId: String) {
        viewModelScope.launch {
            try {
                when (val result = settingsStore.setActiveTemplate(templateId)) {
                    is TemplateSelectionResult.Selected,
                    is TemplateSelectionResult.AlreadyActive,
                    -> Unit
                    is TemplateSelectionResult.Blocked -> {
                        _events.emit(
                            TemplateSelectionEvent.SelectionBlocked(
                                template = result.template,
                                missingPlaceholders = result.missingPlaceholders,
                            ),
                        )
                    }
                    is TemplateSelectionResult.NotFound -> {
                        _events.emit(
                            TemplateSelectionEvent.Error(
                                resourceProvider.getString(R.string.error_template_not_found),
                            ),
                        )
                    }
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: IOException) {
                Log.e(TAG, "Failed to select template", exception)
                _events.emit(
                    TemplateSelectionEvent.Error(
                        resourceProvider.getString(R.string.error_template_select_failed),
                    ),
                )
            }
        }
    }
}
