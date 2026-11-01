package com.kingpaging.qwelcome.ui.settings

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.kingpaging.qwelcome.R
import com.kingpaging.qwelcome.ui.components.NeonButton
import com.kingpaging.qwelcome.ui.components.NeonButtonStyle
import com.kingpaging.qwelcome.viewmodel.settings.UpdateState

@Composable
internal fun SettingsDownloadConfirmation(
    available: UpdateState.Available,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.title_update_available)) },
        text = {
            Text(
                stringResource(
                    R.string.text_update_download_confirm,
                    available.version,
                    formatBytes(available.assetSizeBytes),
                ),
            )
        },
        confirmButton = {
            NeonButton(onClick = onConfirm, style = NeonButtonStyle.PRIMARY) {
                Text(stringResource(R.string.action_download_update))
            }
        },
        dismissButton = {
            NeonButton(onClick = onDismiss, style = NeonButtonStyle.TERTIARY) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}
