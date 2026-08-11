@file:OptIn(ExperimentalMaterial3Api::class)

package com.kingpaging.qwelcome.ui.components

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import com.kingpaging.qwelcome.R
import androidx.core.graphics.createBitmap
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.core.content.ContextCompat
import com.kingpaging.qwelcome.ui.theme.CyberDarkScheme
import com.kingpaging.qwelcome.util.sanitizeFileName
import com.kingpaging.qwelcome.util.WifiQrGenerator
import io.github.alexzhirkevich.qrose.options.QrBallShape
import io.github.alexzhirkevich.qrose.options.QrBrush
import io.github.alexzhirkevich.qrose.options.QrFrameShape
import io.github.alexzhirkevich.qrose.options.QrOptions
import io.github.alexzhirkevich.qrose.options.QrPixelShape
import io.github.alexzhirkevich.qrose.options.brush
import io.github.alexzhirkevich.qrose.options.roundCorners
import io.github.alexzhirkevich.qrose.QrCodePainter
import io.github.alexzhirkevich.qrose.rememberQrCodePainter
import io.github.alexzhirkevich.qrose.toByteArray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream

@Composable
fun QrCodeBottomSheet(
    ssid: String,
    password: String,
    isOpenNetwork: Boolean = false,
    securityType: WifiQrGenerator.SecurityType = WifiQrGenerator.SecurityType.WPA2_PSK,
    isHiddenNetwork: Boolean = false,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isSaving by remember { mutableStateOf(false) }
    var isSharing by remember { mutableStateOf(false) }
    var showSaveWarning by remember { mutableStateOf(false) }
    val wifiString = remember(ssid, password, isOpenNetwork, securityType, isHiddenNetwork) {
        if (isOpenNetwork) {
            WifiQrGenerator.generateOpenNetworkString(ssid, isHiddenNetwork)
        } else {
            WifiQrGenerator.generateWifiString(ssid, password, securityType, isHiddenNetwork)
        }
    }
    val saveQrCode: () -> Unit = {
        scope.launch {
            isSaving = true
            saveQrCodeToGallery(context, wifiString, ssid)
            isSaving = false
        }
    }
    val storagePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            saveQrCode()
        } else {
            Toast.makeText(context, R.string.toast_permission_denied, Toast.LENGTH_SHORT).show()
        }
    }

    // Use CyberDarkScheme for consistent QR styling in both preview and export
    val darkBrush = Brush.linearGradient(
        listOf(
            CyberDarkScheme.secondary,
            CyberDarkScheme.tertiary,
            CyberDarkScheme.primary
        )
    )
    val ballBrush = Brush.linearGradient(
        listOf(
            CyberDarkScheme.primary,
            CyberDarkScheme.secondary
        )
    )
    val frameBrush = Brush.linearGradient(
        listOf(
            CyberDarkScheme.secondary,
            CyberDarkScheme.tertiary
        )
    )

    // Use shared options for both preview and export
    val qrPainter = rememberQrCodePainter(
        data = wifiString,
        options = createQrOptions(darkBrush, ballBrush, frameBrush)
    )

    // Start expanded to show all content
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
                            ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        } else {
                            saveQrCode()
                        }
                    },
                    style = NeonButtonStyle.PRIMARY
                ) {
                    Text(stringResource(R.string.action_save_anyway))
                }
            },
            dismissButton = {
                NeonButton(
                    onClick = { showSaveWarning = false },
                    style = NeonButtonStyle.TERTIARY
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
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
                shape = RoundedCornerShape(2.dp)
            ) {
                Box(Modifier.size(width = 32.dp, height = 4.dp))
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            QrCodeSheetContent(
                qrPainter = qrPainter,
                network = QrCodeNetworkDetails(
                    ssid = ssid,
                    isOpenNetwork = isOpenNetwork,
                    securityType = securityType
                ),
                isSaving = isSaving,
                isSharing = isSharing,
                actions = QrCodeSheetActions(
                    onRequestSave = { showSaveWarning = true },
                    onShare = {
                        scope.launch {
                            isSharing = true
                            try {
                                shareQrCode(context, wifiString, ssid)
                            } finally {
                                isSharing = false
                            }
                        }
                    }
                )
            )
        }
    }
}

