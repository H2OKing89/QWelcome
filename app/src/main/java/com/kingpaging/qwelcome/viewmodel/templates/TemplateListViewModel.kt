package com.kingpaging.qwelcome.viewmodel.templates

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kingpaging.qwelcome.R
import com.kingpaging.qwelcome.data.DEFAULT_TEMPLATE_ID
import com.kingpaging.qwelcome.data.SettingsStore
import com.kingpaging.qwelcome.data.Template
import com.kingpaging.qwelcome.data.TemplateSelectionChange
import com.kingpaging.qwelcome.data.TemplateSelectionResult
import com.kingpaging.qwelcome.util.ResourceProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "TemplateListViewModel"
private const val TEMPLATE_SOFT_LIMIT = 20
internal const val MAX_TEMPLATE_NAME_LENGTH = 50

internal fun filterAndOrderTemplates(
    templates: List<Template>,
    activeTemplateId: String,
    searchQuery: String,
    selectedTags: Set<String>,
    templateLastUsedAt: Map<String, Long> = emptyMap(),
): List<Template> {
    val query = searchQuery.trim()
    val normalizedSelectedTags =
        selectedTags
            .map { it.trim().lowercase() }
            .filter { it.isNotBlank() }
            .toSet()
    val matchingTemplates =
        templates.filter { template ->
            val matchesTags =
                normalizedSelectedTags.isEmpty() ||
                    template.tags.any { tag ->
                        tag.trim().lowercase() in normalizedSelectedTags
                    }
            val matchesQuery =
                query.isEmpty() ||
                    template.name.contains(query, ignoreCase = true) ||
                    template.content.contains(query, ignoreCase = true)
            matchesTags && matchesQuery
        }
    return matchingTemplates
        .withIndex()
        .sortedWith(
            compareByDescending<IndexedValue<Template>> { it.value.id == activeTemplateId }
                .thenByDescending { templateLastUsedAt[it.value.id] ?: Long.MIN_VALUE }
                .thenBy { it.index },
        ).map { it.value }
}

/**
 * UI state for the template list screen.
 */
data class TemplateListUiState(
    val templates: List<Template> = emptyList(),
    val activeTemplateId: String = DEFAULT_TEMPLATE_ID,
    val isLoading: Boolean = true,
    val showDeleteConfirmation: Template? = null,
    val searchQuery: String = "",
    val selectedTags: Set<String> = emptySet(),
    val allTags: Set<String> = emptySet(),
    val templateLastUsedAt: Map<String, Long> = emptyMap(),
    val showTemplateLimitWarning: Boolean = false,
    val warningDismissed: Boolean = false,
)

/**
 * One-shot events for the template list screen.
 */
sealed class TemplateListEvent {
    data class Error(
        val message: String,
    ) : TemplateListEvent()

    data class TemplateDeleted(
        val name: String,
    ) : TemplateListEvent()

    data class TemplateDuplicated(
        val template: Template,
        val openEditor: Boolean = false,
    ) : TemplateListEvent()

    data class ActiveTemplateChanged(
        val template: Template,
        val change: TemplateSelectionChange,
    ) : TemplateListEvent()

    data class TemplateSelectionBlocked(
        val template: Template,
        val missingPlaceholders: List<String>,
    ) : TemplateListEvent()

    data class TemplateRenamed(
        val template: Template,
    ) : TemplateListEvent()
}

enum class TemplateListEventOwner {
    INTAKE,
    LIBRARY,
}

private data class OwnedTemplateListEvent(
    val owner: TemplateListEventOwner,
    val event: TemplateListEvent,
)

/**
 * ViewModel for the template list/management screen.
 * Handles CRUD operations for templates.
 */
