@file:Suppress("FunctionNaming", "PackageNaming")

package com.kingpaging.qwelcome.ui.import_pkg

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kingpaging.qwelcome.R
import com.kingpaging.qwelcome.data.ImportValidationResult
import com.kingpaging.qwelcome.ui.components.NeonButton
import com.kingpaging.qwelcome.ui.components.NeonButtonStyle
import com.kingpaging.qwelcome.ui.components.NeonPanel
import com.kingpaging.qwelcome.ui.theme.LocalCyberColors

private const val CONFIRMATION_BUTTON_WIDTH_FRACTION = 0.9f

@Composable
internal fun ImportConfirmStep(
    isLoading: Boolean,
    validationResult: ImportValidationResult,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(stringResource(R.string.title_ready_to_import), style = MaterialTheme.typography.titleLarge)
        ImportValidationSummary(validationResult)
        Spacer(Modifier.height(16.dp))
        ImportConfirmationActions(isLoading, onConfirm, onCancel)
    }
}

@Composable
private fun ImportValidationSummary(validationResult: ImportValidationResult) {
    NeonPanel {
        Text(
            stringResource(R.string.text_import_overwrite_warning),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        )
        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        ImportValidationDetails(validationResult)
    }
}

@Composable
private fun ImportValidationDetails(validationResult: ImportValidationResult) {
    when (validationResult) {
        is ImportValidationResult.ValidTemplatePack -> ImportTemplatePackDetails(validationResult)
        is ImportValidationResult.ValidFullBackup -> ImportFullBackupDetails(validationResult)
        is ImportValidationResult.Invalid ->
            Text(
                stringResource(R.string.error_invalid_import_data),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
    }
}

@Composable
private fun ImportTemplatePackDetails(result: ImportValidationResult.ValidTemplatePack) {
    InfoRow(stringResource(R.string.label_backup_type), stringResource(R.string.export_type_template_pack))
    InfoRow(
        stringResource(R.string.label_template_count),
        result.pack.templates.size
            .toString(),
    )
    InfoRow(stringResource(R.string.label_tech_profile), stringResource(R.string.label_not_included))
    ImportConflictInfo(result.hasConflicts, result.conflicts.size)
}

@Composable
private fun ImportFullBackupDetails(result: ImportValidationResult.ValidFullBackup) {
    InfoRow(stringResource(R.string.label_backup_type), stringResource(R.string.export_type_full_backup))
    InfoRow(
        stringResource(R.string.label_template_count),
        result.backup.templates.size
            .toString(),
    )
    InfoRow(stringResource(R.string.label_tech_profile), stringResource(R.string.label_included))
    ImportConflictInfo(result.hasConflicts, result.conflicts.size)
}

@Composable
private fun ImportConflictInfo(
    hasConflicts: Boolean,
    count: Int,
) {
    if (hasConflicts) {
        InfoRow(
            stringResource(R.string.label_conflicts),
            pluralStringResource(R.plurals.import_conflicts, count, count),
        )
    }
}

@Composable
private fun ImportConfirmationActions(
    isLoading: Boolean,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    if (isLoading) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
        Text(
            stringResource(R.string.status_importing),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.padding(top = 8.dp),
        )
    } else {
        ImportConfirmationButtons(onConfirm, onCancel)
    }
}

@Composable
private fun ImportConfirmationButtons(
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth(CONFIRMATION_BUTTON_WIDTH_FRACTION),
    ) {
        NeonButton(
            onClick = onCancel,
            modifier = Modifier.weight(1f),
            style = NeonButtonStyle.TERTIARY,
            glowColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        ) {
            Text(stringResource(R.string.action_cancel))
        }
        NeonButton(
            onClick = onConfirm,
            modifier = Modifier.weight(1f),
            glowColor = LocalCyberColors.current.success,
        ) {
            Icon(
                Icons.Default.Check,
                contentDescription = stringResource(R.string.content_desc_confirm_import),
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.action_confirm))
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
