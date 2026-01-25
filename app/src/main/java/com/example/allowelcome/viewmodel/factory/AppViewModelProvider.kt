package com.example.allowelcome.viewmodel.factory

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.allowelcome.data.SettingsStore
import com.example.allowelcome.viewmodel.CustomerIntakeViewModel
import com.example.allowelcome.viewmodel.settings.SettingsViewModel

class AppViewModelProvider(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val settingsStore = SettingsStore(context.applicationContext)
        return when {
            modelClass.isAssignableFrom(CustomerIntakeViewModel::class.java) -> {
                CustomerIntakeViewModel(settingsStore) as T
            }
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> {
                SettingsViewModel(settingsStore) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
