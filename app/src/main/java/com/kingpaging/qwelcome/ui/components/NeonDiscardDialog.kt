package com.kingpaging.qwelcome.ui.components

import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.kingpaging.qwelcome.R

@Composable
fun NeonDiscardDialog(
    message: String,
    onDiscard: () -> Unit,
    onKeepEditing: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        modifier = modifier.imePadding(),
        onDismissRequest = onKeepEditing,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = stringResource(R.string.dialog_discard_changes_title),
                color = MaterialTheme.colorScheme.secondary,
            )
        },
        text = {
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        confirmButton = {
            NeonButton(
                onClick = onDiscard,
                glowColor = MaterialTheme.colorScheme.error,
                style = NeonButtonStyle.PRIMARY,
            ) {
                Text(stringResource(R.string.action_discard))
            }
        },
        dismissButton = {
            NeonButton(
                onClick = onKeepEditing,
                glowColor = MaterialTheme.colorScheme.secondary,
                style = NeonButtonStyle.TERTIARY,
            ) {
                Text(stringResource(R.string.action_keep_editing))
            }
        },
    )
}
