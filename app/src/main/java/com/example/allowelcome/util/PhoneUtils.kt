package com.example.allowelcome.util

object PhoneUtils {
    fun normalize(phone: String): String {
        val digits = phone.replace(Regex("\\D"), "")

        return when {
            // Already has country code (11 digits, starts with 1)
            digits.startsWith("1") && digits.length == 11 -> "+$digits"
            // US 10-digit number
            digits.length == 10 -> "+1$digits"
            // Already formatted with + (strip spaces/dashes, keep digits only)
            phone.trim().startsWith("+") -> "+$digits"
            // Fallback
            else -> "+$digits"
        }
    }

    fun isValid(phone: String): Boolean {
        val digits = phone.replace(Regex("\\D"), "")
        return digits.length in 10..15
    }
}
