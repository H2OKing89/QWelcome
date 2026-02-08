package com.kingpaging.qwelcome

import android.app.Application
import android.util.Log
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
            try {
                SoundManager.restart()
            } catch (e: Exception) {
                Log.e(TAG, "appLifecycleObserver: SoundManager.restart() failed", e)
                FirebaseCrashlytics.getInstance().apply {
                    log("SoundManager.restart() failed in appLifecycleObserver")
                    recordException(e)
                }
            }
        }

        override fun onStop(owner: LifecycleOwner) {
            try {
                SoundManager.shutdown()
            } catch (e: Exception) {
                Log.e(TAG, "appLifecycleObserver: SoundManager.shutdown() failed", e)
                FirebaseCrashlytics.getInstance().apply {
                    log("SoundManager.shutdown() failed in appLifecycleObserver")
                    recordException(e)
                }
            }
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

    companion object {
        private const val TAG = "QWelcomeApplication"
    }
}
