package com.kingpaging.qwelcome.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class FileNameSanitizerTest {

    @Test
    fun `replaces unsafe characters and path traversal segments`() {
        assertEquals("WiFi_QR_Customer_Network___.png", sanitizeFileName("WiFi QR/Customer Network/../.png"))

        listOf("", " ", ".", "...").forEach { invalidName ->
            assertEquals(FALLBACK_FILE_NAME, sanitizeFileName(invalidName))
        }

        assertEquals(FALLBACK_FILE_NAME, sanitizeFileName("."))
        assertNotEquals("..", sanitizeFileName(".."))
    }
}
