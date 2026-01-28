package com.kingpaging.qwelcome.viewmodel.import_pkg

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kingpaging.qwelcome.data.ImportApplyResult
import com.kingpaging.qwelcome.data.ImportExportRepository
import com.kingpaging.qwelcome.data.ImportValidationResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class ImportStep {
    object Idle : ImportStep()
    data class Validated(val validationResult: ImportValidationResult) : ImportStep()
    object Complete : ImportStep()
}

data class ImportUiState(
    val isImporting: Boolean = false,
    val step: ImportStep = ImportStep.Idle,
    val error: String? = null
)

sealed class ImportEvent {
    data class ImportSuccess(val message: String) : ImportEvent()
    data class ImportFailed(val message: String) : ImportEvent()
    object RequestFileOpen : ImportEvent()
}

class ImportViewModel(
    private val repository: ImportExportRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ImportUiState())
    val uiState: StateFlow<ImportUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ImportEvent>(replay = 1)
    val events: SharedFlow<ImportEvent> = _events.asSharedFlow()

    // Track in-flight import jobs for cancellation on reset
    private var importJob: Job? = null

    fun onOpenFileRequest() = viewModelScope.launch {
        _events.emit(ImportEvent.RequestFileOpen)
    }

    fun onJsonContentReceived(json: String) {
        if (_uiState.value.isImporting) return
        _uiState.update { it.copy(isImporting = true, error = null) }

        importJob?.cancel()
        importJob = viewModelScope.launch {
            try {
                when (val result = repository.validateImport(json)) {
                    is ImportValidationResult.ValidTemplatePack,
                    is ImportValidationResult.ValidFullBackup -> {
                        _uiState.update {
                            it.copy(
                                isImporting = false,
                                step = ImportStep.Validated(result)
                            )
                        }
                    }
                    is ImportValidationResult.Invalid -> {
                        _uiState.update {
                            it.copy(
                                isImporting = false,
                                error = result.message,
                                step = ImportStep.Idle
                            )
                        }
                    }
                }
            } catch (e: kotlin.coroutines.cancellation.CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isImporting = false,
                        error = "An unexpected error occurred: ${e.message}",
                        step = ImportStep.Idle
                    )
                }
            }
        }
    }

    fun onImportConfirmed() {
        val currentStep = _uiState.value.step
        if (currentStep !is ImportStep.Validated || _uiState.value.isImporting) return

        _uiState.update { it.copy(isImporting = true, error = null) }
        importJob?.cancel()
        importJob = viewModelScope.launch {
            try {
                // Apply the import based on the validation result type
                val applyResult = when (val validationResult = currentStep.validationResult) {
                    is ImportValidationResult.ValidTemplatePack -> {
                        // For now, replace all conflicts by default (no resolution UI yet)
                        repository.applyTemplatePack(validationResult.pack, emptyMap())
                    }
                    is ImportValidationResult.ValidFullBackup -> {
                        // For now, replace all conflicts and import everything by default
                        repository.applyFullBackup(
                            backup = validationResult.backup,
                            importTechProfile = true,
                            importDefaultTemplate = true,
                            resolutions = emptyMap()
                        )
                    }
                    is ImportValidationResult.Invalid -> {
                        // This shouldn't happen as we check before calling this function
                        _uiState.update { it.copy(isImporting = false) }
                        return@launch
                    }
                }

                when (applyResult) {
                    is ImportApplyResult.Success -> {
                        _uiState.update { it.copy(isImporting = false, step = ImportStep.Complete) }
                        val message = buildString {
                            append("Successfully imported ${applyResult.templatesImported} template")
                            if (applyResult.templatesImported != 1) append("s")
                            if (applyResult.techProfileImported) {
                                append(" and tech profile")
                            }
                        }
                        _events.emit(ImportEvent.ImportSuccess(message))
                    }
                    is ImportApplyResult.Error -> {
                        _uiState.update {
                            it.copy(
                                isImporting = false,
                                error = applyResult.message,
                                step = ImportStep.Idle // Reset on failure
                            )
                        }
                        _events.emit(ImportEvent.ImportFailed(applyResult.message))
                    }
                }
            } catch (e: kotlin.coroutines.cancellation.CancellationException) {
                throw e
            } catch (e: Exception) {
                val errorMessage = "An unexpected error occurred during import: ${e.message}"
                _uiState.update {
                    it.copy(
                        isImporting = false,
                        error = errorMessage,
                        step = ImportStep.Idle // Reset on failure
                    )
                }
                _events.emit(ImportEvent.ImportFailed(errorMessage))
            }
        }
    }
    
    fun onPasteContent(json: String) {
        onJsonContentReceived(json)
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun reset() {
        // Cancel any in-flight import operations
        importJob?.cancel()
        importJob = null
        _uiState.value = ImportUiState()
        _events.resetReplayCache()
    }
}
