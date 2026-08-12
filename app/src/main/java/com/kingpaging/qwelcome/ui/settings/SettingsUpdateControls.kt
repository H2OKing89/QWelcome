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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
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
import com.kingpaging.qwelcome.viewmodel.settings.UpdateState

@Composable
internal fun UpdateAvailableControls(
    version: String,
    onRequestDownloadConfirmation: () -> Unit,
    onDismissUpdate: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        NeonButton(
            onClick = onRequestDownloadConfirmation,
            style = NeonButtonStyle.SECONDARY,
        ) {
            Icon(
                Icons.Filled.Download,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(stringResource(R.string.status_update_available, version))
        }
        IconButton(onClick = onDismissUpdate) {
            Icon(
                Icons.Filled.Close,
                contentDescription = stringResource(R.string.content_desc_dismiss_update),
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun UpdateStatusText(
    @androidx.annotation.StringRes textRes: Int,
) {
    Text(
        stringResource(textRes),
        style = MaterialTheme.typography.bodySmall,
    )
}

@Composable
internal fun UpdateStatusText(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall)
}

@Composable
internal fun UpdateReadyToInstall(onRetryInstall: () -> Unit) {
    Text(
        stringResource(R.string.status_ready_to_install),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.primary,
    )
    Spacer(Modifier.height(8.dp))
    NeonButton(
        onClick = onRetryInstall,
        modifier = Modifier.fillMaxWidth(),
        style = NeonButtonStyle.SECONDARY,
    ) {
        Text(stringResource(R.string.action_install_update))
    }
}

@Composable
internal fun UpdatePermissionRequired(
    onOpenInstallSettings: () -> Unit,
    onRetryInstall: () -> Unit,
) {
    Text(
        stringResource(R.string.status_permission_required),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.tertiary,
    )
    Spacer(Modifier.height(8.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        NeonButton(
            onClick = onOpenInstallSettings,
            modifier = Modifier.weight(1f),
            style = NeonButtonStyle.SECONDARY,
        ) {
            Text(stringResource(R.string.action_open_install_settings))
        }
        NeonButton(
            onClick = onRetryInstall,
            modifier = Modifier.weight(1f),
            style = NeonButtonStyle.SECONDARY,
        ) {
            Text(stringResource(R.string.action_retry_install))
        }
    }
}

@Composable
internal fun UpdateCheckForUpdateAction(
    updateState: UpdateState,
    onCheckForUpdate: () -> Unit,
) {
    NeonButton(
        onClick = onCheckForUpdate,
        modifier = Modifier.fillMaxWidth(),
        enabled = !isUpdateFlowBusy(updateState),
        style = NeonButtonStyle.SECONDARY,
    ) {
        Icon(
            Icons.Filled.Refresh,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            when (updateState) {
                is UpdateState.Checking -> stringResource(R.string.status_checking_updates)
                else -> stringResource(R.string.action_check_updates)
            },
        )
    }
}

@Composable
internal fun ProjectPageAction(onOpenProjectPage: () -> Unit) {
    TextButton(
        onClick = onOpenProjectPage,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Icon(
            Icons.Filled.Code,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.action_view_github))
    }
}
