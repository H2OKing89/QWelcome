package com.kingpaging.qwelcome.ui

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kingpaging.qwelcome.R
import com.kingpaging.qwelcome.data.AppStartupState
import com.kingpaging.qwelcome.data.readLegacyTemplatesForRecovery
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

@Composable
internal fun StartupRecoveryScreen(
    state: AppStartupState,
    onRetry: () -> Unit,
    onContinueWithoutTemplates: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var confirmRecovery by rememberSaveable { mutableStateOf(false) }
    var copyStatus by rememberSaveable { mutableStateOf<Int?>(null) }
    var savingCopy by remember { mutableStateOf(false) }
    val saveCopy =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
            if (uri != null) {
                scope.launch {
                    savingCopy = true
                    try {
                        writeRecoveryCopy(context, uri)
                        copyStatus = R.string.startup_copy_saved
                    } catch (exception: CancellationException) {
                        throw exception
                    } catch (_: Exception) {
                        copyStatus = R.string.startup_copy_failed
                    } finally {
                        savingCopy = false
                    }
                }
            }
        }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (state == AppStartupState.Loading) {
            CircularProgressIndicator()
            Text(stringResource(R.string.startup_loading))
        } else if (state is AppStartupState.Failed) {
            RecoveryActions(
                state = state,
                enabled = !savingCopy,
                status = copyStatus,
                onRetry = onRetry,
                onSaveCopy = { saveCopy.launch("QWelcome-template-recovery.json") },
                onContinue = { confirmRecovery = true },
            )
        }
    }
    val canContinue = !savingCopy && copyStatus == R.string.startup_copy_saved
    if (confirmRecovery && canContinue && (state as? AppStartupState.Failed)?.canRecoverLegacyTemplates == true) {
        ConfirmTemplateRecovery(
            onDismiss = { confirmRecovery = false },
            onContinue = {
                confirmRecovery = false
                onContinueWithoutTemplates()
            },
        )
    }
}

@Composable
private fun ConfirmTemplateRecovery(
    onDismiss: () -> Unit,
    onContinue: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.startup_confirm_title)) },
        text = { Text(stringResource(R.string.startup_confirm_body)) },
        confirmButton = {
            TextButton(onClick = onContinue) { Text(stringResource(R.string.startup_confirm_continue)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.startup_cancel)) }
        },
    )
}

@Composable
private fun RecoveryActions(
    state: AppStartupState.Failed,
    enabled: Boolean,
    status: Int?,
    onRetry: () -> Unit,
    onSaveCopy: () -> Unit,
    onContinue: () -> Unit,
) {
    Text(stringResource(R.string.startup_failed_title), style = MaterialTheme.typography.headlineSmall)
    val bodyResource =
        if (state.canRecoverLegacyTemplates) R.string.startup_legacy_failed_body else R.string.startup_failed_body
    Text(stringResource(bodyResource))
    Button(onClick = onRetry, enabled = enabled) {
        Text(stringResource(R.string.startup_retry))
    }
    if (state.canRecoverLegacyTemplates) {
        TextButton(onClick = onSaveCopy, enabled = enabled) { Text(stringResource(R.string.startup_save_copy)) }
        TextButton(onClick = onContinue, enabled = enabled && status == R.string.startup_copy_saved) {
            Text(stringResource(R.string.startup_skip_templates))
        }
    }
    status?.let { Text(stringResource(it)) }
}

private suspend fun writeRecoveryCopy(
    context: Context,
    uri: Uri,
) = withContext(Dispatchers.IO) {
    val raw = readLegacyTemplatesForRecovery(context) ?: throw IOException("No legacy templates to recover")
    val stream = context.contentResolver.openOutputStream(uri) ?: throw IOException("Cannot open recovery document")
    stream.bufferedWriter(Charsets.UTF_8).use { it.write(raw) }
}
