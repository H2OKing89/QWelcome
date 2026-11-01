package com.kingpaging.qwelcome.data

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.kingpaging.qwelcome.util.sanitizeFileName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream

private const val PNG_QUALITY = 100

/** Owns image resources and storage; the UI receives only a shareable URI. */
class QrCodeImageService(
    context: Context,
    private val renderBitmap: ((WifiQrImageRequest) -> Bitmap)? = null,
) {
    private val appContext = context.applicationContext
    private val shareCache = QrCodeShareCache(File(appContext.cacheDir, "qr_codes"))

    suspend fun pruneExpiredShares() = withContext(Dispatchers.IO) { shareCache.pruneExpired() }

    suspend fun prepareShare(request: WifiQrImageRequest): Uri =
        withContext(Dispatchers.IO) {
            val file =
                withBitmap(request) { bitmap ->
                    shareCache.write(request.ssid) { output -> writePng(bitmap, output) }
                }
            FileProvider.getUriForFile(appContext, "${appContext.packageName}.provider", file)
        }

    suspend fun saveToGallery(request: WifiQrImageRequest) {
        withContext(Dispatchers.IO) {
            val networkName = sanitizeFileName(request.ssid)
            val filename = "WiFi_QR_${networkName}_${System.currentTimeMillis()}.png"
            withBitmap(request) { bitmap ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    saveToMediaStore(bitmap, filename)
                } else {
                    saveToLegacyStorage(bitmap, filename)
                }
            }
        }
    }

    private inline fun <T> withBitmap(
        request: WifiQrImageRequest,
        action: (Bitmap) -> T,
    ): T {
        val bitmap =
            try {
                renderBitmap?.invoke(request)
                    ?: renderWifiQrBitmap(request, labels = wifiQrArtworkLabels(appContext.resources))
            } catch (error: OutOfMemoryError) {
                throw IOException("Not enough memory to create WiFi artwork", error)
            }
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
