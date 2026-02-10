package com.kingpaging.qwelcome.data

import android.content.Intent

/**
 * Abstraction for update checks, APK download, verification, and install handoff.
 */
interface AppUpdater {
    suspend fun checkForUpdate(currentVersionName: String): UpdateCheckResult
    suspend fun enqueueDownload(update: UpdateCheckResult.UpdateAvailable): DownloadEnqueueResult
    suspend fun getDownloadStatus(downloadId: Long): DownloadStatus
    suspend fun verifyDownloadedApk(
        apkPath: String,
        update: UpdateCheckResult.UpdateAvailable
    ): VerificationResult

    fun canRequestPackageInstalls(): Boolean
    fun createUnknownSourcesSettingsIntent(): Intent
    fun createInstallIntent(apkPath: String): Intent?
}

sealed class DownloadEnqueueResult {
    data class Started(val downloadId: Long, val apkPath: String) : DownloadEnqueueResult()
    data class Failed(val message: String) : DownloadEnqueueResult()
}

sealed class DownloadStatus {
    data class InProgress(val bytesDownloaded: Long, val totalBytes: Long?) : DownloadStatus()
    data class Succeeded(val apkPath: String) : DownloadStatus()
    data class Failed(val message: String) : DownloadStatus()
}

sealed class VerificationResult {
    data class Success(val apkPath: String) : VerificationResult()
    data class Failed(val message: String) : VerificationResult()
}
