package com.example.allowelcome.util

object PhoneUtils {
    /** Regex to strip non-digit characters */
    private val nonDigitRegex = Regex("\\D")

    /**
     * Normalizes a valid US/Canada phone number to E.164 format (+1XXXXXXXXXX).
     * Returns null if the phone number is invalid.
     *
     * @param phone The phone number to normalize
     * @return E.164 formatted number, or null if invalid
     */
    fun normalize(phone: String): String? {
        if (!isValid(phone)) return null
        
        val digits = phone.replace(nonDigitRegex, "")

        return when (digits.length) {
            // US 10-digit number - prepend +1
            10 -> "+1$digits"
            // Already has country code (11 digits, starts with 1)
            11 -> "+$digits"
            // Should not reach here due to isValid check
            else -> null
        }
    }

    /**
     * Validates a phone number (US/Canada format)
     * - Must be 10 or 11 digits
     * - Area code cannot start with 0 or 1
     * - Exchange code cannot start with 0 or 1
     */
    fun isValid(phone: String): Boolean {
        val digits = phone.replace(nonDigitRegex, "")

        return when (digits.length) {
            10 -> {
                // Format: NXXNXXXXXX (area code + exchange + line)
                // Area code (NXX): first digit 2-9, second and third any digit
                // Exchange code (NXX): first digit 2-9, second and third any digit
                val areaCode = digits.substring(0, 3)
                val exchange = digits.substring(3, 6)
                areaCode[0] in '2'..'9' && exchange[0] in '2'..'9'
            }
            11 -> {
                // Must start with 1, then same validation as 10-digit
                if (digits[0] != '1') return false
                val areaCode = digits.substring(1, 4)
                val exchange = digits.substring(4, 7)
                areaCode[0] in '2'..'9' && exchange[0] in '2'..'9'
            }
            else -> false
        }
    }
}
