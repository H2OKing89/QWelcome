package com.example.allowelcome.util

/**
 * Generates WiFi QR code strings in the standard WIFI: URI format.
 * 
 * The generated string can be encoded into a QR code that, when scanned,
 * allows devices to automatically connect to the WiFi network.
 */
object WifiQrGenerator {
    
    /** Minimum password length for WPA/WPA2 (8 characters) */
    const val MIN_PASSWORD_LENGTH = 8
    
    /** Maximum password length for WPA/WPA2 (63 characters for passphrase) */
    const val MAX_PASSWORD_LENGTH = 63
    
    /** Maximum SSID length (32 bytes, but we use chars for simplicity) */
    const val MAX_SSID_LENGTH = 32
    
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
     * @return A [ValidationResult] indicating success or failure with error message
     */
    fun validatePassword(password: String): ValidationResult {
        return when {
            password.isBlank() -> ValidationResult.Error("Password cannot be empty")
            password.length < MIN_PASSWORD_LENGTH -> ValidationResult.Error(
                "Password must be at least $MIN_PASSWORD_LENGTH characters"
            )
            password.length > MAX_PASSWORD_LENGTH -> ValidationResult.Error(
                "Password cannot exceed $MAX_PASSWORD_LENGTH characters"
            )
            else -> ValidationResult.Success
        }
    }
    
    /**
     * Validates an SSID (network name).
     * 
     * @param ssid The SSID to validate
     * @return A [ValidationResult] indicating success or failure with error message
     */
    fun validateSsid(ssid: String): ValidationResult {
        return when {
            ssid.isBlank() -> ValidationResult.Error("SSID cannot be empty")
            ssid.length > MAX_SSID_LENGTH -> ValidationResult.Error(
                "SSID cannot exceed $MAX_SSID_LENGTH characters"
            )
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
        // Validate inputs
        validateSsid(ssid).let { 
            if (it is ValidationResult.Error) throw IllegalArgumentException(it.message)
        }
        validatePassword(password).let {
            if (it is ValidationResult.Error) throw IllegalArgumentException(it.message)
        }
        
        val escapedSsid = escapeWifiString(ssid)
        val escapedPassword = escapeWifiString(password)
        return "WIFI:T:WPA;S:$escapedSsid;P:$escapedPassword;;"
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
     */
    sealed class ValidationResult {
        object Success : ValidationResult()
        data class Error(val message: String) : ValidationResult()
    }
}
