package com.kingpaging.qwelcome.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class UpdateDownloadStoreTest {
    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun recordSurvivesStoreRecreationAndCanBeCleared() {
        val file = File(folder.root, "update.json")
        val expected =
            PendingUpdateDownload(
                downloadId = 42L,
                apkPath = "/downloads/updates/app.apk",
                update =
                    UpdateCheckResult.UpdateAvailable(
                        latestVersion = "3.0.0",
                        downloadUrl = "https://github.com/H2OKing89/QWelcome/releases/download/v3.0.0/app.apk",
                        releaseNotes = "notes",
                        assetName = "app.apk",
                        assetSizeBytes = 123L,
                        sha256Hex = "a".repeat(64),
                    ),
            )
        FileUpdateDownloadStore(file).write(expected)

        val recreated = FileUpdateDownloadStore(file)
        assertEquals(expected, recreated.read())
        recreated.clear()
        assertNull(FileUpdateDownloadStore(file).read())
    }

    @Test
    fun malformedRecordIsDiscarded() {
        val file = folder.newFile("malformed.json")
        file.writeText("incomplete record")

        assertNull(FileUpdateDownloadStore(file).read())
        assertNull(FileUpdateDownloadStore(file).read())
    }
}
