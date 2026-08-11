package com.kingpaging.qwelcome.di

import android.content.Context
import android.content.pm.PackageManager
import com.kingpaging.qwelcome.data.AppUpdater
import com.kingpaging.qwelcome.data.GitHubAppUpdater
import com.kingpaging.qwelcome.data.ImportExportRepository
import com.kingpaging.qwelcome.data.SettingsStore
import com.kingpaging.qwelcome.util.AndroidResourceProvider
import com.kingpaging.qwelcome.util.ResourceProvider

class AppContainer(context: Context) {
    private val applicationContext = context.applicationContext

    val settingsStore: SettingsStore by lazy {
        SettingsStore(applicationContext)
    }

    val resourceProvider: ResourceProvider by lazy {
        AndroidResourceProvider(applicationContext)
    }

    val appUpdater: AppUpdater by lazy {
        GitHubAppUpdater(applicationContext)
    }

    val importExportRepository: ImportExportRepository by lazy {
        ImportExportRepository(settingsStore, resourceProvider)
    }

    val packageManager: PackageManager
        get() = applicationContext.packageManager
}
