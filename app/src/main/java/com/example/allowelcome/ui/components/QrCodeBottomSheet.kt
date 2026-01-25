@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.allowelcome.ui.components

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.Image
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.allowelcome.ui.theme.CyberScheme
import com.example.allowelcome.util.WifiQrGenerator
import io.github.alexzhirkevich.qrose.options.QrBallShape
import io.github.alexzhirkevich.qrose.options.QrBrush
import io.github.alexzhirkevich.qrose.options.QrColors
import io.github.alexzhirkevich.qrose.options.QrFrameShape
import io.github.alexzhirkevich.qrose.options.QrOptions
import io.github.alexzhirkevich.qrose.options.QrPixelShape
import io.github.alexzhirkevich.qrose.options.QrShapes
import io.github.alexzhirkevich.qrose.options.brush
import io.github.alexzhirkevich.qrose.options.roundCorners
import io.github.alexzhirkevich.qrose.QrCodePainter
import io.github.alexzhirkevich.qrose.rememberQrCodePainter
import io.github.alexzhirkevich.qrose.toByteArray
import io.github.alexzhirkevich.qrose.ImageFormat
import java.io.File
import java.io.FileOutputStream

@Composable
fun QrCodeBottomSheet(
    ssid: String,
    password: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val wifiString = remember(ssid, password) {
        WifiQrGenerator.generateWifiString(ssid, password)
    }

    // Use shared options for both preview and export
    val qrPainter = rememberQrCodePainter(
        data = wifiString,
        options = createQrOptions()
    )

    // Start expanded to show all content
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CyberScheme.surface,
        contentColor = CyberScheme.onSurface,
        dragHandle = {
            Surface(
                modifier = Modifier.padding(vertical = 12.dp),
                color = CyberScheme.primary.copy(alpha = 0.4f),
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
                color = CyberScheme.primary
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Scan to connect automatically",
                style = MaterialTheme.typography.bodyMedium,
                color = CyberScheme.onSurface.copy(alpha = 0.7f)
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
                    Text("Network:", color = CyberScheme.onSurface.copy(alpha = 0.7f))
                    Text(ssid, color = CyberScheme.primary, fontWeight = FontWeight.Medium)
                }
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Password:", color = CyberScheme.onSurface.copy(alpha = 0.7f))
                    Text(password, color = CyberScheme.secondary, fontWeight = FontWeight.Medium)
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                NeonCyanButton(
                    onClick = { saveQrCodeToGallery(context, wifiString, ssid) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Save")
                }
                NeonMagentaButton(
                    onClick = { shareQrCode(context, wifiString, ssid) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Share")
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

/**
 * Create QR code options matching the preview styling.
 */
private fun createQrOptions(): QrOptions = QrOptions {
    shapes {
        ball = QrBallShape.roundCorners(.25f)
        frame = QrFrameShape.roundCorners(.25f)
        darkPixel = QrPixelShape.roundCorners()
    }
    colors {
        dark = QrBrush.brush { 
            Brush.linearGradient(listOf(CyberScheme.secondary, CyberScheme.tertiary, CyberScheme.primary)) 
        }
        ball = QrBrush.brush { 
            Brush.linearGradient(listOf(CyberScheme.primary, CyberScheme.secondary)) 
        }
        frame = QrBrush.brush { 
            Brush.linearGradient(listOf(CyberScheme.secondary, CyberScheme.tertiary)) 
        }
    }
}

/**
 * Generate a high-res QR code bitmap using qrose library.
 * This creates the same styled QR code as the preview.
 */
private fun generateHighResQrBitmap(
    wifiString: String,
    size: Int = 1024
): Bitmap {
    val painter = QrCodePainter(
        data = wifiString,
        options = createQrOptions()
    )
    
    // Export QR code to PNG bytes
    val bytes = painter.toByteArray(size, size, ImageFormat.PNG)
    
    // Decode bytes to bitmap
    val qrBitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    
    // Create final bitmap with white background and padding
    val padding = size / 10
    val finalSize = size + padding * 2
    val finalBitmap = Bitmap.createBitmap(finalSize, finalSize, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(finalBitmap)
    canvas.drawColor(android.graphics.Color.WHITE)
    
    // Draw QR code centered
    val destRect = android.graphics.Rect(padding, padding, padding + size, padding + size)
    canvas.drawBitmap(qrBitmap, null, destRect, null)
    
    // Recycle intermediate bitmap to free memory
    qrBitmap.recycle()
    
    return finalBitmap
}

private fun saveQrCodeToGallery(
    context: Context,
    wifiString: String,
    ssid: String
) {
    var bitmap: Bitmap? = null
    try {
        bitmap = generateHighResQrBitmap(wifiString)
        val filename = "WiFi_QR_${ssid.replace(" ", "_")}_${System.currentTimeMillis()}.png"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/ALLOWelcome")
            }
            context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)?.let { uri ->
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                }
            }
        } else {
            @Suppress("DEPRECATION")
            val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val qwelcomeDir = File(picturesDir, "ALLOWelcome")
            qwelcomeDir.mkdirs()
            val file = File(qwelcomeDir, filename)
            FileOutputStream(file).use { stream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            }
        }
        Toast.makeText(context, "QR code saved to Pictures/ALLOWelcome", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Failed to save: ${e.message}", Toast.LENGTH_SHORT).show()
    } finally {
        bitmap?.recycle()
    }
}

private fun shareQrCode(
    context: Context,
    wifiString: String,
    ssid: String
) {
    var bitmap: Bitmap? = null
    try {
        bitmap = generateHighResQrBitmap(wifiString)
        val cacheDir = File(context.cacheDir, "qr_codes")
        cacheDir.mkdirs()
        val file = File(cacheDir, "WiFi_QR_${ssid.replace(" ", "_")}.png")
        FileOutputStream(file).use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, "WiFi Network: $ssid\nScan the QR code to connect!")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share WiFi QR Code"))
    } catch (e: Exception) {
        Toast.makeText(context, "Failed to share: ${e.message}", Toast.LENGTH_SHORT).show()
    } finally {
        bitmap?.recycle()
    }
}
