package com.example.allowelcome.viewmodel.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.allowelcome.data.SettingsStore
import com.example.allowelcome.data.TechProfile
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val store: SettingsStore
) : ViewModel() {

    val techProfile: StateFlow<TechProfile> =
        store.techProfileFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = TechProfile()
        )

    fun save(profile: TechProfile) {
        viewModelScope.launch { store.saveTechProfile(profile) }
    }
}
