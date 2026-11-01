@file:OptIn(ExperimentalMaterial3Api::class)

package com.kingpaging.qwelcome.ui.components

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.kingpaging.qwelcome.R
import com.kingpaging.qwelcome.data.QrCodeImageService
import com.kingpaging.qwelcome.data.WifiQrImageRequest
import com.kingpaging.qwelcome.data.renderWifiQrBitmap
import com.kingpaging.qwelcome.data.wifiQrArtworkLabels
import com.kingpaging.qwelcome.util.WifiQrGenerator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val ARTWORK_PREVIEW_WIDTH = 1280

@Composable
fun QrCodeBottomSheet(
    ssid: String,
    password: String,
    isOpenNetwork: Boolean = false,
    securityType: WifiQrGenerator.SecurityType = WifiQrGenerator.DEFAULT_SECURITY_TYPE,
    isHiddenNetwork: Boolean = false,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val imageService = remember(context) { QrCodeImageService(context) }
    val scope = rememberCoroutineScope()
    var showSaveWarning by rememberSaveable { mutableStateOf(false) }
    val request =
        remember(ssid, password, isOpenNetwork, securityType, isHiddenNetwork) {
            WifiQrImageRequest(ssid, password, isOpenNetwork, securityType, isHiddenNetwork)
        }
    val wifiString =
        remember(request) {
            try {
                request.payload()
            } catch (_: IllegalArgumentException) {
                null
            }
        }
    if (wifiString == null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.title_wifi_qr_code)) },
            text = { Text(stringResource(R.string.error_qr_invalid_credentials)) },
            confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) } },
        )
        return
    }
    val preview = rememberQrArtworkPreview(request)
    val actionController =
        remember(context, imageService, scope) {
            QrCodeActionController(context, imageService, scope)
        }
    val saveQrCode: () -> Unit = { actionController.save(request) }
    val storagePermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                saveQrCode()
            } else {
                Toast.makeText(context, R.string.toast_permission_denied, Toast.LENGTH_SHORT).show()
            }
        }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    if (showSaveWarning) {
        AlertDialog(
            onDismissRequest = { showSaveWarning = false },
            modifier = Modifier.imePadding(),
            properties = DialogProperties(decorFitsSystemWindows = false),
            title = { Text(stringResource(R.string.title_save_qr_warning)) },
            text = { Text(stringResource(R.string.strings_qr_artwork_save_warning)) },
            confirmButton = {
                NeonButton(
                    onClick = {
                        showSaveWarning = false
                        if (
                            Build.VERSION.SDK_INT <= Build.VERSION_CODES.P &&
                            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                            PackageManager.PERMISSION_GRANTED
                        ) {
                            storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        } else {
                            saveQrCode()
                        }
                    },
                    style = NeonButtonStyle.PRIMARY,
                ) { Text(stringResource(R.string.action_save_anyway)) }
            },
            dismissButton = {
                NeonButton(
                    onClick = { showSaveWarning = false },
                    style = NeonButtonStyle.TERTIARY,
                ) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        dragHandle = {
            Surface(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                shape = RoundedCornerShape(2.dp),
            ) { Box(Modifier.size(width = 32.dp, height = 4.dp)) }
        },
    ) {
        QrCodeScrollableContent(
            qrPainter = preview.painter,
            previewFailed = preview.failed,
            network = QrCodeNetworkDetails(ssid, request.isOpen, request.effectiveSecurityType, password),
            isSaving = actionController.isSaving,
            isSharing = actionController.isSharing,
            isPrinting = actionController.isPrinting,
            actions =
                QrCodeSheetActions(
                    onRequestSave = {
                        if (!actionController.isBusy) showSaveWarning = true
                    },
                    onPrint = { actionController.print(request) },
                    onShare = { actionController.share(request) },
                ),
        )
    }
}

/** Serializes image/print actions while keeping their busy state independent of sheet composition. */
private class QrCodeActionController(
    private val context: Context,
    private val imageService: QrCodeImageService,
    private val scope: CoroutineScope,
) {
    var isSaving by mutableStateOf(false)
        private set
    var isSharing by mutableStateOf(false)
        private set
    var isPrinting by mutableStateOf(false)
        private set
    val isBusy: Boolean
        get() = isSaving || isSharing || isPrinting

    fun save(request: WifiQrImageRequest) {
        if (isBusy) return
        isSaving = true
        scope.launch {
            try {
                runQrAction(context, R.string.toast_qr_save_failed) {
                    imageService.saveToGallery(request)
                    Toast.makeText(context, R.string.toast_qr_saved, Toast.LENGTH_SHORT).show()
                }
            } finally {
                isSaving = false
            }
        }
    }

    fun share(request: WifiQrImageRequest) {
        if (isBusy) return
        isSharing = true
        scope.launch {
            try {
                runQrAction(context, R.string.toast_qr_share_failed) {
                    val resources = context.resources
                    val uri = imageService.prepareShare(request)
                    val intent =
                        Intent(Intent.ACTION_SEND).apply {
                            type = "image/png"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            putExtra(
                                Intent.EXTRA_TEXT,
                                resources.getString(R.string.share_text_wifi_network_qr, request.ssid),
                            )
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                    context.startActivity(
                        Intent.createChooser(intent, resources.getString(R.string.chooser_share_wifi_qr)),
                    )
                }
            } finally {
                isSharing = false
            }
        }
    }

    fun print(request: WifiQrImageRequest) {
        if (isBusy) return
        isPrinting = true
        scope.launch {
            var submitted = false
            try {
                runQrAction(context, R.string.qr_print_failed) {
                    printWifiQrArtwork(context, request) { isPrinting = false }
                    submitted = true
                }
            } finally {
                if (!submitted) isPrinting = false
            }
        }
    }
}

private data class QrArtworkPreview(
    val painter: Painter? = null,
    val failed: Boolean = false,
)

@Composable
private fun rememberQrArtworkPreview(request: WifiQrImageRequest): QrArtworkPreview {
    val labels = wifiQrArtworkLabels(androidx.compose.ui.platform.LocalResources.current)
    val preview by produceState(QrArtworkPreview(), request, labels) {
        value = QrArtworkPreview()
        var pendingBitmap: Bitmap? = null
        try {
            val bitmap =
                withContext(Dispatchers.Default) {
                    renderWifiQrBitmap(request, ARTWORK_PREVIEW_WIDTH, labels).also { pendingBitmap = it }
                }
            value = QrArtworkPreview(BitmapPainter(bitmap.asImageBitmap()))
            // A displayed bitmap follows painter/GC ownership so a pending display list stays valid.
            pendingBitmap = null
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: java.io.IOException) {
            Log.e("QrCodeBottomSheet", "QR artwork preview failed", exception)
            value = QrArtworkPreview(failed = true)
        } catch (exception: IllegalArgumentException) {
            Log.e("QrCodeBottomSheet", "QR artwork preview failed", exception)
            value = QrArtworkPreview(failed = true)
        } catch (exception: IllegalStateException) {
            Log.e("QrCodeBottomSheet", "QR artwork preview failed", exception)
            value = QrArtworkPreview(failed = true)
        } finally {
            // Also covers cancellation while the result is dispatched back to Main.
            pendingBitmap?.recycle()
        }
    }
    return preview
}

private suspend fun runQrAction(
    context: Context,
    messageRes: Int,
    action: suspend () -> Unit,
) {
    try {
        action()
    } catch (exception: CancellationException) {
        throw exception
    } catch (exception: java.io.IOException) {
        showQrFailure(context, exception, messageRes)
    } catch (exception: SecurityException) {
        showQrFailure(context, exception, messageRes)
    } catch (exception: IllegalArgumentException) {
        showQrFailure(context, exception, messageRes)
    } catch (exception: IllegalStateException) {
        showQrFailure(context, exception, messageRes)
    } catch (exception: ActivityNotFoundException) {
        showQrFailure(context, exception, messageRes)
    }
}

private fun showQrFailure(
    context: Context,
    exception: Exception,
    messageRes: Int,
) {
    Log.e("QrCodeBottomSheet", "QR image action failed", exception)
    when (exception) {
        is SecurityException -> Toast.makeText(context, R.string.toast_permission_denied, Toast.LENGTH_SHORT).show()
        is ActivityNotFoundException ->
            Toast.makeText(context, messageRes, Toast.LENGTH_SHORT).show()
        else -> Toast.makeText(context, messageRes, Toast.LENGTH_SHORT).show()
    }
}
