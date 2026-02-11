package com.kingpaging.qwelcome.ui.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.core.net.toUri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.kingpaging.qwelcome.R
import com.kingpaging.qwelcome.data.TechProfile
import com.kingpaging.qwelcome.di.LocalSettingsViewModel
import com.kingpaging.qwelcome.di.LocalSoundPlayer
import com.kingpaging.qwelcome.ui.components.CyberpunkBackdrop
import com.kingpaging.qwelcome.ui.components.NeonButton
import com.kingpaging.qwelcome.ui.components.NeonButtonStyle
import com.kingpaging.qwelcome.ui.components.NeonMagentaButton
import com.kingpaging.qwelcome.ui.components.NeonOutlinedField
import com.kingpaging.qwelcome.ui.components.NeonPanel
import com.kingpaging.qwelcome.ui.components.NeonTopAppBar
import com.kingpaging.qwelcome.util.rememberHapticFeedback
import com.kingpaging.qwelcome.viewmodel.settings.SettingsEvent
import com.kingpaging.qwelcome.viewmodel.settings.UpdateState
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenExport: () -> Unit = {},
    onOpenImport: () -> Unit = {},
    onOpenTemplates: () -> Unit = {}
) {
    // Get ViewModel from CompositionLocal (provided at Activity level)
    val vm = LocalSettingsViewModel.current
    val soundPlayer = LocalSoundPlayer.current

    // Discard confirmation dialog state
    var showDiscardDialog by rememberSaveable { mutableStateOf(false) }

    val currentProfile by vm.techProfile.collectAsStateWithLifecycle()
    val activeTemplate by vm.activeTemplate.collectAsStateWithLifecycle()
    val updateState by vm.updateState.collectAsStateWithLifecycle()

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

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptic = rememberHapticFeedback()

    val showDownloadConfirmDialog by vm.showDownloadConfirmDialog.collectAsStateWithLifecycle()
    val availableUpdate = updateState as? UpdateState.Available

    val launchIntentFailedMessage = stringResource(R.string.error_update_install_unavailable)

    // Collect one-shot settings events with lifecycle awareness
    LaunchedEffect(lifecycleOwner, vm.settingsEvents) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            vm.settingsEvents.collect { event ->
                when (event) {
                    is SettingsEvent.ShowToast -> {
                        Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                    }
                    is SettingsEvent.ShowToastError -> {
                        soundPlayer.playBeep()
                        Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                    }
                    is SettingsEvent.LaunchIntent -> {
                        try {
                            context.startActivity(event.intent)
                        } catch (_: ActivityNotFoundException) {
                            Toast.makeText(
                                context,
                                launchIntentFailedMessage,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
        }
    }

    // Discard changes confirmation dialog
    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text(stringResource(R.string.dialog_discard_changes_title)) },
            text = { Text(stringResource(R.string.dialog_discard_changes_message)) },
            confirmButton = {
                TextButton(onClick = {
                    haptic()
                    showDiscardDialog = false
                    onBack()
                }) {
                    Text(stringResource(R.string.action_discard), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { haptic(); showDiscardDialog = false }) {
                    Text(stringResource(R.string.action_keep_editing))
                }
            }
        )
    }

    if (showDownloadConfirmDialog && availableUpdate != null) {
        AlertDialog(
            onDismissRequest = { vm.dismissDownloadConfirmation() },
            title = { Text(stringResource(R.string.title_update_available)) },
            text = {
                Text(
                    stringResource(
                        R.string.text_update_download_confirm,
                        availableUpdate.version,
                        formatBytes(availableUpdate.assetSizeBytes)
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    haptic()
                    vm.confirmDownloadFromDialog()
                }) {
                    Text(stringResource(R.string.action_download_update))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    haptic()
                    vm.dismissDownloadConfirmation()
                }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
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
                                contentDescription = stringResource(R.string.content_desc_back)
                            )
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // === TECH PROFILE SECTION ===
                Text(
                    stringResource(R.string.header_tech_profile),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                NeonPanel {
                    NeonOutlinedField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(R.string.label_tech_name)) }
                    )
                    NeonOutlinedField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text(stringResource(R.string.label_title)) }
                    )
                    NeonOutlinedField(
                        value = dept,
                        onValueChange = { dept = it },
                        label = { Text(stringResource(R.string.label_department_line)) }
                    )
                }

                Spacer(Modifier.height(8.dp))

                // === MESSAGE TEMPLATE SECTION ===
                Text(
                    stringResource(R.string.header_message_templates),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                NeonPanel {
                    Text(
                        stringResource(R.string.text_manage_templates_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.text_currently_using_template, activeTemplate.name),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(12.dp))
                    NeonButton(
                        onClick = onOpenTemplates,
                        modifier = Modifier.fillMaxWidth(),
                        style = NeonButtonStyle.SECONDARY
                    ) {
                        Text(stringResource(R.string.action_manage_templates_plain))
                    }
                }

                Spacer(Modifier.height(8.dp))

                // === SAVE PROFILE BUTTON ===
                NeonMagentaButton(
                    onClick = {
                        vm.save(TechProfile(name, title, dept))
                        onBack()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = hasUnsavedChanges,
                    style = NeonButtonStyle.PRIMARY
                ) {
                    Text(
                        if (hasUnsavedChanges) {
                            stringResource(R.string.action_save_profile)
                        } else {
                            stringResource(R.string.label_no_changes)
                        }
                    )
                }

                Spacer(Modifier.height(16.dp))

                // === DATA MANAGEMENT SECTION ===
                Text(
                    stringResource(R.string.header_data_management),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                NeonPanel {
                    Text(
                        stringResource(R.string.text_data_management_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    if (hasUnsavedChanges) {
                        Text(
                            stringResource(R.string.text_export_disabled_unsaved),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        NeonMagentaButton(
                            onClick = onOpenExport,
                            modifier = Modifier.weight(1f),
                            enabled = !hasUnsavedChanges,
                            style = NeonButtonStyle.SECONDARY
                        ) {
                            Text(
                                if (hasUnsavedChanges) {
                                    stringResource(R.string.action_save_first)
                                } else {
                                    stringResource(R.string.action_export)
                                }
                            )
                        }
                        NeonButton(
                            onClick = onOpenImport,
                            modifier = Modifier.weight(1f),
                            style = NeonButtonStyle.SECONDARY
                        ) {
                            Text(stringResource(R.string.action_import))
                        }
                    }
                }

                Spacer(Modifier.height(32.dp))

                // === ABOUT SECTION ===
                Text(
                    stringResource(R.string.header_about),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                val noBrowserMessage = stringResource(R.string.toast_no_browser)

                NeonPanel {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.Info,
                                contentDescription = stringResource(R.string.content_desc_version_info),
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                stringResource(R.string.label_version_format, vm.currentVersion),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        when (updateState) {
                            is UpdateState.Checking -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                            is UpdateState.UpToDate -> {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Filled.CheckCircle,
                                        contentDescription = stringResource(R.string.status_up_to_date),
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        stringResource(R.string.status_up_to_date),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            else -> Unit
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    when (val state = updateState) {
                        is UpdateState.Available -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                NeonButton(
                                    onClick = {
                                        haptic()
                                        vm.requestDownloadConfirmation()
                                    },
                                    style = NeonButtonStyle.SECONDARY
                                ) {
                                    Icon(
                                        Icons.Filled.Download,
                                        contentDescription = stringResource(R.string.content_desc_download_update),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(stringResource(R.string.status_update_available, state.version))
                                }
                                IconButton(
                                    onClick = { haptic(); vm.dismissUpdate() },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.Close,
                                        contentDescription = stringResource(R.string.content_desc_dismiss_update),
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        is UpdateState.DownloadQueued -> {
                            Text(
                                stringResource(R.string.status_download_queued),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        is UpdateState.Downloading -> {
                            Text(
                                downloadingStatusText(state),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        is UpdateState.Verifying -> {
                            Text(
                                stringResource(R.string.status_verifying_update),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        is UpdateState.ReadyToInstall -> {
                            Text(
                                stringResource(R.string.status_ready_to_install),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(8.dp))
                            NeonButton(
                                onClick = { haptic(); vm.retryInstallAfterPermission() },
                                modifier = Modifier.fillMaxWidth(),
                                style = NeonButtonStyle.SECONDARY
                            ) {
                                Text(stringResource(R.string.action_install_update))
                            }
                        }
                        is UpdateState.PermissionRequired -> {
                            Text(
                                stringResource(R.string.status_permission_required),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                NeonButton(
                                    onClick = {
                                        haptic()
                                        try {
                                            context.startActivity(vm.openUnknownSourcesSettingsIntent())
                                        } catch (_: ActivityNotFoundException) {
                                            Toast.makeText(context, noBrowserMessage, Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    style = NeonButtonStyle.SECONDARY
                                ) {
                                    Text(stringResource(R.string.action_open_install_settings))
                                }
                                NeonButton(
                                    onClick = { haptic(); vm.retryInstallAfterPermission() },
                                    modifier = Modifier.weight(1f),
                                    style = NeonButtonStyle.SECONDARY
                                ) {
                                    Text(stringResource(R.string.action_retry_install))
                                }
                            }
                        }
                        is UpdateState.Installing -> {
                            Text(
                                stringResource(R.string.status_installing),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        is UpdateState.Error -> {
                            Text(
                                state.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        else -> Unit
                    }

                    Spacer(Modifier.height(12.dp))

                    NeonButton(
                        onClick = { haptic(); vm.checkForUpdate() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isUpdateFlowBusy(updateState),
                        style = NeonButtonStyle.SECONDARY
                    ) {
                        Icon(
                            Icons.Filled.Refresh,
                            contentDescription = stringResource(R.string.content_desc_check_updates),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            when (updateState) {
                                is UpdateState.Checking -> stringResource(R.string.status_checking_updates)
                                else -> stringResource(R.string.action_check_updates)
                            }
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    TextButton(
                        onClick = {
                            val uri = "https://github.com/H2OKing89/QWelcome".toUri()
                            val intent = Intent(Intent.ACTION_VIEW, uri)
                                .addCategory(Intent.CATEGORY_BROWSABLE)
                            try {
                                context.startActivity(intent)
                            } catch (_: ActivityNotFoundException) {
                                Toast.makeText(context, noBrowserMessage, Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Filled.Code,
                            contentDescription = stringResource(R.string.content_desc_view_github),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.action_view_github))
                    }
                }

                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

private fun isUpdateFlowBusy(state: UpdateState): Boolean {
    return state is UpdateState.Checking ||
        state is UpdateState.DownloadQueued ||
        state is UpdateState.Downloading ||
        state is UpdateState.Verifying
}

@Composable
private fun downloadingStatusText(state: UpdateState.Downloading): String {
    val downloaded = formatBytes(state.bytesDownloaded)
    val total = state.totalBytes?.let(::formatBytes)
    val percent = if (state.totalBytes != null && state.totalBytes > 0L) {
        ((state.bytesDownloaded.toDouble() / state.totalBytes.toDouble()) * 100.0)
            .coerceIn(0.0, 100.0)
            .roundToInt()
    } else {
        null
    }

    return if (total != null && percent != null) {
        stringResource(R.string.status_downloading_progress, downloaded, total, percent)
    } else {
        stringResource(R.string.status_downloading_unknown)
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB")
    var value = bytes.toDouble()
    var unitIndex = 0
    while (value >= 1024.0 && unitIndex < units.lastIndex) {
        value /= 1024.0
        unitIndex++
    }
    return String.format(java.util.Locale.US, "%.1f %s", value, units[unitIndex])
}
