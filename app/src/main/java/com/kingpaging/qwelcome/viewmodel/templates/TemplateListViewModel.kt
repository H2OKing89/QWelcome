package com.kingpaging.qwelcome.viewmodel.templates

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kingpaging.qwelcome.R
import com.kingpaging.qwelcome.data.DEFAULT_TEMPLATE_ID
import com.kingpaging.qwelcome.data.NEW_TEMPLATE_ID
import com.kingpaging.qwelcome.data.SettingsStore
import com.kingpaging.qwelcome.data.Template
import com.kingpaging.qwelcome.data.TemplateSelectionChange
import com.kingpaging.qwelcome.data.TemplateSelectionResult
import com.kingpaging.qwelcome.util.ResourceProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant

private const val TAG = "TemplateListViewModel"
private const val TEMPLATE_SOFT_LIMIT = 20
private const val MAX_TAG_LENGTH = 32
internal const val MAX_TEMPLATE_NAME_LENGTH = 50

internal fun filterAndOrderTemplates(
    templates: List<Template>,
    activeTemplateId: String,
    searchQuery: String,
    selectedTags: Set<String>,
    templateLastUsedAt: Map<String, Long> = emptyMap()
): List<Template> {
    val query = searchQuery.trim()
    val normalizedSelectedTags = selectedTags
        .map { it.trim().lowercase() }
        .filter { it.isNotBlank() }
        .toSet()
    val matchingTemplates = templates.filter { template ->
        val matchesTags = normalizedSelectedTags.isEmpty() || template.tags.any { tag ->
            tag.trim().lowercase() in normalizedSelectedTags
        }
        val matchesQuery = query.isEmpty() ||
            template.name.contains(query, ignoreCase = true) ||
            template.content.contains(query, ignoreCase = true)
        matchesTags && matchesQuery
    }
    return matchingTemplates
        .withIndex()
        .sortedWith(
            compareByDescending<IndexedValue<Template>> { it.value.id == activeTemplateId }
                .thenByDescending { templateLastUsedAt[it.value.id] ?: Long.MIN_VALUE }
                .thenBy { it.index }
        )
        .map { it.value }
}

/**
 * UI state for the template list screen.
 */
data class TemplateListUiState(
    val templates: List<Template> = emptyList(),
    val activeTemplateId: String = DEFAULT_TEMPLATE_ID,
    val isLoading: Boolean = true,
    val editingTemplate: Template? = null,
    val showDeleteConfirmation: Template? = null,
    val searchQuery: String = "",
    val selectedTags: Set<String> = emptySet(),
    val allTags: Set<String> = emptySet(),
    val templateLastUsedAt: Map<String, Long> = emptyMap(),
    val showTemplateLimitWarning: Boolean = false,
    val warningDismissed: Boolean = false,
    val validationError: String? = null // For required placeholder errors
)

/**
 * UI state for the template editor.
 */
data class TemplateEditorUiState(
    val name: String = "",
    val tags: List<String> = emptyList(),
    val newTagInput: String = "",
    val contentText: String = "",
    val nameError: Int? = null,
    val contentError: String? = null,
    val showDiscardDialog: Boolean = false
)

/**
 * One-shot events for the template list screen.
 */
sealed class TemplateListEvent {
    data class Error(val message: String) : TemplateListEvent()
    data class TemplateCreated(val template: Template) : TemplateListEvent()
    data class TemplateUpdated(val template: Template) : TemplateListEvent()
    data class TemplateDeleted(val name: String) : TemplateListEvent()
    data class TemplateDuplicated(val template: Template) : TemplateListEvent()
    data class ActiveTemplateChanged(
        val template: Template,
        val change: TemplateSelectionChange
    ) : TemplateListEvent()
    data class TemplateSelectionBlocked(
        val template: Template,
        val missingPlaceholders: List<String>
    ) : TemplateListEvent()
    data class TemplateRenamed(val template: Template) : TemplateListEvent()
}

enum class TemplateListEventOwner {
    INTAKE,
    LIBRARY,
    EDITOR
}

private data class OwnedTemplateListEvent(
    val owner: TemplateListEventOwner,
    val event: TemplateListEvent
)

/**
 * ViewModel for the template list/management screen.
 * Handles CRUD operations for templates.
 */
