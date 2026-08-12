package com.kingpaging.qwelcome

import android.app.Application
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.kingpaging.qwelcome.di.AppContainer
import com.kingpaging.qwelcome.util.SoundManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * Application class for Q Welcome.
 *
 * Handles app-wide initialization including:
 * - Firebase Crashlytics configuration
 */
class QWelcomeApplication : Application() {
    val appContainer: AppContainer by lazy { AppContainer(this) }

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val appLifecycleObserver =
        object : DefaultLifecycleObserver {
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
        clearQrShareCache()

        // Keep collection disabled until the persisted preference is available.
        val crashlytics = FirebaseCrashlytics.getInstance()
        crashlytics.isCrashlyticsCollectionEnabled = false
        crashlytics.setCustomKey("app_version", BuildConfig.VERSION_NAME)
        crashlytics.setCustomKey("build_type", BuildConfig.BUILD_TYPE)
        applicationScope.launch {
            appContainer.settingsStore.privacySettingsFlow.collect { settings ->
                crashlytics.isCrashlyticsCollectionEnabled =
                    !BuildConfig.DEBUG &&
                    settings.crashReportingEnabled
            }
        }

        ProcessLifecycleOwner.get().lifecycle.addObserver(appLifecycleObserver)
    }

    companion object {
        private const val TAG = "QWelcomeApplication"
        private const val QR_SHARE_CACHE_DIRECTORY = "qr_codes"
    }

    private fun clearQrShareCache() {
        val cacheDirectory = cacheDir.resolve(QR_SHARE_CACHE_DIRECTORY)
        if (cacheDirectory.exists() && !cacheDirectory.deleteRecursively()) {
            Log.w(TAG, "Failed to clear QR share cache")
        }
    }
}
