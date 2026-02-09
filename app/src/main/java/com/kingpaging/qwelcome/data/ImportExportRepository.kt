package com.kingpaging.qwelcome.data

import com.kingpaging.qwelcome.util.ResourceProvider
import kotlinx.serialization.json.Json

private const val BYTES_PER_MB = 1024L * 1024L

internal fun formatBytesAsMb(bytes: Long): String = "${bytes / BYTES_PER_MB}MB"

/**
 * Maximum import size in bytes (10MB).
 * Prevents memory exhaustion from unbounded clipboard/file imports.
 */
internal const val MAX_IMPORT_SIZE_BYTES = 10 * 1024 * 1024

/**
 * Repository for handling JSON import/export of templates and settings.
 *
 * Provides:
 * - Export to JSON (pretty printed for sharing)
 * - Import with validation and conflict detection
 * - Schema version checking
 * - Template conflict detection
 */
class ImportExportRepository(
    settingsStore: SettingsStore,
    resourceProvider: ResourceProvider
) {

    private val json = Json {
        ignoreUnknownKeys = true  // Forward compatibility
        isLenient = true          // Tolerates trailing commas, unquoted strings (clipboard UX)
        prettyPrint = true        // Human readable for sharing
        encodeDefaults = true     // Include default values
    }

    private val exportService = ExportService(
        settingsStore = settingsStore,
        resourceProvider = resourceProvider,
        json = json
    )
    private val importValidationService = ImportValidationService(
        settingsStore = settingsStore,
        resourceProvider = resourceProvider,
        json = json
    )
    private val importApplyService = ImportApplyService(
        settingsStore = settingsStore,
        resourceProvider = resourceProvider
    )

    suspend fun exportTemplatePack(templateIds: List<String> = emptyList()): ExportResult =
        exportService.exportTemplatePack(templateIds)

    suspend fun exportFullBackup(): ExportResult =
        exportService.exportFullBackup()

    suspend fun validateImport(jsonString: String): ImportValidationResult =
        importValidationService.validateImport(jsonString)

    suspend fun applyTemplatePack(
        pack: TemplatePack,
        resolutions: Map<String, ConflictResolution> = emptyMap()
    ): ImportApplyResult = importApplyService.applyTemplatePack(pack, resolutions)

    suspend fun applyFullBackup(
        backup: FullBackup,
        importTechProfile: Boolean = false,
        importDefaultTemplate: Boolean = true,
        resolutions: Map<String, ConflictResolution> = emptyMap()
    ): ImportApplyResult = importApplyService.applyFullBackup(
        backup = backup,
        importTechProfile = importTechProfile,
        importDefaultTemplate = importDefaultTemplate,
        resolutions = resolutions
    )
}

// ========== Result Types ==========

/**
 * Result of an export operation.
 */
sealed class ExportResult {
    data class Success(
        val json: String,
        val templateCount: Int
    ) : ExportResult()

    data class Error(val message: String) : ExportResult()
}

/**
 * Result of validating an import.
 */
sealed class ImportValidationResult {
    data class ValidTemplatePack(
        val pack: TemplatePack,
        val conflicts: List<TemplateConflict>,
        val warnings: List<ImportWarning>
    ) : ImportValidationResult() {
        val hasConflicts: Boolean get() = conflicts.isNotEmpty()
        val hasWarnings: Boolean get() = warnings.isNotEmpty()
    }

    data class ValidFullBackup(
        val backup: FullBackup,
        val conflicts: List<TemplateConflict>,
        val warnings: List<ImportWarning>
    ) : ImportValidationResult() {
        val hasConflicts: Boolean get() = conflicts.isNotEmpty()
        val hasWarnings: Boolean get() = warnings.isNotEmpty()
        val hasTechProfile: Boolean get() = true
    }

    data class Invalid(val message: String) : ImportValidationResult()
}

/**
 * Result of applying an import.
 */
sealed class ImportApplyResult {
    data class Success(
        val templatesImported: Int,
        val techProfileImported: Boolean = false
    ) : ImportApplyResult()

    data class Error(val message: String) : ImportApplyResult()
}

/**
 * Represents a conflict between an imported template and an existing one.
 */
data class TemplateConflict(
    val importedTemplate: Template,
    val existingTemplate: Template
) {
    val templateId: String get() = importedTemplate.id
    val templateName: String get() = importedTemplate.name
}

/**
 * How to resolve a template conflict.
 */
enum class ConflictResolution {
    /** Replace the existing template with the imported one */
    REPLACE,
    /** Keep the existing template, skip the imported one */
    KEEP_EXISTING,
    /** Save the imported template as a new copy with a new ID */
    SAVE_AS_COPY
}

/**
 * Warnings generated during import validation.
 * These don't block import but inform the user of potential issues.
 */
sealed class ImportWarning {
    /**
     * The import was created by a newer version of the app.
     */
    data class NewerVersion(
        val importVersion: Int,
        val supportedVersion: Int
    ) : ImportWarning() {
        override fun toString() = "Created by newer app version (schema v$importVersion, this app supports v$supportedVersion)"
    }

    /**
     * A template is missing a required field.
     */
    data class MissingField(
        val templateIdentifier: String,
        val fieldName: String
    ) : ImportWarning() {
        override fun toString() = "Template '$templateIdentifier' is missing '$fieldName'"
    }

    /**
     * A template has unusually long content.
     */
    data class LongContent(
        val templateName: String,
        val length: Int
    ) : ImportWarning() {
        override fun toString() = "Template '$templateName' has long content ($length chars)"
    }

    /**
     * A template is missing common placeholders.
     */
    data class MissingPlaceholders(
        val templateName: String,
        val placeholders: List<String>
    ) : ImportWarning() {
        override fun toString() = "Template '$templateName' is missing placeholders: ${placeholders.joinToString(", ")}"
    }
}
