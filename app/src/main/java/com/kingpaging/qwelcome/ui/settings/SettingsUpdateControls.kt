@file:Suppress("FunctionNaming")

package com.kingpaging.qwelcome.ui.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
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
    val availability = stringResource(R.string.status_update_available, version)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        NeonButton(
            onClick = onRequestDownloadConfirmation,
            modifier = Modifier.fillMaxWidth().semantics { stateDescription = availability },
            style = NeonButtonStyle.PRIMARY,
        ) {
            Icon(
                Icons.Filled.Download,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.action_download_update))
        }
        TextButton(
            onClick = onDismissUpdate,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                Icons.Filled.Close,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.settings_updates_dismiss_action))
        }
    }
}

@Composable
internal fun UpdateStatusText(
    @StringRes textRes: Int,
) {
    UpdateStatusText(stringResource(textRes))
}

@Composable
internal fun UpdateStatusText(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
internal fun UpdateReadyToInstall(onRetryInstall: () -> Unit) {
    NeonButton(
        onClick = onRetryInstall,
        modifier = Modifier.fillMaxWidth(),
        style = NeonButtonStyle.PRIMARY,
    ) {
        Text(stringResource(R.string.action_install_update))
    }
}

@Composable
internal fun UpdatePermissionRequired(
    onOpenInstallSettings: () -> Unit,
    onRetryInstall: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        NeonButton(
            onClick = onOpenInstallSettings,
            modifier = Modifier.fillMaxWidth(),
            style = NeonButtonStyle.PRIMARY,
        ) {
            Text(stringResource(R.string.action_open_install_settings))
        }
        NeonButton(
            onClick = onRetryInstall,
            modifier = Modifier.fillMaxWidth(),
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
        Text(stringResource(R.string.action_check_updates))
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
