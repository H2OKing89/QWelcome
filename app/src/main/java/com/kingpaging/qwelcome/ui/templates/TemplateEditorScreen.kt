@file:OptIn(ExperimentalMaterial3Api::class)

package com.kingpaging.qwelcome.ui.templates

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.delete
import androidx.compose.foundation.text.input.insert
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.flowWithLifecycle
import com.kingpaging.qwelcome.R
import com.kingpaging.qwelcome.data.MessageTemplate
import com.kingpaging.qwelcome.data.NEW_TEMPLATE_ID
import com.kingpaging.qwelcome.data.Template
import com.kingpaging.qwelcome.di.LocalSoundPlayer
import com.kingpaging.qwelcome.di.LocalTemplateListViewModel
import com.kingpaging.qwelcome.ui.components.CyberpunkBackdrop
import com.kingpaging.qwelcome.ui.components.InteractivePlaceholderChip
import com.kingpaging.qwelcome.ui.components.NeonButton
import com.kingpaging.qwelcome.ui.components.NeonButtonStyle
import com.kingpaging.qwelcome.ui.components.NeonOutlinedField
import com.kingpaging.qwelcome.util.rememberHapticFeedback
import com.kingpaging.qwelcome.viewmodel.templates.TemplateEditorUiState
import com.kingpaging.qwelcome.viewmodel.templates.TemplateListEvent
import kotlinx.coroutines.flow.distinctUntilChanged

@Suppress("LocalContextGetResourceValueCall")
@Composable
fun TemplateEditorScreen(
    onBack: () -> Unit
) {
    val vm = LocalTemplateListViewModel.current
    val soundPlayer = LocalSoundPlayer.current
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val editorUiState by vm.templateEditorUiState.collectAsStateWithLifecycle()
    val template = uiState.editingTemplate

    val hasNavigatedBack = remember { mutableStateOf(false) }
    val safeNavigate: () -> Unit = remember(onBack) {
        {
            if (!hasNavigatedBack.value) {
                hasNavigatedBack.value = true
                onBack()
            }
        }
    }

    if (template == null) {
        LaunchedEffect(Unit) { safeNavigate() }
        return
    }

    LaunchedEffect(lifecycleOwner, vm.events) {
        vm.events
            .flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED)
            .collect { event ->
                when (event) {
                    is TemplateListEvent.TemplateCreated -> {
                        Toast.makeText(
                            context,
                            context.getString(R.string.toast_template_created, event.template.name),
                            Toast.LENGTH_SHORT
                        ).show()
                        safeNavigate()
                    }

                    is TemplateListEvent.TemplateUpdated -> {
                        Toast.makeText(
                            context,
                            context.getString(R.string.toast_template_updated, event.template.name),
                            Toast.LENGTH_SHORT
                        ).show()
                        safeNavigate()
                    }

                    is TemplateListEvent.Error -> {
                        soundPlayer.playBeep()
                        Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
                    }

                    is TemplateListEvent.TemplateDeleted,
                    is TemplateListEvent.TemplateDuplicated,
                    is TemplateListEvent.ActiveTemplateChanged -> Unit
                }
            }
    }

    key(template.id) {
        TemplateEditorContent(
            template = template,
            defaultContent = vm.getDefaultTemplateContent(),
            editorUiState = editorUiState,
            onCreate = vm::createTemplate,
            onUpdate = vm::updateTemplate,
            onCancelEditing = {
                vm.cancelEditing()
                safeNavigate()
            },
            onNameChange = vm::updateName,
            onTagsChange = vm::updateTags,
            onNewTagInputChange = vm::updateNewTagInput,
            onContentChange = vm::updateContent,
            onNameErrorChange = vm::setNameError,
            onToggleDiscardDialog = vm::toggleDiscardDialog
        )
    }
}

