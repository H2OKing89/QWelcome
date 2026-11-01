package com.kingpaging.qwelcome.data

import com.kingpaging.qwelcome.util.sanitizeFileName
import java.io.File
import java.io.IOException
import java.io.OutputStream

/** Each URI names an immutable image; later shares cannot replace an earlier recipient's image. */
internal class QrCodeShareCache(
    private val directory: File,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) {
    fun write(
        ssid: String,
        encode: (OutputStream) -> Unit,
    ): File {
        if (!directory.isDirectory && !directory.mkdirs()) throw IOException("Failed to create QR cache directory")
        pruneExpired()
        val file = File.createTempFile("WiFi_QR_${sanitizeFileName(ssid).take(40)}_", ".png", directory)
        var completed = false
        try {
            file.outputStream().use(encode)
            file.setLastModified(nowMillis())
            completed = true
            return file
        } finally {
            if (!completed) file.delete()
        }
    }

    fun pruneExpired() {
        val cutoff = nowMillis() - SHARE_RETENTION_MILLIS
        directory.listFiles()?.filter { it.isFile && it.lastModified() < cutoff }?.forEach { it.delete() }
    }

    companion object {
        const val SHARE_RETENTION_MILLIS = 24 * 60 * 60 * 1_000L
    }
}