class TemplateListViewModel(
    private val settingsStore: SettingsStore,
    private val resourceProvider: ResourceProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(TemplateListUiState())
    val uiState: StateFlow<TemplateListUiState> = _uiState.asStateFlow()

    private val _templateEditorUiState = MutableStateFlow(TemplateEditorUiState())
    val templateEditorUiState: StateFlow<TemplateEditorUiState> = _templateEditorUiState.asStateFlow()

    private val _events = MutableSharedFlow<OwnedTemplateListEvent>(replay = 0, extraBufferCapacity = 1)

    fun eventsFor(owner: TemplateListEventOwner): Flow<TemplateListEvent> = _events
        .filter { it.owner == owner }
        .map { it.event }

    private val _navigateToEditor = MutableSharedFlow<Unit>(replay = 0, extraBufferCapacity = 1)
    val navigateToEditor: SharedFlow<Unit> = _navigateToEditor.asSharedFlow()

    init {
        // Combine templates and active template ID into UI state
        viewModelScope.launch {
            combine(
                settingsStore.allTemplatesFlow,
                settingsStore.activeTemplateIdFlow,
                settingsStore.templateLastUsedFlow
            ) { templates, activeId, templateLastUsedAt ->
                TemplateListUiState(
                    templates = templates,
                    activeTemplateId = activeId,
                    templateLastUsedAt = templateLastUsedAt,
                    allTags = templates
                        .flatMap { it.tags }
                        .map { it.trim() }
                        .filter { it.isNotBlank() }
                        .toSet(),
                    showTemplateLimitWarning = templates.size > TEMPLATE_SOFT_LIMIT,
                    isLoading = false
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
                        isLoading = newState.isLoading
                    )
                }
            }
        }
    }

    /**
     * Get the default template content for creating new templates.
     */
    fun getDefaultTemplateContent(): String = settingsStore.defaultTemplateContent

    /**
     * Set the active template.
     */
    fun setActiveTemplate(
        templateId: String,
        eventOwner: TemplateListEventOwner = TemplateListEventOwner.LIBRARY
    ) {
        viewModelScope.launch {
            try {
                when (val result = settingsStore.setActiveTemplate(templateId)) {
                    is TemplateSelectionResult.Selected -> {
                        emitEvent(
                            eventOwner,
                            TemplateListEvent.ActiveTemplateChanged(result.template, result.change)
                        )
                    }
                    is TemplateSelectionResult.Blocked -> {
                        emitEvent(
                            eventOwner,
                            TemplateListEvent.TemplateSelectionBlocked(
                                result.template,
                                result.missingPlaceholders
                            )
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

    fun renameTemplate(templateId: String, name: String) {
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
                    TemplateListEvent.TemplateRenamed(renamedTemplate)
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
     * Start editing a template (for creating or updating).
     */
    fun startEditing(template: Template?) {
        _uiState.update { it.copy(editingTemplate = template) }
        if (template != null) {
            initializeTemplateEditorState(template)
            viewModelScope.launch {
                _navigateToEditor.emit(Unit)
            }
        } else {
            resetTemplateEditorState()
        }
    }

    /**
     * Cancel editing without saving.
     */
    fun cancelEditing() {
        _uiState.update { it.copy(editingTemplate = null) }
        resetTemplateEditorState()
    }

    fun updateName(name: String) {
        _templateEditorUiState.update { current ->
            current.copy(
                name = name.take(MAX_TEMPLATE_NAME_LENGTH),
                nameError = null
            )
        }
    }

    fun updateTags(tags: List<String>) {
        _templateEditorUiState.update { it.copy(tags = sanitizeTags(tags)) }
    }

    fun updateNewTagInput(newTagInput: String) {
        _templateEditorUiState.update { it.copy(newTagInput = newTagInput.take(MAX_TAG_LENGTH)) }
    }

    fun updateContent(content: String) {
        _templateEditorUiState.update { current ->
            current.copy(
                contentText = content,
                contentError = missingPlaceholdersError(content)
            )
        }
    }

    fun setNameError(nameError: Int?) {
        _templateEditorUiState.update { it.copy(nameError = nameError) }
    }

    fun setContentError(contentError: String?) {
        _templateEditorUiState.update { it.copy(contentError = contentError) }
    }

    fun toggleDiscardDialog(show: Boolean = !_templateEditorUiState.value.showDiscardDialog) {
        _templateEditorUiState.update { it.copy(showDiscardDialog = show) }
    }

    /**
     * Create a new template.
     * Validates that required placeholders are present before saving.
     */
    fun createTemplate(name: String, content: String, tags: List<String>) {
        // Validate required placeholders (belt + suspenders with UI layer)
        val missingPlaceholders = Template.findMissingPlaceholders(content)
        if (missingPlaceholders.isNotEmpty()) {
            val errorMsg = resourceProvider.getString(
                R.string.error_template_required_placeholders_missing,
                missingPlaceholders.joinToString(", ")
            )
            _uiState.update { it.copy(validationError = errorMsg) }
            setContentError(missingPlaceholdersError(content))
            viewModelScope.launch {
                emitEvent(TemplateListEventOwner.EDITOR, TemplateListEvent.Error(errorMsg))
            }
            return
        }
        
        viewModelScope.launch {
            try {
                val template = Template.create(
                    name.trim().take(MAX_TEMPLATE_NAME_LENGTH),
                    content
                ).copy(tags = sanitizeTags(tags))
                settingsStore.saveTemplate(template)
                _uiState.update { it.copy(editingTemplate = null, validationError = null) }
                resetTemplateEditorState()
                emitEvent(TemplateListEventOwner.EDITOR, TemplateListEvent.TemplateCreated(template))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create template", e)
                emitError(TemplateListEventOwner.EDITOR, R.string.error_template_create_failed)
            }
        }
    }

    /**
     * Update an existing template.
     * Validates that required placeholders are present before saving.
     */
    fun updateTemplate(templateId: String, name: String, content: String, tags: List<String>) {
        // Validate required placeholders (belt + suspenders with UI layer)
        val missingPlaceholders = Template.findMissingPlaceholders(content)
        if (missingPlaceholders.isNotEmpty()) {
            val errorMsg = resourceProvider.getString(
                R.string.error_template_required_placeholders_missing,
                missingPlaceholders.joinToString(", ")
            )
            _uiState.update { it.copy(validationError = errorMsg) }
            setContentError(missingPlaceholdersError(content))
            viewModelScope.launch {
                emitEvent(TemplateListEventOwner.EDITOR, TemplateListEvent.Error(errorMsg))
            }
            return
        }
        
        viewModelScope.launch {
            try {
                val existing = settingsStore.getTemplate(templateId)
                if (existing != null) {
                    val updated = existing.copy(
                        name = name.trim().take(MAX_TEMPLATE_NAME_LENGTH),
                        content = Template.normalizeContent(content),
                        tags = sanitizeTags(tags),
                        modifiedAt = Instant.now().toString()
                    )
                    settingsStore.saveTemplate(updated)
                    _uiState.update { it.copy(editingTemplate = null, validationError = null) }
                    resetTemplateEditorState()
                    emitEvent(TemplateListEventOwner.EDITOR, TemplateListEvent.TemplateUpdated(updated))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update template", e)
                emitError(TemplateListEventOwner.EDITOR, R.string.error_template_update_failed)
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
                    TemplateListEvent.TemplateDuplicated(duplicate)
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
                    TemplateListEvent.TemplateDuplicated(duplicate)
                )
                // Immediately open the duplicate for editing, clear any stale validation error
                _uiState.update { it.copy(editingTemplate = duplicate, validationError = null) }
                initializeTemplateEditorState(duplicate)
                _navigateToEditor.emit(Unit)
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
            val newSelectedTags = if (tag in current.selectedTags) {
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
    
    /**
     * Clear any validation errors (e.g., when user starts typing in editor).
     */
    fun clearValidationError() {
        _uiState.update { it.copy(validationError = null) }
    }

    private fun initializeTemplateEditorState(template: Template) {
        val isNewTemplate = template.id == NEW_TEMPLATE_ID
        val initialName = if (isNewTemplate) "" else template.name
        val initialContent = if (isNewTemplate) {
            settingsStore.defaultTemplateContent
        } else {
            template.content
        }

        _templateEditorUiState.value = TemplateEditorUiState(
            name = initialName,
            tags = sanitizeTags(template.tags),
            newTagInput = "",
            contentText = initialContent,
            nameError = null,
            contentError = missingPlaceholdersError(initialContent),
            showDiscardDialog = false
        )
    }

    private fun resetTemplateEditorState() {
        _templateEditorUiState.value = TemplateEditorUiState()
    }

    private fun missingPlaceholdersError(content: String): String? {
        val missing = Template.findMissingPlaceholders(content)
        return if (missing.isNotEmpty()) {
            missing.joinToString(", ") {
                it.removePrefix("{{ ").removeSuffix(" }}")
            }
        } else {
            null
        }
    }

    private fun sanitizeTags(tags: List<String>): List<String> {
        val seen = mutableSetOf<String>()
        return tags.map { it.trim() }
            .filter { it.isNotBlank() }
            .map { it.take(MAX_TAG_LENGTH) }
            .filter { seen.add(it.lowercase()) }
    }

    private suspend fun emitEvent(owner: TemplateListEventOwner, event: TemplateListEvent) {
        _events.emit(OwnedTemplateListEvent(owner, event))
    }

    private suspend fun emitError(owner: TemplateListEventOwner, messageResId: Int) {
        emitEvent(owner, TemplateListEvent.Error(resourceProvider.getString(messageResId)))
    }
}
