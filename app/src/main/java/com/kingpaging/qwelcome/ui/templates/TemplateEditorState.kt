@file:OptIn(ExperimentalMaterial3Api::class)

package com.kingpaging.qwelcome.ui.templates

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.delete
import androidx.compose.foundation.text.input.insert
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
import com.kingpaging.qwelcome.R
import com.kingpaging.qwelcome.data.NEW_TEMPLATE_ID
import com.kingpaging.qwelcome.data.Template
import com.kingpaging.qwelcome.ui.components.CyberpunkBackdrop
import com.kingpaging.qwelcome.ui.components.NeonDiscardDialog
import com.kingpaging.qwelcome.util.rememberHapticFeedback
import com.kingpaging.qwelcome.viewmodel.templates.TemplateEditorUiState
import kotlinx.coroutines.flow.distinctUntilChanged

private const val TAG_MAX_LENGTH = 32

/** Trims and caps a raw tag input to the shared tag length limit. */
internal fun normalizeTemplateTag(rawTag: String): String = rawTag.trim().take(TAG_MAX_LENGTH)

/** Case-insensitive containment check used to prevent duplicate tags. */
internal fun List<String>.containsTagIgnoreCase(tag: String): Boolean =
    any { it.equals(tag, ignoreCase = true) }

/**
 * Normalizes [draftTagInput] and appends it to [existingTags], unless it is blank after
 * normalization or already present (case-insensitively).
 */
internal fun mergeDraftTag(existingTags: List<String>, draftTagInput: String): List<String> {
    val draftTag = normalizeTemplateTag(draftTagInput)
    return if (draftTag.isBlank() || existingTags.containsTagIgnoreCase(draftTag)) {
        existingTags
    } else {
        existingTags + draftTag
    }
}

private class TemplateEditorState(
    val contentFieldState: TextFieldState,
    private val showContentEditorDialogState: MutableState<Boolean>
) {
    var showContentEditorDialog: Boolean
        get() = showContentEditorDialogState.value
        private set(value) {
            showContentEditorDialogState.value = value
        }

    fun openContentEditor() {
        showContentEditorDialog = true
    }

    fun closeContentEditor() {
        showContentEditorDialog = false
    }

    fun dismiss(
        isDirty: Boolean,
        onToggleDiscardDialog: (Boolean) -> Unit,
        onCancelEditing: () -> Unit
    ) {
        if (isDirty) {
            onToggleDiscardDialog(true)
        } else {
            onCancelEditing()
        }
    }

    fun save(
        isNew: Boolean,
        templateId: String,
        editorUiState: TemplateEditorUiState,
        onCreate: (name: String, content: String, tags: List<String>) -> Unit,
        onUpdate: (templateId: String, name: String, content: String, tags: List<String>) -> Unit,
        onNameErrorChange: (Int?) -> Unit
    ) {
        if (editorUiState.name.isBlank()) {
            onNameErrorChange(R.string.error_name_required)
            return
        }

        if (editorUiState.contentError != null) {
            return
        }

        val tags = mergeDraftTag(editorUiState.tags, editorUiState.newTagInput)

        if (isNew) {
            onCreate(editorUiState.name, editorUiState.contentText, tags)
        } else {
            onUpdate(
                templateId,
                editorUiState.name,
                editorUiState.contentText,
                tags
            )
        }
    }

    fun syncContentFieldValue(contentText: String) {
        if (contentText == contentFieldState.text.toString()) return

        val clampedStart = contentFieldState.selection.start.coerceIn(0, contentText.length)
        val clampedEnd = contentFieldState.selection.end.coerceIn(0, contentText.length)
        contentFieldState.edit {
            delete(0, length)
            insert(0, contentText)
            selection = TextRange(clampedStart, clampedEnd)
        }
    }

    fun insertPlaceholder(
        placeholder: String,
        onContentChange: (String) -> Unit,
        contentFocusRequester: FocusRequester
    ) {
        insertAtCursor(contentFieldState, placeholder)
        onContentChange(contentFieldState.text.toString())
        contentFocusRequester.requestFocus()
    }
}

