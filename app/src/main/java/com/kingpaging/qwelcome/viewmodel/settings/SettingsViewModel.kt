package com.kingpaging.qwelcome.viewmodel.settings

import android.content.Intent
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kingpaging.qwelcome.BuildConfig
import com.kingpaging.qwelcome.R
import com.kingpaging.qwelcome.data.AppUpdater
import com.kingpaging.qwelcome.data.DownloadEnqueueResult
import com.kingpaging.qwelcome.data.DownloadStatus
import com.kingpaging.qwelcome.data.SettingsStore
import com.kingpaging.qwelcome.data.TechProfile
import com.kingpaging.qwelcome.data.Template
import com.kingpaging.qwelcome.data.UpdateCheckResult
import com.kingpaging.qwelcome.data.VerificationResult
import com.kingpaging.qwelcome.util.ResourceProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val TAG = "SettingsViewModel"

class SettingsViewModel(
    private val store: SettingsStore,
    private val resourceProvider: ResourceProvider,
    private val appUpdater: AppUpdater
) : ViewModel() {

    val techProfile: StateFlow<TechProfile> =
        store.techProfileFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = TechProfile()
        )

    val allTemplates: StateFlow<List<Template>> =
        store.allTemplatesFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val activeTemplate: StateFlow<Template> =
        store.activeTemplateFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = Template(name = "Default", content = store.defaultTemplateContent)
        )

    fun getDefaultTemplateContent(): String = store.defaultTemplateContent

    // Error events for UI to observe (buffered to avoid missing events)
    private val _errorEvents = MutableSharedFlow<String>(replay = 0, extraBufferCapacity = 1)
    val errorEvents: SharedFlow<String> = _errorEvents.asSharedFlow()

    // One-shot settings events (toasts + intents)
    private val _settingsEvents = MutableSharedFlow<SettingsEvent>(replay = 0, extraBufferCapacity = 1)
    val settingsEvents: SharedFlow<SettingsEvent> = _settingsEvents.asSharedFlow()

    fun save(profile: TechProfile) {
        viewModelScope.launch {
            try {
                store.saveTechProfile(profile)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save tech profile", e)
                _errorEvents.emit("Failed to save profile: ${e.message}")
            }
        }
    }

    fun saveTemplate(template: Template) {
        viewModelScope.launch {
            try {
                store.saveTemplate(template)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save template", e)
                _errorEvents.emit("Failed to save template: ${e.message}")
            }
        }
    }

    fun setActiveTemplate(templateId: String) {
        viewModelScope.launch {
            try {
                store.setActiveTemplate(templateId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set active template", e)
                _errorEvents.emit("Failed to set active template: ${e.message}")
            }
        }
    }

    fun deleteTemplate(templateId: String) {
        viewModelScope.launch {
            try {
                store.deleteTemplate(templateId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete template", e)
                _errorEvents.emit("Failed to delete template: ${e.message}")
            }
        }
    }

    fun resetTemplate() {
        viewModelScope.launch {
            try {
                store.resetToDefaultTemplate()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to reset template", e)
                _errorEvents.emit("Failed to reset template: ${e.message}")
            }
        }
    }

    // === UPDATE CHECKER ===

    /** Current app version from BuildConfig */
    val currentVersion: String = BuildConfig.VERSION_NAME

    /** Update check state */
    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    /** Download confirmation dialog visibility for SettingsScreen. */
    private val _showDownloadConfirmDialog = MutableStateFlow(false)
    val showDownloadConfirmDialog: StateFlow<Boolean> = _showDownloadConfirmDialog.asStateFlow()

    /** Timestamp of last successful update check start (milliseconds). */
    @VisibleForTesting
    internal var lastCheckTimeMillis: Long = 0L

    /** Check for updates from GitHub Releases */
    fun checkForUpdate() {
        if (_updateState.value is UpdateState.Checking) return // Prevent duplicate checks

        // Enforce 60-second cooldown between checks
        val now = System.currentTimeMillis()
        val elapsed = now - lastCheckTimeMillis
        val cooldownMs = COOLDOWN_SECONDS * 1000L
        if (lastCheckTimeMillis != 0L && elapsed < cooldownMs) {
            val remainingSeconds = ((cooldownMs - elapsed) / 1000).coerceAtLeast(1)
            viewModelScope.launch {
                _settingsEvents.emit(
                    SettingsEvent.ShowToastError(
                        resourceProvider.getString(R.string.toast_check_cooldown, remainingSeconds)
                    )
                )
            }
            return
        }
        lastCheckTimeMillis = now

        viewModelScope.launch {
            _updateState.value = UpdateState.Checking
            _showDownloadConfirmDialog.value = false

            when (val result = appUpdater.checkForUpdate(currentVersion)) {
                is UpdateCheckResult.UpdateAvailable -> {
                    _updateState.value = UpdateState.Available(
                        version = result.latestVersion,
                        downloadUrl = result.downloadUrl,
                        releaseNotes = result.releaseNotes,
                        assetName = result.assetName,
                        assetSizeBytes = result.assetSizeBytes,
                        sha256Hex = result.sha256Hex
                    )
                }
                is UpdateCheckResult.UpToDate -> {
                    _updateState.value = UpdateState.UpToDate
                    _showDownloadConfirmDialog.value = false
                }
                is UpdateCheckResult.Error -> {
                    _updateState.value = UpdateState.Error(result.message)
                    _showDownloadConfirmDialog.value = false
                }
                is UpdateCheckResult.RateLimited -> {
                    val message = if (result.retryAfterSeconds != null) {
                        resourceProvider.getString(
                            R.string.toast_rate_limited_retry,
                            result.retryAfterSeconds
                        )
                    } else {
                        resourceProvider.getString(R.string.toast_rate_limited)
                    }
                    _updateState.value = UpdateState.Error(message)
                    _showDownloadConfirmDialog.value = false
                }
            }
        }
    }

    fun startUpdateDownload() {
        val available = _updateState.value as? UpdateState.Available ?: return

        viewModelScope.launch {
            try {
                when (val enqueueResult = appUpdater.enqueueDownload(available.toUpdateAvailable())) {
                    is DownloadEnqueueResult.Failed -> {
                        _updateState.value = UpdateState.Error(enqueueResult.message)
                    }
                    is DownloadEnqueueResult.Started -> {
                        monitorDownload(available, enqueueResult.downloadId)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _updateState.value = UpdateState.Error(updaterErrorMessage(e))
            }
        }
    }

    fun retryInstallAfterPermission() {
        val state = _updateState.value
        val (version, apkPath) = when (state) {
            is UpdateState.ReadyToInstall -> state.version to state.apkPath
            is UpdateState.PermissionRequired -> state.version to state.apkPath
            else -> return
        }

        viewModelScope.launch {
            beginInstall(version, apkPath)
        }
    }

    fun openUnknownSourcesSettingsIntent(): Intent {
        return appUpdater.createUnknownSourcesSettingsIntent()
    }

    /** Dismiss update notification */
    fun dismissUpdate() {
        _updateState.value = UpdateState.Dismissed
        _showDownloadConfirmDialog.value = false
    }

    fun requestDownloadConfirmation() {
        if (_updateState.value is UpdateState.Available) {
            _showDownloadConfirmDialog.value = true
        }
    }

    fun dismissDownloadConfirmation() {
        _showDownloadConfirmDialog.value = false
    }

    fun confirmDownloadFromDialog() {
        _showDownloadConfirmDialog.value = false
        startUpdateDownload()
    }

    private suspend fun monitorDownload(
        available: UpdateState.Available,
        downloadId: Long
    ) {
        _updateState.value = UpdateState.DownloadQueued(
            version = available.version,
            downloadId = downloadId
        )

        while (true) {
            val status = try {
                appUpdater.getDownloadStatus(downloadId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _updateState.value = UpdateState.Error(updaterErrorMessage(e))
                return
            }

            when (status) {
                is DownloadStatus.InProgress -> {
                    _updateState.value = if (status.bytesDownloaded <= 0L) {
                        UpdateState.DownloadQueued(
                            version = available.version,
                            downloadId = downloadId
                        )
                    } else {
                        UpdateState.Downloading(
                            version = available.version,
                            bytesDownloaded = status.bytesDownloaded,
                            totalBytes = status.totalBytes
                        )
                    }
                    delay(DOWNLOAD_POLL_INTERVAL_MS)
                }
                is DownloadStatus.Succeeded -> {
                    _updateState.value = UpdateState.Verifying(available.version)
                    val verifyResult = try {
                        appUpdater.verifyDownloadedApk(
                            apkPath = status.apkPath,
                            update = available.toUpdateAvailable()
                        )
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        _updateState.value = UpdateState.Error(updaterErrorMessage(e))
                        return
                    }

                    when (verifyResult) {
                        is VerificationResult.Failed -> {
                            _updateState.value = UpdateState.Error(verifyResult.message)
                        }
                        is VerificationResult.Success -> {
                            _updateState.value = UpdateState.ReadyToInstall(
                                version = available.version,
                                apkPath = verifyResult.apkPath
                            )
                        }
                    }
                    return
                }
                is DownloadStatus.Failed -> {
                    _updateState.value = UpdateState.Error(status.message)
                    return
                }
            }
        }
    }

    private suspend fun beginInstall(version: String, apkPath: String) {
        try {
            if (!appUpdater.canRequestPackageInstalls()) {
                _updateState.value = UpdateState.PermissionRequired(version, apkPath)
                return
            }

            val installIntent = appUpdater.createInstallIntent(apkPath)
            if (installIntent == null) {
                _updateState.value = UpdateState.Error(
                    resourceProvider.getString(R.string.error_update_install_unavailable)
                )
                return
            }

            _updateState.value = UpdateState.Installing(version)
            _settingsEvents.emit(SettingsEvent.LaunchIntent(installIntent))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _updateState.value = UpdateState.Error(updaterErrorMessage(e))
        }
    }

    private fun UpdateState.Available.toUpdateAvailable(): UpdateCheckResult.UpdateAvailable {
        return UpdateCheckResult.UpdateAvailable(
            latestVersion = version,
            downloadUrl = downloadUrl,
            releaseNotes = releaseNotes,
            assetName = assetName,
            assetSizeBytes = assetSizeBytes,
            sha256Hex = sha256Hex
        )
    }

    companion object {
        internal const val COOLDOWN_SECONDS = 60
        private const val DOWNLOAD_POLL_INTERVAL_MS = 600L
    }

    private fun updaterErrorMessage(exception: Exception): String {
        val detail = exception.message?.takeIf { it.isNotBlank() } ?: "Unexpected updater error"
        return "Update failed: $detail"
    }
}

/** State for update checking UI */
sealed class UpdateState {
    object Idle : UpdateState()
    object Checking : UpdateState()
    object UpToDate : UpdateState()
    object Dismissed : UpdateState()
    data class Available(
        val version: String,
        val downloadUrl: String,
        val releaseNotes: String,
        val assetName: String,
        val assetSizeBytes: Long,
        val sha256Hex: String
    ) : UpdateState()
    data class DownloadQueued(val version: String, val downloadId: Long) : UpdateState()
    data class Downloading(
        val version: String,
        val bytesDownloaded: Long,
        val totalBytes: Long?
    ) : UpdateState()
    data class Verifying(val version: String) : UpdateState()
    data class ReadyToInstall(val version: String, val apkPath: String) : UpdateState()
    data class PermissionRequired(val version: String, val apkPath: String) : UpdateState()
    data class Installing(val version: String) : UpdateState()
    data class Error(val message: String) : UpdateState()
}

/** One-shot events emitted by [SettingsViewModel]. */
sealed class SettingsEvent {
    /** Informational toast without sound. */
    data class ShowToast(val message: String) : SettingsEvent()

    /** Error toast that should play an error beep in UI. */
    data class ShowToastError(val message: String) : SettingsEvent()

    /** Launch an external Android intent (installer/settings). */
    data class LaunchIntent(val intent: Intent) : SettingsEvent()
}
