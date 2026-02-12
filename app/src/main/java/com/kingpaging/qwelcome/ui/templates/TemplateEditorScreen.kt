@file:OptIn(ExperimentalMaterial3Api::class)

package com.kingpaging.qwelcome.ui.templates

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kingpaging.qwelcome.R
import com.kingpaging.qwelcome.data.MessageTemplate
import com.kingpaging.qwelcome.data.Template
import com.kingpaging.qwelcome.di.LocalSoundPlayer
import com.kingpaging.qwelcome.di.LocalTemplateListViewModel
import com.kingpaging.qwelcome.ui.components.CyberpunkBackdrop
import com.kingpaging.qwelcome.ui.components.InteractivePlaceholderChip
import com.kingpaging.qwelcome.ui.components.NeonButton
import com.kingpaging.qwelcome.ui.components.NeonButtonStyle
import com.kingpaging.qwelcome.ui.components.NeonOutlinedField
import com.kingpaging.qwelcome.util.rememberHapticFeedback
import com.kingpaging.qwelcome.viewmodel.templates.TemplateListEvent
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop

@Suppress("LocalContextGetResourceValueCall")
@OptIn(FlowPreview::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun TemplateEditorScreen(
    onBack: () -> Unit
) {
    val vm = LocalTemplateListViewModel.current
    val soundPlayer = LocalSoundPlayer.current
    val context = LocalContext.current
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val template = uiState.editingTemplate

    if (template == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }

    LaunchedEffect(Unit) {
        vm.events.collect { event ->
            when (event) {
                is TemplateListEvent.TemplateCreated -> {
                    Toast.makeText(
                        context,
                        context.getString(R.string.toast_template_created, event.template.name),
                        Toast.LENGTH_SHORT
                    ).show()
                    onBack()
                }

                is TemplateListEvent.TemplateUpdated -> {
                    Toast.makeText(
                        context,
                        context.getString(R.string.toast_template_updated, event.template.name),
                        Toast.LENGTH_SHORT
                    ).show()
                    onBack()
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
        val isNew = template.id == NEW_TEMPLATE_ID
        val defaultContent = remember { vm.getDefaultTemplateContent() }
        val originalName = remember { if (isNew) "" else template.name }
        val originalContent = remember { if (isNew) defaultContent else template.content }
        val originalTags = remember { template.tags }

        var name by remember { mutableStateOf(originalName) }
        var tags by remember { mutableStateOf(originalTags) }
        var newTagInput by remember { mutableStateOf("") }
        var nameError by remember { mutableStateOf<Int?>(null) }
        var contentError by remember { mutableStateOf<String?>(null) }
        var showDiscardDialog by remember { mutableStateOf(false) }

        val contentState = rememberTextFieldState(initialText = originalContent)
        val contentFocusRequester = remember { FocusRequester() }
        val haptic = rememberHapticFeedback()

        val currentContent = contentState.text.toString()
        val isDirty = name != originalName || currentContent != originalContent || tags != originalTags
        val suggestedTags = listOf(
            stringResource(R.string.tag_residential),
            stringResource(R.string.tag_business),
            stringResource(R.string.tag_install),
            stringResource(R.string.tag_repair),
            stringResource(R.string.tag_troubleshooting)
        )

        val addTag: (String) -> Unit = { rawTag ->
            val normalized = rawTag.trim().take(32)
            if (normalized.isNotBlank() && tags.none { it.equals(normalized, ignoreCase = true) }) {
                tags = tags + normalized
            }
            newTagInput = ""
        }

        val handleDismiss: () -> Unit = {
            if (isDirty) {
                showDiscardDialog = true
            } else {
                vm.cancelEditing()
                onBack()
            }
        }

        BackHandler { handleDismiss() }

        LaunchedEffect(Unit) {
            snapshotFlow { contentState.text.toString() }
                .drop(1)
                .debounce(150)
                .collect { text ->
                    val missing = Template.findMissingPlaceholders(text)
                    contentError = if (missing.isNotEmpty()) {
                        missing.joinToString(", ") {
                            it.removePrefix("{{ ").removeSuffix(" }}")
                        }
                    } else {
                        null
                    }
                }
        }

        if (showDiscardDialog) {
            AlertDialog(
                onDismissRequest = { showDiscardDialog = false },
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
                        onClick = {
                            showDiscardDialog = false
                            vm.cancelEditing()
                            onBack()
                        },
                        glowColor = MaterialTheme.colorScheme.error,
                        style = NeonButtonStyle.PRIMARY
                    ) {
                        Text(stringResource(R.string.action_discard))
                    }
                },
                dismissButton = {
                    NeonButton(
                        onClick = { showDiscardDialog = false },
                        glowColor = MaterialTheme.colorScheme.secondary,
                        style = NeonButtonStyle.TERTIARY
                    ) {
                        Text(stringResource(R.string.action_keep_editing))
                    }
                }
            )
        }

        val canSave = name.isNotBlank() && contentError == null
        val contentInteractionSource = remember { MutableInteractionSource() }

        CyberpunkBackdrop {
            Scaffold(
                containerColor = Color.Transparent,
                topBar = {
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
                            IconButton(onClick = {
                                haptic()
                                handleDismiss()
                            }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.content_desc_back),
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                    )
                },
                bottomBar = {
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
                                onClick = {
                                    haptic()
                                    handleDismiss()
                                },
                                glowColor = MaterialTheme.colorScheme.secondary,
                                style = NeonButtonStyle.TERTIARY
                            ) {
                                Text(stringResource(R.string.action_cancel))
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            NeonButton(
                                onClick = {
                                    haptic()
                                    if (name.isBlank()) {
                                        nameError = R.string.error_name_required
                                    } else if (contentError == null) {
                                        if (isNew) {
                                            vm.createTemplate(name, contentState.text.toString(), tags)
                                        } else {
                                            vm.updateTemplate(template.id, name, contentState.text.toString(), tags)
                                        }
                                    }
                                },
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
                        OutlinedTextField(
                            value = name,
                            onValueChange = { newValue ->
                                if (newValue.length <= 50) {
                                    name = newValue
                                    nameError = null
                                }
                            },
                            label = { Text(stringResource(R.string.label_name)) },
                            isError = nameError != null,
                            supportingText = nameError?.let { { Text(stringResource(it)) } },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { contentFocusRequester.requestFocus() }),
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

                    item(key = "tags_label") {
                        Text(
                            text = stringResource(R.string.label_tags),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (tags.isNotEmpty()) {
                        item(key = "selected_tags") {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                tags.forEach { tag ->
                                    InputChip(
                                        selected = true,
                                        onClick = { tags = tags.filterNot { it.equals(tag, ignoreCase = true) } },
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

                    item(key = "add_tag") {
                        NeonOutlinedField(
                            value = newTagInput,
                            onValueChange = { newTagInput = it },
                            label = { Text(stringResource(R.string.hint_add_tag)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { addTag(newTagInput) })
                        )
                    }

                    val availableSuggestions = suggestedTags.filter { suggestion ->
                        tags.none { it.equals(suggestion, ignoreCase = true) }
                    }
                    if (availableSuggestions.isNotEmpty()) {
                        item(key = "suggested_tags") {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                availableSuggestions.forEach { suggestion ->
                                    SuggestionChip(
                                        onClick = { addTag(suggestion) },
                                        label = { Text(suggestion) }
                                    )
                                }
                            }
                        }
                    }

                    item(key = "placeholder_legend") {
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
                    }

                    item(key = "placeholder_chips") {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            MessageTemplate.PLACEHOLDERS.forEach { (placeholder, _) ->
                                InteractivePlaceholderChip(
                                    placeholder = placeholder,
                                    onClick = {
                                        insertAtCursor(contentState, placeholder)
                                        contentFocusRequester.requestFocus()
                                    },
                                    isRequired = placeholder in Template.REQUIRED_PLACEHOLDERS
                                )
                            }
                        }
                    }

                    item(key = "template_content") {
                        val hasContentError = contentError != null
                        BasicTextField(
                            state = contentState,
                            lineLimits = TextFieldLineLimits.MultiLine(
                                minHeightInLines = 8,
                                maxHeightInLines = Int.MAX_VALUE
                            ),
                            modifier = Modifier
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
                }
            }
        }
    }
}

private fun insertAtCursor(state: TextFieldState, textToInsert: String) {
    if (textToInsert.isEmpty()) return
    state.edit {
        val selection = this.selection
        if (selection.start != selection.end) {
            delete(selection.start, selection.end)
        }
        insert(selection.start, textToInsert)
        placeCursorAfterCharAt(selection.start + textToInsert.length - 1)
    }
}
