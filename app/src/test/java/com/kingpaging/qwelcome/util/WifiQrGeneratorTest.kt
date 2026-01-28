package com.kingpaging.qwelcome.util

import com.kingpaging.qwelcome.R
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for [WifiQrGenerator] - WiFi QR code string generation and validation.
 *
 * Tests cover:
 * - Password validation (WPA/WPA2 requirements: 8-63 characters)
 * - SSID validation (IEEE 802.11: max 32 bytes UTF-8)
 * - WiFi string generation with proper escaping
 */
class WifiQrGeneratorTest {

    // ========== Password Validation Tests ==========

    @Test
    fun `validatePassword returns Success for valid 8-char password`() {
        val result = WifiQrGenerator.validatePassword("12345678")
        assertEquals(WifiQrGenerator.ValidationResult.Success, result)
    }

    @Test
    fun `validatePassword returns Success for valid 63-char password`() {
        val password = "a".repeat(63)
        val result = WifiQrGenerator.validatePassword(password)
        assertEquals(WifiQrGenerator.ValidationResult.Success, result)
    }

    @Test
    fun `validatePassword returns Error for empty password`() {
        val result = WifiQrGenerator.validatePassword("")
        assertTrue(result is WifiQrGenerator.ValidationResult.Error)
        assertEquals(R.string.error_password_empty, (result as WifiQrGenerator.ValidationResult.Error).messageResId)
    }

    @Test
    fun `validatePassword returns Error for blank password`() {
        val result = WifiQrGenerator.validatePassword("   ")
        assertTrue(result is WifiQrGenerator.ValidationResult.Error)
        assertEquals(R.string.error_password_empty, (result as WifiQrGenerator.ValidationResult.Error).messageResId)
    }

    @Test
    fun `validatePassword returns Error for password under 8 chars`() {
        val result = WifiQrGenerator.validatePassword("1234567")
        assertTrue(result is WifiQrGenerator.ValidationResult.Error)
        assertEquals(R.string.error_password_too_short, (result as WifiQrGenerator.ValidationResult.Error).messageResId)
    }

    @Test
    fun `validatePassword returns Error for password over 63 chars`() {
        val password = "a".repeat(64)
        val result = WifiQrGenerator.validatePassword(password)
        assertTrue(result is WifiQrGenerator.ValidationResult.Error)
        assertEquals(R.string.error_password_too_long, (result as WifiQrGenerator.ValidationResult.Error).messageResId)
    }

    @Test
    fun `validatePassword allows special characters`() {
        val result = WifiQrGenerator.validatePassword("P@ssw0rd!")
        assertEquals(WifiQrGenerator.ValidationResult.Success, result)
    }

    @Test
    fun `validatePassword allows purely numeric passwords`() {
        val result = WifiQrGenerator.validatePassword("12345678")
        assertEquals(WifiQrGenerator.ValidationResult.Success, result)
    }

    // ========== SSID Validation Tests ==========

    @Test
    fun `validateSsid returns Success for valid ASCII SSID`() {
        val result = WifiQrGenerator.validateSsid("MyNetwork")
        assertEquals(WifiQrGenerator.ValidationResult.Success, result)
    }

    @Test
    fun `validateSsid returns Success for 32-byte ASCII SSID`() {
        val ssid = "a".repeat(32) // 32 ASCII chars = 32 bytes
        val result = WifiQrGenerator.validateSsid(ssid)
        assertEquals(WifiQrGenerator.ValidationResult.Success, result)
    }

    @Test
    fun `validateSsid returns Error for empty SSID`() {
        val result = WifiQrGenerator.validateSsid("")
        assertTrue(result is WifiQrGenerator.ValidationResult.Error)
        assertEquals(R.string.error_ssid_empty, (result as WifiQrGenerator.ValidationResult.Error).messageResId)
    }

    @Test
    fun `validateSsid returns Error for blank SSID`() {
        val result = WifiQrGenerator.validateSsid("   ")
        assertTrue(result is WifiQrGenerator.ValidationResult.Error)
        assertEquals(R.string.error_ssid_empty, (result as WifiQrGenerator.ValidationResult.Error).messageResId)
    }

    @Test
    fun `validateSsid returns Error for SSID over 32 bytes`() {
        val ssid = "a".repeat(33) // 33 ASCII chars = 33 bytes
        val result = WifiQrGenerator.validateSsid(ssid)
        assertTrue(result is WifiQrGenerator.ValidationResult.Error)
        assertEquals(R.string.error_ssid_too_long, (result as WifiQrGenerator.ValidationResult.Error).messageResId)
    }

