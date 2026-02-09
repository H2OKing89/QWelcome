package com.kingpaging.qwelcome.viewmodel.settings

import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kingpaging.qwelcome.BuildConfig
import com.kingpaging.qwelcome.R
import com.kingpaging.qwelcome.data.SettingsStore
import com.kingpaging.qwelcome.data.TechProfile
import com.kingpaging.qwelcome.data.Template
import com.kingpaging.qwelcome.data.UpdateCheckResult
import com.kingpaging.qwelcome.data.UpdateChecker
import com.kingpaging.qwelcome.util.ResourceProvider
import kotlinx.coroutines.CancellationException
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
    private val resourceProvider: ResourceProvider
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

    // One-shot settings events (Toasts)
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

            when (val result = UpdateChecker.checkForUpdate(currentVersion)) {
                is UpdateCheckResult.UpdateAvailable -> {
                    _updateState.value = UpdateState.Available(
                        version = result.latestVersion,
                        downloadUrl = result.downloadUrl,
                        releaseNotes = result.releaseNotes
                    )
                }
                is UpdateCheckResult.UpToDate -> {
                    _updateState.value = UpdateState.UpToDate
                }
                is UpdateCheckResult.Error -> {
                    _updateState.value = UpdateState.Error(result.message)
                }
                is UpdateCheckResult.RateLimited -> {
                    val message = if (result.retryAfterSeconds != null) {
                        "Rate limited by GitHub. Try again in ${result.retryAfterSeconds}s."
                    } else {
                        "Rate limited by GitHub. Try again later."
                    }
                    _updateState.value = UpdateState.Error(message)
                }
            }
        }
    }

    companion object {
        internal const val COOLDOWN_SECONDS = 60
    }
    
    /** Dismiss update notification */
    fun dismissUpdate() {
        _updateState.value = UpdateState.Dismissed
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
        val releaseNotes: String
    ) : UpdateState()
    data class Error(val message: String) : UpdateState()
}

/** One-shot events emitted by [SettingsViewModel]. */
sealed class SettingsEvent {
    /** Informational toast — no sound effect. */
    data class ShowToast(val message: String) : SettingsEvent()
    /** Error toast — plays an error beep before showing the message. */
    data class ShowToastError(val message: String) : SettingsEvent()
}
