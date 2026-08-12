@file:Suppress("FunctionNaming")

package com.kingpaging.qwelcome.ui.settings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kingpaging.qwelcome.R
import com.kingpaging.qwelcome.ui.components.NeonButton
import com.kingpaging.qwelcome.ui.components.NeonButtonStyle
import com.kingpaging.qwelcome.ui.components.NeonMagentaButton
import com.kingpaging.qwelcome.ui.components.NeonOutlinedField
import com.kingpaging.qwelcome.ui.components.NeonPanel

@Composable
internal fun SettingsProfileSection(
    activeTemplateName: String,
    name: String,
    title: String,
    department: String,
    hasUnsavedChanges: Boolean,
    onNameChange: (String) -> Unit,
    onTitleChange: (String) -> Unit,
    onDepartmentChange: (String) -> Unit,
    onOpenTemplates: () -> Unit,
    onSaveProfile: () -> Unit,
) {
    SettingsTechProfileFields(
        name = name,
        title = title,
        department = department,
        onNameChange = onNameChange,
        onTitleChange = onTitleChange,
        onDepartmentChange = onDepartmentChange,
    )

    Spacer(Modifier.height(8.dp))

    SettingsTemplateSummary(
        activeTemplateName = activeTemplateName,
        onOpenTemplates = onOpenTemplates,
    )

    Spacer(Modifier.height(8.dp))

    SettingsSaveProfileButton(
        hasUnsavedChanges = hasUnsavedChanges,
        onSaveProfile = onSaveProfile,
    )

    Spacer(Modifier.height(16.dp))
}

@Composable
private fun SettingsTechProfileFields(
    name: String,
    title: String,
    department: String,
    onNameChange: (String) -> Unit,
    onTitleChange: (String) -> Unit,
    onDepartmentChange: (String) -> Unit,
) {
    Text(
        stringResource(R.string.header_tech_profile),
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.primary,
    )
    NeonPanel {
        NeonOutlinedField(
            value = name,
            onValueChange = onNameChange,
            label = { Text(stringResource(R.string.label_tech_name)) },
        )
        NeonOutlinedField(
            value = title,
            onValueChange = onTitleChange,
            label = { Text(stringResource(R.string.label_title)) },
        )
        NeonOutlinedField(
            value = department,
            onValueChange = onDepartmentChange,
            label = { Text(stringResource(R.string.label_department_line)) },
        )
    }
}

@Composable
private fun SettingsTemplateSummary(
    activeTemplateName: String,
    onOpenTemplates: () -> Unit,
) {
    Text(
        stringResource(R.string.header_message_templates),
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.primary,
    )
    NeonPanel {
        Text(
            stringResource(R.string.text_manage_templates_description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(R.string.text_currently_using_template, activeTemplateName),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(12.dp))
        NeonButton(
            onClick = onOpenTemplates,
            modifier = Modifier.fillMaxWidth(),
            style = NeonButtonStyle.SECONDARY,
        ) {
            Text(stringResource(R.string.action_manage_templates_plain))
        }
    }
}

@Composable
private fun SettingsSaveProfileButton(
    hasUnsavedChanges: Boolean,
    onSaveProfile: () -> Unit,
) {
    NeonMagentaButton(
        onClick = onSaveProfile,
        modifier = Modifier.fillMaxWidth(),
        enabled = hasUnsavedChanges,
        style = NeonButtonStyle.PRIMARY,
    ) {
        Text(
            if (hasUnsavedChanges) {
                stringResource(R.string.action_save_profile)
            } else {
                stringResource(R.string.label_no_changes)
            },
        )
    }
}
