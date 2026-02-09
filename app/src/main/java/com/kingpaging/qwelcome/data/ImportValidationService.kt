package com.kingpaging.qwelcome.data

import android.util.Log
import com.kingpaging.qwelcome.R
import com.kingpaging.qwelcome.util.ResourceProvider
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

private const val TAG = "ImportValidationService"
private const val MAX_SUPPORTED_SCHEMA_VERSION = EXPORT_SCHEMA_VERSION
private const val TEMPLATE_CONTENT_WARNING_LENGTH = 2000

internal class ImportValidationService(
    private val settingsStore: SettingsStore,
    private val resourceProvider: ResourceProvider,
    private val json: Json
) {

    suspend fun validateImport(jsonString: String): ImportValidationResult {
        val trimmedJson = jsonString.trim()
        if (trimmedJson.isBlank()) {
            return ImportValidationResult.Invalid(
                resourceProvider.getString(R.string.error_import_empty_input)
            )
        }

        // Size checks protect clipboard/file import paths from oversized payloads.
        val maxSizeLabel = formatBytesAsMb(MAX_IMPORT_SIZE_BYTES.toLong())
        if (trimmedJson.length > MAX_IMPORT_SIZE_BYTES) {
            return ImportValidationResult.Invalid(
                resourceProvider.getString(R.string.error_import_too_large, maxSizeLabel)
            )
        }
        val preciseSize = trimmedJson.toByteArray(Charsets.UTF_8).size
        if (preciseSize > MAX_IMPORT_SIZE_BYTES) {
            return ImportValidationResult.Invalid(
                resourceProvider.getString(R.string.error_import_too_large, maxSizeLabel)
            )
        }

        val metadata = try {
            json.decodeFromString<ExportMetadata>(trimmedJson)
        } catch (e: SerializationException) {
            Log.e(TAG, "Failed to parse export metadata", e)
            return ImportValidationResult.Invalid(
                resourceProvider.getString(R.string.error_import_invalid_json_format, e.message ?: "")
            )
        }

        val warnings = mutableListOf<ImportWarning>()
        if (metadata.schemaVersion > MAX_SUPPORTED_SCHEMA_VERSION) {
            warnings.add(
                ImportWarning.NewerVersion(
                    importVersion = metadata.schemaVersion,
                    supportedVersion = MAX_SUPPORTED_SCHEMA_VERSION
                )
            )
        }

        return when (metadata.kind) {
            ExportKind.TEMPLATE_PACK -> parseTemplatePack(trimmedJson, warnings)
            ExportKind.FULL_BACKUP -> parseFullBackup(trimmedJson, warnings)
            else -> ImportValidationResult.Invalid(
                resourceProvider.getString(R.string.error_import_unknown_export_kind, metadata.kind)
            )
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
            return ImportValidationResult.Invalid(
                resourceProvider.getString(
                    R.string.error_import_invalid_template_pack_format,
                    e.message ?: ""
                )
            )
        }

        warnings.addAll(validateTemplates(pack.templates))

        if (pack.templates.isEmpty()) {
            return ImportValidationResult.Invalid(
                resourceProvider.getString(R.string.error_import_template_pack_empty)
            )
        }

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
            return ImportValidationResult.Invalid(
                resourceProvider.getString(R.string.error_import_invalid_backup_format, e.message ?: "")
            )
        }

        warnings.addAll(validateTemplates(backup.templates))

        val existingTemplates = settingsStore.getUserTemplates()
        val conflicts = detectConflicts(backup.templates, existingTemplates)

        return ImportValidationResult.ValidFullBackup(
            backup = backup,
            conflicts = conflicts,
            warnings = warnings
        )
    }

    private fun validateTemplates(templates: List<Template>): List<ImportWarning> {
        val warnings = mutableListOf<ImportWarning>()

        templates.forEach { template ->
            if (template.id.isBlank()) {
                warnings.add(ImportWarning.MissingField(template.name, "id"))
            }
            if (template.name.isBlank()) {
                warnings.add(ImportWarning.MissingField(template.id, "name"))
            }
            if (template.content.isBlank()) {
                warnings.add(ImportWarning.MissingField(template.name.ifBlank { template.id }, "content"))
            }

            if (template.content.length > TEMPLATE_CONTENT_WARNING_LENGTH) {
                warnings.add(
                    ImportWarning.LongContent(
                        templateName = template.name,
                        length = template.content.length
                    )
                )
            }

            val missingPlaceholders = Template.findMissingPlaceholders(template.content)
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
}
