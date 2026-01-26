package com.kingpaging.qwelcome.data

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.kingpaging.qwelcome.R
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore(name = "settings")

/**
 * Tech profile information - stored locally per device.
 */
data class TechProfile(
    val name: String = "",
    val title: String = "",
    val dept: String = ""
)

/**
 * ID for the built-in default template.
 */
const val DEFAULT_TEMPLATE_ID = "default"

private const val TAG = "SettingsStore"

/**
 * Manages app settings and template storage using DataStore.
 *
 * Supports multiple templates with JSON serialization for storage.
 * Templates can be exported/imported using the ExportModels classes.
 */
class SettingsStore(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
    }

    private object Keys {
        val TECH_NAME = stringPreferencesKey("tech_name")
        val TECH_TITLE = stringPreferencesKey("tech_title")
        val TECH_DEPT = stringPreferencesKey("tech_dept")
        val TEMPLATES_JSON = stringPreferencesKey("templates_json")
        val ACTIVE_TEMPLATE_ID = stringPreferencesKey("active_template_id")
    }

    /** The default template that ships with the app */
    val defaultTemplateContent: String = context.getString(R.string.welcome_template)

    /** Built-in default template as a Template object (lazily initialized once) */
    private val builtInDefaultTemplate: Template by lazy {
        Template(
            id = DEFAULT_TEMPLATE_ID,
            name = "Default",
            content = defaultTemplateContent,
            createdAt = "2024-01-01T00:00:00Z",
            modifiedAt = "2024-01-01T00:00:00Z"
        )
    }

    // ========== Tech Profile ==========

    val techProfileFlow: Flow<TechProfile> =
        context.dataStore.data.map { prefs ->
            TechProfile(
                name = prefs[Keys.TECH_NAME].orEmpty(),
                title = prefs[Keys.TECH_TITLE].orEmpty(),
                dept = prefs[Keys.TECH_DEPT].orEmpty()
            )
        }

    suspend fun saveTechProfile(profile: TechProfile) {
        context.dataStore.edit { prefs ->
            prefs[Keys.TECH_NAME] = profile.name.trim()
            prefs[Keys.TECH_TITLE] = profile.title.trim()
            prefs[Keys.TECH_DEPT] = profile.dept.trim()
        }
    }

    suspend fun getTechProfile(): TechProfile {
        return techProfileFlow.first()
    }

    // ========== Templates ==========

    /**
     * Flow of all user-created templates (excludes built-in default).
     */
    val userTemplatesFlow: Flow<List<Template>> =
        context.dataStore.data.map { prefs ->
            val jsonString = prefs[Keys.TEMPLATES_JSON]
            if (jsonString.isNullOrBlank()) {
                emptyList()
            } else {
                try {
                    json.decodeFromString<List<Template>>(jsonString)
                } catch (e: SerializationException) {
                    Log.e(TAG, "Failed to decode templates JSON", e)
                    emptyList()
                }
            }
        }

    /**
     * Flow of all templates (built-in default + user templates).
     */
    val allTemplatesFlow: Flow<List<Template>> =
        userTemplatesFlow.map { userTemplates ->
            listOf(builtInDefaultTemplate) + userTemplates
        }

    /**
     * Flow of the currently active template ID.
     */
    val activeTemplateIdFlow: Flow<String> =
        context.dataStore.data.map { prefs ->
            prefs[Keys.ACTIVE_TEMPLATE_ID] ?: DEFAULT_TEMPLATE_ID
        }

    /**
     * Flow of the currently active template.
     */
    val activeTemplateFlow: Flow<Template> =
        context.dataStore.data.map { prefs ->
            val activeId = prefs[Keys.ACTIVE_TEMPLATE_ID] ?: DEFAULT_TEMPLATE_ID
            val userTemplates = prefs[Keys.TEMPLATES_JSON]?.let { jsonString ->
                try {
                    json.decodeFromString<List<Template>>(jsonString)
                } catch (e: SerializationException) {
                    Log.e(TAG, "Failed to decode templates JSON in activeTemplateFlow", e)
                    emptyList()
                }
            } ?: emptyList()

            if (activeId == DEFAULT_TEMPLATE_ID) {
                builtInDefaultTemplate
            } else {
                userTemplates.find { it.id == activeId } ?: builtInDefaultTemplate
            }
        }

    /**
     * Flow that returns the active template's content (for backward compatibility).
     */
    val activeTemplateContentFlow: Flow<String> =
        activeTemplateFlow.map { it.content }

    /**
     * Get all user templates (suspend version).
     */
    suspend fun getUserTemplates(): List<Template> {
        return userTemplatesFlow.first()
    }

    /**
     * Get all templates including built-in default.
     */
    suspend fun getAllTemplates(): List<Template> {
        return allTemplatesFlow.first()
    }

    /**
     * Get a specific template by ID.
     */
    suspend fun getTemplate(id: String): Template? {
        if (id == DEFAULT_TEMPLATE_ID) {
            return builtInDefaultTemplate
        }
        return getUserTemplates().find { it.id == id }
    }

    /**
     * Get the currently active template ID.
     */
    suspend fun getActiveTemplateId(): String {
        return activeTemplateIdFlow.first()
    }

    /**
     * Get the currently active template.
     */
    suspend fun getActiveTemplate(): Template {
        return activeTemplateFlow.first()
    }

    /**
     * Set the active template by ID.
     */
    suspend fun setActiveTemplate(templateId: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.ACTIVE_TEMPLATE_ID] = templateId
        }
    }

    /**
     * Save a new template or update an existing one.
     * Cannot update the built-in default template.
     */
    suspend fun saveTemplate(template: Template) {
        require(template.id != DEFAULT_TEMPLATE_ID) {
            "Cannot modify the built-in default template"
        }

        context.dataStore.edit { prefs ->
            val currentTemplates = prefs[Keys.TEMPLATES_JSON]?.let { jsonString ->
                try {
                    json.decodeFromString<List<Template>>(jsonString)
                } catch (e: SerializationException) {
                    Log.e(TAG, "Failed to decode templates JSON in saveTemplate", e)
                    emptyList()
                }
            } ?: emptyList()

            val updatedTemplates = currentTemplates
                .filter { it.id != template.id } + template

            prefs[Keys.TEMPLATES_JSON] = json.encodeToString(updatedTemplates)
        }
    }

    /**
     * Save multiple templates at once (for import).
     */
    suspend fun saveTemplates(templates: List<Template>) {
        val validTemplates = templates.filter { it.id != DEFAULT_TEMPLATE_ID }
        if (validTemplates.isEmpty()) return

        context.dataStore.edit { prefs ->
            val currentTemplates = prefs[Keys.TEMPLATES_JSON]?.let { jsonString ->
                try {
                    json.decodeFromString<List<Template>>(jsonString)
                } catch (e: SerializationException) {
                    Log.e(TAG, "Failed to decode templates JSON in saveTemplates", e)
                    emptyList()
                }
            } ?: emptyList()

            // Use LinkedHashMap to preserve insertion order
            val templateMap = LinkedHashMap<String, Template>()
            currentTemplates.forEach { templateMap[it.id] = it }
            validTemplates.forEach { templateMap[it.id] = it }

            prefs[Keys.TEMPLATES_JSON] = json.encodeToString(templateMap.values.toList())
        }
    }

    /**
     * Delete a template by ID.
     * Cannot delete the built-in default template.
     * If the deleted template was active, switches to default.
     */
    suspend fun deleteTemplate(templateId: String) {
        require(templateId != DEFAULT_TEMPLATE_ID) {
            "Cannot delete the built-in default template"
        }

        context.dataStore.edit { prefs ->
            val currentTemplates = prefs[Keys.TEMPLATES_JSON]?.let { jsonString ->
                try {
                    json.decodeFromString<List<Template>>(jsonString)
                } catch (e: SerializationException) {
                    Log.e(TAG, "Failed to decode templates JSON in deleteTemplate", e)
                    emptyList()
                }
            } ?: emptyList()

            val updatedTemplates = currentTemplates.filter { it.id != templateId }
            prefs[Keys.TEMPLATES_JSON] = json.encodeToString(updatedTemplates)

            // If we deleted the active template, switch to default
            if (prefs[Keys.ACTIVE_TEMPLATE_ID] == templateId) {
                prefs[Keys.ACTIVE_TEMPLATE_ID] = DEFAULT_TEMPLATE_ID
            }
        }
    }

    /**
     * Delete all user templates and reset to default.
     */
    suspend fun resetToDefaultTemplate() {
        context.dataStore.edit { prefs ->
            prefs[Keys.TEMPLATES_JSON] = "[]"
            prefs[Keys.ACTIVE_TEMPLATE_ID] = DEFAULT_TEMPLATE_ID
        }
    }

    // ========== Legacy Compatibility ==========
    // These methods provide backward compatibility with old single-template code

    @Deprecated("Use activeTemplateContentFlow instead", ReplaceWith("activeTemplateContentFlow"))
    val templateSettingsFlow: Flow<TemplateSettings>
        get() = activeTemplateFlow.map { template ->
            TemplateSettings(
                useCustomTemplate = template.id != DEFAULT_TEMPLATE_ID,
                customTemplate = if (template.id != DEFAULT_TEMPLATE_ID) template.content else ""
            )
        }

    @Deprecated("Use allTemplatesFlow instead")
    val defaultTemplate: String
        get() = defaultTemplateContent
}

/**
 * Legacy data class for backward compatibility.
 */
@Deprecated("Use Template class instead")
data class TemplateSettings(
    val useCustomTemplate: Boolean = false,
    val customTemplate: String = ""
)
