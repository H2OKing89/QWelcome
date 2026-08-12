@file:Suppress("FunctionNaming", "PackageNaming")

package com.kingpaging.qwelcome.ui.import_pkg

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.FileCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kingpaging.qwelcome.R
import com.kingpaging.qwelcome.ui.components.NeonButton
import com.kingpaging.qwelcome.ui.components.NeonButtonStyle
import com.kingpaging.qwelcome.ui.components.NeonPanel

private const val SOURCE_BUTTON_WIDTH_FRACTION = 0.8f

@Composable
internal fun ImportIdleStep(
    isLoading: Boolean,
    error: String?,
    onOpenFile: () -> Unit,
    onPaste: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        ImportIdleIntro()
        Spacer(Modifier.height(16.dp))
        ImportSourceActions(isLoading, onOpenFile, onPaste)
        ImportIdleError(error)
    }
}

@Composable
private fun ImportIdleIntro() {
    Icon(
        imageVector = Icons.Default.UploadFile,
        contentDescription = stringResource(R.string.content_desc_import_data),
        modifier = Modifier.size(64.dp),
        tint = MaterialTheme.colorScheme.secondary,
    )
    Text(
        stringResource(R.string.title_import_intro),
        style = MaterialTheme.typography.titleLarge,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurface,
    )
    Text(
        stringResource(R.string.text_import_intro_description),
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
    )
}

@Composable
private fun ImportSourceActions(
    isLoading: Boolean,
    onOpenFile: () -> Unit,
    onPaste: () -> Unit,
) {
    if (isLoading) {
        ImportDecodingIndicator()
    } else {
        ImportSourceButtons(onOpenFile, onPaste)
    }
}

@Composable
private fun ImportDecodingIndicator() {
    CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
    Text(
        stringResource(R.string.status_decoding),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
private fun ImportSourceButtons(
    onOpenFile: () -> Unit,
    onPaste: () -> Unit,
) {
    NeonButton(
        onClick = onOpenFile,
        modifier = Modifier.fillMaxWidth(SOURCE_BUTTON_WIDTH_FRACTION),
        glowColor = MaterialTheme.colorScheme.secondary,
    ) {
        Icon(
            Icons.Default.UploadFile,
            contentDescription = stringResource(R.string.content_desc_select_file_to_import),
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.action_select_file))
    }

    NeonButton(
        onClick = onPaste,
        modifier = Modifier.fillMaxWidth(SOURCE_BUTTON_WIDTH_FRACTION),
        style = NeonButtonStyle.SECONDARY,
        glowColor = MaterialTheme.colorScheme.tertiary,
    ) {
        Icon(
            Icons.Default.FileCopy,
            contentDescription = stringResource(R.string.content_desc_paste_from_clipboard),
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.action_paste_from_clipboard))
    }
}

@Composable
private fun ImportIdleError(error: String?) {
    var retainedErrorMessage by remember { mutableStateOf(error) }
    if (error != null) {
        retainedErrorMessage = error
    }

    AnimatedVisibility(visible = error != null) {
        NeonPanel(modifier = Modifier.padding(top = 24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = stringResource(R.string.content_desc_error),
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(end = 8.dp),
                )
                Text(
                    text = retainedErrorMessage.orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}
