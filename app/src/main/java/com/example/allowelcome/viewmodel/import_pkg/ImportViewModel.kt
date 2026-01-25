package com.example.allowelcome.viewmodel.import_pkg

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.allowelcome.data.ConflictResolution
import com.example.allowelcome.data.FullBackup
import com.example.allowelcome.data.ImportApplyResult
import com.example.allowelcome.data.ImportExportRepository
import com.example.allowelcome.data.ImportValidationResult
import com.example.allowelcome.data.ImportWarning
import com.example.allowelcome.data.Template
import com.example.allowelcome.data.TemplateConflict
import com.example.allowelcome.data.TemplatePack
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "ImportViewModel"

/**
 * Import screen states representing the import flow.
 */
enum class ImportStep {
    /** Initial state - paste/input JSON */
    INPUT,
    /** Validating the JSON */
    VALIDATING,
    /** Showing preview with templates and options */
    PREVIEW,
    /** Applying the import */
    APPLYING,
    /** Import complete */
    COMPLETE
}

/**
 * Template status in the import preview.
 */
enum class TemplateImportStatus {
    /** New template, will be added */
    NEW,
    /** Template exists, will be replaced */
    WILL_REPLACE,
    /** Template exists, conflict needs resolution */
    CONFLICT
}

/**
 * Preview data for a single template in the import.
 */
data class TemplatePreviewItem(
    val template: Template,
    val status: TemplateImportStatus,
    val existingTemplate: Template? = null,
    val isSelected: Boolean = true,
    val conflictResolution: ConflictResolution = ConflictResolution.REPLACE
)

/**
 * UI state for the import screen.
 */
data class ImportUiState(
    val step: ImportStep = ImportStep.INPUT,
    val jsonInput: String = "",
    val isProcessing: Boolean = false,
    val errorMessage: String? = null,
    
    // Preview state
    val importKind: String? = null,
    val packName: String? = null,
    val templatePreviews: List<TemplatePreviewItem> = emptyList(),
    val warnings: List<ImportWarning> = emptyList(),
    
    // Full backup specific
    val hasTechProfile: Boolean = false,
    val importTechProfile: Boolean = false,
    val techProfileName: String? = null,
    
    // Import result
    val importedCount: Int = 0
) {
    val hasConflicts: Boolean get() = templatePreviews.any { it.status == TemplateImportStatus.CONFLICT }
    val selectedCount: Int get() = templatePreviews.count { it.isSelected }
    val canApply: Boolean get() = selectedCount > 0 && !isProcessing
}

/**
 * One-shot events for the import screen.
 */
sealed class ImportEvent {
    data object ValidationSuccess : ImportEvent()
    data class ValidationError(val message: String) : ImportEvent()
    data class ImportSuccess(val count: Int, val techProfileImported: Boolean = false) : ImportEvent()
    data class ImportError(val message: String) : ImportEvent()
    /** Request to open file picker for loading JSON */
    data object RequestFileLoad : ImportEvent()
    /** File was loaded and JSON text is ready */
    data class FileLoaded(val fileName: String) : ImportEvent()
}

/**
 * ViewModel for the import screen.
 * Handles JSON validation, preview, and applying imports.
 */
