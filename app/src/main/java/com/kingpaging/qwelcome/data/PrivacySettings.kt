package com.kingpaging.qwelcome.data

data class PrivacySettings(
    val crashReportingEnabled: Boolean = true,
    val screenCaptureProtectionEnabled: Boolean = false
) {
    companion object
}
