package com.kingpaging.qwelcome.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kingpaging.qwelcome.R
import com.kingpaging.qwelcome.data.TechProfile
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

    // Tech profile state - use rememberSaveable so rotation does not lose edits
    var name by rememberSaveable(currentProfile) { mutableStateOf(currentProfile.name) }
    var title by rememberSaveable(currentProfile) { mutableStateOf(currentProfile.title) }
    var dept by rememberSaveable(currentProfile) { mutableStateOf(currentProfile.dept) }

    // Update local state when currentProfile changes (e.g., after import on another screen)
    LaunchedEffect(currentProfile) {
        name = currentProfile.name
        title = currentProfile.title
        dept = currentProfile.dept
    }

    // Detect unsaved changes - only profile changes now (template editing moved to TemplateListScreen)
    val hasUnsavedChanges by remember(name, title, dept, currentProfile) {
        derivedStateOf {
            name != currentProfile.name ||
                title != currentProfile.title ||
                dept != currentProfile.dept
        }
    }

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
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                SettingsProfileSection(
                    activeTemplateName = uiState.activeTemplate.name,
                    name = name,
                    title = title,
                    department = dept,
                    hasUnsavedChanges = hasUnsavedChanges,
                    onNameChange = { name = it },
                    onTitleChange = { title = it },
                    onDepartmentChange = { dept = it },
                    onOpenTemplates = onOpenTemplates,
                    onSaveProfile = { onSaveProfile(TechProfile(name, title, dept)) },
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
