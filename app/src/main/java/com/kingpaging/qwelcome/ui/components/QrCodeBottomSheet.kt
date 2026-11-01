@file:OptIn(ExperimentalMaterial3Api::class)

package com.kingpaging.qwelcome.ui.components

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.kingpaging.qwelcome.R
import com.kingpaging.qwelcome.data.QrCodeImageService
import com.kingpaging.qwelcome.data.wifiQrOptions
import com.kingpaging.qwelcome.util.WifiQrGenerator
import io.github.alexzhirkevich.qrose.rememberQrCodePainter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun QrCodeBottomSheet(
    ssid: String,
    password: String,
    isOpenNetwork: Boolean = false,
    securityType: WifiQrGenerator.SecurityType = WifiQrGenerator.SecurityType.WPA2_PSK,
    isHiddenNetwork: Boolean = false,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val resources = androidx.compose.ui.platform.LocalResources.current
    val imageService = remember(context) { QrCodeImageService(context) }
    val scope = rememberCoroutineScope()
    var isSaving by remember { mutableStateOf(false) }
    var isSharing by remember { mutableStateOf(false) }
    var showSaveWarning by rememberSaveable { mutableStateOf(false) }
    val wifiString =
        remember(ssid, password, isOpenNetwork, securityType, isHiddenNetwork) {
            try {
                if (isOpenNetwork) {
                    WifiQrGenerator.generateOpenNetworkString(ssid, isHiddenNetwork)
                } else {
                    WifiQrGenerator.generateWifiString(ssid, password, securityType, isHiddenNetwork)
                }
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
    val saveQrCode: () -> Unit = {
        if (!isSaving && !isSharing) {
            isSaving = true
            scope.launch {
                try {
                    runQrAction(context, R.string.toast_failed_save) {
                        imageService.saveToGallery(wifiString, ssid)
                        Toast.makeText(context, R.string.toast_qr_saved, Toast.LENGTH_SHORT).show()
                    }
                } finally {
                    isSaving = false
                }
            }
        }
    }
    val storagePermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                saveQrCode()
            } else {
                Toast.makeText(context, R.string.toast_permission_denied, Toast.LENGTH_SHORT).show()
            }
        }
    val options = remember { wifiQrOptions() }
    val qrPainter = rememberQrCodePainter(data = wifiString, options = options)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    if (showSaveWarning) {
        AlertDialog(
            onDismissRequest = { showSaveWarning = false },
            modifier = Modifier.imePadding(),
            properties = DialogProperties(decorFitsSystemWindows = false),
            title = { Text(stringResource(R.string.title_save_qr_warning)) },
            text = { Text(stringResource(R.string.text_save_qr_warning)) },
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
            qrPainter = qrPainter,
            network = QrCodeNetworkDetails(ssid, isOpenNetwork, securityType),
            isSaving = isSaving,
            isSharing = isSharing,
            actions =
                QrCodeSheetActions(
                    onRequestSave = { showSaveWarning = true },
                    onShare = {
                        if (!isSaving && !isSharing) {
                            isSharing = true
                            scope.launch {
                                try {
                                    runQrAction(context, R.string.toast_failed_share) {
                                        val uri = imageService.prepareShare(wifiString, ssid)
                                        val intent =
                                            Intent(Intent.ACTION_SEND).apply {
                                                type = "image/png"
                                                putExtra(Intent.EXTRA_STREAM, uri)
                                                putExtra(
                                                    Intent.EXTRA_TEXT,
                                                    resources.getString(R.string.share_text_wifi_network_qr, ssid),
                                                )
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                        context.startActivity(
                                            Intent.createChooser(
                                                intent,
                                                resources.getString(R.string.chooser_share_wifi_qr),
                                            ),
                                        )
                                    }
                                } finally {
                                    isSharing = false
                                }
                            }
                        }
                    },
                ),
        )
    }
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
            Toast
                .makeText(
                    context,
                    context.getString(R.string.toast_failed_share, exception.message),
                    Toast.LENGTH_SHORT,
                ).show()
        else -> Toast.makeText(context, context.getString(messageRes, exception.message), Toast.LENGTH_SHORT).show()
    }
}
