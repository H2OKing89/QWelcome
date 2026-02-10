package com.kingpaging.qwelcome.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateCheckerTest {

    @Test
    fun `parseUpdateResponse with valid apk digest returns UpdateAvailable`() {
        val response = """
            {
              "tag_name": "v3.0.0",
              "html_url": "https://github.com/H2OKing89/QWelcome/releases/tag/v3.0.0",
              "body": "Release notes",
              "assets": [
                {
                  "name": "QWelcome-v3.0.0.apk",
                  "browser_download_url": "https://github.com/H2OKing89/QWelcome/releases/download/v3.0.0/QWelcome-v3.0.0.apk",
                  "size": 12345,
                  "digest": "sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
                }
              ]
            }
        """.trimIndent()

        val result = UpdateChecker.parseUpdateResponse(response, currentVersionName = "2.5.0")
        assertTrue(result is UpdateCheckResult.UpdateAvailable)

        result as UpdateCheckResult.UpdateAvailable
        assertEquals("3.0.0", result.latestVersion)
        assertEquals("QWelcome-v3.0.0.apk", result.assetName)
        assertEquals(12345L, result.assetSizeBytes)
        assertEquals("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", result.sha256Hex)
    }

    @Test
    fun `parseUpdateResponse missing digest returns Error`() {
        val response = """
            {
              "tag_name": "v3.0.0",
              "html_url": "https://github.com/H2OKing89/QWelcome/releases/tag/v3.0.0",
              "assets": [
                {
                  "name": "QWelcome-v3.0.0.apk",
                  "browser_download_url": "https://github.com/H2OKing89/QWelcome/releases/download/v3.0.0/QWelcome-v3.0.0.apk",
                  "size": 12345
                }
              ]
            }
        """.trimIndent()

        val result = UpdateChecker.parseUpdateResponse(response, currentVersionName = "2.5.0")
        assertTrue(result is UpdateCheckResult.Error)
        assertTrue((result as UpdateCheckResult.Error).message.contains("digest"))
    }

    @Test
    fun `parseUpdateResponse with untrusted host returns Error`() {
        val response = """
            {
              "tag_name": "v3.0.0",
              "html_url": "https://github.com/H2OKing89/QWelcome/releases/tag/v3.0.0",
              "assets": [
                {
                  "name": "QWelcome-v3.0.0.apk",
                  "browser_download_url": "https://evil.example.com/QWelcome-v3.0.0.apk",
                  "size": 12345,
                  "digest": "sha256:bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"
                }
              ]
            }
        """.trimIndent()

        val result = UpdateChecker.parseUpdateResponse(response, currentVersionName = "2.5.0")
        assertTrue(result is UpdateCheckResult.Error)
        assertTrue((result as UpdateCheckResult.Error).message.contains("Invalid release URL"))
    }

    @Test
    fun `parseUpdateResponse when already up to date returns UpToDate`() {
        val response = """
            {
              "tag_name": "v2.5.0",
              "html_url": "https://github.com/H2OKing89/QWelcome/releases/tag/v2.5.0",
              "assets": [
                {
                  "name": "QWelcome-v2.5.0.apk",
                  "browser_download_url": "https://github.com/H2OKing89/QWelcome/releases/download/v2.5.0/QWelcome-v2.5.0.apk",
                  "size": 12345,
                  "digest": "sha256:cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc"
                }
              ]
            }
        """.trimIndent()

        val result = UpdateChecker.parseUpdateResponse(response, currentVersionName = "2.5.0")
        assertTrue(result is UpdateCheckResult.UpToDate)
    }

    @Test
    fun `extractSha256Hex accepts valid hash and rejects malformed`() {
        val valid = UpdateChecker.extractSha256Hex(
            "sha256:dddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd"
        )
        val invalid = UpdateChecker.extractSha256Hex("sha256:not-a-hash")

        assertEquals("dddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd", valid)
        assertNull(invalid)
    }

    @Test
    fun `parseUpdateResponse with empty assets returns Error`() {
        val response = """
            {
              "tag_name": "v3.0.0",
              "html_url": "https://github.com/H2OKing89/QWelcome/releases/tag/v3.0.0",
              "body": "Release notes",
              "assets": []
            }
        """.trimIndent()

        val result = UpdateChecker.parseUpdateResponse(response, currentVersionName = "2.5.0")
        assertTrue(result is UpdateCheckResult.Error)
        assertTrue((result as UpdateCheckResult.Error).message.contains("no APK", ignoreCase = true))
    }

    @Test
    fun `parseUpdateResponse with only non-apk assets returns Error`() {
        val response = """
            {
              "tag_name": "v3.0.0",
              "html_url": "https://github.com/H2OKing89/QWelcome/releases/tag/v3.0.0",
              "body": "Release notes",
              "assets": [
                {
                  "name": "checksums.txt",
                  "browser_download_url": "https://github.com/H2OKing89/QWelcome/releases/download/v3.0.0/checksums.txt",
                  "size": 256
                },
                {
                  "name": "release-notes.md",
                  "browser_download_url": "https://github.com/H2OKing89/QWelcome/releases/download/v3.0.0/release-notes.md",
                  "size": 1024
                }
              ]
            }
        """.trimIndent()

        val result = UpdateChecker.parseUpdateResponse(response, currentVersionName = "2.5.0")
        assertTrue(result is UpdateCheckResult.Error)
        assertTrue((result as UpdateCheckResult.Error).message.contains("no APK", ignoreCase = true))
    }
}
