package com.example.allowelcome.viewmodel.factory

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.allowelcome.data.ImportExportRepository
import com.example.allowelcome.data.SettingsStore
import com.example.allowelcome.viewmodel.CustomerIntakeViewModel
import com.example.allowelcome.viewmodel.export.ExportViewModel
import com.example.allowelcome.viewmodel.settings.SettingsViewModel

/**
 * Provides ViewModels with shared dependencies.
 * 
 * The [Companion] object maintains a **process-wide singleton** of [SettingsStore],
 * initialized with the application [Context]. This ensures all ViewModels share the
 * same [SettingsStore] instance regardless of which Activity or Fragment creates them.
 * 
 * Note: The singleton is tied to the process lifetime, not any specific Activity or
 * application context instance. Once created, the same [SettingsStore] is reused
 * until the process is killed or [resetForTesting] is called.
 */
class AppViewModelProvider(private val context: Context) : ViewModelProvider.Factory {
    
    companion object {
        @Volatile
        private var settingsStoreInstance: SettingsStore? = null
        
        @Volatile
        private var importExportRepositoryInstance: ImportExportRepository? = null
        
        private fun getSettingsStore(context: Context): SettingsStore {
            return settingsStoreInstance ?: synchronized(this) {
                settingsStoreInstance ?: SettingsStore(context.applicationContext).also {
                    settingsStoreInstance = it
                }
            }
        }
        
        private fun getImportExportRepository(context: Context): ImportExportRepository {
            return importExportRepositoryInstance ?: synchronized(this) {
                importExportRepositoryInstance ?: ImportExportRepository(getSettingsStore(context)).also {
                    importExportRepositoryInstance = it
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
                importExportRepositoryInstance = null
            }
        }
    }
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(CustomerIntakeViewModel::class.java) -> {
                CustomerIntakeViewModel(getSettingsStore(context)) as T
            }
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> {
                SettingsViewModel(getSettingsStore(context)) as T
            }
            modelClass.isAssignableFrom(ExportViewModel::class.java) -> {
                ExportViewModel(getImportExportRepository(context)) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
