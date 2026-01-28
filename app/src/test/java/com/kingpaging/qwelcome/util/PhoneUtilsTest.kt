package com.kingpaging.qwelcome.util

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for [PhoneUtils] - US phone number validation and normalization.
 *
 * Tests cover NANP (North American Numbering Plan) validation rules:
 * - 10-digit format: NXX-NXX-XXXX (area code + exchange + subscriber)
 * - 11-digit format: 1-NXX-NXX-XXXX (country code + above)
 * - N must be 2-9 (cannot start with 0 or 1)
 * 
 * Note: Test phone numbers use valid NANP format (area code and exchange start with 2-9)
 */
class PhoneUtilsTest {

    // ========== isValid() Tests ==========

    @Test
    fun `isValid returns true for valid 10-digit number`() {
        assertTrue(PhoneUtils.isValid("2125551234"))
    }

    @Test
    fun `isValid returns true for valid 10-digit with dashes`() {
        assertTrue(PhoneUtils.isValid("212-555-1234"))
    }

    @Test
    fun `isValid returns true for valid 10-digit with parentheses`() {
        assertTrue(PhoneUtils.isValid("(212) 555-1234"))
    }

    @Test
    fun `isValid returns true for valid 10-digit with dots`() {
        assertTrue(PhoneUtils.isValid("212.555.1234"))
    }

    @Test
    fun `isValid returns true for valid 10-digit with spaces`() {
        assertTrue(PhoneUtils.isValid("212 555 1234"))
    }

    @Test
    fun `isValid returns true for valid 11-digit with country code`() {
        assertTrue(PhoneUtils.isValid("12125551234"))
    }

    @Test
    fun `isValid returns true for valid 11-digit with country code and dashes`() {
        assertTrue(PhoneUtils.isValid("1-212-555-1234"))
    }

    @Test
    fun `isValid returns false for area code starting with 0`() {
        assertFalse(PhoneUtils.isValid("012-555-1234"))
    }

    @Test
    fun `isValid returns false for area code starting with 1`() {
        assertFalse(PhoneUtils.isValid("155-555-1234"))
    }

    @Test
    fun `isValid returns false for exchange starting with 0`() {
        assertFalse(PhoneUtils.isValid("212-012-1234"))
    }

    @Test
    fun `isValid returns false for exchange starting with 1`() {
        assertFalse(PhoneUtils.isValid("212-123-1234"))
    }

    @Test
    fun `isValid returns false for 11-digit not starting with 1`() {
        assertFalse(PhoneUtils.isValid("22125551234"))
    }

    @Test
    fun `isValid returns false for too few digits`() {
        assertFalse(PhoneUtils.isValid("212-555-123"))
        assertFalse(PhoneUtils.isValid("21255512"))
        assertFalse(PhoneUtils.isValid(""))
    }

    @Test
    fun `isValid returns false for too many digits`() {
        assertFalse(PhoneUtils.isValid("212-555-12345"))
        assertFalse(PhoneUtils.isValid("121255512345"))
    }

    @Test
    fun `isValid handles edge case area codes`() {
        // Area codes 200-999 are valid (first digit 2-9)
        assertTrue(PhoneUtils.isValid("200-234-5678"))
        assertTrue(PhoneUtils.isValid("999-234-5678"))
        
        // Area codes starting with 0 or 1 are invalid
        assertFalse(PhoneUtils.isValid("000-234-5678"))
        assertFalse(PhoneUtils.isValid("100-234-5678"))
    }

    @Test
    fun `isValid handles edge case exchanges`() {
        // Exchanges 200-999 are valid (first digit 2-9)
        assertTrue(PhoneUtils.isValid("212-200-5678"))
        assertTrue(PhoneUtils.isValid("212-999-5678"))
        
        // Exchanges starting with 0 or 1 are invalid
        assertFalse(PhoneUtils.isValid("212-000-5678"))
        assertFalse(PhoneUtils.isValid("212-100-5678"))
    }

    // ========== normalize() Tests ==========

    @Test
    fun `normalize returns E164 format for valid 10-digit`() {
        assertEquals("+12125551234", PhoneUtils.normalize("2125551234"))
    }

    @Test
    fun `normalize returns E164 format for valid 10-digit with formatting`() {
        assertEquals("+12125551234", PhoneUtils.normalize("212-555-1234"))
        assertEquals("+12125551234", PhoneUtils.normalize("(212) 555-1234"))
        assertEquals("+12125551234", PhoneUtils.normalize("212.555.1234"))
    }

    @Test
    fun `normalize returns E164 format for valid 11-digit`() {
        assertEquals("+12125551234", PhoneUtils.normalize("12125551234"))
        assertEquals("+12125551234", PhoneUtils.normalize("1-212-555-1234"))
    }

    @Test
    fun `normalize returns null for invalid numbers`() {
        assertNull(PhoneUtils.normalize("012-555-1234"))  // Invalid area code
        assertNull(PhoneUtils.normalize("212-012-4567"))  // Invalid exchange
        assertNull(PhoneUtils.normalize("22125551234"))   // Invalid country code (2 instead of 1)
        assertNull(PhoneUtils.normalize("123456"))        // Too short
        assertNull(PhoneUtils.normalize(""))              // Empty
    }

    @Test
    fun `normalize strips all non-digit characters`() {
        assertEquals("+12125551234", PhoneUtils.normalize("(212) 555-1234"))
        assertEquals("+12125551234", PhoneUtils.normalize("+1 212 555 1234"))
        assertEquals("+12125551234", PhoneUtils.normalize("1.212.555.1234"))
    }

    // ========== Real-world Format Tests ==========

    @Test
    fun `isValid accepts common real-world formats`() {
        // Common US phone number formats with valid area codes
        assertTrue(PhoneUtils.isValid("(800) 234-5678"))   // Toll-free
        assertTrue(PhoneUtils.isValid("212-234-5678"))     // NYC area code
        assertTrue(PhoneUtils.isValid("415.234.5678"))     // SF area code with dots
        assertTrue(PhoneUtils.isValid("1 (800) 234-5678")) // With country code
    }

    @Test
    fun `isValid rejects international numbers`() {
        // UK number - after stripping non-digits becomes "442079460958" (12 digits)
        // which fails due to incorrect digit count (not 10 or 11)
        assertFalse(PhoneUtils.isValid("+44 20 7946 0958"))
        // 12 digits - too many digits to be a valid US number
        assertFalse(PhoneUtils.isValid("442079460958"))
    }
}
