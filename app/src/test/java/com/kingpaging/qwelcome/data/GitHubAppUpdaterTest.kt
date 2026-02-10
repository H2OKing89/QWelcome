package com.kingpaging.qwelcome.data

import android.app.DownloadManager
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.kingpaging.qwelcome.viewmodel.factory.AppViewModelProvider
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class GitHubAppUpdaterTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Before
    fun setup() {
        mockkStatic(FirebaseCrashlytics::class)
        val crashlytics = mockk<FirebaseCrashlytics>(relaxed = true)
        every { FirebaseCrashlytics.getInstance() } returns crashlytics
    }

    @After
    fun tearDown() {
        unmockkStatic(FirebaseCrashlytics::class)
        AppViewModelProvider.resetForTesting()
    }

    @Test
    fun `computeSha256 matches expected hash`() {
        val updater = createUpdater()
        val tempFile = tempFolder.newFile("qwelcome-updater.txt")
        try {
            tempFile.writeText("hello")

            val sha = updater.computeSha256(tempFile)
            assertEquals("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824", sha)
        } finally {
            tempFile.delete()
        }
    }

    @Test
    fun `hashesMatch returns true when hashes are equal and false when different`() {
        val updater = createUpdater()

        assertTrue(updater.hashesMatch("abc", "ABC"))
        assertFalse(updater.hashesMatch("abc", "def"))
    }

    @Test
    fun `signerSetsMatch returns true when archive signers are subset of installed`() {
        val updater = createUpdater()

        assertTrue(updater.signerSetsMatch(setOf("a", "b"), setOf("a", "b")))
        assertTrue(updater.signerSetsMatch(setOf("a", "b", "c"), setOf("a", "b")))
        assertFalse(updater.signerSetsMatch(setOf("a"), setOf("a", "b")))
        assertFalse(updater.signerSetsMatch(setOf("a"), setOf("x")))
        assertFalse(updater.signerSetsMatch(setOf("a", "b"), setOf("b", "c")))
    }

    @Test
    fun `packageNameMatches returns true only for exact package names`() {
        val updater = createUpdater()

        assertTrue(updater.packageNameMatches("com.kingpaging.qwelcome", "com.kingpaging.qwelcome"))
        assertFalse(updater.packageNameMatches("com.kingpaging.qwelcome", "com.example.other"))
    }

    @Test
    fun `enqueueDownload returns Started with download id`() = runTest {
        val downloadManager = mockk<DownloadManager>(relaxed = true)
        val updater = createUpdater(downloadManager = downloadManager)
        val downloadsDir = tempFolder.newFolder("downloads")

        every { downloadManager.enqueue(any()) } returns 42L

        val context = mockk<Context>()
        every { context.applicationContext } returns context
        every { context.packageManager } returns mockk(relaxed = true)
        every { context.packageName } returns "com.kingpaging.qwelcome"
        every { context.getSystemService(Context.DOWNLOAD_SERVICE) } returns downloadManager
        every { context.getExternalFilesDir(any()) } returns downloadsDir

        val testUpdater = GitHubAppUpdater(context)
        val update = UpdateCheckResult.UpdateAvailable(
            latestVersion = "3.0.0",
            downloadUrl = "https://github.com/H2OKing89/QWelcome/releases/download/v3.0.0/QWelcome-v3.0.0.apk",
            releaseNotes = "notes",
            assetName = "QWelcome-v3.0.0.apk",
            assetSizeBytes = 1000L,
            sha256Hex = "a".repeat(64)
        )

        val result = testUpdater.enqueueDownload(update)
        assertTrue(result is DownloadEnqueueResult.Started)
        assertEquals(42L, (result as DownloadEnqueueResult.Started).downloadId)
        verify { downloadManager.enqueue(any()) }
    }

    @Test
    fun `getDownloadStatus returns InProgress for running download`() = runTest {
        val downloadManager = mockk<DownloadManager>(relaxed = true)
        val cursor = mockk<Cursor>(relaxed = true)
        every { cursor.moveToFirst() } returns true
        every { cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS) } returns 0
        every { cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR) } returns 1
        every { cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES) } returns 2
        every { cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON) } returns 3
        every { cursor.getInt(0) } returns DownloadManager.STATUS_RUNNING
        every { cursor.getLong(1) } returns 500L
        every { cursor.getLong(2) } returns 1000L
        every { cursor.getInt(3) } returns 0
        every { cursor.close() } just io.mockk.Runs
        every { downloadManager.query(any()) } returns cursor

        val updater = createUpdater(downloadManager = downloadManager)
        val status = updater.getDownloadStatus(1L)
        assertTrue(status is DownloadStatus.InProgress)
        assertEquals(500L, (status as DownloadStatus.InProgress).bytesDownloaded)
        assertEquals(1000L, status.totalBytes)
    }

    @Test
    fun `getDownloadStatus returns Failed for failed download`() = runTest {
        val downloadManager = mockk<DownloadManager>(relaxed = true)
        val cursor = mockk<Cursor>(relaxed = true)
        every { cursor.moveToFirst() } returns true
        every { cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS) } returns 0
        every { cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR) } returns 1
        every { cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES) } returns 2
        every { cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON) } returns 3
        every { cursor.getInt(0) } returns DownloadManager.STATUS_FAILED
        every { cursor.getLong(1) } returns 0L
        every { cursor.getLong(2) } returns -1L
        every { cursor.getInt(3) } returns DownloadManager.ERROR_INSUFFICIENT_SPACE
        every { cursor.close() } just io.mockk.Runs
        every { downloadManager.query(any()) } returns cursor

        val updater = createUpdater(downloadManager = downloadManager)
        val status = updater.getDownloadStatus(1L)
        assertTrue(status is DownloadStatus.Failed)
    }

    @Test
    fun `enqueueDownload fails fast when existing destination cannot be deleted`() = runTest {
        val downloadManager = mockk<DownloadManager>(relaxed = true)
        val downloadsDir = tempFolder.newFolder("downloads-delete-fail")
        val updatesDir = File(downloadsDir, "updates")
        updatesDir.mkdirs()
        val destination = File(updatesDir, "QWelcome-v3.0.0.apk")
        destination.mkdirs()
        File(destination, "nested").writeText("cannot delete non-empty directory")

        val updater = createUpdater(downloadManager = downloadManager, externalFilesDir = downloadsDir)
        val update = UpdateCheckResult.UpdateAvailable(
            latestVersion = "3.0.0",
            downloadUrl = "https://github.com/H2OKing89/QWelcome/releases/download/v3.0.0/QWelcome-v3.0.0.apk",
            releaseNotes = "notes",
            assetName = "QWelcome-v3.0.0.apk",
            assetSizeBytes = 1000L,
            sha256Hex = "a".repeat(64)
        )

        val result = updater.enqueueDownload(update)
        assertTrue(result is DownloadEnqueueResult.Failed)
        assertTrue((result as DownloadEnqueueResult.Failed).message.contains("replace existing"))
        verify(exactly = 0) { downloadManager.enqueue(any()) }
    }

    @Test
    fun `download path tracking is cleared after successful status`() = runTest {
        val downloadManager = mockk<DownloadManager>(relaxed = true)
        val cursor = mockk<Cursor>(relaxed = true)
        val downloadsDir = tempFolder.newFolder("downloads-success")

        every { downloadManager.enqueue(any()) } returns 7L
        every { downloadManager.query(any()) } returns cursor
        every { cursor.moveToFirst() } returns true
        every { cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS) } returns 0
        every { cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR) } returns 1
        every { cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES) } returns 2
        every { cursor.getInt(0) } returns DownloadManager.STATUS_SUCCESSFUL
        every { cursor.getLong(1) } returns 100L
        every { cursor.getLong(2) } returns 100L
        every { cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI) } returns -1
        every { cursor.close() } just io.mockk.Runs

        val updater = createUpdater(downloadManager = downloadManager, externalFilesDir = downloadsDir)
        val update = UpdateCheckResult.UpdateAvailable(
            latestVersion = "3.0.0",
            downloadUrl = "https://github.com/H2OKing89/QWelcome/releases/download/v3.0.0/QWelcome-v3.0.0.apk",
            releaseNotes = "notes",
            assetName = "QWelcome-v3.0.0.apk",
            assetSizeBytes = 1000L,
            sha256Hex = "a".repeat(64)
        )

        updater.enqueueDownload(update)
        assertEquals(1, updater.trackedDownloadCount())
        val status = updater.getDownloadStatus(7L)
        assertTrue(status is DownloadStatus.Succeeded)
        assertEquals(0, updater.trackedDownloadCount())
    }

    @Test
    fun `download path tracking is cleared after failed status`() = runTest {
        val downloadManager = mockk<DownloadManager>(relaxed = true)
        val cursor = mockk<Cursor>(relaxed = true)
        val downloadsDir = tempFolder.newFolder("downloads-failed")

        every { downloadManager.enqueue(any()) } returns 11L
        every { downloadManager.query(any()) } returns cursor
        every { cursor.moveToFirst() } returns true
        every { cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS) } returns 0
        every { cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR) } returns 1
        every { cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES) } returns 2
        every { cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON) } returns 3
        every { cursor.getInt(0) } returns DownloadManager.STATUS_FAILED
        every { cursor.getLong(1) } returns 0L
        every { cursor.getLong(2) } returns -1L
        every { cursor.getInt(3) } returns DownloadManager.ERROR_UNKNOWN
        every { cursor.close() } just io.mockk.Runs

        val updater = createUpdater(downloadManager = downloadManager, externalFilesDir = downloadsDir)
        val update = UpdateCheckResult.UpdateAvailable(
            latestVersion = "3.0.0",
            downloadUrl = "https://github.com/H2OKing89/QWelcome/releases/download/v3.0.0/QWelcome-v3.0.0.apk",
            releaseNotes = "notes",
            assetName = "QWelcome-v3.0.0.apk",
            assetSizeBytes = 1000L,
            sha256Hex = "a".repeat(64)
        )

        updater.enqueueDownload(update)
        assertEquals(1, updater.trackedDownloadCount())
        val status = updater.getDownloadStatus(11L)
        assertTrue(status is DownloadStatus.Failed)
        assertEquals(0, updater.trackedDownloadCount())
    }

    @Test
    fun `download path tracking is cleared when download is missing`() = runTest {
        val downloadManager = mockk<DownloadManager>(relaxed = true)
        val cursor = mockk<Cursor>(relaxed = true)
        val downloadsDir = tempFolder.newFolder("downloads-missing")

        every { downloadManager.enqueue(any()) } returns 13L
        every { downloadManager.query(any()) } returns cursor
        every { cursor.moveToFirst() } returns false
        every { cursor.close() } just io.mockk.Runs

        val updater = createUpdater(downloadManager = downloadManager, externalFilesDir = downloadsDir)
        val update = UpdateCheckResult.UpdateAvailable(
            latestVersion = "3.0.0",
            downloadUrl = "https://github.com/H2OKing89/QWelcome/releases/download/v3.0.0/QWelcome-v3.0.0.apk",
            releaseNotes = "notes",
            assetName = "QWelcome-v3.0.0.apk",
            assetSizeBytes = 1000L,
            sha256Hex = "a".repeat(64)
        )

        updater.enqueueDownload(update)
        assertEquals(1, updater.trackedDownloadCount())
        val status = updater.getDownloadStatus(13L)
        assertTrue(status is DownloadStatus.Failed)
        assertEquals(0, updater.trackedDownloadCount())
    }

    private fun createUpdater(
        downloadManager: DownloadManager? = null,
        externalFilesDir: File? = tempFolder.root
    ): GitHubAppUpdater {
        val context = mockk<Context>()
        val packageManager = mockk<PackageManager>(relaxed = true)
        val dm = downloadManager ?: mockk<DownloadManager>(relaxed = true)

        every { context.applicationContext } returns context
        every { context.packageManager } returns packageManager
        every { context.packageName } returns "com.kingpaging.qwelcome"
        every { context.getSystemService(Context.DOWNLOAD_SERVICE) } returns dm
        every { context.getExternalFilesDir(any()) } returns externalFilesDir

        return GitHubAppUpdater(context)
    }
}
