package com.kingpaging.qwelcome.util

import org.junit.Assert.assertEquals
import org.junit.Test

class FileNameSanitizerTest {

    @Test
    fun `replaces unsafe characters and path traversal segments`() {
        assertEquals("WiFi_QR_Customer_Network___.png", sanitizeFileName("WiFi QR/Customer Network/../.png"))
    }
}
