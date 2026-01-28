package com.kingpaging.qwelcome.viewmodel.export

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kingpaging.qwelcome.data.DEFAULT_TEMPLATE_ID
import com.kingpaging.qwelcome.data.ExportResult
import com.kingpaging.qwelcome.data.ImportExportRepository
import com.kingpaging.qwelcome.data.SettingsStore
import com.kingpaging.qwelcome.data.Template
import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

enum class ExportType {
    TEMPLATE_PACK, FULL_BACKUP
}

data class ExportUiState(
    val isExporting: Boolean = false,
    val currentlyExportingType: ExportType? = null,
    val lastExportedJson: String? = null,
    val lastExportType: ExportType? = null,
    val templateCount: Int = 0,
    // Template selection state
    val availableTemplates: List<Template> = emptyList(),
    val selectedTemplateIds: Set<String> = emptySet(),
    val showTemplateSelectionDialog: Boolean = false
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
    private val repository: ImportExportRepository,
    private val settingsStore: SettingsStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExportUiState())
    val uiState: StateFlow<ExportUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ExportEvent>(replay = 1)
    val events: SharedFlow<ExportEvent> = _events.asSharedFlow()

    // Thread-safe storage for pending file export content and type.
    // Uses StateFlow to avoid race conditions when exporting twice rapidly.
    private val _pendingFileExportContent = MutableStateFlow<String?>(null)
    private val _pendingFileExportType = MutableStateFlow<ExportType?>(null)

    companion object {
        private const val TAG = "ExportViewModel"
    }

    // ========== Template Selection ==========

    /**
     * Called when user clicks Template Pack - loads templates and shows selection dialog.
     */
    fun onTemplatePackRequested() {
        if (_uiState.value.isExporting) return
        viewModelScope.launch {
            val templates = settingsStore.getUserTemplates()
            if (templates.isEmpty()) {
                _events.emit(ExportEvent.ExportError("No custom templates to export"))
                return@launch
            }
            _uiState.update {
                it.copy(
                    availableTemplates = templates,
                    selectedTemplateIds = templates.map { t -> t.id }.toSet(), // Select all by default
                    showTemplateSelectionDialog = true
                )
            }
        }
    }

    /**
     * Toggle selection of a single template.
     */
    fun toggleTemplateSelection(templateId: String) {
        _uiState.update { state ->
            val newSelection = if (templateId in state.selectedTemplateIds) {
                state.selectedTemplateIds - templateId
            } else {
                state.selectedTemplateIds + templateId
            }
            state.copy(selectedTemplateIds = newSelection)
        }
    }

    /**
     * Toggle select all / deselect all.
     */
    fun toggleSelectAll() {
        _uiState.update { state ->
            val allIds = state.availableTemplates.map { it.id }.toSet()
            val newSelection = if (state.selectedTemplateIds == allIds) {
                emptySet()
            } else {
                allIds
            }
            state.copy(selectedTemplateIds = newSelection)
        }
    }

    /**
     * Dismiss the template selection dialog.
     */
    fun dismissTemplateSelection() {
        _uiState.update {
            it.copy(showTemplateSelectionDialog = false)
        }
    }

    /**
     * Export only the selected templates.
     */
    fun exportSelectedTemplates() {
        val selectedIds = _uiState.value.selectedTemplateIds.toList()
        if (selectedIds.isEmpty()) return

        _uiState.update { it.copy(showTemplateSelectionDialog = false) }

        export(ExportType.TEMPLATE_PACK) {
            repository.exportTemplatePack(templateIds = selectedIds)
        }
    }

    /**
     * Check if there are user templates available for export.
     */
    fun hasUserTemplates(): Boolean = _uiState.value.availableTemplates.isNotEmpty()

    // ========== Direct Export Functions ==========

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
            } catch (e: kotlin.coroutines.cancellation.CancellationException) {
                throw e
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
            _pendingFileExportContent.value = currentState.lastExportedJson
            _pendingFileExportType.value = currentState.lastExportType
            val filename = generateFileNameForExport(currentState.lastExportType)
            _events.emit(ExportEvent.RequestFileSave(filename))
        }
    }

    private fun generateFileNameForExport(type: ExportType): String {
        // Using java.time API for thread-safety (SimpleDateFormat is not thread-safe)
        val timestamp = LocalDateTime.now().format(
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss")
        )

        return when (type) {
            ExportType.TEMPLATE_PACK -> "qwelcome_templates_$timestamp.json"
            ExportType.FULL_BACKUP -> "qwelcome_backup_$timestamp.json"
        }
    }

    /**
     * Atomically retrieves and clears the pending export content.
     * Thread-safe implementation using StateFlow.
     */
    fun getPendingFileExportContent(): String? {
        return _pendingFileExportContent.getAndUpdate { null }
    }

    fun onFileSaveComplete() = viewModelScope.launch {
        // Use stored pending type first, then fall back to UI state
        val exportType = _pendingFileExportType.getAndUpdate { null }
            ?: _uiState.value.lastExportType

        if (exportType != null) {
            _events.emit(ExportEvent.FileSaved(exportType))
        } else {
            Log.w(TAG, "onFileSaveComplete called but no export type available")
        }
        // Content already cleared by getPendingFileExportContent()
    }

    fun onFileSaveCancelled() {
        _pendingFileExportContent.value = null
        _pendingFileExportType.value = null
    }

    /**
     * Reset the ViewModel state when entering the screen.
     * Clears any stale events from the replay cache.
     */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun reset() {
        _uiState.value = ExportUiState()
        _pendingFileExportContent.value = null
        _pendingFileExportType.value = null
        _events.resetReplayCache()
    }
}
