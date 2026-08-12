@file:OptIn(ExperimentalMaterial3Api::class)

package com.kingpaging.qwelcome.ui.templates

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kingpaging.qwelcome.R
import com.kingpaging.qwelcome.data.MessageTemplate
import com.kingpaging.qwelcome.data.Template
import com.kingpaging.qwelcome.ui.components.InteractivePlaceholderChip
import com.kingpaging.qwelcome.ui.components.NeonOutlinedField
import kotlinx.coroutines.launch

internal const val TEMPLATE_VARIABLE_TOOLBAR_TEST_TAG = "template_variable_toolbar"

@Composable
internal fun TemplateNameField(
    name: String,
    nameError: Int?,
    onNameChange: (String) -> Unit,
    onNext: () -> Unit,
) {
    NeonOutlinedField(
        value = name,
        onValueChange = onNameChange,
        label = { Text(stringResource(R.string.label_name)) },
        accentColor = MaterialTheme.colorScheme.secondary,
        isError = nameError != null,
        supportingText = nameError?.let { { Text(stringResource(it)) } },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        keyboardActions = KeyboardActions(onNext = { onNext() }),
        modifier = Modifier.fillMaxWidth(),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Suppress("FunctionNaming", "LongMethod")
@Composable
private fun TagsEditor(
    tags: List<String>,
    newTagInput: String,
    availableSuggestions: List<String>,
    onNewTagInputChange: (String) -> Unit,
    onAddTag: (String) -> Unit,
    onRemoveTag: (String) -> Unit,
    onSuggestionSelected: (String) -> Unit,
) {
    if (tags.isNotEmpty()) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
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
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription =
                                    stringResource(
                                        R.string.content_desc_remove_tag,
                                        tag,
                                    ),
                                modifier = Modifier.size(16.dp),
                            )
                        },
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
        trailingIcon = {
            IconButton(
                onClick = { onAddTag(newTagInput) },
                enabled = newTagInput.isNotBlank(),
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.action_add_tag),
                )
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )

    if (availableSuggestions.isNotEmpty()) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 8.dp),
        ) {
            availableSuggestions.forEach { suggestion ->
                SuggestionChip(
                    onClick = { onSuggestionSelected(suggestion) },
                    label = { Text(suggestion) },
                )
            }
        }
    }
}

@Suppress("FunctionNaming")
@Composable
internal fun TemplateTagsSummary(
    tags: List<String>,
    onOpen: () -> Unit,
) {
    Surface(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = stringResource(R.string.label_tags),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text =
                        if (tags.isEmpty()) {
                            stringResource(R.string.text_no_tags)
                        } else {
                            tags.joinToString(", ")
                        },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = stringResource(R.string.action_manage_tags),
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("FunctionNaming")
@Composable
internal fun TemplateTagsSheet(
    sheetState: SheetState,
    tags: List<String>,
    newTagInput: String,
    availableSuggestions: List<String>,
    onNewTagInputChange: (String) -> Unit,
    onAddTag: (String) -> Unit,
    onRemoveTag: (String) -> Unit,
    onSuggestionSelected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val dismissSheet: () -> Unit = {
        coroutineScope.launch {
            sheetState.hide()
            if (!sheetState.isVisible) {
                onDismiss()
            }
        }
    }

    BackHandler(onBack = dismissSheet)

    ModalBottomSheet(
        onDismissRequest = dismissSheet,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.label_tags),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.secondary,
                )
                IconButton(onClick = dismissSheet) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.content_desc_close_tags),
                    )
                }
            }

            TagsEditor(
                tags = tags,
                newTagInput = newTagInput,
                availableSuggestions = availableSuggestions,
                onNewTagInputChange = onNewTagInputChange,
                onAddTag = onAddTag,
                onRemoveTag = onRemoveTag,
                onSuggestionSelected = onSuggestionSelected,
            )
        }
    }
}

@Suppress("FunctionNaming")
@Composable
internal fun PlaceholderToolbar(onInsertPlaceholder: (String) -> Unit) {
    LazyRow(
        modifier =
            Modifier
                .fillMaxWidth()
                .testTag(TEMPLATE_VARIABLE_TOOLBAR_TEST_TAG),
        contentPadding = PaddingValues(end = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        item(key = "insert_label") {
            Text(
                text = stringResource(R.string.label_insert),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        }

        items(
            items = MessageTemplate.PLACEHOLDERS,
            key = { it.first },
        ) { (placeholder, _) ->
            InteractivePlaceholderChip(
                placeholder = placeholder,
                onClick = { onInsertPlaceholder(placeholder) },
                isRequired = placeholder in Template.REQUIRED_PLACEHOLDERS,
            )
        }
    }
}

@Composable
internal fun MessageContentLauncher(
    contentText: String,
    contentError: String?,
    onOpenEditor: () -> Unit,
) {
    Surface(
        onClick = onOpenEditor,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
        shape = MaterialTheme.shapes.small,
        border =
            BorderStroke(
                width = 1.dp,
                color =
                    if (contentError == null) {
                        MaterialTheme.colorScheme.outlineVariant
                    } else {
                        MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                    },
            ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MessagePreviewHeader()

            Text(
                text = contentText.ifBlank { stringResource(R.string.hint_template_empty_content) },
                style = MaterialTheme.typography.bodyMedium,
                color =
                    if (contentText.isBlank()) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                    },
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )

            if (contentError != null) {
                Text(
                    text =
                        stringResource(
                            R.string.error_template_missing_placeholders,
                            contentError,
                        ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Suppress("FunctionNaming")
@Composable
private fun MessagePreviewHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.label_message),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.action_edit_message),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
