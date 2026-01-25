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
         * Generate a URL-friendly slug from a name.
         * Example: "Residential Welcome" -> "residential_welcome"
         */
        private fun generateSlug(name: String): String {
            return name.lowercase()
                .replace(Regex("[^a-z0-9]+"), "_")
                .trim('_')
                .take(50) // Limit length
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
     * Create a copy with updated name and modification timestamp.
     */
    fun withUpdatedName(newName: String): Template {
        return copy(
            name = newName,
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