class ImportViewModel(
    private val repository: ImportExportRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ImportUiState())
    val uiState: StateFlow<ImportUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ImportEvent>(replay = 0, extraBufferCapacity = 1)
    val events: SharedFlow<ImportEvent> = _events.asSharedFlow()

    // Cached validation result for applying
    private var cachedTemplatePack: TemplatePack? = null
    private var cachedFullBackup: FullBackup? = null

    /**
     * Update the JSON input text.
     */
    fun updateJsonInput(json: String) {
        _uiState.update { it.copy(jsonInput = json, errorMessage = null) }
    }

    /**
     * Validate the current JSON input.
     */
    fun validateInput() {
        val json = _uiState.value.jsonInput
        if (json.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please paste JSON to import") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(step = ImportStep.VALIDATING, isProcessing = true, errorMessage = null) }

            try {
                when (val result = repository.validateImport(json)) {
                    is ImportValidationResult.ValidTemplatePack -> {
                        cachedTemplatePack = result.pack
                        cachedFullBackup = null
                        
                        val previews = result.pack.templates.map { template ->
                            val conflict = result.conflicts.find { it.templateId == template.id }
                            TemplatePreviewItem(
                                template = template,
                                status = when {
                                    conflict != null -> TemplateImportStatus.CONFLICT
                                    else -> TemplateImportStatus.NEW
                                },
                                existingTemplate = conflict?.existingTemplate,
                                isSelected = true,
                                conflictResolution = ConflictResolution.REPLACE
                            )
                        }
                        
                        _uiState.update {
                            it.copy(
                                step = ImportStep.PREVIEW,
                                isProcessing = false,
                                importKind = "Template Pack",
                                packName = null, // Template packs don't have a name in current schema
                                templatePreviews = previews,
                                warnings = result.warnings,
                                hasTechProfile = false
                            )
                        }
                        _events.emit(ImportEvent.ValidationSuccess)
                    }
                    
                    is ImportValidationResult.ValidFullBackup -> {
                        cachedTemplatePack = null
                        cachedFullBackup = result.backup
                        
                        val previews = result.backup.templates.map { template ->
                            val conflict = result.conflicts.find { it.templateId == template.id }
                            TemplatePreviewItem(
                                template = template,
                                status = when {
                                    conflict != null -> TemplateImportStatus.CONFLICT
                                    else -> TemplateImportStatus.NEW
                                },
                                existingTemplate = conflict?.existingTemplate,
                                isSelected = true,
                                conflictResolution = ConflictResolution.REPLACE
                            )
                        }
                        
                        _uiState.update {
                            it.copy(
                                step = ImportStep.PREVIEW,
                                isProcessing = false,
                                importKind = "Full Backup",
                                packName = null,
                                templatePreviews = previews,
                                warnings = result.warnings,
                                hasTechProfile = true,
                                importTechProfile = false, // Default unchecked per design
                                techProfileName = result.backup.techProfile.name
                            )
                        }
                        _events.emit(ImportEvent.ValidationSuccess)
                    }
                    
                    is ImportValidationResult.Invalid -> {
                        _uiState.update {
                            it.copy(
                                step = ImportStep.INPUT,
                                isProcessing = false,
                                errorMessage = result.message
                            )
                        }
                        _events.emit(ImportEvent.ValidationError(result.message))
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Validation failed", e)
                _uiState.update {
                    it.copy(
                        step = ImportStep.INPUT,
                        isProcessing = false,
                        errorMessage = "Validation failed: ${e.message}"
                    )
                }
                _events.emit(ImportEvent.ValidationError("Validation failed: ${e.message}"))
            }
        }
    }

    /**
     * Toggle whether a template is selected for import.
     */
    fun toggleTemplateSelection(templateId: String) {
        _uiState.update { state ->
            state.copy(
                templatePreviews = state.templatePreviews.map { preview ->
                    if (preview.template.id == templateId) {
                        preview.copy(isSelected = !preview.isSelected)
                    } else {
                        preview
                    }
                }
            )
        }
    }

    /**
     * Set conflict resolution for a specific template.
     */
    fun setConflictResolution(templateId: String, resolution: ConflictResolution) {
        _uiState.update { state ->
            state.copy(
                templatePreviews = state.templatePreviews.map { preview ->
                    if (preview.template.id == templateId) {
                        preview.copy(conflictResolution = resolution)
                    } else {
                        preview
                    }
                }
            )
        }
    }

    /**
     * Toggle whether to import the tech profile (full backup only).
     */
    fun toggleImportTechProfile() {
        _uiState.update { it.copy(importTechProfile = !it.importTechProfile) }
    }

    /**
     * Apply the validated import with selected options.
     */
    fun applyImport() {
        val state = _uiState.value
        if (!state.canApply) return

        viewModelScope.launch {
            _uiState.update { it.copy(step = ImportStep.APPLYING, isProcessing = true) }

            try {
                // Build resolutions map from selected templates
                val selectedTemplates = state.templatePreviews.filter { it.isSelected }
                val resolutions = selectedTemplates
                    .filter { it.status == TemplateImportStatus.CONFLICT }
                    .associate { it.template.id to it.conflictResolution }

                // Filter to only selected template IDs
                val selectedIds = selectedTemplates.map { it.template.id }.toSet()

                // Capture cached values to avoid null assertions
                val localPack = cachedTemplatePack
                val localBackup = cachedFullBackup

                val result = when {
                    localPack != null -> {
                        val filteredPack = localPack.copy(
                            templates = localPack.templates.filter { it.id in selectedIds }
                        )
                        repository.applyTemplatePack(filteredPack, resolutions)
                    }
                    localBackup != null -> {
                        val filteredBackup = localBackup.copy(
                            templates = localBackup.templates.filter { it.id in selectedIds }
                        )
                        repository.applyFullBackup(
                            backup = filteredBackup,
                            importTechProfile = state.importTechProfile,
                            importDefaultTemplate = true,
                            resolutions = resolutions
                        )
                    }
                    else -> {
                        ImportApplyResult.Error("No validated import to apply")
                    }
                }

                when (result) {
                    is ImportApplyResult.Success -> {
                        _uiState.update {
                            it.copy(
                                step = ImportStep.COMPLETE,
                                isProcessing = false,
                                importedCount = result.templatesImported
                            )
                        }
                        _events.emit(ImportEvent.ImportSuccess(result.templatesImported, result.techProfileImported))
                    }
                    is ImportApplyResult.Error -> {
                        _uiState.update {
                            it.copy(
                                step = ImportStep.PREVIEW,
                                isProcessing = false,
                                errorMessage = result.message
                            )
                        }
                        _events.emit(ImportEvent.ImportError(result.message))
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Apply import failed", e)
                _uiState.update {
                    it.copy(
                        step = ImportStep.PREVIEW,
                        isProcessing = false,
                        errorMessage = "Import failed: ${e.message}"
                    )
                }
                _events.emit(ImportEvent.ImportError("Import failed: ${e.message}"))
            }
        }
    }

    /**
     * Go back to input step from preview.
     */
    fun backToInput() {
        cachedTemplatePack = null
        cachedFullBackup = null
        _uiState.update {
            ImportUiState(
                step = ImportStep.INPUT,
                jsonInput = it.jsonInput
            )
        }
    }

    /**
     * Reset to initial state.
     */
    fun reset() {
        cachedTemplatePack = null
        cachedFullBackup = null
        _uiState.value = ImportUiState()
    }

    /**
     * Request to open file picker for importing from a file.
     */
    fun requestFileLoad() {
        viewModelScope.launch {
            _events.emit(ImportEvent.RequestFileLoad)
        }
    }

    /**
     * Called when a file has been loaded from the file picker.
     * @param json The JSON content read from the file
     * @param fileName The name of the file that was loaded
     */
    fun onFileLoaded(json: String, fileName: String) {
        _uiState.update { it.copy(jsonInput = json) }
        viewModelScope.launch {
            _events.emit(ImportEvent.FileLoaded(fileName))
        }
    }
}
