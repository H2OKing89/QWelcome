package com.kingpaging.qwelcome.data

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class PendingUpdateDownloadTest {
    @Test
    fun `download persistence retains exact verification metadata`() {
        val pending =
            PendingUpdateDownload(
                downloadId = 42L,
                apkPath = "/updates/app.apk",
                update =
                    UpdateCheckResult.UpdateAvailable(
                        latestVersion = "3.0.0",
                        downloadUrl = "https://github.com/H2OKing89/QWelcome/releases/download/v3.0.0/app.apk",
                        releaseNotes = "Quoted \"notes\"\nand another line",
                        assetName = "app.apk",
                        assetSizeBytes = 123L,
                        sha256Hex = "a".repeat(64),
                    ),
            )

        val encoded = Json.encodeToString(pending)

        assertEquals(pending, Json.decodeFromString<PendingUpdateDownload>(encoded))
    }
}
