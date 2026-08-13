@file:Suppress("FunctionNaming")

package com.kingpaging.qwelcome.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kingpaging.qwelcome.R
import com.kingpaging.qwelcome.ui.components.NeonDropdownMenuBox
import com.kingpaging.qwelcome.viewmodel.templates.TemplateSelectionUiState

@Composable
internal fun CustomerIntakeTemplateSelector(
    templateUiState: TemplateSelectionUiState,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onTemplateSelected: (String) -> Unit,
    onManageTemplates: () -> Unit,
) {
    if (templateUiState.templates.isEmpty()) return

    val activeTemplate =
        remember(
            templateUiState.templates,
            templateUiState.activeTemplateId,
        ) {
            templateUiState.templates.find { it.id == templateUiState.activeTemplateId }
        }

    NeonDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { onExpandedChange(it) },
        selectedText = activeTemplate?.name ?: "",
        label = { Text(stringResource(R.string.label_template)) },
        modifier = Modifier.fillMaxWidth(),
    ) {
        templateUiState.templates.forEach { template ->
            key(template.id) {
                DropdownMenuItem(
                    text = {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(template.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (template.id == templateUiState.activeTemplateId) {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = stringResource(R.string.label_active),
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    },
                    onClick = { onTemplateSelected(template.id) },
                )
            }
        }

        HorizontalDivider()

        DropdownMenuItem(
            text = {
                Text(
                    stringResource(R.string.action_manage_templates),
                    color = MaterialTheme.colorScheme.tertiary,
                )
            },
            onClick = onManageTemplates,
        )
    }
}
