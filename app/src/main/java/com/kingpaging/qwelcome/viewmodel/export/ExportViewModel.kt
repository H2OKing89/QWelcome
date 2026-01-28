package com.kingpaging.qwelcome.viewmodel.export

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kingpaging.qwelcome.data.ExportResult
import com.kingpaging.qwelcome.data.ImportExportRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ExportType {
    TEMPLATE_PACK, FULL_BACKUP
}

data class ExportUiState(
    val isExporting: Boolean = false,
    val currentlyExportingType: ExportType? = null,
    val lastExportedJson: String? = null,
    val lastExportType: ExportType? = null,
    val templateCount: Int = 0
)

sealed class ExportEvent {
    data class ExportSuccess(val type: ExportType, val json: String) : ExportEvent()
    data class ExportError(val message: String) : ExportEvent()
    data class CopiedToClipboard(val type: ExportType) : ExportEvent()
    data class ShareReady(val json: String, val type: ExportType) : ExportEvent()
    data class RequestFileSave(val suggestedName: String) : ExportEvent()
    data class FileSaved(val type: ExportType) : ExportEvent()
}

class ExportViewModel(
    private val repository: ImportExportRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExportUiState())
    val uiState: StateFlow<ExportUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ExportEvent>()
    val events: SharedFlow<ExportEvent> = _events.asSharedFlow()
    
    // To handle cases where file picker is cancelled, we need to hold the generated JSON
    // temporarily until the user decides to save it.
    private var pendingFileExportContent: String? = null

    fun exportTemplatePack() {
        if (_uiState.value.isExporting) return
        export(ExportType.TEMPLATE_PACK) {
            repository.exportTemplatePack()
        }
    }

    fun exportFullBackup() {
        if (_uiState.value.isExporting) return
        export(ExportType.FULL_BACKUP) {
            repository.exportFullBackup()
        }
    }

    private fun export(type: ExportType, action: suspend () -> ExportResult) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isExporting = true,
                    currentlyExportingType = type,
                    lastExportedJson = null, // Clear previous result
                    lastExportType = null
                )
            }
            try {
                when (val result = action()) {
                    is ExportResult.Success -> {
                        _uiState.update {
                            it.copy(
                                isExporting = false,
                                currentlyExportingType = null,
                                lastExportedJson = result.json,
                                lastExportType = type,
                                templateCount = result.templateCount
                            )
                        }
                        _events.emit(ExportEvent.ExportSuccess(type, result.json))
                    }
                    is ExportResult.Error -> {
                        _uiState.update { it.copy(isExporting = false, currentlyExportingType = null) }
                        _events.emit(ExportEvent.ExportError(result.message))
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isExporting = false, currentlyExportingType = null) }
                _events.emit(ExportEvent.ExportError("An unexpected error occurred: ${e.message}"))
            }
        }
    }
    
    fun onCopiedToClipboard() = viewModelScope.launch {
        _uiState.value.lastExportType?.let {
            _events.emit(ExportEvent.CopiedToClipboard(it))
        }
    }
    
    fun onShareRequested() = viewModelScope.launch {
        val currentState = _uiState.value
        if (currentState.lastExportedJson != null && currentState.lastExportType != null) {
            _events.emit(ExportEvent.ShareReady(currentState.lastExportedJson, currentState.lastExportType))
        }
    }
    
    fun onSaveToFileRequested() = viewModelScope.launch {
        val currentState = _uiState.value
        if (currentState.lastExportedJson != null && currentState.lastExportType != null) {
            pendingFileExportContent = currentState.lastExportedJson
            val filename = generateFileNameForExport(currentState.lastExportType)
            _events.emit(ExportEvent.RequestFileSave(filename))
        }
    }

    private fun generateFileNameForExport(type: ExportType): String {
        val timestamp = java.text.SimpleDateFormat(
            "yyyy-MM-dd_HHmmss",
            java.util.Locale.US
        ).format(java.util.Date())

        return when (type) {
            ExportType.TEMPLATE_PACK -> "qwelcome_templates_$timestamp.json"
            ExportType.FULL_BACKUP -> "qwelcome_backup_$timestamp.json"
        }
    }
    
    fun getPendingFileExportContent(): String? {
        return pendingFileExportContent
    }
    
    fun onFileSaveComplete() = viewModelScope.launch {
        _uiState.value.lastExportType?.let {
            _events.emit(ExportEvent.FileSaved(it))
        }
        pendingFileExportContent = null // Clear after use
    }
    
    fun onFileSaveCancelled() {
        pendingFileExportContent = null // Clear if user cancels
    }

    fun clearExport() {
        _uiState.update {
            it.copy(
                lastExportedJson = null,
                lastExportType = null,
                templateCount = 0
            )
        }
    }
}