private class TemplateEditorState(
    val contentFieldState: TextFieldState
) {
    var pendingPlaceholder by mutableStateOf<String?>(null)
        private set

    var showContentEditorDialog by mutableStateOf(false)
        private set

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

        if (isNew) {
            onCreate(editorUiState.name, editorUiState.contentText, editorUiState.tags)
        } else {
            onUpdate(
                templateId,
                editorUiState.name,
                editorUiState.contentText,
                editorUiState.tags
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

    fun requestPlaceholderInsert(
        placeholder: String,
        onContentChange: (String) -> Unit,
        contentFocusRequester: FocusRequester
    ) {
        if (showContentEditorDialog) {
            insertPlaceholder(placeholder, onContentChange, contentFocusRequester)
        } else {
            pendingPlaceholder = placeholder
            showContentEditorDialog = true
        }
    }

    fun consumePendingPlaceholder(
        onContentChange: (String) -> Unit,
        contentFocusRequester: FocusRequester
    ) {
        val placeholder = pendingPlaceholder
        if (showContentEditorDialog && placeholder != null) {
            insertPlaceholder(placeholder, onContentChange, contentFocusRequester)
            pendingPlaceholder = null
        }
    }
}

@Composable
private fun rememberTemplateEditorState(
    initialContentText: String
): TemplateEditorState {
    val contentFieldState = rememberTextFieldState(initialText = initialContentText)
    return remember { TemplateEditorState(contentFieldState) }
}

@Composable
private fun BindTemplateEditorState(
    state: TemplateEditorState,
    contentText: String,
    onContentChange: (String) -> Unit,
    contentFocusRequester: FocusRequester
) {
    LaunchedEffect(contentText) {
        state.syncContentFieldValue(contentText)
    }
    LaunchedEffect(state.showContentEditorDialog, state.pendingPlaceholder) {
        state.consumePendingPlaceholder(onContentChange, contentFocusRequester)
    }
    LaunchedEffect(state.contentFieldState) {
        snapshotFlow { state.contentFieldState.text.toString() }
            .distinctUntilChanged()
            .collect { onContentChange(it) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TemplateEditorContent(
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
    val haptic = rememberHapticFeedback()

    BindTemplateEditorState(
        state = editorState,
        contentText = editorUiState.contentText,
        onContentChange = onContentChange,
        contentFocusRequester = contentFocusRequester
    )

    val isDirty = editorUiState.name != originalName ||
        editorUiState.contentText != originalContent ||
        editorUiState.tags != originalTags
    val canSave = editorUiState.name.isNotBlank() && editorUiState.contentError == null
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
        val normalized = rawTag.trim().take(32)
        if (normalized.isNotBlank() &&
            editorUiState.tags.none { it.equals(normalized, ignoreCase = true) }
        ) {
            onTagsChange(editorUiState.tags + normalized)
        }
        onNewTagInputChange("")
    }

    BackHandler(enabled = editorState.showContentEditorDialog) { editorState.closeContentEditor() }
    BackHandler(enabled = !editorState.showContentEditorDialog) {
        editorState.dismiss(isDirty, onToggleDiscardDialog, onCancelEditing)
    }

    if (editorUiState.showDiscardDialog) {
        DiscardChangesDialog(
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
                    onBack = {
                        haptic()
                        editorState.dismiss(isDirty, onToggleDiscardDialog, onCancelEditing)
                    }
                )
            },
            bottomBar = {
                TemplateEditorBottomBar(
                    isNew = isNew,
                    canSave = canSave,
                    onCancel = {
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
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 12.dp)
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

                item(key = "tags_section") {
                    TagsSection(
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
                        onSuggestionSelected = addTag
                    )
                }

                item(key = "placeholder_chips") {
                    PlaceholderChipsSection(
                        onInsertPlaceholder = { placeholder ->
                            editorState.requestPlaceholderInsert(
                                placeholder = placeholder,
                                onContentChange = onContentChange,
                                contentFocusRequester = contentFocusRequester
                            )
                        }
                    )
                }

                item(key = "message_launcher") {
                    MessageContentLauncher(
                        contentText = editorUiState.contentText,
                        contentError = editorUiState.contentError,
                        onOpenEditor = {
                            haptic()
                            editorState.openContentEditor()
                        }
                    )
                }
            }
        }

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

@Composable
private fun TemplateEditorTopBar(
    isNew: Boolean,
    onBack: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = if (isNew) {
                    stringResource(R.string.title_create_template)
                } else {
                    stringResource(R.string.title_edit_template)
                },
                color = MaterialTheme.colorScheme.secondary
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.content_desc_back),
                    tint = MaterialTheme.colorScheme.secondary
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
    )
}

@Composable
private fun TemplateEditorBottomBar(
    isNew: Boolean,
    canSave: Boolean,
    onCancel: () -> Unit,
    onSave: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        tonalElevation = 6.dp,
        shadowElevation = 6.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NeonButton(
                onClick = onCancel,
                glowColor = MaterialTheme.colorScheme.secondary,
                style = NeonButtonStyle.TERTIARY
            ) {
                Text(stringResource(R.string.action_cancel))
            }

            Spacer(modifier = Modifier.width(8.dp))

            NeonButton(
                onClick = onSave,
                enabled = canSave,
                glowColor = MaterialTheme.colorScheme.secondary,
                style = NeonButtonStyle.PRIMARY
            ) {
                Text(
                    text = if (isNew) {
                        stringResource(R.string.action_create)
                    } else {
                        stringResource(R.string.action_save)
                    }
                )
            }
        }
    }
}

@Composable
private fun TemplateNameField(
    name: String,
    nameError: Int?,
    onNameChange: (String) -> Unit,
    onNext: () -> Unit
) {
    OutlinedTextField(
        value = name,
        onValueChange = onNameChange,
        label = { Text(stringResource(R.string.label_name)) },
        isError = nameError != null,
        supportingText = nameError?.let { { Text(stringResource(it)) } },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        keyboardActions = KeyboardActions(onNext = { onNext() }),
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.secondary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            cursorColor = MaterialTheme.colorScheme.secondary,
            focusedLabelColor = MaterialTheme.colorScheme.secondary,
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagsSection(
    tags: List<String>,
    newTagInput: String,
    availableSuggestions: List<String>,
    onNewTagInputChange: (String) -> Unit,
    onAddTag: (String) -> Unit,
    onRemoveTag: (String) -> Unit,
    onSuggestionSelected: (String) -> Unit
) {
    Text(
        text = stringResource(R.string.label_tags),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    if (tags.isNotEmpty()) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 8.dp)
        ) {
            tags.forEach { tag ->
                key(tag) {
                    InputChip(
                        selected = true,
                        onClick = { onRemoveTag(tag) },
                        label = {
                            Text(
                                text = tag,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(
                                    R.string.content_desc_remove_tag,
                                    tag
                                ),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }
            }
        }
    }

    NeonOutlinedField(
        value = newTagInput,
        onValueChange = onNewTagInputChange,
        label = { Text(stringResource(R.string.hint_add_tag)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onAddTag(newTagInput) }),
        modifier = Modifier.padding(top = 8.dp)
    )

    if (availableSuggestions.isNotEmpty()) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 8.dp)
        ) {
            availableSuggestions.forEach { suggestion ->
                SuggestionChip(
                    onClick = { onSuggestionSelected(suggestion) },
                    label = { Text(suggestion) }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlaceholderChipsSection(
    onInsertPlaceholder: (String) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = stringResource(R.string.label_insert),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Text(
            text = "·",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        )
        Text(
            text = stringResource(R.string.hint_template_required_placeholders),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f)
        )
    }

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.padding(top = 8.dp)
    ) {
        MessageTemplate.PLACEHOLDERS.forEach { (placeholder, _) ->
            InteractivePlaceholderChip(
                placeholder = placeholder,
                onClick = { onInsertPlaceholder(placeholder) },
                isRequired = placeholder in Template.REQUIRED_PLACEHOLDERS
            )
        }
    }
}

@Composable
private fun MessageContentLauncher(
    contentText: String,
    contentError: String?,
    onOpenEditor: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
        tonalElevation = 2.dp,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.label_message),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                IconButton(onClick = onOpenEditor) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(R.string.action_edit),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            Text(
                text = contentText.ifBlank { " " },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )

            if (contentError != null) {
                Text(
                    text = stringResource(
                        R.string.error_template_missing_placeholders,
                        contentError
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ContentEditorDialog(
    contentState: TextFieldState,
    contentFocusRequester: FocusRequester,
    contentInteractionSource: MutableInteractionSource,
    contentError: String?,
    onDismissRequest: () -> Unit,
    onInsertPlaceholder: (String) -> Unit
) {
    LaunchedEffect(Unit) {
        contentFocusRequester.requestFocus()
    }

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.label_message),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    IconButton(onClick = onDismissRequest) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.action_done)
                        )
                    }
                }

                PlaceholderChipsSection(
                    onInsertPlaceholder = onInsertPlaceholder
                )

                ContentEditorField(
                    contentState = contentState,
                    contentFocusRequester = contentFocusRequester,
                    contentInteractionSource = contentInteractionSource,
                    contentError = contentError,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 260.dp, max = 460.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    NeonButton(
                        onClick = onDismissRequest,
                        glowColor = MaterialTheme.colorScheme.secondary,
                        style = NeonButtonStyle.TERTIARY
                    ) {
                        Text(stringResource(R.string.action_done))
                    }
                }
            }
        }
    }
}

