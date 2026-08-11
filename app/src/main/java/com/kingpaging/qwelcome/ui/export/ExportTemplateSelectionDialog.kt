@file:Suppress("FunctionNaming")

package com.kingpaging.qwelcome.ui.export

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.kingpaging.qwelcome.R
import com.kingpaging.qwelcome.data.Template
import com.kingpaging.qwelcome.ui.components.NeonButton
import com.kingpaging.qwelcome.ui.components.NeonButtonStyle
import com.kingpaging.qwelcome.ui.theme.LocalDarkTheme
import com.kingpaging.qwelcome.util.rememberHapticFeedback

@Composable
internal fun ExportTemplateSelectionDialog(
    templates: List<Template>,
    selectedIds: Set<String>,
    onToggleTemplate: (String) -> Unit,
    onToggleSelectAll: () -> Unit,
    onDismiss: () -> Unit,
    onExport: () -> Unit
) {
    val isDark = LocalDarkTheme.current
    val allSelected = templates.isNotEmpty() && selectedIds.size == templates.size
    val selectedCount = selectedIds.size

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = if (isDark) 0.dp else 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                TemplateSelectionDialogHeader()
                Spacer(Modifier.height(16.dp))
                SelectAllTemplatesRow(allSelected, onToggleSelectAll)
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                TemplateSelectionList(templates, selectedIds, onToggleTemplate)
                Spacer(Modifier.height(16.dp))
                TemplateSelectionActions(selectedCount, onDismiss, onExport)
            }
        }
    }
}

@Composable
private fun TemplateSelectionDialogHeader() {
    Text(
        text = stringResource(R.string.title_select_templates),
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
private fun SelectAllTemplatesRow(allSelected: Boolean, onToggleSelectAll: () -> Unit) {
    val haptic = rememberHapticFeedback()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = allSelected,
                onValueChange = { haptic(); onToggleSelectAll() },
                role = Role.Checkbox
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = allSelected,
            onCheckedChange = null,
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary,
                uncheckedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.action_select_all),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun TemplateSelectionList(
    templates: List<Template>,
    selectedIds: Set<String>,
    onToggleTemplate: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 300.dp)
    ) {
        items(templates, key = { it.id }) { template ->
            TemplateSelectionItem(
                template = template,
                isSelected = template.id in selectedIds,
                onToggle = { onToggleTemplate(template.id) }
            )
        }
    }
}

@Composable
private fun TemplateSelectionActions(
    selectedCount: Int,
    onDismiss: () -> Unit,
    onExport: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        NeonButton(
            onClick = onDismiss,
            glowColor = MaterialTheme.colorScheme.primary,
            style = NeonButtonStyle.TERTIARY
        ) {
            Text(stringResource(R.string.action_cancel))
        }
        Spacer(Modifier.width(8.dp))
        NeonButton(
            onClick = onExport,
            enabled = selectedCount > 0,
            glowColor = MaterialTheme.colorScheme.primary,
            style = NeonButtonStyle.PRIMARY
        ) {
            val templateCountText = pluralStringResource(
                R.plurals.template_count,
                selectedCount,
                selectedCount
            )
            Text(stringResource(R.string.action_export_count, templateCountText))
        }
    }
}

@Composable
private fun TemplateSelectionItem(
    template: Template,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    val haptic = rememberHapticFeedback()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = isSelected,
                onValueChange = { haptic(); onToggle() },
                role = Role.Checkbox
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isSelected,
            onCheckedChange = null,
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.secondary,
                uncheckedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        )
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = template.name,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = formatTemplatePreview(template.content),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun formatTemplatePreview(content: String, maxChars: Int = 60): String {
    return content.replace("\n", " ").take(maxChars)
}
