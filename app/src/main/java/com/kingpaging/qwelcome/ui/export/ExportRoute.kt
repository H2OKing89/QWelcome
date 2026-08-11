package com.kingpaging.qwelcome.ui.export

import android.content.ClipData
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kingpaging.qwelcome.R
import com.kingpaging.qwelcome.di.LocalExportViewModel
import com.kingpaging.qwelcome.di.LocalNavigator
import com.kingpaging.qwelcome.di.LocalSoundPlayer
import com.kingpaging.qwelcome.viewmodel.export.ExportEvent
import com.kingpaging.qwelcome.viewmodel.export.ExportType
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Suppress("CyclomaticComplexMethod", "FunctionNaming", "LongMethod")
@Composable
fun ExportRoute(onBack: () -> Unit) {
    val viewModel = LocalExportViewModel.current
    val soundPlayer = LocalSoundPlayer.current
    val context = LocalContext.current
    val resources = LocalResources.current
    val navigator = LocalNavigator.current
    val clipboardManager = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val saveFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) {
            viewModel.onFileSaveCancelled()
        } else {
            val json = viewModel.getPendingFileExportContent()
            if (json != null) {
                scope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            val outputStream = context.contentResolver.openOutputStream(uri)
                                ?: throw IOException("Could not open output stream")
                            val bytes = json.toByteArray(Charsets.UTF_8)
                            outputStream.use { it.write(bytes) }
                        }
                        viewModel.onFileSaveComplete()
                    } catch (exception: SecurityException) {
                        soundPlayer.playBeep()
                        showSaveError(context, exception)
                        viewModel.onFileSaveCancelled()
                    } catch (exception: IOException) {
                        soundPlayer.playBeep()
                        showSaveError(context, exception)
                        viewModel.onFileSaveCancelled()
                    }
                }
            } else {
                viewModel.onFileSaveCancelled()
            }
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.reset()
        viewModel.events.collect { event ->
            when (event) {
                is ExportEvent.ExportSuccess -> Unit
                is ExportEvent.ExportError -> {
                    soundPlayer.playBeep()
                    Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
                }
                is ExportEvent.CopiedToClipboard -> {
                    val typeName = resources.getString(event.type.labelRes())
                    Toast.makeText(
                        context,
                        resources.getString(R.string.toast_type_copied, typeName),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                is ExportEvent.ShareReady -> {
                    val typeName = resources.getString(event.type.labelRes())
                    navigator.shareText(
                        message = event.json,
                        chooserTitle = resources.getString(R.string.chooser_share_type, typeName),
                        subject = resources.getString(R.string.subject_qwelcome_export, typeName)
                    )
                }
                is ExportEvent.ShareToAppReady -> {
                    val typeName = resources.getString(event.type.labelRes())
                    navigator.shareToApp(
                        packageName = event.packageName,
                        message = event.json,
                        subject = resources.getString(R.string.subject_qwelcome_export, typeName),
                        chooserTitle = resources.getString(R.string.chooser_share_type, typeName)
                    )
                }
                is ExportEvent.RequestFileSave -> saveFileLauncher.launch(event.suggestedName)
                is ExportEvent.FileSaved -> {
                    val typeName = resources.getString(event.type.labelRes())
                    Toast.makeText(
                        context,
                        resources.getString(R.string.toast_type_saved, typeName),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    ExportScreen(
        uiState = uiState,
        onBack = onBack,
        onTemplatePackRequested = viewModel::onTemplatePackRequested,
        onFullBackupRequested = viewModel::exportFullBackup,
        onShareToPackageRequested = viewModel::onShareToPackageRequested,
        onCopy = {
            uiState.lastExportedJson?.let { json ->
                scope.launch {
                    clipboardManager.setClipEntry(
                        ClipEntry(ClipData.newPlainText(resources.getString(R.string.title_export), json))
                    )
                    viewModel.onCopiedToClipboard()
                }
            }
        },
        onShareRequested = viewModel::onShareRequested,
        onSaveToFileRequested = viewModel::onSaveToFileRequested,
        onToggleTemplateSelection = viewModel::toggleTemplateSelection,
        onToggleSelectAll = viewModel::toggleSelectAll,
        onDismissTemplateSelection = viewModel::dismissTemplateSelection,
        onExportSelectedTemplates = viewModel::exportSelectedTemplates
    )
}

private fun ExportType.labelRes(): Int = when (this) {
    ExportType.TEMPLATE_PACK -> R.string.export_type_template_pack
    ExportType.FULL_BACKUP -> R.string.export_type_full_backup
}

private fun showSaveError(context: android.content.Context, exception: Exception) {
    Toast.makeText(
        context,
        context.getString(R.string.toast_failed_save_file, exception.message),
        Toast.LENGTH_LONG
    ).show()
}
