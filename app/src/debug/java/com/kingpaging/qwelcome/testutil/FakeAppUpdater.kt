package com.kingpaging.qwelcome.testutil

import android.content.Intent
import com.kingpaging.qwelcome.data.AppUpdater
import com.kingpaging.qwelcome.data.DownloadEnqueueResult
import com.kingpaging.qwelcome.data.DownloadStatus
import com.kingpaging.qwelcome.data.UpdateCheckResult
import com.kingpaging.qwelcome.data.VerificationResult
import java.util.ArrayDeque

class FakeAppUpdater : AppUpdater {
    var checkForUpdateResult: UpdateCheckResult = UpdateCheckResult.UpToDate
    var enqueueResult: DownloadEnqueueResult =
        DownloadEnqueueResult.Failed("enqueue not configured")
    val downloadStatusQueue: ArrayDeque<DownloadStatus> = ArrayDeque()
    var verificationResult: VerificationResult =
        VerificationResult.Failed("verification not configured")
    var canRequestPackageInstallsValue: Boolean = true
    var unknownSourcesIntent: Intent = Intent("fake.unknown.sources")
    var installIntent: Intent? = Intent("fake.install")

    var checkCallCount: Int = 0
    var lastCheckedVersion: String? = null
    var enqueueCallCount: Int = 0
    var lastEnqueuedUpdate: UpdateCheckResult.UpdateAvailable? = null
    var lastVerifiedApkPath: String? = null
    var lastVerifiedUpdate: UpdateCheckResult.UpdateAvailable? = null
    var createInstallIntentCallCount: Int = 0

    override suspend fun checkForUpdate(currentVersionName: String): UpdateCheckResult {
        checkCallCount++
        lastCheckedVersion = currentVersionName
        return checkForUpdateResult
    }

    override suspend fun enqueueDownload(update: UpdateCheckResult.UpdateAvailable): DownloadEnqueueResult {
        enqueueCallCount++
        lastEnqueuedUpdate = update
        return enqueueResult
    }

    override suspend fun getDownloadStatus(downloadId: Long): DownloadStatus {
        return if (downloadStatusQueue.isEmpty()) {
            DownloadStatus.Failed("No queued fake download status")
        } else {
            downloadStatusQueue.removeFirst()
        }
    }

    override suspend fun verifyDownloadedApk(
        apkPath: String,
        update: UpdateCheckResult.UpdateAvailable
    ): VerificationResult {
        lastVerifiedApkPath = apkPath
        lastVerifiedUpdate = update
        return verificationResult
    }

    override fun canRequestPackageInstalls(): Boolean = canRequestPackageInstallsValue

    override fun createUnknownSourcesSettingsIntent(): Intent = unknownSourcesIntent

    override fun createInstallIntent(apkPath: String): Intent? {
        createInstallIntentCallCount++
        return installIntent
    }
}