class TemplateListViewModel(
    private val settingsStore: SettingsStore,
    private val resourceProvider: ResourceProvider,
) : ViewModel() {
    private val _uiState = MutableStateFlow(TemplateListUiState())
    val uiState: StateFlow<TemplateListUiState> = _uiState.asStateFlow()

    private val events = MutableSharedFlow<OwnedTemplateListEvent>(replay = 0, extraBufferCapacity = 1)

    fun eventsFor(owner: TemplateListEventOwner): Flow<TemplateListEvent> =
        events
            .filter { it.owner == owner }
            .map { it.event }

    init {
        // Combine templates and active template ID into UI state
        viewModelScope.launch {
            combine(
                settingsStore.allTemplatesFlow,
                settingsStore.activeTemplateIdFlow,
                settingsStore.templateLastUsedFlow,
            ) { templates, activeId, templateLastUsedAt ->
                TemplateListUiState(
                    templates = templates,
                    activeTemplateId = activeId,
                    templateLastUsedAt = templateLastUsedAt,
                    allTags =
                        templates
                            .flatMap { it.tags }
                            .map { it.trim() }
                            .filter { it.isNotBlank() }
                            .toSet(),
                    showTemplateLimitWarning = templates.size > TEMPLATE_SOFT_LIMIT,
                    isLoading = false,
                )
            }.collect { newState ->
                _uiState.update { current ->
                    val selectedTags = current.selectedTags.intersect(newState.allTags)
                    current.copy(
                        templates = newState.templates,
                        activeTemplateId = newState.activeTemplateId,
                        selectedTags = selectedTags,
                        allTags = newState.allTags,
                        templateLastUsedAt = newState.templateLastUsedAt,
                        showTemplateLimitWarning = newState.showTemplateLimitWarning,
                        isLoading = newState.isLoading,
                    )
                }
            }
        }
    }

    /**
     * Set the active template.
     */
    fun setActiveTemplate(
        templateId: String,
        eventOwner: TemplateListEventOwner = TemplateListEventOwner.LIBRARY,
    ) {
        viewModelScope.launch {
            try {
                when (val result = settingsStore.setActiveTemplate(templateId)) {
                    is TemplateSelectionResult.Selected -> {
                        emitEvent(
                            eventOwner,
                            TemplateListEvent.ActiveTemplateChanged(result.template, result.change),
                        )
                    }
                    is TemplateSelectionResult.Blocked -> {
                        emitEvent(
                            eventOwner,
                            TemplateListEvent.TemplateSelectionBlocked(
                                result.template,
                                result.missingPlaceholders,
                            ),
                        )
                    }
                    is TemplateSelectionResult.NotFound -> {
                        emitError(eventOwner, R.string.error_template_not_found)
                    }
                    is TemplateSelectionResult.AlreadyActive -> Unit
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set active template", e)
                emitError(eventOwner, R.string.error_template_select_failed)
            }
        }
    }

    fun undoTemplateSelection(change: TemplateSelectionChange) {
        viewModelScope.launch {
            try {
                settingsStore.undoTemplateSelection(change)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to undo template selection", e)
                emitError(TemplateListEventOwner.LIBRARY, R.string.error_template_undo_failed)
            }
        }
    }

    fun renameTemplate(
        templateId: String,
        name: String,
    ) {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) {
            viewModelScope.launch {
                emitError(TemplateListEventOwner.LIBRARY, R.string.error_name_required)
            }
            return
        }

        viewModelScope.launch {
            try {
                val template = settingsStore.getTemplate(templateId)
                if (template == null || template.id == DEFAULT_TEMPLATE_ID) {
                    emitError(TemplateListEventOwner.LIBRARY, R.string.error_template_cannot_rename)
                    return@launch
                }
                val renamedTemplate = template.withUpdatedName(trimmedName.take(MAX_TEMPLATE_NAME_LENGTH))
                settingsStore.saveTemplate(renamedTemplate)
                emitEvent(
                    TemplateListEventOwner.LIBRARY,
                    TemplateListEvent.TemplateRenamed(renamedTemplate),
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to rename template", e)
                emitError(TemplateListEventOwner.LIBRARY, R.string.error_template_rename_failed)
            }
        }
    }

    /**
     * Show delete confirmation dialog for a template.
     */
    fun showDeleteConfirmation(template: Template) {
        _uiState.update { it.copy(showDeleteConfirmation = template) }
    }

    /**
     * Dismiss delete confirmation dialog.
     */
    fun dismissDeleteConfirmation() {
        _uiState.update { it.copy(showDeleteConfirmation = null) }
    }

    /**
     * Delete a template after confirmation.
     * If deleting the currently active template, resets to DEFAULT_TEMPLATE_ID.
     */
    fun deleteTemplate(templateId: String) {
        viewModelScope.launch {
            try {
                val template = settingsStore.getTemplate(templateId)
                val name = template?.name ?: resourceProvider.getString(R.string.label_template)

                settingsStore.deleteTemplate(templateId)
                _uiState.update { it.copy(showDeleteConfirmation = null) }
                emitEvent(TemplateListEventOwner.LIBRARY, TemplateListEvent.TemplateDeleted(name))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete template", e)
                emitError(TemplateListEventOwner.LIBRARY, R.string.error_template_delete_failed)
            }
        }
    }

    /**
     * Duplicate a template.
     */
    fun duplicateTemplate(template: Template) {
        viewModelScope.launch {
            try {
                val duplicate = template.duplicate()
                settingsStore.saveTemplate(duplicate)
                emitEvent(
                    TemplateListEventOwner.LIBRARY,
                    TemplateListEvent.TemplateDuplicated(duplicate),
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to duplicate template", e)
                emitError(TemplateListEventOwner.LIBRARY, R.string.error_template_duplicate_failed)
            }
        }
    }

    /**
     * Duplicate a template and immediately open it for editing.
     * This is the recommended flow for customizing the default template.
     */
    fun duplicateAndEdit(template: Template) {
        viewModelScope.launch {
            try {
                val duplicate = template.duplicate()
                settingsStore.saveTemplate(duplicate)
                emitEvent(
                    TemplateListEventOwner.LIBRARY,
                    TemplateListEvent.TemplateDuplicated(duplicate, openEditor = true),
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to duplicate template", e)
                emitError(TemplateListEventOwner.LIBRARY, R.string.error_template_duplicate_failed)
            }
        }
    }

    /**
     * Update the search query for filtering templates.
     */
    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun updateTagFilter(tag: String) {
        _uiState.update { current ->
            val newSelectedTags =
                if (tag in current.selectedTags) {
                    current.selectedTags - tag
                } else {
                    current.selectedTags + tag
                }
            current.copy(selectedTags = newSelectedTags)
        }
    }

    fun clearTagFilter() {
        _uiState.update { it.copy(selectedTags = emptySet()) }
    }

    fun dismissTemplateLimitWarning() {
        _uiState.update { it.copy(warningDismissed = true) }
    }

    private suspend fun emitEvent(
        owner: TemplateListEventOwner,
        event: TemplateListEvent,
    ) {
        events.emit(OwnedTemplateListEvent(owner, event))
    }

    private suspend fun emitError(
        owner: TemplateListEventOwner,
        messageResId: Int,
    ) {
        emitEvent(owner, TemplateListEvent.Error(resourceProvider.getString(messageResId)))
    }
}
