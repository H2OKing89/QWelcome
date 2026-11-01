@file:Suppress("FunctionNaming")

package com.kingpaging.qwelcome.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.kingpaging.qwelcome.R
import com.kingpaging.qwelcome.ui.components.NeonButton
import com.kingpaging.qwelcome.ui.components.NeonOutlinedField

@Composable
internal fun SettingsProfileSection(
    name: String,
    title: String,
    department: String,
    hasUnsavedChanges: Boolean,
    onNameChange: (String) -> Unit,
    onTitleChange: (String) -> Unit,
    onDepartmentChange: (String) -> Unit,
    onSaveProfile: () -> Unit,
    showSaveAction: Boolean = true,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        ProfileEditorIntro()
        SettingsTechProfileFields(name, title, department, onNameChange, onTitleChange, onDepartmentChange)
        ProfileSignatureExample(name, title, department, hasUnsavedChanges)
        if (showSaveAction) {
            SettingsProfileSaveAction(hasUnsavedChanges, onSaveProfile)
        }
    }
}

@Composable
private fun ProfileEditorIntro() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(R.string.settings_profile_editor_heading),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            stringResource(R.string.settings_profile_editor_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
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
    val focusManager = LocalFocusManager.current
    val nextField = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) })
    val nextOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        NeonOutlinedField(
            value = name,
            onValueChange = onNameChange,
            label = { Text(stringResource(R.string.label_tech_name)) },
            keyboardOptions = nextOptions,
            keyboardActions = nextField,
        )
        NeonOutlinedField(
            value = title,
            onValueChange = onTitleChange,
            label = { Text(stringResource(R.string.label_title)) },
            keyboardOptions = nextOptions,
            keyboardActions = nextField,
        )
        NeonOutlinedField(
            value = department,
            onValueChange = onDepartmentChange,
            label = { Text(stringResource(R.string.label_department_line)) },
            keyboardOptions =
                KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done,
                ),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
        )
    }
}

@Composable
private fun ProfileSignatureExample(
    name: String,
    title: String,
    department: String,
    hasUnsavedChanges: Boolean,
) {
    val signature =
        listOf(name, title, department)
            .map(String::trim)
            .filter(String::isNotBlank)
            .joinToString("\n")
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                stringResource(R.string.settings_profile_signature_heading),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                signature.ifEmpty { stringResource(R.string.settings_profile_signature_empty) },
                style = MaterialTheme.typography.bodyLarge,
                color =
                    if (signature.isEmpty()) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
            )
            Text(
                stringResource(
                    if (hasUnsavedChanges) {
                        R.string.settings_profile_signature_draft
                    } else {
                        R.string.settings_profile_signature_saved
                    },
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun SettingsProfileSaveAction(
    hasUnsavedChanges: Boolean,
    onSaveProfile: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (!hasUnsavedChanges) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
            Text(
                stringResource(
                    if (hasUnsavedChanges) {
                        R.string.settings_profile_unsaved_status
                    } else {
                        R.string.settings_profile_saved_status
                    },
                ),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (hasUnsavedChanges) {
            NeonButton(
                onClick = {
                    focusManager.clearFocus()
                    onSaveProfile()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.action_save_profile))
            }
        }
    }
}
