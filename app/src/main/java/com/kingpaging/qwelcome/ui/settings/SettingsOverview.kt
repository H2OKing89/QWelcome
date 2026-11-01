@file:Suppress("FunctionNaming")

package com.kingpaging.qwelcome.ui.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.kingpaging.qwelcome.R
import com.kingpaging.qwelcome.data.TechProfile
import com.kingpaging.qwelcome.viewmodel.settings.UpdateState

@Composable
internal fun SettingsOverview(
    uiState: SettingsUiState,
    onOpenSection: (SettingsSection) -> Unit,
    onOpenTemplates: () -> Unit,
) {
    SettingsProfileSummary(uiState.profile, onClick = { onOpenSection(SettingsSection.PROFILE) })
    SettingsOverviewHeading(R.string.settings_workspace_heading)
    SettingsActionRow(
        title = stringResource(R.string.settings_templates_title),
        description = stringResource(R.string.text_currently_using_template, uiState.activeTemplate.name),
        icon = Icons.Filled.Description,
        onClick = onOpenTemplates,
    )
    SettingsActionRow(
        title = stringResource(R.string.settings_backup_title),
        description = stringResource(R.string.settings_backup_summary),
        icon = Icons.Filled.Backup,
        onClick = { onOpenSection(SettingsSection.BACKUP) },
    )
    SettingsOverviewHeading(R.string.settings_app_heading)
    SettingsActionRow(
        title = stringResource(R.string.settings_privacy_title),
        description = stringResource(R.string.settings_privacy_summary),
        icon = Icons.Filled.Security,
        onClick = { onOpenSection(SettingsSection.PRIVACY) },
    )
    SettingsActionRow(
        title = stringResource(R.string.settings_updates_title),
        description = settingsUpdateSummary(uiState.updateState, uiState.currentVersion),
        icon = Icons.Filled.SystemUpdate,
        onClick = { onOpenSection(SettingsSection.ABOUT) },
    )
}

@Composable
private fun SettingsProfileSummary(
    profile: TechProfile,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val details = listOf(profile.title, profile.dept).filter(String::isNotBlank).joinToString(" · ")
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = colors.primaryContainer,
        contentColor = colors.onPrimaryContainer,
        shape = RoundedCornerShape(20.dp),
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Person, contentDescription = null, modifier = Modifier.size(32.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(stringResource(R.string.settings_profile_title), style = MaterialTheme.typography.labelMedium)
                Text(
                    profile.name.ifBlank { stringResource(R.string.settings_profile_setup) },
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    details.ifBlank { stringResource(R.string.settings_profile_setup_hint) },
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(stringResource(R.string.settings_profile_edit), style = MaterialTheme.typography.labelLarge)
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
        }
    }
}

@Composable
private fun SettingsOverviewHeading(
    @StringRes titleResource: Int,
) {
    Text(
        text = stringResource(titleResource),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp).semantics { heading() },
    )
}

@Composable
private fun settingsUpdateSummary(
    state: UpdateState,
    currentVersion: String,
): String =
    when (state) {
        is UpdateState.Available -> stringResource(R.string.settings_update_available_summary, state.version)
        is UpdateState.Checking -> stringResource(R.string.status_checking_updates)
        is UpdateState.DownloadQueued -> stringResource(R.string.status_download_queued)
        is UpdateState.Downloading -> stringResource(R.string.status_downloading_unknown)
        is UpdateState.Verifying -> stringResource(R.string.status_verifying_update)
        is UpdateState.Installing -> stringResource(R.string.status_installing)
        is UpdateState.ReadyToInstall, is UpdateState.PermissionRequired ->
            stringResource(R.string.settings_update_ready_summary)
        is UpdateState.Error -> stringResource(R.string.settings_update_error_summary)
        else -> stringResource(R.string.label_version_format, currentVersion)
    }
