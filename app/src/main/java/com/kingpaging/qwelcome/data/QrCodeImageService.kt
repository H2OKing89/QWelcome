package com.kingpaging.qwelcome.data

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.ui.graphics.SolidColor
import androidx.core.content.FileProvider
import androidx.core.graphics.createBitmap
import com.kingpaging.qwelcome.util.sanitizeFileName
import io.github.alexzhirkevich.qrose.ImageFormat
import io.github.alexzhirkevich.qrose.QrCodePainter
import io.github.alexzhirkevich.qrose.options.QrBackground
import io.github.alexzhirkevich.qrose.options.QrBrush
import io.github.alexzhirkevich.qrose.options.QrColors
import io.github.alexzhirkevich.qrose.options.QrOptions
import io.github.alexzhirkevich.qrose.options.solid
import io.github.alexzhirkevich.qrose.toByteArray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import androidx.compose.ui.graphics.Color as ComposeColor

private const val PNG_QUALITY = 100
private const val QR_CODE_SCALE = 0.8f

/** Owns image resources and storage; the UI receives only a shareable URI. */
class QrCodeImageService(
    context: Context,
    private val renderBitmap: (String) -> Bitmap = { renderWifiQrBitmap(it) },
) {
    private val appContext = context.applicationContext
    private val shareCache = QrCodeShareCache(File(appContext.cacheDir, "qr_codes"))

    suspend fun pruneExpiredShares() = withContext(Dispatchers.IO) { shareCache.pruneExpired() }

    suspend fun prepareShare(
        wifiString: String,
        ssid: String,
    ): Uri =
        withContext(Dispatchers.IO) {
            val file =
                withBitmap(wifiString) { bitmap ->
                    shareCache.write(ssid) { output -> writePng(bitmap, output) }
                }
            FileProvider.getUriForFile(appContext, "${appContext.packageName}.provider", file)
        }

    suspend fun saveToGallery(
        wifiString: String,
        ssid: String,
    ) {
        withContext(Dispatchers.IO) {
            val networkName = sanitizeFileName(ssid)
            val filename = "WiFi_QR_${networkName}_${System.currentTimeMillis()}.png"
            withBitmap(wifiString) { bitmap ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    saveToMediaStore(bitmap, filename)
                } else {
                    saveToLegacyStorage(bitmap, filename)
                }
            }
        }
    }

    private inline fun <T> withBitmap(
        wifiString: String,
        action: (Bitmap) -> T,
    ): T {
        val bitmap = renderBitmap(wifiString)
        return try {
            action(bitmap)
        } finally {
            // Cleanup stays on the producing dispatcher, even if the caller is canceled.
            bitmap.recycle()
        }
    }

    private fun saveToMediaStore(
        bitmap: Bitmap,
        filename: String,
    ) {
        val resolver = appContext.contentResolver
        val values =
            ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/QWelcome")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        val uri =
            resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: throw IOException("Failed to create media entry")
        var completed = false
        try {
            writeMediaBitmap(resolver, uri, bitmap)
            val published = ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }
            if (resolver.update(uri, published, null, null) == 0) {
                throw IOException("Failed to publish media entry")
            }
            completed = true
        } finally {
            if (!completed) resolver.delete(uri, null, null)
        }
    }

    @Suppress("DEPRECATION")
    private fun saveToLegacyStorage(
        bitmap: Bitmap,
        filename: String,
    ) {
        val directory = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "QWelcome")
        if (!directory.isDirectory && !directory.mkdirs()) throw IOException("Failed to create pictures directory")
        val file = File(directory, filename)
        var completed = false
        try {
            FileOutputStream(file).use { writePng(bitmap, it) }
            completed = true
            android.media.MediaScannerConnection.scanFile(appContext, arrayOf(file.path), arrayOf("image/png"), null)
        } finally {
            if (!completed) file.delete()
        }
    }
}

private fun writeMediaBitmap(
    resolver: ContentResolver,
    uri: Uri,
    bitmap: Bitmap,
) {
    val output = resolver.openOutputStream(uri) ?: throw IOException("Failed to open output stream")
    output.use { writePng(bitmap, it) }
}

private fun writePng(
    bitmap: Bitmap,
    output: OutputStream,
) {
    if (!bitmap.compress(Bitmap.CompressFormat.PNG, PNG_QUALITY, output)) throw IOException("Failed to encode QR PNG")
}

/**
 * Scanners depend on high-contrast finder patterns and an uninterrupted quiet zone.
 * Keep branding outside the code; both the screen and PNG use these same options.
 */
internal fun wifiQrOptions(): QrOptions =
    QrOptions(
        colors =
            QrColors(
                dark = QrBrush.Default,
                light = QrBrush.solid(ComposeColor.White),
                ball = QrBrush.Default,
                frame = QrBrush.Default,
            ),
        background = QrBackground(fill = SolidColor(ComposeColor.White)),
        scale = QR_CODE_SCALE,
    )

internal fun renderWifiQrBitmap(
    wifiString: String,
    size: Int = 1024,
): Bitmap {
    val painter = QrCodePainter(data = wifiString, options = wifiQrOptions())
    val bytes = painter.toByteArray(size, size, ImageFormat.PNG)
    val qrBitmap =
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: throw IOException("Failed to decode QR bitmap")
    try {
        val padding = size / 10
        val finalBitmap = createBitmap(size + padding * 2, size + padding * 2, Bitmap.Config.ARGB_8888)
        var completed = false
        try {
            Canvas(finalBitmap).apply {
                drawColor(Color.WHITE)
                drawBitmap(qrBitmap, padding.toFloat(), padding.toFloat(), null)
            }
            completed = true
            return finalBitmap
        } finally {
            if (!completed) finalBitmap.recycle()
        }
    } finally {
        qrBitmap.recycle()
    }
}
