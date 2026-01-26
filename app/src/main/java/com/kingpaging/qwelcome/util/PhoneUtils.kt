package com.kingpaging.qwelcome.util

/**
 * Utility object for phone number validation and normalization.
 *
 * ============================================================================
 * IMPORTANT: US-ONLY PHONE NUMBER SUPPORT
 * ============================================================================
 * This utility is designed EXCLUSIVELY for United States phone numbers
 * following the North American Numbering Plan (NANP).
 *
 * Supported formats:
 * - 10-digit: (XXX) XXX-XXXX, XXX-XXX-XXXX, XXXXXXXXXX
 * - 11-digit: 1-XXX-XXX-XXXX, 1XXXXXXXXXX (with US country code)
 *
 * NOT supported:
 * - International numbers (non-US country codes)
 * - Canadian numbers (even though they share NANP, this app targets US only)
 * - Toll-free numbers validation (though format may pass)
 *
 * If international support is needed in the future, consider using
 * Google's libphonenumber library instead.
 * ============================================================================
 */
object PhoneUtils {
    /** Regex to strip non-digit characters (parentheses, dashes, spaces, etc.) */
    private val nonDigitRegex = Regex("\\D")

    /**
     * Normalizes a valid US phone number to E.164 format (+1XXXXXXXXXX).
     * Returns null if the phone number is invalid.
     *
     * NOTE: Only US phone numbers are supported. See class documentation.
     *
     * @param phone The US phone number to normalize (any common format)
     * @return E.164 formatted number (+1XXXXXXXXXX), or null if invalid
     *
     * Examples:
     * - "555-123-4567" -> "+15551234567"
     * - "(555) 123-4567" -> "+15551234567"
     * - "1-555-123-4567" -> "+15551234567"
     * - "123-456-7890" -> null (invalid: area code starts with 1)
     */
    fun normalize(phone: String): String? {
        if (!isValid(phone)) return null
        
        val digits = phone.replace(nonDigitRegex, "")

        return when (digits.length) {
            // US 10-digit number - prepend +1 country code
            10 -> "+1$digits"
            // Already has US country code (11 digits, starts with 1)
            11 -> "+$digits"
            // Should not reach here due to isValid check
            else -> null
        }
    }

    /**
     * Validates a US phone number format.
     *
     * NOTE: Only US phone numbers are supported. See class documentation.
     *
     * Validation rules per NANP (North American Numbering Plan):
     * - Must be 10 or 11 digits total
     * - If 11 digits, must start with country code '1'
     * - Area code (NXX): First digit must be 2-9 (N), followed by any two digits (XX)
     * - Exchange code (NXX): First digit must be 2-9 (N), followed by any two digits (XX)
     * - Subscriber number: Any four digits (XXXX)
     *
     * @param phone The phone number to validate (digits, dashes, parentheses, spaces allowed)
     * @return true if valid US phone number format, false otherwise
     *
     * Examples:
     * - "555-123-4567" -> true (valid)
     * - "055-123-4567" -> false (area code starts with 0)
     * - "555-023-4567" -> false (exchange starts with 0)
     * - "+44 20 7946 0958" -> false (UK number, not US)
     */
    fun isValid(phone: String): Boolean {
        val digits = phone.replace(nonDigitRegex, "")

        return when (digits.length) {
            // 10-digit US number format: NXX-NXX-XXXX
            10 -> {
                // Area code (NXX): first digit 2-9, second and third any digit
                // Exchange code (NXX): first digit 2-9, second and third any digit
                val areaCode = digits.substring(0, 3)
                val exchange = digits.substring(3, 6)
                areaCode[0] in '2'..'9' && exchange[0] in '2'..'9'
            }
            // 11-digit US number format: 1-NXX-NXX-XXXX
            11 -> {
                // Must start with US country code '1'
                if (digits[0] != '1') return false
                val areaCode = digits.substring(1, 4)
                val exchange = digits.substring(4, 7)
                areaCode[0] in '2'..'9' && exchange[0] in '2'..'9'
            }
            // Any other length is invalid for US numbers
            else -> false
        }
    }
}