@Composable
private fun rememberTemplateEditorState(
    initialContentText: String
): TemplateEditorState {
    val contentFieldState = rememberTextFieldState(initialText = initialContentText)
    val showContentEditorDialogState = rememberSaveable { mutableStateOf(false) }
    return remember {
        TemplateEditorState(contentFieldState, showContentEditorDialogState)
    }
}

@Composable
private fun BindTemplateEditorState(
    state: TemplateEditorState,
    contentText: String,
    onContentChange: (String) -> Unit
) {
    LaunchedEffect(contentText) {
        state.syncContentFieldValue(contentText)
    }
    LaunchedEffect(state.contentFieldState) {
        snapshotFlow { state.contentFieldState.text.toString() }
            .distinctUntilChanged()
            .collect { onContentChange(it) }
    }
}

@Suppress("FunctionNaming")
@Composable
private fun TemplateTagsSheetHost(
    visible: Boolean,
    editorUiState: TemplateEditorUiState,
    availableSuggestions: List<String>,
    onNewTagInputChange: (String) -> Unit,
    onTagsChange: (List<String>) -> Unit,
    addTag: (String) -> Unit,
    onDismiss: () -> Unit
) {
    if (!visible) return

    TemplateTagsSheet(
        tags = editorUiState.tags,
        newTagInput = editorUiState.newTagInput,
        availableSuggestions = availableSuggestions,
        onNewTagInputChange = onNewTagInputChange,
        onAddTag = addTag,
        onRemoveTag = { tag ->
            onTagsChange(
                editorUiState.tags.filterNot {
                    it.equals(tag, ignoreCase = true)
                }
            )
        },
        onSuggestionSelected = addTag,
        onDismiss = onDismiss
    )
}