/**
 * Create QR code options matching the preview styling.
 */
private fun createQrOptions(
    darkBrush: Brush,
    ballBrush: Brush,
    frameBrush: Brush
): QrOptions = QrOptions {
    shapes {
        ball = QrBallShape.roundCorners(.25f)
        frame = QrFrameShape.roundCorners(.25f)
        darkPixel = QrPixelShape.roundCorners()
    }
    colors {
        dark = QrBrush.brush { darkBrush }
        ball = QrBrush.brush { ballBrush }
        frame = QrBrush.brush { frameBrush }
    }
}

/**
 * Generate a high-res QR code bitmap using qrose library.
 * This creates the same styled QR code as the preview.
 * Uses CyberDarkScheme for consistent styling regardless of current theme.
 */
private fun generateHighResQrBitmap(
    wifiString: String,
    size: Int = 1024
): Bitmap {
    // Use dark scheme colors for consistent QR code styling
    val darkBrush = Brush.linearGradient(
        listOf(
            CyberDarkScheme.secondary,
            CyberDarkScheme.tertiary,
            CyberDarkScheme.primary
        )
    )
    val ballBrush = Brush.linearGradient(
        listOf(
            CyberDarkScheme.primary,
            CyberDarkScheme.secondary
        )
    )
    val frameBrush = Brush.linearGradient(
        listOf(
            CyberDarkScheme.secondary,
            CyberDarkScheme.tertiary
        )
    )
    val painter = QrCodePainter(
        data = wifiString,
        options = createQrOptions(darkBrush, ballBrush, frameBrush)
    )

    // Export QR code to PNG bytes
    val bytes = painter.toByteArray(size, size, Bitmap.CompressFormat.PNG)

    // Decode bytes to bitmap
    val qrBitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        ?: throw IllegalStateException("Failed to decode QR code bitmap from bytes")

    // Create final bitmap with white background and padding
    val padding = size / 10
    val finalSize = size + padding * 2
    val finalBitmap = createBitmap(finalSize, finalSize, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(finalBitmap)
    canvas.drawColor(android.graphics.Color.WHITE)

    // Draw QR code centered
    val destRect = android.graphics.Rect(padding, padding, padding + size, padding + size)
    canvas.drawBitmap(qrBitmap, null, destRect, null)

    // Recycle intermediate bitmap to free memory
    qrBitmap.recycle()

    return finalBitmap
}

private suspend fun saveQrCodeToGallery(
    context: Context,
    wifiString: String,
    ssid: String
) {
    try {
        withContext(Dispatchers.IO) {
            val bmp = generateHighResQrBitmap(wifiString)
            try {
                val filename = "WiFi_QR_${sanitizeFileName(ssid)}_${System.currentTimeMillis()}.png"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    writeQrBitmapToMediaStore(context, bmp, filename)
                } else {
                    writeQrBitmapToLegacyStorage(bmp, filename)
                }
            } finally {
                // Unconditional: covers success, thrown exceptions, and cancellation (even if it
                // happens while the result is being delivered back from withContext).
                bmp.recycle()
            }
        }
        Toast.makeText(context, R.string.toast_qr_saved, Toast.LENGTH_SHORT).show()
    } catch (e: SecurityException) {
        Log.e("QrCodeBottomSheet", "Failed to save QR image", e)
        Toast.makeText(context, R.string.toast_permission_denied, Toast.LENGTH_SHORT).show()
    } catch (e: IOException) {
        Log.e("QrCodeBottomSheet", "Failed to save QR image", e)
        Toast.makeText(context, context.getString(R.string.toast_failed_save, e.message), Toast.LENGTH_SHORT).show()
    }
}

