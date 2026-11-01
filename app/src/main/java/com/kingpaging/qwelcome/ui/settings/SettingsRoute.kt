package com.kingpaging.qwelcome.ui.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.kingpaging.qwelcome.R
import com.kingpaging.qwelcome.di.LocalSettingsViewModel
import com.kingpaging.qwelcome.di.LocalSoundPlayer
import com.kingpaging.qwelcome.viewmodel.settings.SettingsEvent

@Suppress("FunctionNaming", "LongMethod")
@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    onOpenExport: () -> Unit = {},
    onOpenImport: () -> Unit = {},
    onOpenTemplates: () -> Unit = {},
) {
    val viewModel = LocalSettingsViewModel.current
    val soundPlayer = LocalSoundPlayer.current
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentProfile by viewModel.techProfile.collectAsStateWithLifecycle()
    val privacySettings by viewModel.privacySettings.collectAsStateWithLifecycle()
    val activeTemplate by viewModel.activeTemplate.collectAsStateWithLifecycle()
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()
    val showDownloadConfirmDialog by viewModel.showDownloadConfirmDialog.collectAsStateWithLifecycle()
    val noBrowserMessage = stringResource(R.string.toast_no_browser)
    val currentOnBack by rememberUpdatedState(onBack)
    val installerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            viewModel.onInstallerReturned()
        }

    LaunchedEffect(viewModel) {
        viewModel.restorePendingUpdate()
    }

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) viewModel.onInstallerReturned()
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(lifecycleOwner, viewModel.settingsEvents) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.settingsEvents.collect { event ->
                when (event) {
                    SettingsEvent.ProfileSaved -> currentOnBack()
                    is SettingsEvent.ShowToast -> showToast(context, event.message)
                    is SettingsEvent.ShowToastError -> {
                        soundPlayer.playBeep()
                        showToast(context, event.message)
                    }
                    is SettingsEvent.LaunchIntent -> {
                        try {
                            installerLauncher.launch(event.intent)
                        } catch (_: ActivityNotFoundException) {
                            viewModel.onInstallerLaunchFailed()
                        } catch (_: SecurityException) {
                            viewModel.onInstallerLaunchFailed()
                        } catch (_: IllegalArgumentException) {
                            viewModel.onInstallerLaunchFailed()
                        }
                    }
                }
            }
        }
    }

    SettingsScreen(
        uiState =
            SettingsUiState(
                profile = currentProfile,
                privacySettings = privacySettings,
                activeTemplate = activeTemplate,
                updateState = updateState,
                showDownloadConfirmDialog = showDownloadConfirmDialog,
                currentVersion = viewModel.currentVersion,
            ),
        onBack = onBack,
        onOpenExport = onOpenExport,
        onOpenImport = onOpenImport,
        onOpenTemplates = onOpenTemplates,
        onSaveProfile = viewModel::save,
        onSetCrashReportingEnabled = viewModel::setCrashReportingEnabled,
        onSetScreenCaptureProtectionEnabled = viewModel::setScreenCaptureProtectionEnabled,
        onDismissDownloadConfirmation = viewModel::dismissDownloadConfirmation,
        onConfirmDownload = viewModel::confirmDownloadFromDialog,
        onRequestDownloadConfirmation = viewModel::requestDownloadConfirmation,
        onDismissUpdate = viewModel::dismissUpdate,
        onRetryInstall = viewModel::retryInstallAfterPermission,
        onOpenInstallSettings = {
            launchIntent(
                context,
                viewModel.openUnknownSourcesSettingsIntent(),
                noBrowserMessage,
            )
        },
        onCheckForUpdate = viewModel::checkForUpdate,
        onOpenProjectPage = {
            val intent =
                Intent(Intent.ACTION_VIEW, PROJECT_URI.toUri())
                    .addCategory(Intent.CATEGORY_BROWSABLE)
            launchIntent(context, intent, noBrowserMessage)
        },
    )
}

private fun launchIntent(
    context: Context,
    intent: Intent,
    failureMessage: String,
) {
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        showToast(context, failureMessage)
    }
}

private fun showToast(
    context: Context,
    message: String,
) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}

private const val PROJECT_URI = "https://github.com/H2OKing89/QWelcome"
