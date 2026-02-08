@file:OptIn(ExperimentalMaterial3Api::class)

package com.kingpaging.qwelcome.ui.components

import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import com.kingpaging.qwelcome.R
import androidx.compose.foundation.Image
import androidx.core.graphics.createBitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.kingpaging.qwelcome.ui.theme.CyberDarkScheme
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

@Composable
fun QrCodeBottomSheet(
    ssid: String,
    password: String,
    isOpenNetwork: Boolean = false,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isSaving by remember { mutableStateOf(false) }
    var isSharing by remember { mutableStateOf(false) }
    val wifiString = remember(ssid, password, isOpenNetwork) {
        if (isOpenNetwork) {
            WifiQrGenerator.generateOpenNetworkString(ssid)
        } else {
            WifiQrGenerator.generateWifiString(ssid, password)
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
            Text(
                text = "WiFi QR Code",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Scan to connect automatically",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = qrPainter,
                    contentDescription = "WiFi QR Code",
                    modifier = Modifier.fillMaxSize()
                )
            }
            Spacer(Modifier.height(16.dp))
            NeonPanel(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Network:", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    Text(ssid, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                }
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Security:", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    if (isOpenNetwork) {
                        Text(stringResource(R.string.label_open_no_password), color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Medium)
                    } else {
                        Text(password, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Medium)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                NeonCyanButton(
                    onClick = {
                        scope.launch {
                            isSaving = true
                            saveQrCodeToGallery(context, wifiString, ssid)
                            isSaving = false
                        }
                    },
                    enabled = !isSaving && !isSharing,
                    modifier = Modifier.weight(1f)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(
                            Icons.Default.Download,
                            contentDescription = stringResource(R.string.action_save),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.action_save))
                }
                NeonMagentaButton(
                    onClick = {
                        scope.launch {
                            isSharing = true
                            shareQrCode(context, wifiString, ssid)
                            isSharing = false
                        }
                    },
                    enabled = !isSaving && !isSharing,
                    modifier = Modifier.weight(1f)
                ) {
                    if (isSharing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = stringResource(R.string.action_share),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.action_share))
                }
            }
            Spacer(Modifier.height(24.dp))
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
    var bitmap: Bitmap? = null
    try {
        bitmap = withContext(Dispatchers.IO) {
            val bmp = generateHighResQrBitmap(wifiString)
            val filename = "WiFi_QR_${ssid.replace(" ", "_")}_${System.currentTimeMillis()}.png"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/QWelcome")
                }
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                    ?: throw IOException("Failed to create media entry")
                try {
                    val outputStream = resolver.openOutputStream(uri)
                        ?: throw IOException("Failed to open media output stream")
                    outputStream.use { stream ->
                        val encoded = bmp.compress(Bitmap.CompressFormat.PNG, 100, stream)
                        if (!encoded) {
                            throw IOException("Failed to encode QR PNG")
                        }
                    }
                } catch (e: IOException) {
                    resolver.delete(uri, null, null)
                    throw e
                } catch (e: SecurityException) {
                    resolver.delete(uri, null, null)
                    throw e
                }
            } else {
                @Suppress("DEPRECATION")
                val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val qwelcomeDir = File(picturesDir, "QWelcome")
                if (!qwelcomeDir.exists() && !qwelcomeDir.mkdirs()) {
                    throw IOException("Failed to create pictures directory")
                }
                val file = File(qwelcomeDir, filename)
                FileOutputStream(file).use { stream ->
                    val encoded = bmp.compress(Bitmap.CompressFormat.PNG, 100, stream)
                    if (!encoded) {
                        throw IOException("Failed to encode QR PNG")
                    }
                }
            }
            bmp
        }
        Toast.makeText(context, R.string.toast_qr_saved, Toast.LENGTH_SHORT).show()
    } catch (e: SecurityException) {
        Log.e("QrCodeBottomSheet", "Failed to save QR image", e)
        Toast.makeText(context, R.string.toast_permission_denied, Toast.LENGTH_SHORT).show()
    } catch (e: IOException) {
        Log.e("QrCodeBottomSheet", "Failed to save QR image", e)
        Toast.makeText(context, context.getString(R.string.toast_failed_save, e.message), Toast.LENGTH_SHORT).show()
    } finally {
        bitmap?.recycle()
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
                throw Exception("Failed to create cache directory")
            }
            val file = File(cacheDir, "WiFi_QR_${ssid.replace(" ", "_")}.png")
            FileOutputStream(file).use { stream ->
                bmp.compress(Bitmap.CompressFormat.PNG, 100, stream)
            }
            FileProvider.getUriForFile(context, "${context.packageName}.provider", file) to bmp
        }
        bitmap = bmp
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, "WiFi Network: $ssid\nScan the QR code to connect!")
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
