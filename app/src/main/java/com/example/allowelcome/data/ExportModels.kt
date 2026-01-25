package com.example.allowelcome.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Schema version for JSON export/import format.
 * Increment when making breaking changes to the schema.
 */
const val EXPORT_SCHEMA_VERSION = 1

/**
 * Kind identifiers for discriminating between export types.
 */
object ExportKind {
    const val TEMPLATE_PACK = "template-pack"
    const val FULL_BACKUP = "full-backup"
}

/**
 * A Template Pack export - used for sharing templates with teammates.
 *
 * This format intentionally EXCLUDES techProfile (personal info stays local).
 * The {{ tech_signature }} placeholder preserves personalization while
 * allowing the template content itself to be shared.
 *
 * Example JSON:
 * ```json
 * {
 *   "schemaVersion": 1,
 *   "kind": "template-pack",
 *   "exportedAt": "2026-01-25T10:30:00Z",
 *   "appVersion": "1.0",
 *   "templates": [
 *     {
 *       "id": "abc-123",
 *       "name": "Standard Welcome",
 *       "content": "Hello {{ customer_name }}!\n\nBest,\n{{ tech_signature }}",
 *       "createdAt": "2026-01-20T08:00:00Z",
 *       "modifiedAt": "2026-01-25T10:00:00Z"
 *     }
 *   ]
 * }
 * ```
 */
@Serializable
data class TemplatePack(
    val schemaVersion: Int = EXPORT_SCHEMA_VERSION,
    val kind: String = ExportKind.TEMPLATE_PACK,
    val exportedAt: String = java.time.Instant.now().toString(),
    val appVersion: String = "1.0",
    val templates: List<Template>
) {
    init {
        require(kind == ExportKind.TEMPLATE_PACK) {
            "TemplatePack kind must be '${ExportKind.TEMPLATE_PACK}', got '$kind'"
        }
    }

    companion object {
        /**
         * Create a template pack from a list of templates.
         */
        fun create(templates: List<Template>, appVersion: String = "1.0"): TemplatePack {
            return TemplatePack(
                schemaVersion = EXPORT_SCHEMA_VERSION,
                kind = ExportKind.TEMPLATE_PACK,
                exportedAt = java.time.Instant.now().toString(),
                appVersion = appVersion,
                templates = templates
            )
        }
    }
}

/**
 * Tech profile information - personal to each device.
 * Included in FullBackup but NOT in TemplatePack.
 */
@Serializable
data class ExportedTechProfile(
    val name: String,
    val title: String,
    val dept: String
)

/**
 * A Full Backup export - used for personal backup/restore across devices.
 *
 * This format INCLUDES techProfile for complete restoration.
 * Intended for personal use, not sharing with teammates.
 *
 * Example JSON:
 * ```json
 * {
 *   "schemaVersion": 1,
 *   "kind": "full-backup",
 *   "exportedAt": "2026-01-25T10:30:00Z",
 *   "appVersion": "1.0",
 *   "techProfile": {
 *     "name": "John Smith",
 *     "title": "Field Tech",
 *     "dept": "Network Services"
 *   },
 *   "templates": [...],
 *   "defaultTemplateId": "abc-123"
 * }
 * ```
 */
@Serializable
data class FullBackup(
    val schemaVersion: Int = EXPORT_SCHEMA_VERSION,
    val kind: String = ExportKind.FULL_BACKUP,
    val exportedAt: String = java.time.Instant.now().toString(),
    val appVersion: String = "1.0",
    val techProfile: ExportedTechProfile,
    val templates: List<Template>,
    val defaultTemplateId: String? = null
) {
    init {
        require(kind == ExportKind.FULL_BACKUP) {
            "FullBackup kind must be '${ExportKind.FULL_BACKUP}', got '$kind'"
        }
    }

    companion object {
        /**
         * Create a full backup from current app state.
         */
        fun create(
            techProfile: TechProfile,
            templates: List<Template>,
            defaultTemplateId: String? = null,
            appVersion: String = "1.0"
        ): FullBackup {
            return FullBackup(
                schemaVersion = EXPORT_SCHEMA_VERSION,
                kind = ExportKind.FULL_BACKUP,
                exportedAt = java.time.Instant.now().toString(),
                appVersion = appVersion,
                techProfile = ExportedTechProfile(
                    name = techProfile.name,
                    title = techProfile.title,
                    dept = techProfile.dept
                ),
                templates = templates,
                defaultTemplateId = defaultTemplateId
            )
        }
    }
}

/**
 * Wrapper for parsing unknown export JSON to determine its kind.
 * Used for initial parsing before deserializing to specific type.
 */
@Serializable
data class ExportMetadata(
    val schemaVersion: Int,
    val kind: String
)

/**
 * Result of parsing an import string.
 */
sealed class ImportParseResult {
    data class TemplatePackResult(val pack: TemplatePack) : ImportParseResult()
    data class FullBackupResult(val backup: FullBackup) : ImportParseResult()
    data class Error(val message: String, val cause: Throwable? = null) : ImportParseResult()
}
