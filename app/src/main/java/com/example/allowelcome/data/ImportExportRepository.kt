package com.example.allowelcome.data

import android.util.Log
import com.example.allowelcome.BuildConfig
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val TAG = "ImportExportRepository"

/**
 * Maximum supported schema version for imports.
 * Imports with higher versions will show a warning but attempt to parse known fields.
 */
private const val MAX_SUPPORTED_SCHEMA_VERSION = EXPORT_SCHEMA_VERSION

/**
 * Content length threshold for warning (not blocking).
 */
private const val TEMPLATE_CONTENT_WARNING_LENGTH = 2000

/**
 * Repository for handling JSON import/export of templates and settings.
 *
 * Provides:
 * - Export to JSON (pretty printed for sharing)
 * - Import with validation and conflict detection
 * - Schema version checking
 * - Template conflict detection
 */
class ImportExportRepository(private val settingsStore: SettingsStore) {

    private val json = Json {
        ignoreUnknownKeys = true  // Forward compatibility
        prettyPrint = true        // Human readable for sharing
        encodeDefaults = true     // Include default values
    }

    // ========== Export Functions ==========

    /**
     * Export selected templates as a Template Pack JSON string.
     *
     * @param templateIds List of template IDs to export. If empty, exports all user templates.
     * @return ExportResult with JSON string or error
     */
    suspend fun exportTemplatePack(templateIds: List<String> = emptyList()): ExportResult {
        return try {
            val allTemplates = settingsStore.getAllTemplates()
            val templatesToExport = if (templateIds.isEmpty()) {
                // Export all user templates (exclude built-in default)
                allTemplates.filter { it.id != DEFAULT_TEMPLATE_ID }
            } else {
                allTemplates.filter { it.id in templateIds && it.id != DEFAULT_TEMPLATE_ID }
            }

            if (templatesToExport.isEmpty()) {
                return ExportResult.Error("No templates to export")
            }

            val pack = TemplatePack.create(
                templates = templatesToExport,
                appVersion = getAppVersion()
            )

            val jsonString = json.encodeToString(pack)
            ExportResult.Success(jsonString, templatesToExport.size)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to export template pack", e)
            ExportResult.Error("Failed to export: ${e.message}")
        }
    }

    /**
     * Export everything as a Full Backup JSON string.
     *
     * @return ExportResult with JSON string or error
     */
    suspend fun exportFullBackup(): ExportResult {
        return try {
            val techProfile = settingsStore.getTechProfile()
            val allTemplates = settingsStore.getAllTemplates()
            val userTemplates = allTemplates.filter { it.id != DEFAULT_TEMPLATE_ID }
            val activeTemplateId = settingsStore.getActiveTemplateId()

            val backup = FullBackup.create(
                techProfile = techProfile,
                templates = userTemplates,
                defaultTemplateId = activeTemplateId.takeIf { it != DEFAULT_TEMPLATE_ID },
                appVersion = getAppVersion()
            )

            val jsonString = json.encodeToString(backup)
            ExportResult.Success(jsonString, userTemplates.size)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to export full backup", e)
            ExportResult.Error("Failed to export: ${e.message}")
        }
    }

    // ========== Import Functions ==========