    @Test
    fun `validateSsid counts bytes not characters for emoji`() {
        // 🏠 is 4 bytes in UTF-8
        // 8 emoji = 32 bytes (valid)
        val ssid8Emoji = "🏠".repeat(8)
        assertEquals(32, ssid8Emoji.toByteArray(Charsets.UTF_8).size)
        assertEquals(WifiQrGenerator.ValidationResult.Success, WifiQrGenerator.validateSsid(ssid8Emoji))
        
        // 9 emoji = 36 bytes (invalid)
        val ssid9Emoji = "🏠".repeat(9)
        assertEquals(36, ssid9Emoji.toByteArray(Charsets.UTF_8).size)
        assertTrue(WifiQrGenerator.validateSsid(ssid9Emoji) is WifiQrGenerator.ValidationResult.Error)
    }

    @Test
    fun `validateSsid counts bytes not characters for CJK`() {
        // Chinese characters are 3 bytes each in UTF-8
        // 10 chars = 30 bytes (valid)
        val ssid10Chars = "中文网络名称测试用的"
        assertEquals(30, ssid10Chars.toByteArray(Charsets.UTF_8).size)
        assertEquals(WifiQrGenerator.ValidationResult.Success, WifiQrGenerator.validateSsid(ssid10Chars))
        
        // 11 chars = 33 bytes (invalid)
        val ssid11Chars = "中文网络名称测试用的一"
        assertEquals(33, ssid11Chars.toByteArray(Charsets.UTF_8).size)
        assertTrue(WifiQrGenerator.validateSsid(ssid11Chars) is WifiQrGenerator.ValidationResult.Error)
    }

    // ========== WiFi String Generation Tests ==========

    @Test
    fun `generateWifiString produces correct format`() {
        val result = WifiQrGenerator.generateWifiString("MyNetwork", "password123")
        assertEquals("WIFI:T:WPA;S:MyNetwork;P:password123;;", result)
    }

    @Test
    fun `generateWifiString escapes semicolons`() {
        val result = WifiQrGenerator.generateWifiString("My;Network", "pass;word")
        assertEquals("WIFI:T:WPA;S:My\\;Network;P:pass\\;word;;", result)
    }

    @Test
    fun `generateWifiString escapes commas`() {
        val result = WifiQrGenerator.generateWifiString("My,Network", "pass,word")
        assertEquals("WIFI:T:WPA;S:My\\,Network;P:pass\\,word;;", result)
    }

    @Test
    fun `generateWifiString escapes backslashes`() {
        val result = WifiQrGenerator.generateWifiString("My\\Network", "pass\\word")
        assertEquals("WIFI:T:WPA;S:My\\\\Network;P:pass\\\\word;;", result)
    }

    @Test
    fun `generateWifiString escapes double quotes`() {
        val result = WifiQrGenerator.generateWifiString("My\"Network", "pass\"word")
        assertEquals("WIFI:T:WPA;S:My\\\"Network;P:pass\\\"word;;", result)
    }

    @Test
    fun `generateWifiString escapes multiple special characters`() {
        val result = WifiQrGenerator.generateWifiString("Net;work,name", "p\\a\"ss;word")
        assertEquals("WIFI:T:WPA;S:Net\\;work\\,name;P:p\\\\a\\\"ss\\;word;;", result)
    }

    @Test
    fun `generateWifiString throws for invalid SSID`() {
        assertThrows(IllegalArgumentException::class.java) {
            WifiQrGenerator.generateWifiString("", "password123")
        }
    }

    @Test
    fun `generateWifiString throws for invalid password`() {
        assertThrows(IllegalArgumentException::class.java) {
            WifiQrGenerator.generateWifiString("MyNetwork", "short")
        }
    }

    @Test
    fun `generateWifiStringUnchecked skips validation`() {
        // This would fail validation but unchecked version allows it
        val result = WifiQrGenerator.generateWifiStringUnchecked("", "short")
        assertEquals("WIFI:T:WPA;S:;P:short;;", result)
    }

    // ========== Constants Tests ==========

    @Test
    fun `constants have expected values`() {
        assertEquals(8, WifiQrGenerator.MIN_PASSWORD_LENGTH)
        assertEquals(63, WifiQrGenerator.MAX_PASSWORD_LENGTH)
        assertEquals(32, WifiQrGenerator.MAX_SSID_LENGTH_BYTES)
    }
}
