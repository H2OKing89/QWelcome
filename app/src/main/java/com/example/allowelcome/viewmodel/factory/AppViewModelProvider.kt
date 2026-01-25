package com.example.allowelcome.viewmodel.factory

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.allowelcome.data.SettingsStore
import com.example.allowelcome.viewmodel.CustomerIntakeViewModel
import com.example.allowelcome.viewmodel.settings.SettingsViewModel

/**
 * Provides ViewModels with shared dependencies.
 * Uses a companion object cache to ensure SettingsStore is a singleton per application context.
 */
class AppViewModelProvider(private val context: Context) : ViewModelProvider.Factory {
    
    companion object {
        @Volatile
        private var settingsStoreInstance: SettingsStore? = null
        
        private fun getSettingsStore(context: Context): SettingsStore {
            return settingsStoreInstance ?: synchronized(this) {
                settingsStoreInstance ?: SettingsStore(context.applicationContext).also {
                    settingsStoreInstance = it
                }
            }
        }
        
        /**
         * Resets the singleton SettingsStore instance.
         * 
         * **TEST-ONLY**: This function exists solely for testing purposes to allow
         * clearing cached state between tests. Do not call this in production code.
         * 
         * Usage in tests:
         * ```
         * @After
         * fun tearDown() {
         *     AppViewModelProvider.resetForTesting()
         * }
         * ```
         */
        @VisibleForTesting
        fun resetForTesting() {
            synchronized(this) {
                settingsStoreInstance = null
            }
        }
    }
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val settingsStore = getSettingsStore(context)
        return when {
            modelClass.isAssignableFrom(CustomerIntakeViewModel::class.java) -> {
                CustomerIntakeViewModel(settingsStore) as T
            }
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> {
                SettingsViewModel(settingsStore) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
