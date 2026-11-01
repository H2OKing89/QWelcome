package com.kingpaging.qwelcome.data

import android.content.Context
import android.util.AtomicFile
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException

/**
 * Durable update handoff metadata. Stored outside backup; never stores customer information.
 * Callers perform all access on an I/O dispatcher.
 */
internal interface UpdateDownloadStore {
    fun read(): PendingUpdateDownload?

    fun write(download: PendingUpdateDownload)

    fun clear()
}

@Serializable
data class PendingUpdateDownload(
    val downloadId: Long,
    val apkPath: String,
    val update: UpdateCheckResult.UpdateAvailable,
)

internal class FileUpdateDownloadStore private constructor(
    private val recordPath: () -> File,
) : UpdateDownloadStore {
    constructor(context: Context) : this({ File(context.noBackupFilesDir, "pending_update.json") })

    constructor(recordFile: File) : this({ recordFile })

    private val file by lazy { AtomicFile(recordPath()) }
    private val json = Json { ignoreUnknownKeys = true }

    override fun read(): PendingUpdateDownload? =
        try {
            json.decodeFromString<PendingUpdateDownload>(file.readFully().toString(Charsets.UTF_8))
        } catch (_: java.io.FileNotFoundException) {
            null
        } catch (_: kotlinx.serialization.SerializationException) {
            file.delete()
            null
        }

    override fun write(download: PendingUpdateDownload) {
        val bytes = json.encodeToString(download).toByteArray(Charsets.UTF_8)
        val stream = file.startWrite()
        try {
            stream.write(bytes)
            file.finishWrite(stream)
        } catch (exception: IOException) {
            file.failWrite(stream)
            throw exception
        }
    }

    override fun clear() {
        file.delete()
    }
}
