package com.kingpaging.qwelcome.data

import com.kingpaging.qwelcome.util.WifiQrGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class WifiQrImageRequestTest {
    @Test
    fun `toString redacts passwords and retains other request fields for every security mode`() {
        val password = "private-passphrase-123"
        WifiQrGenerator.SecurityType.entries.forEach { securityType ->
            val request =
                WifiQrImageRequest(
                    ssid = "Office",
                    password = password,
                    isOpenNetwork = securityType == WifiQrGenerator.SecurityType.OPEN,
                    securityType = securityType,
                    isHiddenNetwork = true,
                )

            assertFalse(request.toString().contains(password))
            assertEquals(
                "WifiQrImageRequest(ssid=Office, password=<redacted>, " +
                    "isOpenNetwork=${request.isOpenNetwork}, securityType=$securityType, isHiddenNetwork=true)",
                request.toString(),
            )
        }
    }
}
