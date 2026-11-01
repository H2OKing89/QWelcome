package com.kingpaging.qwelcome.ui

import com.kingpaging.qwelcome.util.WifiQrGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomerIntakeUiStateTest {
    @Test
    fun `all caps customer names are formatted for messages`() {
        val names =
            mapOf(
                "QUENTIN KING" to "Quentin King",
                "MARY-JANE WATSON" to "Mary-Jane Watson",
                "JOHN O'NEIL" to "John O'Neil",
                "SEÁN O’CONNOR" to "Seán O’Connor",
                "  JANE\tDOE  " to "  Jane\tDoe  ",
            )

        names.forEach { (entered, expected) ->
            val state = CustomerIntakeUiState(customerName = entered)

            assertEquals(expected, state.toCustomerData().customerName)
            assertEquals(entered, state.customerName)
        }
    }

    @Test
    fun `names with explicit casing or no uppercase letters keep their spelling`() {
        listOf("McDonald O'Neil", "van der Waals", "alice", "李明", "", "   ").forEach { name ->
            assertEquals(name, CustomerIntakeUiState(customerName = name).toCustomerData().customerName)
        }
    }

    @Test
    fun `QR eligibility checks credentials even when no field errors are displayed`() {
        val valid = CustomerIntakeUiState(ssid = "WiFi", password = "password123")
        assertTrue(valid.qrEnabled)
        assertFalse(valid.copy(password = "a".repeat(64)).qrEnabled)
        assertFalse(valid.copy(password = " ".repeat(8)).qrEnabled)
        assertFalse(valid.copy(password = "short").qrEnabled)
        assertFalse(valid.copy(ssid = "界".repeat(11)).qrEnabled)
        assertTrue(valid.copy(password = "", securityType = WifiQrGenerator.SecurityType.OPEN).qrEnabled)
        assertFalse(valid.copy(ssid = "", securityType = WifiQrGenerator.SecurityType.OPEN).qrEnabled)
    }

    @Test
    fun `default security is mixed personal and open state follows the selected mode`() {
        assertEquals(
            WifiQrGenerator.SecurityType.WPA2_WPA3_PERSONAL,
            CustomerIntakeUiState().securityType,
        )
        WifiQrGenerator.SecurityType.entries.forEach { mode ->
            val state = CustomerIntakeUiState(ssid = "WiFi", password = "password123", securityType = mode)
            val isOpen = mode == WifiQrGenerator.SecurityType.OPEN
            assertEquals(isOpen, state.isOpenNetwork)
            assertTrue(state.qrEnabled)
            assertEquals(isOpen, state.copy(password = "").qrEnabled)
            assertEquals(isOpen, state.copy(passwordError = "Invalid password").isValid)
            assertFalse(state.copy(ssid = "").qrEnabled)
        }
    }

    @Test
    fun `name formatting preserves other customer fields`() {
        val data =
            CustomerIntakeUiState(
                customerName = "JANE DOE",
                customerPhone = "2125551234",
                ssid = "UPPERCASE_WIFI",
                password = "SECRET_password",
                accountNumber = "ACC-001",
                securityType = WifiQrGenerator.SecurityType.OPEN,
            ).toCustomerData()

        assertEquals("Jane Doe", data.customerName)
        assertEquals("2125551234", data.customerPhone)
        assertEquals("UPPERCASE_WIFI", data.ssid)
        assertEquals("SECRET_password", data.password)
        assertEquals("ACC-001", data.accountNumber)
        assertEquals(true, data.isOpenNetwork)
    }
}