    /**
     * Parse and validate an import JSON string.
     * Does NOT apply the import - just parses and returns what would be imported.
     *
     * @param jsonString The JSON string to parse
     * @return ImportValidationResult with parsed data and any warnings/conflicts
     */
    suspend fun validateImport(jsonString: String): ImportValidationResult {
        // Step 1: Basic JSON parsing
        val trimmedJson = jsonString.trim()
        if (trimmedJson.isBlank()) {
            return ImportValidationResult.Invalid("Empty input")
        }

        // Step 2: Parse metadata to determine kind
        val metadata = try {
            json.decodeFromString<ExportMetadata>(trimmedJson)
        } catch (e: SerializationException) {
            Log.e(TAG, "Failed to parse export metadata", e)
            return ImportValidationResult.Invalid("Invalid JSON format: ${e.message}")
        }

        // Step 3: Schema version check
        val warnings = mutableListOf<ImportWarning>()
        if (metadata.schemaVersion > MAX_SUPPORTED_SCHEMA_VERSION) {
            warnings.add(
                ImportWarning.NewerVersion(
                    importVersion = metadata.schemaVersion,
                    supportedVersion = MAX_SUPPORTED_SCHEMA_VERSION
                )
            )
        }

        // Step 4: Parse based on kind
        return when (metadata.kind) {
            ExportKind.TEMPLATE_PACK -> parseTemplatePack(trimmedJson, warnings)
            ExportKind.FULL_BACKUP -> parseFullBackup(trimmedJson, warnings)
            else -> ImportValidationResult.Invalid("Unknown export kind: '${metadata.kind}'")
        }
    }

    private suspend fun parseTemplatePack(
        jsonString: String,
        warnings: MutableList<ImportWarning>
    ): ImportValidationResult {
        val pack = try {
            json.decodeFromString<TemplatePack>(jsonString)
        } catch (e: SerializationException) {
            Log.e(TAG, "Failed to parse template pack", e)
            return ImportValidationResult.Invalid("Invalid template pack format: ${e.message}")
        }

        // Validate templates
        val templateWarnings = validateTemplates(pack.templates)
        warnings.addAll(templateWarnings)

        if (pack.templates.isEmpty()) {
            return ImportValidationResult.Invalid("Template pack contains no templates")
        }

        // Detect conflicts with existing templates
        val existingTemplates = settingsStore.getUserTemplates()
        val conflicts = detectConflicts(pack.templates, existingTemplates)

        return ImportValidationResult.ValidTemplatePack(
            pack = pack,
            conflicts = conflicts,
            warnings = warnings
        )
    }

    private suspend fun parseFullBackup(
        jsonString: String,
        warnings: MutableList<ImportWarning>
    ): ImportValidationResult {
        val backup = try {
            json.decodeFromString<FullBackup>(jsonString)
        } catch (e: SerializationException) {
            Log.e(TAG, "Failed to parse full backup", e)
            return ImportValidationResult.Invalid("Invalid backup format: ${e.message}")
        }

        // Validate templates
        val templateWarnings = validateTemplates(backup.templates)
        warnings.addAll(templateWarnings)

        // Detect conflicts with existing templates
        val existingTemplates = settingsStore.getUserTemplates()
        val conflicts = detectConflicts(backup.templates, existingTemplates)

        return ImportValidationResult.ValidFullBackup(
            backup = backup,
            conflicts = conflicts,
            warnings = warnings
        )
    }

    /**
     * Validate a list of templates and return any warnings.
     */
    private fun validateTemplates(templates: List<Template>): List<ImportWarning> {
        val warnings = mutableListOf<ImportWarning>()

        templates.forEach { template ->
            // Check required fields
            if (template.id.isBlank()) {
                warnings.add(ImportWarning.MissingField(template.name, "id"))
            }
            if (template.name.isBlank()) {
                warnings.add(ImportWarning.MissingField(template.id, "name"))
            }
            if (template.content.isBlank()) {
                warnings.add(ImportWarning.MissingField(template.name.ifBlank { template.id }, "content"))
            }

            // Check content length
            if (template.content.length > TEMPLATE_CONTENT_WARNING_LENGTH) {
                warnings.add(
                    ImportWarning.LongContent(
                        templateName = template.name,
                        length = template.content.length
                    )
                )
            }

            // Check for common placeholders (warn but don't block)
            val missingPlaceholders = findMissingPlaceholders(template.content)
            if (missingPlaceholders.isNotEmpty()) {
                warnings.add(
                    ImportWarning.MissingPlaceholders(
                        templateName = template.name,
                        placeholders = missingPlaceholders
                    )
                )
            }
        }

        return warnings
    }

