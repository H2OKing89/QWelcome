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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

private const val TAG = "GitHubAppUpdater"

class GitHubAppUpdater(
    private val context: Context
) : AppUpdater {
    private val appContext = context.applicationContext
    private val downloadManager =
        appContext.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    private val downloadPaths = ConcurrentHashMap<Long, String>()

    override suspend fun checkForUpdate(currentVersionName: String): UpdateCheckResult {
        return UpdateChecker.checkForUpdate(currentVersionName)
    }

    override suspend fun enqueueDownload(update: UpdateCheckResult.UpdateAvailable): DownloadEnqueueResult =
        withContext(Dispatchers.IO) {
            try {
                if (!UpdateChecker.isTrustedHttpsUrl(update.downloadUrl)) {
                    return@withContext DownloadEnqueueResult.Failed("Blocked untrusted update link")
                }

                val downloadsDir = appContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                    ?: return@withContext DownloadEnqueueResult.Failed("Downloads directory unavailable")
                val updatesDir = File(downloadsDir, "updates")
                if (!updatesDir.exists() && !updatesDir.mkdirs()) {
                    return@withContext DownloadEnqueueResult.Failed("Unable to create updates directory")
                }

                val safeAssetName = sanitizeAssetName(update.assetName)
                val destinationFile = File(updatesDir, safeAssetName)
                if (destinationFile.exists()) {
                    destinationFile.delete()
                }

                val request = DownloadManager.Request(Uri.parse(update.downloadUrl)).apply {
                    setTitle("Q Welcome Update")
                    setDescription("Downloading ${update.assetName}")
                    setMimeType("application/vnd.android.package-archive")
                    setAllowedOverMetered(true)
                    setAllowedOverRoaming(false)
                    setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    setDestinationInExternalFilesDir(
                        appContext,
                        Environment.DIRECTORY_DOWNLOADS,
                        "updates/$safeAssetName"
                    )
                }

                val downloadId = downloadManager.enqueue(request)
                val apkPath = destinationFile.absolutePath
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
            val cursor = downloadManager.query(query)
                ?: return@withContext DownloadStatus.Failed("Download query failed")

            cursor.use {
                if (!it.moveToFirst()) {
                    return@withContext DownloadStatus.Failed("Download no longer available")
                }

                val status = it.getInt(DownloadManager.COLUMN_STATUS)
                val downloadedBytes = it.getLong(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                val totalBytes = it.getLong(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)

                return@withContext when (status) {
                    DownloadManager.STATUS_PENDING,
                    DownloadManager.STATUS_RUNNING,
                    DownloadManager.STATUS_PAUSED -> {
                        val total = if (totalBytes >= 0) totalBytes else null
                        DownloadStatus.InProgress(downloadedBytes, total)
                    }

                    DownloadManager.STATUS_SUCCESSFUL -> {
                        val localPath = downloadPaths[downloadId] ?: run {
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
                        val reasonCode = it.getInt(DownloadManager.COLUMN_REASON)
                        val reasonMessage = mapDownloadFailure(reasonCode)
                        logError("Update download failed ($reasonCode): $reasonMessage", null)
                        DownloadStatus.Failed(reasonMessage)
                    }

                    else -> DownloadStatus.Failed("Unknown download status")
                }
            }
        }

    override suspend fun verifyDownloadedApk(
        apkPath: String,
        update: UpdateCheckResult.UpdateAvailable
    ): VerificationResult = withContext(Dispatchers.IO) {
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

        val archiveInfo = getPackageArchiveInfo(file.absolutePath)
            ?: run {
                file.delete()
                return@withContext VerificationResult.Failed("Unable to inspect downloaded APK")
            }

        if (!packageNameMatches(appContext.packageName, archiveInfo.packageName)) {
            file.delete()
            return@withContext VerificationResult.Failed("Downloaded APK package does not match this app")
        }

        val installedInfo = getInstalledPackageInfo(appContext.packageName)
            ?: run {
                file.delete()
                return@withContext VerificationResult.Failed("Unable to inspect installed app signature")
            }

        val installedSigners = getSignerFingerprints(installedInfo)
        val archiveSigners = getSignerFingerprints(archiveInfo)
        if (!signerSetsMatch(installedSigners, archiveSigners)) {
            file.delete()
            logError("APK signature mismatch for update ${update.assetName}", null)
            return@withContext VerificationResult.Failed("Signature verification failed")
        }

        VerificationResult.Success(file.absolutePath)
    }

    override fun canRequestPackageInstalls(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            appContext.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    override fun createUnknownSourcesSettingsIntent(): Intent {
        return Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${appContext.packageName}")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    override fun createInstallIntent(apkPath: String): Intent? {
        val file = File(apkPath)
        if (!file.exists()) return null

        val uri = FileProvider.getUriForFile(
            appContext,
            "${appContext.packageName}.provider",
            file
        )
        return Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
            data = uri
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
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

    private fun sanitizeAssetName(name: String): String {
        return name.replace(Regex("[^A-Za-z0-9._-]"), "_")
    }

    @Suppress("DEPRECATION")
    private fun getPackageArchiveInfo(apkPath: String): PackageInfo? {
        val pm = appContext.packageManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getPackageArchiveInfo(
                apkPath,
                PackageManager.PackageInfoFlags.of(PackageManager.GET_SIGNING_CERTIFICATES.toLong())
            )
        } else {
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
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
                    PackageManager.PackageInfoFlags.of(PackageManager.GET_SIGNING_CERTIFICATES.toLong())
                )
            } else {
                val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
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
    private fun getSignerFingerprints(packageInfo: PackageInfo): Set<String> {
        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val signingInfo = packageInfo.signingInfo
            if (signingInfo != null && !signingInfo.hasMultipleSigners()) {
                // Include certificate history for key-rotated apps
                signingInfo.signingCertificateHistory?.toList().orEmpty()
            } else {
                signingInfo?.apkContentsSigners?.toList().orEmpty()
            }
        } else {
            packageInfo.signatures?.toList().orEmpty()
        }
        return signatures.map(::signatureFingerprint).toSet()
    }

    private fun signatureFingerprint(signature: Signature): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(signature.toByteArray())
        return digest.toHexLowercase()
    }

    private fun mapDownloadFailure(reasonCode: Int): String {
        return when (reasonCode) {
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
    }

    private fun logError(message: String, throwable: Throwable?) {
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

    internal fun hashesMatch(expectedSha256: String, actualSha256: String): Boolean {
        return expectedSha256.equals(actualSha256, ignoreCase = true)
    }

    internal fun packageNameMatches(expectedPackageName: String, archivePackageName: String): Boolean {
        return expectedPackageName == archivePackageName
    }

    internal fun signerSetsMatch(installedSigners: Set<String>, archiveSigners: Set<String>): Boolean {
        return installedSigners.containsAll(archiveSigners)
    }
}

private fun ByteArray.toHexLowercase(): String = joinToString("") { "%02x".format(it) }

private fun android.database.Cursor.getInt(columnName: String): Int {
    return getInt(getColumnIndexOrThrow(columnName))
}

private fun android.database.Cursor.getLong(columnName: String): Long {
    return getLong(getColumnIndexOrThrow(columnName))
}

private fun android.database.Cursor.getStringOrNull(columnName: String): String? {
    val index = getColumnIndex(columnName)
    if (index == -1 || isNull(index)) return null
    return getString(index)
}
