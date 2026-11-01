package com.kingpaging.qwelcome.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.kingpaging.qwelcome.R
import com.kingpaging.qwelcome.data.TechProfile
import com.kingpaging.qwelcome.ui.components.CenteredFormColumn
import com.kingpaging.qwelcome.ui.components.CyberpunkBackdrop
import com.kingpaging.qwelcome.ui.components.NeonButton
import com.kingpaging.qwelcome.ui.components.NeonButtonStyle
import com.kingpaging.qwelcome.ui.components.NeonDiscardDialog
import com.kingpaging.qwelcome.ui.components.NeonTopAppBar
import com.kingpaging.qwelcome.util.rememberHapticFeedback
import com.kingpaging.qwelcome.viewmodel.settings.UpdateState

@Suppress("CyclomaticComplexMethod", "FunctionNaming", "LongMethod", "LongParameterList")
@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onBack: () -> Unit,
    onOpenExport: () -> Unit,
    onOpenImport: () -> Unit,
    onOpenTemplates: () -> Unit,
    onSaveProfile: (TechProfile) -> Unit,
    onSetCrashReportingEnabled: (Boolean) -> Unit,
    onSetScreenCaptureProtectionEnabled: (Boolean) -> Unit,
    onDismissDownloadConfirmation: () -> Unit,
    onConfirmDownload: () -> Unit,
    onRequestDownloadConfirmation: () -> Unit,
    onDismissUpdate: () -> Unit,
    onRetryInstall: () -> Unit,
    onOpenInstallSettings: () -> Unit,
    onCheckForUpdate: () -> Unit,
    onOpenProjectPage: () -> Unit,
) {
    var showDiscardDialog by rememberSaveable { mutableStateOf(false) }
    val currentProfile = uiState.profile
    val privacySettings = uiState.privacySettings
    val updateState = uiState.updateState

    val draft = rememberSettingsProfileDraft(currentProfile)
    val hasUnsavedChanges = draft.toProfile() != currentProfile

    val haptic = rememberHapticFeedback()
    val availableUpdate = updateState as? UpdateState.Available

    // Discard changes confirmation dialog
    if (showDiscardDialog) {
        NeonDiscardDialog(
            message = stringResource(R.string.dialog_discard_changes_message),
            onDiscard = {
                showDiscardDialog = false
                onBack()
            },
            onKeepEditing = { showDiscardDialog = false },
        )
    }

    if (uiState.showDownloadConfirmDialog && availableUpdate != null) {
        AlertDialog(
            onDismissRequest = onDismissDownloadConfirmation,
            title = { Text(stringResource(R.string.title_update_available)) },
            text = {
                Text(
                    stringResource(
                        R.string.text_update_download_confirm,
                        availableUpdate.version,
                        formatBytes(availableUpdate.assetSizeBytes),
                    ),
                )
            },
            confirmButton = {
                NeonButton(
                    onClick = onConfirmDownload,
                    style = NeonButtonStyle.PRIMARY,
                ) {
                    Text(stringResource(R.string.action_download_update))
                }
            },
            dismissButton = {
                NeonButton(
                    onClick = onDismissDownloadConfirmation,
                    style = NeonButtonStyle.TERTIARY,
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    // Handle system back button - respect unsaved changes
    BackHandler {
        if (hasUnsavedChanges) showDiscardDialog = true else onBack()
    }

    CyberpunkBackdrop {
        Scaffold(
            // Intentional: keep scaffold transparent so the cyberpunk backdrop remains visible.
            containerColor = Color.Transparent,
            topBar = {
                NeonTopAppBar(
                    title = { Text(stringResource(R.string.title_settings)) },
                    navigationIcon = {
                        IconButton(onClick = {
                            haptic()
                            if (hasUnsavedChanges) showDiscardDialog = true else onBack()
                        }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.content_desc_back),
                            )
                        }
                    },
                )
            },
        ) { padding ->
            CenteredFormColumn(padding = padding) {
                SettingsProfileSection(
                    activeTemplateName = uiState.activeTemplate.name,
                    name = draft.name,
                    title = draft.title,
                    department = draft.department,
                    hasUnsavedChanges = hasUnsavedChanges,
                    onNameChange = { draft.name = it },
                    onTitleChange = { draft.title = it },
                    onDepartmentChange = { draft.department = it },
                    onOpenTemplates = onOpenTemplates,
                    onSaveProfile = { onSaveProfile(draft.toProfile()) },
                )

                SettingsPrivacySection(
                    privacySettings = privacySettings,
                    onSetCrashReportingEnabled = {
                        haptic()
                        onSetCrashReportingEnabled(it)
                    },
                    onSetScreenCaptureProtectionEnabled = {
                        haptic()
                        onSetScreenCaptureProtectionEnabled(it)
                    },
                )

                SettingsDataManagementSection(
                    hasUnsavedChanges = hasUnsavedChanges,
                    onOpenExport = onOpenExport,
                    onOpenImport = onOpenImport,
                )

                SettingsUpdateSection(
                    currentVersion = uiState.currentVersion,
                    updateState = updateState,
                    downloadActions =
                        UpdateDownloadActions(
                            onRequestDownloadConfirmation = {
                                haptic()
                                onRequestDownloadConfirmation()
                            },
                            onDismissUpdate = {
                                haptic()
                                onDismissUpdate()
                            },
                            onRetryInstall = {
                                haptic()
                                onRetryInstall()
                            },
                            onOpenInstallSettings = {
                                haptic()
                                onOpenInstallSettings()
                            },
                        ),
                    navigationActions =
                        UpdateNavigationActions(
                            onCheckForUpdate = {
                                haptic()
                                onCheckForUpdate()
                            },
                            onOpenProjectPage = onOpenProjectPage,
                        ),
                )
            }
        }
    }
}
