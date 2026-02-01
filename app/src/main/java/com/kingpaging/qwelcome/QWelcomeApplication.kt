package com.kingpaging.qwelcome

import android.app.Application
import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * Application class for Q Welcome.
 *
 * Handles app-wide initialization including:
 * - Firebase Crashlytics configuration
 */
class QWelcomeApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Configure Crashlytics
        // Disable crash collection in debug builds for cleaner development
        val crashlytics = FirebaseCrashlytics.getInstance()
        crashlytics.isCrashlyticsCollectionEnabled = !BuildConfig.DEBUG

        // Set custom keys for better crash context
        crashlytics.setCustomKey("app_version", BuildConfig.VERSION_NAME)
        crashlytics.setCustomKey("build_type", BuildConfig.BUILD_TYPE)
    }
}
