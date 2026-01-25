package com.example.allowelcome.data

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
 */
@Serializable
data class Template(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val content: String,
    val createdAt: String = currentIsoTimestamp(),
    val modifiedAt: String = createdAt,
    val slug: String? = null
) {
    companion object {
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
     */
    fun withUpdatedName(newName: String): Template {
        return copy(
            name = newName,
            slug = slug?.let { generateSlug(newName) },
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
