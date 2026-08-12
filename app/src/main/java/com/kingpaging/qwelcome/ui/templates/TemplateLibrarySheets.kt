@file:Suppress("FunctionNaming")

package com.kingpaging.qwelcome.ui.templates

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.kingpaging.qwelcome.R
import com.kingpaging.qwelcome.data.Template
import com.kingpaging.qwelcome.viewmodel.templates.MAX_TEMPLATE_NAME_LENGTH

private fun String.takeCodePoints(maxCodePoints: Int): String {
    if (codePointCount(0, length) <= maxCodePoints) return this
    return substring(0, offsetByCodePoints(0, maxCodePoints))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TemplateTagFilterSheet(
    allTags: Set<String>,
    selectedTags: Set<String>,
    onToggle: (String) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 12.dp),
        ) {
            SheetHeader(
                title = stringResource(R.string.title_filter_tags),
                action = {
                    TextButton(onClick = onClear, enabled = selectedTags.isNotEmpty()) {
                        Text(stringResource(R.string.action_clear))
                    }
                },
                onDismiss = onDismiss,
            )
            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = 480.dp),
            ) {
                item(key = "all_tags") {
                    TagFilterRow(
                        label = stringResource(R.string.label_all_tags),
                        selected = selectedTags.isEmpty(),
                        onSelect = onClear,
                    )
                }
                items(allTags.sorted(), key = { it }) { tag ->
                    TagFilterRow(
                        label = tag,
                        selected = tag in selectedTags,
                        onSelect = { onToggle(tag) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongMethod")
@Composable
internal fun TemplatePreviewSheet(
    template: Template,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val missingPlaceholders =
        remember(template.content) {
            Template.findMissingPlaceholders(template.content)
        }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp),
        ) {
            SheetHeader(
                title = template.name,
                onDismiss = onDismiss,
            )
            Text(
                text =
                    template.tags
                        .ifEmpty { listOf(stringResource(R.string.text_no_tags)) }
                        .joinToString(" • "),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
            if (missingPlaceholders.isNotEmpty()) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text =
                            stringResource(
                                R.string.error_template_missing_placeholders,
                                missingPlaceholders.joinToString(", "),
                            ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp))
            SelectionContainer {
                Text(
                    text = template.content,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
            }
            TextButton(
                onClick = onDismiss,
                modifier =
                    Modifier
                        .align(Alignment.End)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text(stringResource(R.string.action_close))
            }
        }
    }
}

@Composable
internal fun RenameTemplateDialog(
    template: Template,
    onRename: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable(template.id) { mutableStateOf(template.name) }
    val trimmedName = name.trim()
    val canRename = trimmedName.isNotEmpty() && trimmedName != template.name
    val nameCodePointCount = name.codePointCount(0, name.length)
    val shouldShowSupportingText =
        name.isBlank() ||
            trimmedName == template.name ||
            nameCodePointCount >= MAX_TEMPLATE_NAME_LENGTH
    val nameDescription = stringResource(R.string.content_desc_rename_template_name)
    val submit = {
        if (canRename) {
            onRename(trimmedName)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.imePadding(),
        properties = DialogProperties(decorFitsSystemWindows = false),
        title = { Text(stringResource(R.string.title_rename_template)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.takeCodePoints(MAX_TEMPLATE_NAME_LENGTH) },
                label = { Text(stringResource(R.string.label_name)) },
                singleLine = true,
                isError = name.isBlank(),
                supportingText =
                    if (shouldShowSupportingText) {
                        {
                            RenameSupportingText(
                                nameIsBlank = name.isBlank(),
                                nameIsUnchanged = trimmedName == template.name,
                                nameCodePointCount = nameCodePointCount,
                            )
                        }
                    } else {
                        null
                    },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = nameDescription },
            )
        },
        confirmButton = {
            TextButton(onClick = submit, enabled = canRename) {
                Text(stringResource(R.string.action_rename_template))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

@Composable
private fun RenameSupportingText(
    nameIsBlank: Boolean,
    nameIsUnchanged: Boolean,
    nameCodePointCount: Int,
) {
    when {
        nameIsBlank -> Text(stringResource(R.string.error_name_required))
        else ->
            Column {
                if (nameIsUnchanged) {
                    Text(stringResource(R.string.hint_name_unchanged))
                }
                if (nameCodePointCount >= MAX_TEMPLATE_NAME_LENGTH) {
                    Text(
                        stringResource(
                            R.string.text_template_name_character_count,
                            nameCodePointCount,
                            MAX_TEMPLATE_NAME_LENGTH,
                        ),
                    )
                }
            }
    }
}

@Composable
private fun SheetHeader(
    title: String,
    onDismiss: () -> Unit,
    action: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        action?.invoke()
        IconButton(onClick = onDismiss) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(R.string.action_close),
            )
        }
    }
}

@Composable
private fun TagFilterRow(
    label: String,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(52.dp)
                .toggleable(
                    value = selected,
                    role = Role.Checkbox,
                    onValueChange = { onSelect() },
                ).padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = selected, onCheckedChange = null)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
