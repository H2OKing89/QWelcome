@file:Suppress("FunctionNaming")

package com.kingpaging.qwelcome.ui.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.kingpaging.qwelcome.R

@Composable
internal fun SettingsDataManagementSection(
    hasUnsavedChanges: Boolean,
    onOpenExport: () -> Unit,
    onOpenImport: () -> Unit,
) {
    Text(
        text = stringResource(R.string.settings_backup_intro),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    if (hasUnsavedChanges) {
        Surface(
            modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        ) {
            Text(
                text = stringResource(R.string.settings_backup_save_profile_first),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
    SettingsActionRow(
        title = stringResource(R.string.settings_backup_export_title),
        description = stringResource(R.string.settings_backup_export_summary),
        icon = Icons.Default.Backup,
        onClick = onOpenExport,
        enabled = !hasUnsavedChanges,
    )
    SettingsActionRow(
        title = stringResource(R.string.settings_backup_import_title),
        description = stringResource(R.string.settings_backup_import_summary),
        icon = Icons.Default.UploadFile,
        onClick = onOpenImport,
    )
}
