package com.kingpaging.qwelcome.data

import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Represents a message template that can be exported/imported.
 *
 * @property id Unique UUID identifier for the template (used for merge/conflict detection)
 * @property name User-friendly display name for the template
 * @property content The template text with placeholders (e.g., {{ customer_name }}, {{ tech_signature }})
 * @property createdAt ISO-8601 timestamp when the template was created
 * @property modifiedAt ISO-8601 timestamp when the template was last modified
 * @property slug Optional human-readable identifier for readability in exports (not used for merging)
 * @property sortOrder Display order (lower = first). Default template uses Int.MIN_VALUE to stay pinned.
 * @property tags Optional tags for future categorization (not used in UI yet)
 */
@Serializable
data class Template(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val content: String,
    val createdAt: String = currentIsoTimestamp(),
    val modifiedAt: String = createdAt,
    val slug: String? = null,
    val sortOrder: Int = 0,
    val tags: List<String> = emptyList()
) {
    companion object {
        /**
         * Required placeholders that must be present in every template.
         * These are essential for the app's core functionality.
         */
        val REQUIRED_PLACEHOLDERS = setOf("{{ customer_name }}", "{{ ssid }}")
        
        // Regex patterns for flexible placeholder matching (tolerates whitespace)
        private val PATTERN_CUSTOMER_NAME = Regex("""\{\{\s*customer_name\s*\}\}""")
        private val PATTERN_SSID = Regex("""\{\{\s*ssid\s*\}\}""")
        
        /**
         * Validates template content and returns a list of missing required placeholders.
         * Uses regex to tolerate whitespace variations like {{customer_name}} or {{ customer_name }}.
         * 
         * @return List of canonical placeholder strings that are missing (empty if all present)
         */
        fun findMissingPlaceholders(content: String): List<String> {
            val missing = mutableListOf<String>()
            if (!PATTERN_CUSTOMER_NAME.containsMatchIn(content)) {
                missing += "{{ customer_name }}"
            }
            if (!PATTERN_SSID.containsMatchIn(content)) {
                missing += "{{ ssid }}"
            }
            return missing
        }
        
        /**
         * Checks if template content has all required placeholders.
         */
        fun hasRequiredPlaceholders(content: String): Boolean = 
            findMissingPlaceholders(content).isEmpty()
        
        /**
         * Create a template from just name and content (common use case).
         * Auto-generates a slug from the name for readability.
         */
        fun create(name: String, content: String): Template {
            val now = currentIsoTimestamp()
            return Template(
                id = UUID.randomUUID().toString(),
                name = name,
                content = content,
                createdAt = now,
                modifiedAt = now,
                slug = generateSlug(name)
            )
        }

        /**
         * Reserved slugs that should not be used directly.
         */
        private val RESERVED_SLUGS = setOf("default", "null", "undefined")

        /**
         * Generate a URL-friendly slug from a name.
         * Example: "Residential Welcome" -> "residential_welcome"
         * If no alphanumeric chars remain, returns a fallback like "template_abc123"
         * Reserved slugs get a UUID suffix appended.
         */
        private fun generateSlug(name: String): String {
            val cleaned = name.lowercase()
                .replace(Regex("[^a-z0-9]+"), "_")
                .replace(Regex("_+"), "_") // Collapse consecutive underscores
                .trim('_')
                .take(50) // Limit length
            
            return when {
                // Fallback if slug is empty (e.g., name was "!!!" or "🎉🎉🎉")
                cleaned.isEmpty() -> "template_${UUID.randomUUID().toString().take(8)}"
                // Append suffix for reserved slugs
                cleaned in RESERVED_SLUGS -> "${cleaned}_${UUID.randomUUID().toString().take(8)}"
                else -> cleaned
            }
        }

        /**
         * Returns current timestamp in ISO-8601 format.
         */
        private fun currentIsoTimestamp(): String {
            return java.time.Instant.now().toString()
        }
    }

    /**
     * Create a copy with updated modification timestamp.
     */
    fun withUpdatedContent(newContent: String): Template {
        return copy(
            content = newContent,
            modifiedAt = currentIsoTimestamp()
        )
    }

    /**
     * Create a copy with updated name, slug, and modification timestamp.
     * Always regenerates the slug to keep it consistent with the name.
     */
    fun withUpdatedName(newName: String): Template {
        return copy(
            name = newName,
            slug = generateSlug(newName),
            modifiedAt = currentIsoTimestamp()
        )
    }

    /**
     * Create a copy as a duplicate with new ID, timestamps, and slug.
     */
    fun duplicate(newName: String = "$name (Copy)"): Template {
        val now = currentIsoTimestamp()
        return copy(
            id = UUID.randomUUID().toString(),
            name = newName,
            slug = generateSlug(newName),
            createdAt = now,
            modifiedAt = now
        )
    }
}
