package com.kingpaging.qwelcome.ui.settings

import androidx.annotation.StringRes
import com.kingpaging.qwelcome.R

internal enum class SettingsSection(
    @StringRes val titleResource: Int,
) {
    OVERVIEW(R.string.title_settings),
    PROFILE(R.string.settings_profile_title),
    PRIVACY(R.string.settings_privacy_title),
    BACKUP(R.string.settings_backup_title),
    ABOUT(R.string.settings_updates_title),
}
