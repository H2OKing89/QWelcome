@file:Suppress("FunctionNaming")

package com.kingpaging.qwelcome.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kingpaging.qwelcome.R
import com.kingpaging.qwelcome.ui.components.NeonButton
import com.kingpaging.qwelcome.ui.components.NeonButtonStyle
import com.kingpaging.qwelcome.ui.components.NeonPanel
import com.kingpaging.qwelcome.viewmodel.settings.UpdateState
import kotlin.math.roundToInt

internal data class UpdateDownloadActions(
    val onRequestDownloadConfirmation: () -> Unit,
    val onDismissUpdate: () -> Unit,
    val onRetryInstall: () -> Unit,
    val onOpenInstallSettings: () -> Unit
)

internal data class UpdateNavigationActions(
    val onCheckForUpdate: () -> Unit,
    val onOpenProjectPage: () -> Unit
)

private const val BYTES_PER_UNIT = 1024.0
private const val LAST_FILE_SIZE_UNIT_INDEX = 3

@Composable
internal fun SettingsUpdateSection(
    currentVersion: String,
    updateState: UpdateState,
    downloadActions: UpdateDownloadActions,
    navigationActions: UpdateNavigationActions
) {
    Text(
        stringResource(R.string.header_about),
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.primary
    )

    NeonPanel {
        UpdateVersionHeader(currentVersion, updateState)

        Spacer(Modifier.height(12.dp))

        UpdateStatusContent(updateState, downloadActions)

        Spacer(Modifier.height(12.dp))

        UpdateCheckForUpdateAction(updateState, navigationActions.onCheckForUpdate)

        Spacer(Modifier.height(8.dp))

        ProjectPageAction(navigationActions.onOpenProjectPage)
    }

    Spacer(Modifier.height(32.dp))
}

@Composable
private fun UpdateVersionHeader(currentVersion: String, updateState: UpdateState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Filled.Info,
                contentDescription = stringResource(R.string.content_desc_version_info),
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(8.dp))
            Text(
                stringResource(R.string.label_version_format, currentVersion),
                style = MaterialTheme.typography.bodyMedium
            )
        }
        UpdateStateIndicator(updateState)
    }
}

@Composable
private fun UpdateStateIndicator(updateState: UpdateState) {
    when (updateState) {
        is UpdateState.Checking -> CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            strokeWidth = 2.dp
        )
        is UpdateState.UpToDate -> UpdateUpToDateIndicator()
        else -> Unit
    }
}

@Composable
private fun UpdateUpToDateIndicator() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Filled.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.width(4.dp))
        Text(
            stringResource(R.string.status_up_to_date),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun UpdateStatusContent(
    updateState: UpdateState,
    actions: UpdateDownloadActions
) {
    when (val state = updateState) {
        is UpdateState.Available -> UpdateAvailableControls(
            version = state.version,
            onRequestDownloadConfirmation = actions.onRequestDownloadConfirmation,
            onDismissUpdate = actions.onDismissUpdate
        )
        is UpdateState.DownloadQueued -> UpdateStatusText(R.string.status_download_queued)
        is UpdateState.Downloading -> UpdateStatusText(downloadingStatusText(state))
        is UpdateState.Verifying -> UpdateStatusText(R.string.status_verifying_update)
        is UpdateState.ReadyToInstall -> UpdateReadyToInstall(actions.onRetryInstall)
        is UpdateState.PermissionRequired -> UpdatePermissionRequired(
            onOpenInstallSettings = actions.onOpenInstallSettings,
            onRetryInstall = actions.onRetryInstall
        )
        is UpdateState.Installing -> UpdateStatusText(R.string.status_installing)
        is UpdateState.Error -> Text(
            state.message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )
        else -> Unit
    }
}

internal fun isUpdateFlowBusy(state: UpdateState): Boolean {
    return state is UpdateState.Checking ||
        state is UpdateState.DownloadQueued ||
        state is UpdateState.Downloading ||
        state is UpdateState.Verifying
}

@Composable
private fun downloadingStatusText(state: UpdateState.Downloading): String {
    val downloaded = formatBytes(state.bytesDownloaded)
    val totalBytes = state.totalBytes
    val total = if (totalBytes != null) formatBytes(totalBytes) else null
    val percent = if (state.totalBytes != null && state.totalBytes > 0L) {
        ((state.bytesDownloaded.toDouble() / state.totalBytes.toDouble()) * 100.0)
            .coerceIn(0.0, 100.0)
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
    val unit = when (unitIndex) {
        0 -> bytesUnit
        1 -> stringResource(R.string.unit_kilobytes)
        2 -> stringResource(R.string.unit_megabytes)
        else -> stringResource(R.string.unit_gigabytes)
    }
    return stringResource(R.string.format_file_size, value, unit)
}
