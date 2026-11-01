package com.kingpaging.qwelcome.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kingpaging.qwelcome.R
import com.kingpaging.qwelcome.data.TechProfile
import com.kingpaging.qwelcome.ui.components.CenteredFormColumn
import com.kingpaging.qwelcome.ui.components.CyberpunkBackdrop
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
    var section by rememberSaveable { mutableStateOf(SettingsSection.OVERVIEW) }
    var showDiscardDialog by rememberSaveable { mutableStateOf(false) }
    val pageState = rememberSaveableStateHolder()
    val currentProfile = uiState.profile
    val draft = rememberSettingsProfileDraft(currentProfile)
    val hasUnsavedChanges = draft.toProfile() != currentProfile
    val focusManager = LocalFocusManager.current
    val haptic = rememberHapticFeedback()
    val goBack: () -> Unit = {
        focusManager.clearFocus()
        when {
            section == SettingsSection.PROFILE && hasUnsavedChanges -> showDiscardDialog = true
            section != SettingsSection.OVERVIEW -> {
                if (section == SettingsSection.PROFILE) draft.resetTo(currentProfile)
                section = SettingsSection.OVERVIEW
            }
            else -> onBack()
        }
    }

    if (showDiscardDialog) {
        NeonDiscardDialog(
            message = stringResource(R.string.dialog_discard_changes_message),
            onDiscard = {
                draft.resetTo(currentProfile)
                showDiscardDialog = false
                section = SettingsSection.OVERVIEW
            },
            onKeepEditing = { showDiscardDialog = false },
        )
    }
    val availableUpdate = uiState.updateState as? UpdateState.Available
    if (uiState.showDownloadConfirmDialog && availableUpdate != null) {
        SettingsDownloadConfirmation(
            available = availableUpdate,
            onConfirm = onConfirmDownload,
            onDismiss = onDismissDownloadConfirmation,
        )
    }
    BackHandler(onBack = goBack)

    CyberpunkBackdrop {
        Scaffold(
            modifier = Modifier.imePadding(),
            containerColor = Color.Transparent,
            topBar = {
                NeonTopAppBar(
                    title = {
                        Text(stringResource(section.titleResource), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            haptic()
                            goBack()
                        }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.content_desc_back),
                            )
                        }
                    },
                )
            },
            bottomBar = {
                if (section == SettingsSection.PROFILE) {
                    Surface(color = MaterialTheme.colorScheme.surface) {
                        Box(
                            modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                modifier =
                                    Modifier
                                        .widthIn(max = 720.dp)
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                            ) {
                                SettingsProfileSaveAction(
                                    hasUnsavedChanges = hasUnsavedChanges,
                                    onSaveProfile = {
                                        focusManager.clearFocus()
                                        onSaveProfile(draft.toProfile())
                                    },
                                )
                            }
                        }
                    }
                }
            },
        ) { padding ->
            pageState.SaveableStateProvider(section) {
                CenteredFormColumn(padding = padding) {
                    when (section) {
                        SettingsSection.OVERVIEW ->
                            SettingsOverview(
                                uiState = uiState,
                                onOpenSection = { section = it },
                                onOpenTemplates = onOpenTemplates,
                            )
                        SettingsSection.PROFILE ->
                            SettingsProfileSection(
                                name = draft.name,
                                title = draft.title,
                                department = draft.department,
                                hasUnsavedChanges = hasUnsavedChanges,
                                onNameChange = { draft.name = it },
                                onTitleChange = { draft.title = it },
                                onDepartmentChange = { draft.department = it },
                                onSaveProfile = { onSaveProfile(draft.toProfile()) },
                                showSaveAction = false,
                            )
                        SettingsSection.PRIVACY ->
                            SettingsPrivacySection(
                                privacySettings = uiState.privacySettings,
                                onSetCrashReportingEnabled = {
                                    haptic()
                                    onSetCrashReportingEnabled(it)
                                },
                                onSetScreenCaptureProtectionEnabled = {
                                    haptic()
                                    onSetScreenCaptureProtectionEnabled(it)
                                },
                            )
                        SettingsSection.BACKUP ->
                            SettingsDataManagementSection(
                                hasUnsavedChanges = hasUnsavedChanges,
                                onOpenExport = onOpenExport,
                                onOpenImport = onOpenImport,
                            )
                        SettingsSection.ABOUT ->
                            SettingsUpdateSection(
                                currentVersion = uiState.currentVersion,
                                updateState = uiState.updateState,
                                downloadActions =
                                    UpdateDownloadActions(
                                        onRequestDownloadConfirmation = onRequestDownloadConfirmation,
                                        onDismissUpdate = onDismissUpdate,
                                        onRetryInstall = onRetryInstall,
                                        onOpenInstallSettings = onOpenInstallSettings,
                                    ),
                                navigationActions =
                                    UpdateNavigationActions(
                                        onCheckForUpdate = onCheckForUpdate,
                                        onOpenProjectPage = onOpenProjectPage,
                                    ),
                            )
                    }
                }
            }
        }
    }
}