private fun writeQrBitmapToMediaStore(context: Context, bmp: Bitmap, filename: String) {
    val resolver = context.contentResolver
    val contentValues = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, filename)
        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/QWelcome")
    }
    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        ?: throw IOException("Failed to create media entry")
    var writeCompleted = false
    try {
        writeBitmapPngToStream(resolver.openOutputStream(uri), bmp)
        writeCompleted = true
    } finally {
        // Covers thrown exceptions AND coroutine cancellation (e.g. the bottom
        // sheet is dismissed mid-write), so no orphaned/incomplete entry lingers.
        if (!writeCompleted) {
            resolver.delete(uri, null, null)
        }
    }
}

private fun writeQrBitmapToLegacyStorage(bmp: Bitmap, filename: String) {
    @Suppress("DEPRECATION")
    val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
    val qwelcomeDir = File(picturesDir, "QWelcome")
    if (!qwelcomeDir.exists() && !qwelcomeDir.mkdirs()) {
        throw IOException("Failed to create pictures directory")
    }
    val file = File(qwelcomeDir, filename)
    var writeCompleted = false
    try {
        writeBitmapPngToStream(FileOutputStream(file), bmp)
        writeCompleted = true
    } finally {
        // Covers thrown exceptions AND coroutine cancellation, matching the MediaStore path,
        // so no orphaned/incomplete file lingers on disk.
        if (!writeCompleted) {
            file.delete()
        }
    }
}

/** Encodes [bmp] as PNG into [outputStream], closing it and throwing if either step fails. */
private fun writeBitmapPngToStream(outputStream: OutputStream?, bmp: Bitmap) {
    val stream = outputStream ?: throw IOException("Failed to open output stream")
    stream.use {
        val encoded = bmp.compress(Bitmap.CompressFormat.PNG, 100, it)
        if (!encoded) {
            throw IOException("Failed to encode QR PNG")
        }
    }
}

private suspend fun shareQrCode(
    context: Context,
    wifiString: String,
    ssid: String
) {
    var bitmap: Bitmap? = null
    try {
        val (uri, bmp) = withContext(Dispatchers.IO) {
            val bmp = generateHighResQrBitmap(wifiString)
            val cacheDir = File(context.cacheDir, "qr_codes")
            if (!cacheDir.exists() && !cacheDir.mkdirs()) {
                throw IOException("Failed to create cache directory")
            }
            val file = File(cacheDir, "WiFi_QR_${sanitizeFileName(ssid)}.png")
            FileOutputStream(file).use { stream ->
                bmp.compress(Bitmap.CompressFormat.PNG, 100, stream)
            }
            FileProvider.getUriForFile(context, "${context.packageName}.provider", file) to bmp
        }
        bitmap = bmp
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, context.getString(R.string.share_text_wifi_network_qr, ssid))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.chooser_share_wifi_qr)))
    } catch (e: SecurityException) {
        Log.e("QrCodeBottomSheet", "Permission denied while sharing QR code", e)
        Toast.makeText(context, R.string.toast_permission_denied, Toast.LENGTH_SHORT).show()
    } catch (e: IOException) {
        Log.e("QrCodeBottomSheet", "Failed to share QR code", e)
        Toast.makeText(context, R.string.toast_failed_create_temp, Toast.LENGTH_SHORT).show()
    } catch (e: ActivityNotFoundException) {
        Log.e("QrCodeBottomSheet", "No activity found for share intent", e)
        Toast.makeText(context, context.getString(R.string.toast_failed_share, e.message), Toast.LENGTH_SHORT).show()
    } catch (e: IllegalArgumentException) {
        Log.e("QrCodeBottomSheet", "Failed to share QR code", e)
        Toast.makeText(context, context.getString(R.string.toast_failed_share, e.message), Toast.LENGTH_SHORT).show()
    } finally {
        bitmap?.recycle()
    }
}
