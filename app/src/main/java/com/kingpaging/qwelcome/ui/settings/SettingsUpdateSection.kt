@file:Suppress("FunctionNaming")

package com.kingpaging.qwelcome.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.kingpaging.qwelcome.R
import com.kingpaging.qwelcome.ui.components.NeonPanel
import com.kingpaging.qwelcome.ui.theme.LocalCyberColors
import com.kingpaging.qwelcome.viewmodel.settings.UpdateState
import kotlin.math.roundToInt

internal data class UpdateDownloadActions(
    val onRequestDownloadConfirmation: () -> Unit,
    val onDismissUpdate: () -> Unit,
    val onRetryInstall: () -> Unit,
    val onOpenInstallSettings: () -> Unit,
)

internal data class UpdateNavigationActions(
    val onCheckForUpdate: () -> Unit,
    val onOpenProjectPage: () -> Unit,
)

internal const val SETTINGS_UPDATE_PROGRESS_TAG = "settings_update_progress"
private const val BYTES_PER_UNIT = 1024.0
private const val LAST_FILE_SIZE_UNIT_INDEX = 3
private const val MAX_PROGRESS_PERCENT = 100.0

@Composable
internal fun SettingsUpdateSection(
    currentVersion: String,
    updateState: UpdateState,
    downloadActions: UpdateDownloadActions,
    navigationActions: UpdateNavigationActions,
) {
    NeonPanel {
        UpdateVersionHeader(currentVersion)
        Text(
            stringResource(R.string.settings_updates_app_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    NeonPanel {
        Text(
            stringResource(R.string.settings_updates_card_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() },
        )
        UpdateStatusHeader(updateState)
        UpdateStatusContent(updateState, downloadActions)
        UpdateCheckForUpdateAction(updateState, navigationActions.onCheckForUpdate)
    }

    ProjectPageAction(navigationActions.onOpenProjectPage)
}

@Composable
private fun UpdateVersionHeader(currentVersion: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.q_logo),
                contentDescription = null,
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary),
                modifier = Modifier.size(36.dp),
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                stringResource(R.string.label_version_format, currentVersion),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun UpdateStatusHeader(updateState: UpdateState) {
    val statusColor =
        when (updateState) {
            is UpdateState.Error -> MaterialTheme.colorScheme.error
            is UpdateState.UpToDate -> LocalCyberColors.current.success
            else -> MaterialTheme.colorScheme.primary
        }
    val statusIcon =
        when (updateState) {
            is UpdateState.Error, is UpdateState.PermissionRequired -> Icons.Filled.Warning
            is UpdateState.UpToDate, is UpdateState.ReadyToInstall -> Icons.Filled.CheckCircle
            else -> Icons.Filled.Info
        }

    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            statusIcon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = statusColor,
        )
        Text(
            updateStatusTitle(updateState),
            style = MaterialTheme.typography.titleSmall,
            color = statusColor,
            modifier =
                Modifier.weight(1f).semantics {
                    liveRegion = LiveRegionMode.Polite
                },
        )
    }
}

@Composable
private fun updateStatusTitle(state: UpdateState): String =
    when (state) {
        is UpdateState.Idle -> stringResource(R.string.settings_updates_idle_title)
        is UpdateState.Dismissed -> stringResource(R.string.settings_updates_dismissed_title)
        is UpdateState.Checking -> stringResource(R.string.status_checking_updates)
        is UpdateState.UpToDate -> stringResource(R.string.status_up_to_date)
        is UpdateState.Available -> stringResource(R.string.status_update_available, state.version)
        is UpdateState.DownloadQueued -> stringResource(R.string.status_download_queued)
        is UpdateState.Downloading -> stringResource(R.string.status_downloading_unknown)
        is UpdateState.Verifying -> stringResource(R.string.status_verifying_update)
        is UpdateState.ReadyToInstall -> stringResource(R.string.status_ready_to_install)
        is UpdateState.PermissionRequired -> stringResource(R.string.settings_updates_permission_title)
        is UpdateState.Installing -> stringResource(R.string.status_installing)
        is UpdateState.Error -> stringResource(R.string.settings_updates_error_title)
    }

