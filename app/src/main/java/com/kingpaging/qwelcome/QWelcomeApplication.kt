package com.kingpaging.qwelcome

import android.app.Application
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.kingpaging.qwelcome.util.SoundManager
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner

/**
 * Application class for Q Welcome.
 *
 * Handles app-wide initialization including:
 * - Firebase Crashlytics configuration
 */
class QWelcomeApplication : Application() {

    private val appLifecycleObserver = object : DefaultLifecycleObserver {
        override fun onStart(owner: LifecycleOwner) {
            SoundManager.restart()
        }

        override fun onStop(owner: LifecycleOwner) {
            SoundManager.shutdown()
        }
    }

    override fun onCreate() {
        super.onCreate()

        // Configure Crashlytics
        // Disable crash collection in debug builds for cleaner development
        val crashlytics = FirebaseCrashlytics.getInstance()
        crashlytics.isCrashlyticsCollectionEnabled = !BuildConfig.DEBUG

        // Set custom keys for better crash context
        crashlytics.setCustomKey("app_version", BuildConfig.VERSION_NAME)
        crashlytics.setCustomKey("build_type", BuildConfig.BUILD_TYPE)

        ProcessLifecycleOwner.get().lifecycle.addObserver(appLifecycleObserver)
    }
}
