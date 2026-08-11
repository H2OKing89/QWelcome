package com.kingpaging.qwelcome.viewmodel.templates

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kingpaging.qwelcome.R
import com.kingpaging.qwelcome.data.NEW_TEMPLATE_ID
import com.kingpaging.qwelcome.data.SettingsStore
import com.kingpaging.qwelcome.data.Template
import com.kingpaging.qwelcome.util.ResourceProvider
import java.time.Instant
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "TemplateEditorViewModel"
private const val MAX_TAG_LENGTH = 32

private const val KEY_TEMPLATE_ID = "templateId"
private const val KEY_NAME = "editorName"
private const val KEY_TAGS = "editorTags"
private const val KEY_NEW_TAG_INPUT = "editorNewTagInput"
private const val KEY_CONTENT = "editorContent"
private const val KEY_SHOW_DISCARD_DIALOG = "editorShowDiscardDialog"
private const val KEY_SAVE_COMPLETED = "editorSaveCompleted"

data class TemplateEditorUiState(
    val template: Template? = null,
    val isLoading: Boolean = true,
    val name: String = "",
    val tags: List<String> = emptyList(),
    val newTagInput: String = "",
    val contentText: String = "",
    val nameError: Int? = null,
    val contentError: String? = null,
    val showDiscardDialog: Boolean = false,
    val isSaveCompleted: Boolean = false
)

sealed class TemplateEditorEvent {
    data class TemplateCreated(val template: Template) : TemplateEditorEvent()
    data class TemplateUpdated(val template: Template) : TemplateEditorEvent()
    data class Error(val message: String) : TemplateEditorEvent()
}

@Suppress("TooManyFunctions", "TooGenericExceptionCaught")
class TemplateEditorViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val settingsStore: SettingsStore,
    private val resourceProvider: ResourceProvider
) : ViewModel() {

    private val templateId: String = checkNotNull(savedStateHandle[KEY_TEMPLATE_ID])

    private val _uiState = MutableStateFlow(TemplateEditorUiState())
    val uiState: StateFlow<TemplateEditorUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<TemplateEditorEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<TemplateEditorEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            val template = if (templateId == NEW_TEMPLATE_ID) {
                Template(
                    id = NEW_TEMPLATE_ID,
                    name = "",
                    content = settingsStore.defaultTemplateContent
                )
            } else {
                settingsStore.getTemplate(templateId)
            }
            val initialContent = savedStateHandle[KEY_CONTENT] ?: template?.content.orEmpty()
            _uiState.value = TemplateEditorUiState(
                template = template,
                isLoading = false,
                name = savedStateHandle[KEY_NAME] ?: template?.name.orEmpty(),
                tags = savedStateHandle.get<List<String>>(KEY_TAGS) ?: template?.tags.orEmpty(),
                newTagInput = savedStateHandle[KEY_NEW_TAG_INPUT] ?: "",
                contentText = initialContent,
                contentError = missingPlaceholdersError(Template.findMissingPlaceholders(initialContent)),
                showDiscardDialog = savedStateHandle[KEY_SHOW_DISCARD_DIALOG] ?: false,
                isSaveCompleted = savedStateHandle[KEY_SAVE_COMPLETED] ?: false
            )
        }
    }

    fun updateName(name: String) {
        val value = name.take(MAX_TEMPLATE_NAME_LENGTH)
        savedStateHandle[KEY_NAME] = value
        _uiState.update { it.copy(name = value, nameError = null) }
    }

    fun updateTags(tags: List<String>) {
        val value = sanitizeTags(tags)
        savedStateHandle[KEY_TAGS] = value
        _uiState.update { it.copy(tags = value) }
    }

    fun updateNewTagInput(newTagInput: String) {
        val value = newTagInput.take(MAX_TAG_LENGTH)
        savedStateHandle[KEY_NEW_TAG_INPUT] = value
        _uiState.update { it.copy(newTagInput = value) }
    }

    fun updateContent(content: String) {
        savedStateHandle[KEY_CONTENT] = content
        _uiState.update {
            it.copy(
                contentText = content,
                contentError = missingPlaceholdersError(Template.findMissingPlaceholders(content))
            )
        }
    }

    fun setNameError(nameError: Int?) {
        _uiState.update { it.copy(nameError = nameError) }
    }

    fun toggleDiscardDialog(show: Boolean = !_uiState.value.showDiscardDialog) {
        savedStateHandle[KEY_SHOW_DISCARD_DIALOG] = show
        _uiState.update { it.copy(showDiscardDialog = show) }
    }

    fun createTemplate(name: String, content: String, tags: List<String>) {
        if (!validateContent(content)) return

        viewModelScope.launch {
            try {
                val template = Template.create(
                    name.trim().take(MAX_TEMPLATE_NAME_LENGTH),
                    content
                ).copy(tags = sanitizeTags(tags))
                settingsStore.saveTemplate(template)
                _events.emit(TemplateEditorEvent.TemplateCreated(template))
                markSaveCompleted()
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to create template", exception)
                emitError(R.string.error_template_create_failed)
            }
        }
    }

    fun updateTemplate(name: String, content: String, tags: List<String>) {
        if (!validateContent(content)) return

        viewModelScope.launch {
            try {
                val existing = settingsStore.getTemplate(templateId) ?: run {
                    emitError(R.string.error_template_update_failed)
                    return@launch
                }
                val updated = existing.copy(
                    name = name.trim().take(MAX_TEMPLATE_NAME_LENGTH),
                    content = Template.normalizeContent(content),
                    tags = sanitizeTags(tags),
                    modifiedAt = Instant.now().toString()
                )
                settingsStore.saveTemplate(updated)
                _events.emit(TemplateEditorEvent.TemplateUpdated(updated))
                markSaveCompleted()
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to update template", exception)
                emitError(R.string.error_template_update_failed)
            }
        }
    }

    private fun validateContent(content: String): Boolean {
        val missingPlaceholders = Template.findMissingPlaceholders(content)
        if (missingPlaceholders.isEmpty()) return true

        val message = resourceProvider.getString(
            R.string.error_template_required_placeholders_missing,
            missingPlaceholders.joinToString(", ")
        )
        _uiState.update { it.copy(contentError = missingPlaceholdersError(missingPlaceholders)) }
        viewModelScope.launch { _events.emit(TemplateEditorEvent.Error(message)) }
        return false
    }

    private fun missingPlaceholdersError(missingPlaceholders: List<String>): String? {
        return missingPlaceholders.takeIf { it.isNotEmpty() }?.joinToString(", ") {
            it.removePrefix("{{ ").removeSuffix(" }}")
        }
    }

    private fun markSaveCompleted() {
        savedStateHandle[KEY_SAVE_COMPLETED] = true
        _uiState.update { it.copy(isSaveCompleted = true) }
    }

    private fun sanitizeTags(tags: List<String>): List<String> {
        val seen = mutableSetOf<String>()
        return tags.map { it.trim() }
            .filter { it.isNotBlank() }
            .map { it.take(MAX_TAG_LENGTH) }
            .filter { seen.add(it.lowercase()) }
    }

    private suspend fun emitError(messageResId: Int) {
        _events.emit(TemplateEditorEvent.Error(resourceProvider.getString(messageResId)))
    }
}
