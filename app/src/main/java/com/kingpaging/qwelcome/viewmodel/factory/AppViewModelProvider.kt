package com.kingpaging.qwelcome.viewmodel.factory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import com.kingpaging.qwelcome.di.AppContainer
import com.kingpaging.qwelcome.viewmodel.CustomerIntakeViewModel
import com.kingpaging.qwelcome.viewmodel.export.ExportViewModel
import com.kingpaging.qwelcome.viewmodel.import_pkg.ImportViewModel
import com.kingpaging.qwelcome.viewmodel.settings.SettingsViewModel
import com.kingpaging.qwelcome.viewmodel.templates.TemplateEditorViewModel
import com.kingpaging.qwelcome.viewmodel.templates.TemplateListViewModel

/**
 * Provides ViewModels with shared dependencies.
 *
 * Dependency lifetime and construction are owned by [AppContainer]. This factory only
 * adapts that graph to Android's ViewModel creation APIs.
 */
class AppViewModelProvider(
    private val container: AppContainer,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        modelClass: Class<T>,
        extras: CreationExtras,
    ): T =
        when {
            modelClass.isAssignableFrom(CustomerIntakeViewModel::class.java) -> {
                val savedStateHandle = extras.createSavedStateHandle()
                CustomerIntakeViewModel(
                    savedStateHandle = savedStateHandle,
                    settingsStore = container.settingsStore,
                    resourceProvider = container.resourceProvider,
                ) as T
            }
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> {
                SettingsViewModel(
                    store = container.settingsStore,
                    resourceProvider = container.resourceProvider,
                    appUpdater = container.appUpdater,
                ) as T
            }
            modelClass.isAssignableFrom(ExportViewModel::class.java) -> {
                ExportViewModel(
                    repository = container.importExportRepository,
                    settingsStore = container.settingsStore,
                    packageManager = container.packageManager,
                    contentResolver = container.contentResolver,
                ) as T
            }
            modelClass.isAssignableFrom(ImportViewModel::class.java) -> {
                ImportViewModel(
                    container.importExportRepository,
                    container.resourceProvider,
                ) as T
            }
            modelClass.isAssignableFrom(TemplateListViewModel::class.java) -> {
                TemplateListViewModel(
                    container.settingsStore,
                    container.resourceProvider,
                ) as T
            }
            modelClass.isAssignableFrom(TemplateEditorViewModel::class.java) -> {
                TemplateEditorViewModel(
                    savedStateHandle = extras.createSavedStateHandle(),
                    settingsStore = container.settingsStore,
                    resourceProvider = container.resourceProvider,
                ) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
}
