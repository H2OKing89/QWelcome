package com.kingpaging.qwelcome.data

data class PrivacySettings(
    val crashReportingEnabled: Boolean = false,
    val screenCaptureProtectionEnabled: Boolean = false,
) {
    companion object
}
