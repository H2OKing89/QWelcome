package com.kingpaging.qwelcome.data

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.kingpaging.qwelcome.util.UPDATE_FALLBACK_FILE_NAME
import com.kingpaging.qwelcome.util.sanitizeFileName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

private const val TAG = "GitHubAppUpdater"
private const val APK_MIME_TYPE = "application/vnd.android.package-archive"
private const val APK_RETENTION_MILLIS = 7L * 24 * 60 * 60 * 1000

class GitHubAppUpdater internal constructor(
    private val context: Context,
    private val pendingDownloadStore: UpdateDownloadStore,
) : AppUpdater {
    constructor(context: Context) : this(context, FileUpdateDownloadStore(context.applicationContext))

    private val appContext = context.applicationContext
    private val downloadManager =
        appContext.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    private val downloadPaths = ConcurrentHashMap<Long, String>()

    override suspend fun checkForUpdate(currentVersionName: String): UpdateCheckResult =
        UpdateChecker.checkForUpdate(currentVersionName)

    override suspend fun restorePendingDownload(currentVersionName: String): PendingUpdateDownload? =
        withContext(Dispatchers.IO) {
            val pending = pendingDownloadStore.read()
            if (
                pending != null &&
                !VersionComparator.isNewerVersion(pending.update.latestVersion, currentVersionName)
            ) {
                downloadManager.remove(pending.downloadId)
                pendingDownloadStore.clear()
                pruneOldApks()
                return@withContext null
            }
            pending?.let { downloadPaths[it.downloadId] = it.apkPath }
            pruneOldApks(pending?.apkPath)
            pending
        }

    override suspend fun forgetPendingDownload(downloadId: Long) =
        withContext(Dispatchers.IO) {
            if (pendingDownloadStore.read()?.downloadId == downloadId) {
                pendingDownloadStore.clear()
            }
            downloadPaths.remove(downloadId)
            Unit
        }

    override suspend fun enqueueDownload(update: UpdateCheckResult.UpdateAvailable): DownloadEnqueueResult =
        withContext(Dispatchers.IO) {
            try {
                if (!UpdateChecker.isTrustedHttpsUrl(update.downloadUrl)) {
                    return@withContext DownloadEnqueueResult.Failed("Blocked untrusted update link")
                }

                val downloadsDir =
                    appContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                        ?: return@withContext DownloadEnqueueResult.Failed("Downloads directory unavailable")
                val updatesDir = File(downloadsDir, "updates")
                if (!updatesDir.exists() && !updatesDir.mkdirs()) {
                    return@withContext DownloadEnqueueResult.Failed("Unable to create updates directory")
                }

                val previous = pendingDownloadStore.read()
                previous?.let { downloadManager.remove(it.downloadId) }
                pendingDownloadStore.clear()
                downloadPaths.clear()
                pruneOldApks()

                val safeAssetName = sanitizeFileName(update.assetName, UPDATE_FALLBACK_FILE_NAME)
                val destinationFile = File(updatesDir, safeAssetName)
                if (destinationFile.exists()) {
                    val deleted = destinationFile.delete()
                    if (!deleted) {
                        return@withContext DownloadEnqueueResult.Failed(
                            "Unable to replace existing update file",
                        )
                    }
                }

                val request =
                    DownloadManager.Request(Uri.parse(update.downloadUrl)).apply {
                        setTitle("Q Welcome Update")
                        setDescription("Downloading ${update.assetName}")
                        setMimeType(APK_MIME_TYPE)
                        setAllowedOverMetered(true)
                        setAllowedOverRoaming(false)
                        setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                        setDestinationInExternalFilesDir(
                            appContext,
                            Environment.DIRECTORY_DOWNLOADS,
                            "updates/$safeAssetName",
                        )
                    }

                val downloadId = downloadManager.enqueue(request)
                val apkPath = destinationFile.absolutePath
                try {
                    pendingDownloadStore.write(PendingUpdateDownload(downloadId, apkPath, update))
                } catch (exception: java.io.IOException) {
                    downloadManager.remove(downloadId)
                    throw exception
                }
                downloadPaths[downloadId] = apkPath
                DownloadEnqueueResult.Started(downloadId = downloadId, apkPath = apkPath)
            } catch (e: java.io.IOException) {
                logError("Failed to enqueue update download", e)
                DownloadEnqueueResult.Failed("Failed to start download: ${e.message}")
            } catch (e: IllegalArgumentException) {
                logError("Failed to enqueue update download", e)
                DownloadEnqueueResult.Failed("Failed to start download: ${e.message}")
            }
        }

    override suspend fun getDownloadStatus(downloadId: Long): DownloadStatus =
        withContext(Dispatchers.IO) {
            val query = DownloadManager.Query().setFilterById(downloadId)
            val cursor =
                downloadManager.query(query)
                    ?: run {
                        downloadPaths.remove(downloadId)
                        return@withContext DownloadStatus.Failed("Download query failed")
                    }

            cursor.use {
                if (!it.moveToFirst()) {
                    downloadPaths.remove(downloadId)
                    return@withContext DownloadStatus.Failed("Download no longer available")
                }

                val status = it.getInt(DownloadManager.COLUMN_STATUS)
                val downloadedBytes = it.getLong(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                val totalBytes = it.getLong(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)

                return@withContext when (status) {
                    DownloadManager.STATUS_PENDING,
                    DownloadManager.STATUS_RUNNING,
                    DownloadManager.STATUS_PAUSED,
                    -> {
                        val total = if (totalBytes >= 0) totalBytes else null
                        DownloadStatus.InProgress(downloadedBytes, total)
                    }

                    DownloadManager.STATUS_SUCCESSFUL -> {
                        val localPath =
                            downloadPaths.remove(downloadId) ?: run {
                                val localUri = it.getStringOrNull(DownloadManager.COLUMN_LOCAL_URI)
                                localUri?.let { uri -> Uri.parse(uri).path }
                            }
                        if (localPath.isNullOrBlank()) {
                            DownloadStatus.Failed("Downloaded file path is unavailable")
                        } else {
                            DownloadStatus.Succeeded(localPath)
                        }
                    }

                    DownloadManager.STATUS_FAILED -> {
                        downloadPaths.remove(downloadId)
                        val reasonCode = it.getInt(DownloadManager.COLUMN_REASON)
                        val reasonMessage = mapDownloadFailure(reasonCode)
                        logError("Update download failed ($reasonCode): $reasonMessage", null)
                        DownloadStatus.Failed(reasonMessage)
                    }

                    else -> {
                        downloadPaths.remove(downloadId)
                        DownloadStatus.Failed("Unknown download status")
                    }
                }
            }
        }

    override suspend fun verifyDownloadedApk(
        apkPath: String,
        update: UpdateCheckResult.UpdateAvailable,
    ): VerificationResult =
        withContext(Dispatchers.IO) {
            val file = File(apkPath)
            if (!file.exists()) {
                return@withContext VerificationResult.Failed("Downloaded file missing")
            }

            val expectedSha = update.sha256Hex.lowercase()
            val actualSha = computeSha256(file)
            if (!hashesMatch(expectedSha, actualSha)) {
                file.delete()
                logError("APK SHA-256 mismatch. expected=$expectedSha actual=$actualSha", null)
                return@withContext VerificationResult.Failed("Integrity check failed (SHA-256 mismatch)")
            }

            val archiveInfo =
                getPackageArchiveInfo(file.absolutePath)
                    ?: run {
                        file.delete()
                        return@withContext VerificationResult.Failed("Unable to inspect downloaded APK")
                    }

            if (!packageNameMatches(appContext.packageName, archiveInfo.packageName)) {
                file.delete()
                return@withContext VerificationResult.Failed("Downloaded APK package does not match this app")
            }

            val installedInfo =
                getInstalledPackageInfo(appContext.packageName)
                    ?: run {
                        file.delete()
                        return@withContext VerificationResult.Failed("Unable to inspect installed app signature")
                    }

            val installedSigners = getSigningIdentity(installedInfo)
            val archiveSigners = getSigningIdentity(archiveInfo)
            if (!installedSigners.acceptsUpdate(archiveSigners)) {
                file.delete()
                logError("APK signature mismatch for update ${update.assetName}", null)
                return@withContext VerificationResult.Failed("Signature verification failed")
            }

            VerificationResult.Success(file.absolutePath)
        }

    override fun canRequestPackageInstalls(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            appContext.packageManager.canRequestPackageInstalls()
        } else {
            true
        }

    override fun createUnknownSourcesSettingsIntent(): Intent =
        Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${appContext.packageName}"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    override fun createInstallIntent(apkPath: String): Intent? {
        val file = File(apkPath)
        if (!file.exists()) return null

        val uri =
            FileProvider.getUriForFile(
                appContext,
                "${appContext.packageName}.provider",
                file,
            )
        return Intent(Intent.ACTION_VIEW)
            .apply {
                setDataAndType(uri, APK_MIME_TYPE)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                putExtra(Intent.EXTRA_RETURN_RESULT, true)
            }.takeIf { installIntent ->
                installIntent.resolveActivity(appContext.packageManager) != null
            }
    }

    internal fun computeSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read == -1) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().toHexLowercase()
    }

    @Suppress("DEPRECATION")
    private fun getPackageArchiveInfo(apkPath: String): PackageInfo? {
        val pm = appContext.packageManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getPackageArchiveInfo(
                apkPath,
                PackageManager.PackageInfoFlags.of(PackageManager.GET_SIGNING_CERTIFICATES.toLong()),
            )
        } else {
            val flags =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    PackageManager.GET_SIGNING_CERTIFICATES
                } else {
                    PackageManager.GET_SIGNATURES
                }
            pm.getPackageArchiveInfo(apkPath, flags)
        }
    }

    @Suppress("DEPRECATION")
    private fun getInstalledPackageInfo(packageName: String): PackageInfo? {
        val pm = appContext.packageManager
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(
                    packageName,
                    PackageManager.PackageInfoFlags.of(PackageManager.GET_SIGNING_CERTIFICATES.toLong()),
                )
            } else {
                val flags =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        PackageManager.GET_SIGNING_CERTIFICATES
                    } else {
                        PackageManager.GET_SIGNATURES
                    }
                pm.getPackageInfo(packageName, flags)
            }
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
    }

    @Suppress("DEPRECATION")
    private fun getSigningIdentity(packageInfo: PackageInfo): ApkSigningIdentity {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val signingInfo = packageInfo.signingInfo ?: return ApkSigningIdentity(emptySet())
            val current =
                signingInfo.apkContentsSigners
                    .orEmpty()
                    .map(::signatureFingerprint)
                    .toSet()
            val history =
                if (signingInfo.hasMultipleSigners()) {
                    current
                } else {
                    signingInfo.signingCertificateHistory
                        .orEmpty()
                        .map(::signatureFingerprint)
                        .toSet()
                }
            ApkSigningIdentity(current, history)
        } else {
            ApkSigningIdentity(
                packageInfo.signatures
                    .orEmpty()
                    .map(::signatureFingerprint)
                    .toSet(),
            )
        }
    }

    /** Only touches old APK files directly inside our updater directory, never an active download. */
    internal fun pruneOldApks(protectedPath: String? = null) {
        val downloads = appContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: return
        val directory = File(downloads, "updates")
        val protectedFiles = downloadPaths.values.toSet() + listOfNotNull(protectedPath)
        val cutoff = System.currentTimeMillis() - APK_RETENTION_MILLIS
        directory.listFiles()?.forEach { file ->
            val oldApk =
                file.isFile &&
                    file.extension.equals("apk", ignoreCase = true) &&
                    file.lastModified() < cutoff
            if (oldApk && file.absolutePath !in protectedFiles) {
                file.delete()
            }
        }
    }

    private fun signatureFingerprint(signature: Signature): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(signature.toByteArray())
        return digest.toHexLowercase()
    }

    private fun mapDownloadFailure(reasonCode: Int): String =
        when (reasonCode) {
            DownloadManager.ERROR_CANNOT_RESUME -> "Download cannot resume"
            DownloadManager.ERROR_DEVICE_NOT_FOUND -> "Storage device unavailable"
            DownloadManager.ERROR_FILE_ALREADY_EXISTS -> "Download file already exists"
            DownloadManager.ERROR_FILE_ERROR -> "File write error"
            DownloadManager.ERROR_HTTP_DATA_ERROR -> "Network transfer error"
            DownloadManager.ERROR_INSUFFICIENT_SPACE -> "Not enough storage space"
            DownloadManager.ERROR_TOO_MANY_REDIRECTS -> "Too many redirects while downloading"
            DownloadManager.ERROR_UNHANDLED_HTTP_CODE -> "Server returned an unsupported response"
            DownloadManager.ERROR_UNKNOWN -> "Unknown download error"
            DownloadManager.PAUSED_QUEUED_FOR_WIFI -> "Waiting for Wi-Fi"
            DownloadManager.PAUSED_WAITING_FOR_NETWORK -> "Waiting for network"
            DownloadManager.PAUSED_WAITING_TO_RETRY -> "Retrying download"
            else -> "Download failed"
        }

    private fun logError(
        message: String,
        throwable: Throwable?,
    ) {
        if (throwable != null) {
            Log.e(TAG, message, throwable)
        } else {
            Log.e(TAG, message)
        }
        val crashlytics = FirebaseCrashlytics.getInstance()
        crashlytics.log(message)
        if (throwable != null) {
            crashlytics.recordException(throwable)
        }
    }

    internal fun hashesMatch(
        expectedSha256: String,
        actualSha256: String,
    ): Boolean = expectedSha256.equals(actualSha256, ignoreCase = true)

    internal fun packageNameMatches(
        expectedPackageName: String,
        archivePackageName: String,
    ): Boolean = expectedPackageName == archivePackageName

    internal fun trackedDownloadCount(): Int = downloadPaths.size
}

private fun ByteArray.toHexLowercase(): String = joinToString("") { "%02x".format(it) }

private fun android.database.Cursor.getInt(columnName: String): Int = getInt(getColumnIndexOrThrow(columnName))

private fun android.database.Cursor.getLong(columnName: String): Long = getLong(getColumnIndexOrThrow(columnName))

private fun android.database.Cursor.getStringOrNull(columnName: String): String? {
    val index = getColumnIndex(columnName)
    if (index == -1 || isNull(index)) return null
    return getString(index)
}
