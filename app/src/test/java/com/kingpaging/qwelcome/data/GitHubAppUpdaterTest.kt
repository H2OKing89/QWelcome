package com.kingpaging.qwelcome.data

import android.app.DownloadManager
import android.content.Context
import android.content.pm.PackageManager
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class GitHubAppUpdaterTest {

    @Test
    fun `computeSha256 matches expected hash`() {
        val updater = createUpdater()
        val tempFile = File.createTempFile("qwelcome-updater", ".txt")
        tempFile.writeText("hello")

        val sha = updater.computeSha256(tempFile)
        assertEquals("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824", sha)

        tempFile.delete()
    }

    @Test
    fun `hashesMatch returns true when hashes are equal and false when different`() {
        val updater = createUpdater()

        assertTrue(updater.hashesMatch("abc", "ABC"))
        assertFalse(updater.hashesMatch("abc", "def"))
    }

    @Test
    fun `signerSetsMatch returns true on overlap and false on mismatch`() {
        val updater = createUpdater()

        assertTrue(updater.signerSetsMatch(setOf("a", "b"), setOf("b", "c")))
        assertFalse(updater.signerSetsMatch(setOf("a"), setOf("x")))
    }

    @Test
    fun `packageNameMatches returns true only for exact package names`() {
        val updater = createUpdater()

        assertTrue(updater.packageNameMatches("com.kingpaging.qwelcome", "com.kingpaging.qwelcome"))
        assertFalse(updater.packageNameMatches("com.kingpaging.qwelcome", "com.example.other"))
    }

    private fun createUpdater(): GitHubAppUpdater {
        val context = mockk<Context>()
        val packageManager = mockk<PackageManager>(relaxed = true)
        val downloadManager = mockk<DownloadManager>(relaxed = true)

        every { context.applicationContext } returns context
        every { context.packageManager } returns packageManager
        every { context.packageName } returns "com.kingpaging.qwelcome"
        every { context.getSystemService(Context.DOWNLOAD_SERVICE) } returns downloadManager

        return GitHubAppUpdater(context)
    }
}