@Composable
private fun UpdateStatusContent(
    updateState: UpdateState,
    actions: UpdateDownloadActions,
) {
    UpdateStatusText(updateStatusDescription(updateState))

    when (updateState) {
        is UpdateState.Available ->
            UpdateAvailableControls(
                version = updateState.version,
                onRequestDownloadConfirmation = actions.onRequestDownloadConfirmation,
                onDismissUpdate = actions.onDismissUpdate,
            )
        is UpdateState.Downloading -> UpdateDownloadProgress(updateState)
        is UpdateState.ReadyToInstall -> UpdateReadyToInstall(actions.onRetryInstall)
        is UpdateState.PermissionRequired ->
            UpdatePermissionRequired(
                onOpenInstallSettings = actions.onOpenInstallSettings,
                onRetryInstall = actions.onRetryInstall,
            )
        is UpdateState.Checking, is UpdateState.DownloadQueued, is UpdateState.Verifying, is UpdateState.Installing ->
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        else -> Unit
    }
}

@Composable
private fun updateStatusDescription(state: UpdateState): String =
    when (state) {
        is UpdateState.Idle -> stringResource(R.string.settings_updates_idle_description)
        is UpdateState.Dismissed -> stringResource(R.string.settings_updates_dismissed_description)
        is UpdateState.Checking -> stringResource(R.string.settings_updates_checking_description)
        is UpdateState.UpToDate -> stringResource(R.string.settings_updates_current_description)
        is UpdateState.Available ->
            stringResource(R.string.settings_updates_available_description, formatBytes(state.assetSizeBytes))
        is UpdateState.DownloadQueued -> stringResource(R.string.settings_updates_queued_description)
        is UpdateState.Downloading -> downloadingStatusText(state)
        is UpdateState.Verifying -> stringResource(R.string.settings_updates_verifying_description)
        is UpdateState.ReadyToInstall -> stringResource(R.string.settings_updates_ready_description)
        is UpdateState.PermissionRequired -> stringResource(R.string.status_permission_required)
        is UpdateState.Installing -> stringResource(R.string.settings_updates_installing_description)
        is UpdateState.Error -> state.message
    }

@Composable
private fun UpdateDownloadProgress(state: UpdateState.Downloading) {
    val progress =
        state.totalBytes?.takeIf { it > 0L }?.let { total ->
            (state.bytesDownloaded.toDouble() / total.toDouble()).coerceIn(0.0, 1.0).toFloat()
        }
    val modifier = Modifier.fillMaxWidth().testTag(SETTINGS_UPDATE_PROGRESS_TAG)

    if (progress != null) {
        LinearProgressIndicator(progress = { progress }, modifier = modifier)
    } else {
        LinearProgressIndicator(modifier = modifier)
    }
}

internal fun isUpdateFlowBusy(state: UpdateState): Boolean = state.isBusy

@Composable
private fun downloadingStatusText(state: UpdateState.Downloading): String {
    val downloaded = formatBytes(state.bytesDownloaded)
    val totalBytes = state.totalBytes
    val total = if (totalBytes != null) formatBytes(totalBytes) else null
    val percent =
        if (state.totalBytes != null && state.totalBytes > 0L) {
            ((state.bytesDownloaded.toDouble() / state.totalBytes.toDouble()) * MAX_PROGRESS_PERCENT)
                .coerceIn(0.0, MAX_PROGRESS_PERCENT)
                .roundToInt()
        } else {
            null
        }

    return if (total != null && percent != null) {
        stringResource(R.string.status_downloading_progress, downloaded, total, percent)
    } else {
        stringResource(R.string.status_downloading_unknown)
    }
}

@Composable
internal fun formatBytes(bytes: Long): String {
    val bytesUnit = stringResource(R.string.unit_bytes)
    if (bytes <= 0L) {
        return stringResource(R.string.format_file_size_zero, bytesUnit)
    }

    var value = bytes.toDouble()
    var unitIndex = 0
    while (value >= BYTES_PER_UNIT && unitIndex < LAST_FILE_SIZE_UNIT_INDEX) {
        value /= BYTES_PER_UNIT
        unitIndex++
    }
    val unit =
        when (unitIndex) {
            0 -> bytesUnit
            1 -> stringResource(R.string.unit_kilobytes)
            2 -> stringResource(R.string.unit_megabytes)
            else -> stringResource(R.string.unit_gigabytes)
        }
    return stringResource(R.string.format_file_size, value, unit)
}
