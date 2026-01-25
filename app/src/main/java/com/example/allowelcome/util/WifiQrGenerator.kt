package com.example.allowelcome.util

object WifiQrGenerator {
    /**
     * Generates a string formatted for Wi-Fi QR codes.
     * Assumes WPA/WPA2 encryption.
     * 
     * Special characters must be escaped in order: \ first, then ; , and "
     */
    fun generateWifiString(ssid: String, password: String): String {
        val escapedSsid = escapeWifiString(ssid)
        val escapedPassword = escapeWifiString(password)
        return "WIFI:T:WPA;S:$escapedSsid;P:$escapedPassword;;"
    }
    
    private fun escapeWifiString(value: String): String {
        // Order matters: escape backslash first, then other special chars
        return value
            .replace("\\", "\\\\")
            .replace(";", "\\;")
            .replace(",", "\\,")
            .replace("\"", "\\\"")
    }
}
