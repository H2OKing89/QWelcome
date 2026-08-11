@file:Suppress("PackageNaming")

package com.kingpaging.qwelcome.ui.import_pkg

import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kingpaging.qwelcome.R
import com.kingpaging.qwelcome.data.MAX_IMPORT_SIZE_BYTES
import com.kingpaging.qwelcome.data.formatBytesAsMb
import com.kingpaging.qwelcome.di.LocalImportViewModel
import com.kingpaging.qwelcome.di.LocalSoundPlayer
import com.kingpaging.qwelcome.viewmodel.import_pkg.ImportEvent
import java.io.ByteArrayOutputStream
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Suppress("CyclomaticComplexMethod", "FunctionNaming", "LongMethod", "TooGenericExceptionCaught")
@Composable
fun ImportRoute(
    onBack: () -> Unit,
    onImportComplete: () -> Unit
) {
    val viewModel = LocalImportViewModel.current
    val soundPlayer = LocalSoundPlayer.current
    val context = LocalContext.current
    val resources = LocalResources.current
    val clipboardManager = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val maxImportSizeLabel = remember { formatBytesAsMb(MAX_IMPORT_SIZE_BYTES.toLong()) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val json = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use { inputStream ->
                            readUtf8TextWithLimit(inputStream, MAX_IMPORT_SIZE_BYTES)
                        }
                    }
                    withContext(Dispatchers.Main) {
                        if (json != null) {
                            viewModel.onJsonContentReceived(json)
                        } else {
                            showToast(context, R.string.toast_could_not_open_file)
                        }
                    }
                } catch (exception: InputTooLargeException) {
                    Log.w(TAG, "Import input exceeds size limit", exception)
                    withContext(Dispatchers.Main) {
                        soundPlayer.playBeep()
                        showToast(
                            context,
                            resources.getString(R.string.toast_import_too_large, maxImportSizeLabel)
                        )
                    }
                } catch (exception: SecurityException) {
                    Log.w(TAG, "File permission denied", exception)
                    withContext(Dispatchers.Main) {
                        soundPlayer.playBeep()
                        showToast(context, R.string.toast_permission_denied_read)
                    }
                } catch (exception: IOException) {
                    Log.w(TAG, "File read error", exception)
                    val detail = exception.message ?: exception.javaClass.simpleName
                    withContext(Dispatchers.Main) {
                        soundPlayer.playBeep()
                        showToast(context, resources.getString(R.string.toast_error_reading_file, detail))
                    }
                } catch (exception: Exception) {
                    Log.e(TAG, "Unexpected file error", exception)
                    val detail = exception.message ?: exception.javaClass.simpleName
                    withContext(Dispatchers.Main) {
                        soundPlayer.playBeep()
                        showToast(context, resources.getString(R.string.toast_unexpected_error, detail))
                    }
                }
            }
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.reset()
        viewModel.events.collect { event ->
            when (event) {
                is ImportEvent.ImportSuccess -> {
                    val message = buildString {
                        append(
                            resources.getQuantityString(
                                R.plurals.import_success,
                                event.templatesImported,
                                event.templatesImported
                            )
                        )
                        if (event.techProfileImported) {
                            append(resources.getString(R.string.import_success_with_profile))
                        }
                    }
                    showToast(context, message)
                }
                is ImportEvent.ImportFailed -> {
                    soundPlayer.playBeep()
                    showToast(context, event.message)
                }
                is ImportEvent.RequestFileOpen -> filePickerLauncher.launch("application/json")
            }
        }
    }

    ImportScreen(
        uiState = uiState,
        onBack = onBack,
        onImportComplete = onImportComplete,
        onOpenFile = viewModel::onOpenFileRequest,
        onPaste = {
            scope.launch {
                try {
                    val text = clipboardManager.getClipEntry()
                        ?.clipData
                        ?.getItemAt(0)
                        ?.coerceToText(context)
                        ?.toString()
                    when {
                        text.isNullOrBlank() -> {
                            soundPlayer.playBeep()
                            Toast.makeText(context, R.string.toast_clipboard_empty, Toast.LENGTH_SHORT).show()
                        }
                        exceedsImportLimit(text, MAX_IMPORT_SIZE_BYTES) -> {
                            soundPlayer.playBeep()
                            showToast(
                                context,
                                resources.getString(R.string.toast_import_too_large, maxImportSizeLabel)
                            )
                        }
                        else -> viewModel.onPasteContent(text)
                    }
                } catch (exception: SecurityException) {
                    Log.w(TAG, "Clipboard access denied", exception)
                    soundPlayer.playBeep()
                    Toast.makeText(
                        context,
                        R.string.toast_cannot_access_clipboard,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        },
        onConfirm = viewModel::onImportConfirmed,
        onCancel = viewModel::reset
    )
}

private class InputTooLargeException(maxBytes: Int) :
    IOException("Input exceeds ${formatBytesAsMb(maxBytes.toLong())} limit")

private fun readUtf8TextWithLimit(inputStream: java.io.InputStream, maxBytes: Int): String {
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    val output = ByteArrayOutputStream(minOf(maxBytes, 1024 * 1024))
    var totalRead = 0

    while (true) {
        val bytesRead = inputStream.read(buffer)
        if (bytesRead == -1) break
        totalRead += bytesRead
        if (totalRead > maxBytes) throw InputTooLargeException(maxBytes)
        output.write(buffer, 0, bytesRead)
    }

    return output.toString(Charsets.UTF_8.name())
}

private fun exceedsImportLimit(text: CharSequence, maxBytes: Int): Boolean {
    return text.length > maxBytes ||
        (text.length > maxBytes / 2 && text.toString().toByteArray(Charsets.UTF_8).size > maxBytes)
}

private fun showToast(context: android.content.Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
}

private fun showToast(context: android.content.Context, messageRes: Int) {
    Toast.makeText(context, messageRes, Toast.LENGTH_LONG).show()
}

private const val TAG = "ImportRoute"