    /**
     * Find commonly expected placeholders that are missing from template content.
     */
    private fun findMissingPlaceholders(content: String): List<String> {
        val expectedPlaceholders = listOf(
            MessageTemplate.KEY_CUSTOMER_NAME,
            MessageTemplate.KEY_SSID,
            MessageTemplate.KEY_PASSWORD
        )
        return expectedPlaceholders.filter { !content.contains(it) }
    }

    /**
     * Detect conflicts between imported templates and existing templates.
     */
    private fun detectConflicts(
        importTemplates: List<Template>,
        existingTemplates: List<Template>
    ): List<TemplateConflict> {
        val existingIds = existingTemplates.associateBy { it.id }
        
        return importTemplates.mapNotNull { importTemplate ->
            val existing = existingIds[importTemplate.id]
            if (existing != null) {
                TemplateConflict(
                    importedTemplate = importTemplate,
                    existingTemplate = existing
                )
            } else {
                null
            }
        }
    }

    // ========== Apply Import Functions ==========

    /**
     * Apply a validated template pack import.
     *
     * @param pack The validated template pack
     * @param resolutions Map of template ID to conflict resolution
     * @return ImportApplyResult indicating success or failure
     */
    suspend fun applyTemplatePack(
        pack: TemplatePack,
        resolutions: Map<String, ConflictResolution> = emptyMap()
    ): ImportApplyResult {
        return try {
            val templatesToSave = resolveTemplates(pack.templates, resolutions)
            settingsStore.saveTemplates(templatesToSave)
            ImportApplyResult.Success(templatesToSave.size)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to apply template pack", e)
            ImportApplyResult.Error("Failed to apply import: ${e.message}")
        }
    }

    /**
     * Apply a validated full backup import.
     *
     * @param backup The validated backup
     * @param importTechProfile Whether to import the tech profile
     * @param importDefaultTemplate Whether to set the default template
     * @param resolutions Map of template ID to conflict resolution
     * @return ImportApplyResult indicating success or failure
     */
    suspend fun applyFullBackup(
        backup: FullBackup,
        importTechProfile: Boolean = false,
        importDefaultTemplate: Boolean = true,
        resolutions: Map<String, ConflictResolution> = emptyMap()
    ): ImportApplyResult {
        return try {
            // Import templates
            val templatesToSave = resolveTemplates(backup.templates, resolutions)
            settingsStore.saveTemplates(templatesToSave)

            // Import tech profile if requested
            if (importTechProfile) {
                settingsStore.saveTechProfile(
                    TechProfile(
                        name = backup.techProfile.name,
                        title = backup.techProfile.title,
                        dept = backup.techProfile.dept
                    )
                )
            }

            // Set default template if requested and valid
            if (importDefaultTemplate && backup.defaultTemplateId != null) {
                // Only set if the template was actually imported
                val importedIds = templatesToSave.map { it.id }.toSet()
                if (backup.defaultTemplateId in importedIds) {
                    settingsStore.setActiveTemplate(backup.defaultTemplateId)
                }
            }

            ImportApplyResult.Success(
                templatesImported = templatesToSave.size,
                techProfileImported = importTechProfile
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to apply full backup", e)
            ImportApplyResult.Error("Failed to apply import: ${e.message}")
        }
    }

    /**
     * Resolve template conflicts based on user choices.
     */
    private fun resolveTemplates(
        templates: List<Template>,
        resolutions: Map<String, ConflictResolution>
    ): List<Template> {
        return templates.mapNotNull { template ->
            when (resolutions[template.id]) {
                ConflictResolution.KEEP_EXISTING -> null  // Skip this template
                ConflictResolution.SAVE_AS_COPY -> template.duplicate()  // Create copy with new ID
                ConflictResolution.REPLACE, null -> template  // Replace or no conflict
            }
        }
    }

    // ========== Utility Functions ==========

    private fun getAppVersion(): String {
        return BuildConfig.VERSION_NAME.ifEmpty { "1.0" }
    }
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