@Composable
private fun ContentEditorField(
    contentState: TextFieldState,
    contentFocusRequester: FocusRequester,
    contentInteractionSource: MutableInteractionSource,
    contentError: String?,
    modifier: Modifier = Modifier
) {
    val hasContentError = contentError != null
    BasicTextField(
        state = contentState,
        lineLimits = TextFieldLineLimits.MultiLine(
            minHeightInLines = 8,
            maxHeightInLines = Int.MAX_VALUE
        ),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 240.dp)
            .focusRequester(contentFocusRequester),
        textStyle = MaterialTheme.typography.bodyMedium.copy(
            color = MaterialTheme.colorScheme.onSurface
        ),
        cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.secondary),
        interactionSource = contentInteractionSource,
        decorator = { innerTextField ->
            OutlinedTextFieldDefaults.DecorationBox(
                value = contentState.text.toString(),
                innerTextField = innerTextField,
                enabled = true,
                singleLine = false,
                visualTransformation = androidx.compose.ui.text.input.VisualTransformation.None,
                interactionSource = contentInteractionSource,
                label = { Text(stringResource(R.string.label_message)) },
                isError = hasContentError,
                supportingText = if (hasContentError) {
                    {
                        Text(
                            text = stringResource(
                                R.string.error_template_missing_placeholders,
                                contentError ?: ""
                            ),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                } else {
                    null
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (hasContentError) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.secondary
                    },
                    unfocusedBorderColor = if (hasContentError) {
                        MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                    } else {
                        MaterialTheme.colorScheme.outline
                    },
                    cursorColor = MaterialTheme.colorScheme.secondary,
                    focusedLabelColor = if (hasContentError) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.secondary
                    },
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                container = {
                    OutlinedTextFieldDefaults.Container(
                        enabled = true,
                        isError = hasContentError,
                        interactionSource = contentInteractionSource,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (hasContentError) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.secondary
                            },
                            unfocusedBorderColor = if (hasContentError) {
                                MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                            } else {
                                MaterialTheme.colorScheme.outline
                            }
                        )
                    )
                }
            )
        }
    )
}

@Composable
private fun DiscardChangesDialog(
    onDiscard: () -> Unit,
    onKeepEditing: () -> Unit
) {
    AlertDialog(
        modifier = Modifier.imePadding(),
        onDismissRequest = onKeepEditing,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = stringResource(R.string.dialog_discard_changes_title),
                color = MaterialTheme.colorScheme.secondary
            )
        },
        text = {
            Text(
                text = stringResource(R.string.text_template_unsaved_changes_warning),
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        confirmButton = {
            NeonButton(
                onClick = onDiscard,
                glowColor = MaterialTheme.colorScheme.error,
                style = NeonButtonStyle.PRIMARY
            ) {
                Text(stringResource(R.string.action_discard))
            }
        },
        dismissButton = {
            NeonButton(
                onClick = onKeepEditing,
                glowColor = MaterialTheme.colorScheme.secondary,
                style = NeonButtonStyle.TERTIARY
            ) {
                Text(stringResource(R.string.action_keep_editing))
            }
        }
    )
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
