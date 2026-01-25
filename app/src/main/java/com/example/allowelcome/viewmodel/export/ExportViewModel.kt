package com.example.allowelcome.viewmodel.export

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.allowelcome.data.ExportResult
import com.example.allowelcome.data.ImportExportRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "ExportViewModel"

/**
 * UI state for the export screen.
 */
data class ExportUiState(
    val isExporting: Boolean = false,
    val currentlyExportingType: ExportType? = null,
    val lastExportedJson: String? = null,
    val lastExportType: ExportType? = null,
    val templateCount: Int = 0,
    // File export state
    val pendingFileExportJson: String? = null,
    val pendingFileExportType: ExportType? = null
)

/**
 * Type of export being performed.
 */
enum class ExportType {
    TEMPLATE_PACK,
    FULL_BACKUP
}

/**
 * One-shot events for the export screen.
 */
sealed class ExportEvent {
    data class ExportSuccess(val json: String, val type: ExportType, val templateCount: Int) : ExportEvent()
    data class ExportError(val message: String) : ExportEvent()
    data class CopiedToClipboard(val type: ExportType) : ExportEvent()
    data class ShareReady(val json: String, val type: ExportType) : ExportEvent()
    /** Request to open file picker for saving */
    data class RequestFileSave(val suggestedName: String, val type: ExportType) : ExportEvent()
    /** File was saved successfully */
    data class FileSaved(val type: ExportType) : ExportEvent()
}

/**
 * ViewModel for the export screen.
 * Handles exporting templates and full backups to JSON.
 */
class ExportViewModel(
    private val repository: ImportExportRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExportUiState())
    val uiState: StateFlow<ExportUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ExportEvent>(replay = 0, extraBufferCapacity = 1)
    val events: SharedFlow<ExportEvent> = _events.asSharedFlow()

    /**
     * Export all user templates as a Template Pack.
     */
    fun exportTemplatePack() {
        if (_uiState.value.isExporting) return

        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, currentlyExportingType = ExportType.TEMPLATE_PACK) }

            try {
                when (val result = repository.exportTemplatePack()) {
                    is ExportResult.Success -> {
                        _uiState.update {
                            it.copy(
                                isExporting = false,
                                currentlyExportingType = null,
                                lastExportedJson = result.json,
                                lastExportType = ExportType.TEMPLATE_PACK,
                                templateCount = result.templateCount
                            )
                        }
                        _events.emit(
                            ExportEvent.ExportSuccess(
                                json = result.json,
                                type = ExportType.TEMPLATE_PACK,
                                templateCount = result.templateCount
                            )
                        )
                    }
                    is ExportResult.Error -> {
                        _uiState.update { it.copy(isExporting = false, currentlyExportingType = null) }
                        _events.emit(ExportEvent.ExportError(result.message))
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to export template pack", e)
                _uiState.update { it.copy(isExporting = false, currentlyExportingType = null) }
                _events.emit(ExportEvent.ExportError("Export failed: ${e.message}"))
            }
        }
    }

    /**
     * Export everything as a Full Backup.
     */
    fun exportFullBackup() {
        if (_uiState.value.isExporting) return

        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, currentlyExportingType = ExportType.FULL_BACKUP) }

            try {
                when (val result = repository.exportFullBackup()) {
                    is ExportResult.Success -> {
                        _uiState.update {
                            it.copy(
                                isExporting = false,
                                currentlyExportingType = null,
                                lastExportedJson = result.json,
                                lastExportType = ExportType.FULL_BACKUP,
                                templateCount = result.templateCount
                            )
                        }
                        _events.emit(
                            ExportEvent.ExportSuccess(
                                json = result.json,
                                type = ExportType.FULL_BACKUP,
                                templateCount = result.templateCount
                            )
                        )
                    }
                    is ExportResult.Error -> {
                        _uiState.update { it.copy(isExporting = false, currentlyExportingType = null) }
                        _events.emit(ExportEvent.ExportError(result.message))
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to export full backup", e)
                _uiState.update { it.copy(isExporting = false, currentlyExportingType = null) }
                _events.emit(ExportEvent.ExportError("Export failed: ${e.message}"))
            }
        }
    }

    /**
     * Notify that JSON was copied to clipboard.
     */
    fun onCopiedToClipboard() {
        val type = _uiState.value.lastExportType ?: return
        viewModelScope.launch {
            _events.emit(ExportEvent.CopiedToClipboard(type))
        }
    }

    /**
     * Prepare to share the last exported JSON.
     */
    fun onShareRequested() {
        val json = _uiState.value.lastExportedJson ?: return
        val type = _uiState.value.lastExportType ?: return
        viewModelScope.launch {
            _events.emit(ExportEvent.ShareReady(json, type))
        }
    }

    /**
     * Clear the last export state.
     */
    fun clearExport() {
        _uiState.update {
            it.copy(
                lastExportedJson = null,
                lastExportType = null,
                templateCount = 0,
                pendingFileExportJson = null,
                pendingFileExportType = null
            )
        }
    }

    /**
     * Request to save current export to a file.
     * Triggers file picker via event.
     */
    fun onSaveToFileRequested() {
        val json = _uiState.value.lastExportedJson ?: return
        val type = _uiState.value.lastExportType ?: return

        // Store pending export for when file picker returns
        _uiState.update {
            it.copy(
                pendingFileExportJson = json,
                pendingFileExportType = type
            )
        }

        val suggestedName = when (type) {
            ExportType.TEMPLATE_PACK -> "q-welcome-templates.json"
            ExportType.FULL_BACKUP -> "q-welcome-backup.json"
        }

        viewModelScope.launch {
            _events.emit(ExportEvent.RequestFileSave(suggestedName, type))
        }
    }

    /**
     * Called when user selects a file location from the picker.
     * Returns the JSON content to write to the file.
     */
    fun getPendingFileExportContent(): String? {
        return _uiState.value.pendingFileExportJson
    }

    /**
     * Called after file has been written successfully.
     */
    fun onFileSaveComplete() {
        val type = _uiState.value.pendingFileExportType ?: return
        _uiState.update {
            it.copy(
                pendingFileExportJson = null,
                pendingFileExportType = null
            )
        }
        viewModelScope.launch {
            _events.emit(ExportEvent.FileSaved(type))
        }
    }

    /**
     * Called if file save was cancelled or failed.
     */
    fun onFileSaveCancelled() {
        _uiState.update {
            it.copy(
                pendingFileExportJson = null,
                pendingFileExportType = null
            )
        }
    }
}
