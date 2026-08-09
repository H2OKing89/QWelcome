package com.kingpaging.qwelcome.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FileNameSanitizerTest {

    @Test
    fun `replaces unsafe characters and path traversal segments`() {
        assertEquals("WiFi_QR_Customer_Network___.png", sanitizeFileName("WiFi QR/Customer Network/../.png"))

        listOf("", " ", ".", "...").forEach { invalidName ->
            assertEquals(FALLBACK_FILE_NAME, sanitizeFileName(invalidName))
        }

        assertEquals(FALLBACK_FILE_NAME, sanitizeFileName("."))
        assertEquals(FALLBACK_FILE_NAME, sanitizeFileName(".."))
    }

    @Test
    fun `caps sanitized file name length while preserving extension`() {
        val longName = "a".repeat(400) + ".png"

        val result = sanitizeFileName(longName)

        assertTrue(result.length <= 255)
        assertTrue(result.endsWith(".png"))
    }

    @Test
    fun `caps sanitized file name length even when extension exceeds the limit`() {
        val longName = "a".repeat(10) + "." + "b".repeat(400)

        val result = sanitizeFileName(longName)

        assertTrue(result.length <= 255)
    }
}
