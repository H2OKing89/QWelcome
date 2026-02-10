package com.kingpaging.qwelcome.data

import android.app.DownloadManager
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
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

    @Test
    fun `computeSha256 matches expected hash`() {
        val updater = createUpdater()
        val tempFile = tempFolder.newFile("qwelcome-updater.txt")
        tempFile.writeText("hello")

        val sha = updater.computeSha256(tempFile)
        assertEquals("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824", sha)
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

    private fun createUpdater(downloadManager: DownloadManager? = null): GitHubAppUpdater {
        val context = mockk<Context>()
        val packageManager = mockk<PackageManager>(relaxed = true)
        val dm = downloadManager ?: mockk<DownloadManager>(relaxed = true)

        every { context.applicationContext } returns context
        every { context.packageManager } returns packageManager
        every { context.packageName } returns "com.kingpaging.qwelcome"
        every { context.getSystemService(Context.DOWNLOAD_SERVICE) } returns dm

        return GitHubAppUpdater(context)
    }
}