@Suppress("FunctionNaming", "LongMethod")
@Composable
internal fun TemplateEditorContent(
    template: Template,
    defaultContent: String,
    editorUiState: TemplateEditorUiState,
    onCreate: (name: String, content: String, tags: List<String>) -> Unit,
    onUpdate: (templateId: String, name: String, content: String, tags: List<String>) -> Unit,
    onCancelEditing: () -> Unit,
    onNameChange: (String) -> Unit,
    onTagsChange: (List<String>) -> Unit,
    onNewTagInputChange: (String) -> Unit,
    onContentChange: (String) -> Unit,
    onNameErrorChange: (Int?) -> Unit,
    onToggleDiscardDialog: (Boolean) -> Unit
) {
    val isNew = template.id == NEW_TEMPLATE_ID
    val originalName = if (isNew) "" else template.name
    val originalContent = if (isNew) defaultContent else template.content
    val originalTags = template.tags

    val editorState = rememberTemplateEditorState(editorUiState.contentText)
    val contentFocusRequester = remember { FocusRequester() }
    val contentInteractionSource = remember { MutableInteractionSource() }
    val showTagsSheet = rememberSaveable { mutableStateOf(false) }
    val haptic = rememberHapticFeedback()

    BindTemplateEditorState(
        state = editorState,
        contentText = editorUiState.contentText,
        onContentChange = onContentChange
    )

    val isDirty = editorUiState.name != originalName ||
        editorUiState.contentText != originalContent ||
        editorUiState.tags != originalTags ||
        editorUiState.newTagInput.isNotBlank()
    val canSave = editorUiState.name.isNotBlank() && editorUiState.contentError == null
    val saveEnabled = canSave && isDirty
    val suggestedTags = listOf(
        stringResource(R.string.tag_residential),
        stringResource(R.string.tag_business),
        stringResource(R.string.tag_install),
        stringResource(R.string.tag_repair),
        stringResource(R.string.tag_troubleshooting)
    )
    val availableSuggestions = suggestedTags.filter { suggestion ->
        editorUiState.tags.none { it.equals(suggestion, ignoreCase = true) }
    }

    val addTag: (String) -> Unit = { rawTag ->
        val normalized = normalizeTemplateTag(rawTag)
        if (normalized.isNotBlank() && !editorUiState.tags.containsTagIgnoreCase(normalized)) {
            onTagsChange(editorUiState.tags + normalized)
        }
        onNewTagInputChange("")
    }

    BackHandler(enabled = editorState.showContentEditorDialog) { editorState.closeContentEditor() }
    BackHandler(enabled = showTagsSheet.value) { showTagsSheet.value = false }
    BackHandler(enabled = !editorState.showContentEditorDialog && !showTagsSheet.value) {
        editorState.dismiss(isDirty, onToggleDiscardDialog, onCancelEditing)
    }

    if (editorUiState.showDiscardDialog) {
        NeonDiscardDialog(
            message = stringResource(R.string.text_template_unsaved_changes_warning),
            onDiscard = {
                onToggleDiscardDialog(false)
                onCancelEditing()
            },
            onKeepEditing = { onToggleDiscardDialog(false) }
        )
    }

    CyberpunkBackdrop {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TemplateEditorTopBar(
                    isNew = isNew,
                    canSave = saveEnabled,
                    onBack = {
                        haptic()
                        editorState.dismiss(isDirty, onToggleDiscardDialog, onCancelEditing)
                    },
                    onSave = {
                        haptic()
                        editorState.save(
                            isNew = isNew,
                            templateId = template.id,
                            editorUiState = editorUiState,
                            onCreate = onCreate,
                            onUpdate = onUpdate,
                            onNameErrorChange = onNameErrorChange
                        )
                    }
                )
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
            ) {
                item(key = "template_name") {
                    TemplateNameField(
                        name = editorUiState.name,
                        nameError = editorUiState.nameError,
                        onNameChange = {
                            if (it.length <= 50) {
                                onNameChange(it)
                                onNameErrorChange(null)
                            }
                        },
                        onNext = { editorState.openContentEditor() }
                    )
                }

                item(key = "message_preview") {
                    MessageContentLauncher(
                        contentText = editorUiState.contentText,
                        contentError = editorUiState.contentError,
                        onOpenEditor = {
                            haptic()
                            editorState.openContentEditor()
                        }
                    )
                }

                item(key = "tags_summary") {
                    TemplateTagsSummary(
                        tags = editorUiState.tags,
                        onOpen = {
                            haptic()
                            showTagsSheet.value = true
                        }
                    )
                }
            }
        }

        TemplateTagsSheetHost(
            visible = showTagsSheet.value,
            editorUiState = editorUiState,
            availableSuggestions = availableSuggestions,
            onNewTagInputChange = onNewTagInputChange,
            onTagsChange = onTagsChange,
            addTag = addTag,
            onDismiss = { showTagsSheet.value = false }
        )

        if (editorState.showContentEditorDialog) {
            ContentEditorDialog(
                contentState = editorState.contentFieldState,
                contentFocusRequester = contentFocusRequester,
                contentInteractionSource = contentInteractionSource,
                contentError = editorUiState.contentError,
                onDismissRequest = editorState::closeContentEditor,
                onInsertPlaceholder = { placeholder ->
                    editorState.insertPlaceholder(
                        placeholder = placeholder,
                        onContentChange = onContentChange,
                        contentFocusRequester = contentFocusRequester
                    )
                }
            )
        }
    }
}

private fun insertAtCursor(state: TextFieldState, textToInsert: String) {
    if (textToInsert.isEmpty()) return
    state.edit {
        val start = minOf(selection.start, selection.end).coerceIn(0, length)
        val end = maxOf(selection.start, selection.end).coerceIn(0, length)
        if (start != end) {
            delete(start, end)
        }
        insert(start, textToInsert)
        selection = TextRange(start + textToInsert.length)
    }
}
