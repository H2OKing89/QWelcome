package com.kingpaging.qwelcome.ui.settings

import com.kingpaging.qwelcome.data.PrivacySettings
import com.kingpaging.qwelcome.data.TechProfile
import com.kingpaging.qwelcome.data.Template
import com.kingpaging.qwelcome.viewmodel.settings.UpdateState

data class SettingsUiState(
    val profile: TechProfile,
    val privacySettings: PrivacySettings?,
    val activeTemplate: Template,
    val updateState: UpdateState,
    val showDownloadConfirmDialog: Boolean,
    val currentVersion: String,
)
