package com.kingpaging.qwelcome.util

import androidx.annotation.StringRes
import com.kingpaging.qwelcome.R

/**
 * Generates WiFi QR code strings in the standard WIFI: URI format.
 * 
 * The generated string can be encoded into a QR code that, when scanned,
 * allows devices to automatically connect to the WiFi network.
 */
object WifiQrGenerator {
    
    /** Minimum password length for WPA/WPA2 (8 characters) */
    const val MIN_PASSWORD_LENGTH = WIFI_MIN_PASSWORD_LENGTH
    
    /** Maximum password length for WPA/WPA2 (63 characters for passphrase) */
    const val MAX_PASSWORD_LENGTH = WIFI_MAX_PASSWORD_LENGTH
    
    /** 
     * Maximum SSID length in bytes (per IEEE 802.11 specification).
     * Note: UTF-8 encoded SSIDs with multi-byte characters may have fewer
     * visible characters than this limit.
     */
    const val MAX_SSID_LENGTH_BYTES = WIFI_MAX_SSID_LENGTH_BYTES
    
    /**
     * Validates a WiFi password according to WPA/WPA2 requirements.
     * 
     * WPA/WPA2 passwords must be:
     * - At least 8 characters long
     * - No more than 63 characters (for passphrase mode)
     * - Can be purely numeric (e.g., "12345678")
     * - Can contain any printable ASCII characters
     * 
     * @param password The password to validate
     * @return A [ValidationResult] indicating success or failure with string resource ID
     */
    fun validatePassword(password: String): ValidationResult {
        return when {
            password.isBlank() -> ValidationResult.Error(R.string.error_password_empty)
            WifiValidationRules.isPasswordTooShort(password) -> ValidationResult.Error(R.string.error_password_too_short)
            WifiValidationRules.isPasswordTooLong(password) -> ValidationResult.Error(R.string.error_password_too_long)
            else -> ValidationResult.Success
        }
    }
    
    /**
     * Validates an SSID (network name).
     * 
     * Per IEEE 802.11, SSIDs are limited to 32 bytes. This validation checks
     * the UTF-8 encoded byte length, not character count, to properly handle
     * multi-byte characters (e.g., emoji, CJK characters).
     * 
     * @param ssid The SSID to validate
     * @return A [ValidationResult] indicating success or failure with string resource ID
     */
    fun validateSsid(ssid: String): ValidationResult {
        return when {
            ssid.isBlank() -> ValidationResult.Error(R.string.error_ssid_empty)
            WifiValidationRules.isSsidTooLong(ssid) ->
                ValidationResult.Error(R.string.error_ssid_too_long)
            else -> ValidationResult.Success
        }
    }
    
    /**
     * Generates a string formatted for Wi-Fi QR codes.
     * Assumes WPA/WPA2 encryption (most common for home/business networks).
     * 
     * Special characters must be escaped in order: \ first, then ; , and "
     * 
     * Format: WIFI:T:WPA;S:<ssid>;P:<password>;;
     * 
     * @param ssid The network name (will be escaped)
     * @param password The network password (will be escaped)
     * @return The WiFi URI string for QR code encoding
     * @throws IllegalArgumentException if ssid or password are invalid
     */
    fun generateWifiString(ssid: String, password: String): String {
        // Validate inputs - throw with generic message since we don't have context for string resolution
        validateSsid(ssid).let { 
            if (it is ValidationResult.Error) throw IllegalArgumentException("Invalid SSID")
        }
        validatePassword(password).let {
            if (it is ValidationResult.Error) throw IllegalArgumentException("Invalid password")
        }
        
        val escapedSsid = escapeWifiString(ssid)
        val escapedPassword = escapeWifiString(password)
        return "WIFI:T:WPA;S:$escapedSsid;P:$escapedPassword;;"
    }

    /**
     * Generates a WiFi QR string for open networks (no password required).
     *
     * Format: WIFI:T:nopass;S:<ssid>;;
     *
     * @param ssid The network name (will be escaped)
     * @return The WiFi URI string for QR code encoding
     * @throws IllegalArgumentException if ssid is invalid
     */
    fun generateOpenNetworkString(ssid: String): String {
        validateSsid(ssid).let {
            if (it is ValidationResult.Error) throw IllegalArgumentException("Invalid SSID")
        }

        val escapedSsid = escapeWifiString(ssid)
        return "WIFI:T:nopass;S:$escapedSsid;;"
    }

    /**
     * Generates a WiFi string without validation (for cases where validation
     * has already been performed elsewhere).
     */
    fun generateWifiStringUnchecked(ssid: String, password: String): String {
        val escapedSsid = escapeWifiString(ssid)
        val escapedPassword = escapeWifiString(password)
        return "WIFI:T:WPA;S:$escapedSsid;P:$escapedPassword;;"
    }
    
    /**
     * Escapes special characters in WiFi QR code strings.
     * 
     * Per the WiFi QR code specification, the following characters must be escaped:
     * - Backslash (\) -> \\
     * - Semicolon (;) -> \;
     * - Comma (,) -> \,
     * - Double quote (") -> \"
     * 
     * Order matters: escape backslash first to avoid double-escaping.
     */
    private fun escapeWifiString(value: String): String {
        return value
            .replace("\\", "\\\\")
            .replace(";", "\\;")
            .replace(",", "\\,")
            .replace("\"", "\\\"")
    }
    
    /**
     * Result of validating WiFi credentials.
     * 
     * Use [Error.messageResId] with `context.getString()` to get the localized error message.
     */
    sealed class ValidationResult {
        object Success : ValidationResult()
        
        /**
         * Validation failed with an error.
         * @param messageResId String resource ID for the localized error message.
         *                     Use `context.getString(messageResId)` to resolve.
         */
        data class Error(@param:StringRes val messageResId: Int) : ValidationResult()
    }
}
