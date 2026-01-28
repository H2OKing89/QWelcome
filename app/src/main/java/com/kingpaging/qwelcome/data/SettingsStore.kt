package com.kingpaging.qwelcome.data

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.kingpaging.qwelcome.R
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.catch
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.IOException

private const val TAG = "SettingsStore"
private const val DATA_STORE_FILE_NAME = "user_preferences.pb"

// Temp preferences data store for migration
private val Context.tempPreferencesDataStore by preferencesDataStore(name = "settings")

// Proto DataStore
private val Context.protoDataStore: DataStore<UserPreferences> by dataStore(
    fileName = DATA_STORE_FILE_NAME,
    serializer = UserPreferencesSerializer,
    produceMigrations = { context ->
        listOf(
            // Create migration from Preferences to Proto DataStore
            object : DataMigration<UserPreferences> {
                override suspend fun shouldMigrate(currentData: UserPreferences): Boolean {
                    // Check if preferences has any data. If so, we should migrate.
                    // This is a simplified check; a more robust check might look for a version key.
                    return context.tempPreferencesDataStore.data.first().asMap().isNotEmpty()
                }

                override suspend fun migrate(currentData: UserPreferences): UserPreferences {
                    val prefs = context.tempPreferencesDataStore.data.first()
                    val json = Json { ignoreUnknownKeys = true }

                    val techProfile = TechProfileProto.newBuilder()
                        .setName(prefs[stringPreferencesKey("tech_name")].orEmpty())
                        .setTitle(prefs[stringPreferencesKey("tech_title")].orEmpty())
                        .setDept(prefs[stringPreferencesKey("tech_dept")].orEmpty())
                        .build()

                    val templatesJson = prefs[stringPreferencesKey("templates_json")]
                    val templates = if (templatesJson.isNullOrBlank()) {
                        emptyList()
                    } else {
                        try {
                            json.decodeFromString<List<Template>>(templatesJson).map { it.toProto() }
                        } catch (e: SerializationException) {
                            Log.e(TAG, "Error decoding templates from preferences", e)
                            emptyList()
                        }
                    }

                    return UserPreferences.newBuilder()
                        .setActiveTemplateId(prefs[stringPreferencesKey("active_template_id")] ?: DEFAULT_TEMPLATE_ID)
                        .setTechProfile(techProfile)
                        .addAllTemplates(templates)
                        .build()
                }

                override suspend fun cleanUp() {
                    // Clear the old preferences data store after migration
                    context.tempPreferencesDataStore.edit { it.clear() }
                }
            }
        )
    }
)

/**
 * ID for the built-in default template.
 */
const val DEFAULT_TEMPLATE_ID = "default"

/**
 * Manages app settings and template storage using Proto DataStore.
 */
class SettingsStore(private val context: Context) {

    /** The default template that ships with the app */
    private val builtInDefaultTemplate: Template by lazy {
        Template(
            id = DEFAULT_TEMPLATE_ID,
            name = "Default",
            content = context.getString(R.string.welcome_template)
        )
    }

    /** Public accessor for the default template content */
    val defaultTemplateContent: String
        get() = builtInDefaultTemplate.content

    private val dataStore = context.protoDataStore

    // ========== Tech Profile ==========

    val techProfileFlow: Flow<TechProfile> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                Log.e(TAG, "Error reading tech profile.", exception)
                emit(UserPreferences.getDefaultInstance())
            } else {
                throw exception
            }
        }.map { preferences -> TechProfile.fromProto(preferences.techProfile) }

    suspend fun saveTechProfile(profile: TechProfile) {
        dataStore.updateData { preferences ->
            preferences.toBuilder()
                .setTechProfile(profile.toProto())
                .build()
        }
    }

    suspend fun getTechProfile(): TechProfile = techProfileFlow.first()

    // ========== Templates ==========

    val userTemplatesFlow: Flow<List<Template>> = dataStore.data.map { prefs ->
        prefs.templatesList.map { Template.fromProto(it) }
    }

    val allTemplatesFlow: Flow<List<Template>> = userTemplatesFlow.map { userTemplates ->
        listOf(builtInDefaultTemplate) + userTemplates
    }

    val activeTemplateIdFlow: Flow<String> = dataStore.data.map { prefs ->
        prefs.activeTemplateId.ifEmpty { DEFAULT_TEMPLATE_ID }
    }

    val activeTemplateFlow: Flow<Template> = dataStore.data.map { prefs ->
        val activeId = prefs.activeTemplateId.ifEmpty { DEFAULT_TEMPLATE_ID }
        if (activeId == DEFAULT_TEMPLATE_ID) {
            builtInDefaultTemplate
        } else {
            prefs.templatesList.find { it.id == activeId }?.let { Template.fromProto(it) } ?: builtInDefaultTemplate
        }
    }

    suspend fun getUserTemplates(): List<Template> = userTemplatesFlow.first()

    suspend fun getAllTemplates(): List<Template> = allTemplatesFlow.first()

    suspend fun getTemplate(id: String): Template? {
        if (id == DEFAULT_TEMPLATE_ID) return builtInDefaultTemplate
        return getUserTemplates().find { it.id == id }
    }

    suspend fun getActiveTemplateId(): String = activeTemplateIdFlow.first()

    suspend fun getActiveTemplate(): Template = activeTemplateFlow.first()

    suspend fun setActiveTemplate(templateId: String) {
        dataStore.updateData { it.toBuilder().setActiveTemplateId(templateId).build() }
    }

    suspend fun saveTemplate(template: Template) {
        require(template.id != DEFAULT_TEMPLATE_ID) { "Cannot modify the default template" }
        dataStore.updateData { prefs ->
            val newTemplates = prefs.templatesList.filterNot { it.id == template.id } + template.toProto()
            prefs.toBuilder().clearTemplates().addAllTemplates(newTemplates).build()
        }
    }

    suspend fun saveTemplates(templates: List<Template>) {
        val validTemplates = templates.filter { it.id != DEFAULT_TEMPLATE_ID }
        if (validTemplates.isEmpty()) return

        dataStore.updateData { prefs ->
            val templateMap = prefs.templatesList.associateBy { it.id }.toMutableMap()
            validTemplates.forEach { templateMap[it.id] = it.toProto() }
            prefs.toBuilder().clearTemplates().addAllTemplates(templateMap.values).build()
        }
    }

    suspend fun deleteTemplate(templateId: String) {
        require(templateId != DEFAULT_TEMPLATE_ID) { "Cannot delete the default template" }
        dataStore.updateData { prefs ->
            val builder = prefs.toBuilder()
            val newTemplates = prefs.templatesList.filterNot { it.id == templateId }
            builder.clearTemplates().addAllTemplates(newTemplates)
            if (builder.activeTemplateId == templateId) {
                builder.activeTemplateId = DEFAULT_TEMPLATE_ID
            }
            builder.build()
        }
    }

    suspend fun resetToDefaultTemplate() {
        dataStore.updateData { prefs ->
            prefs.toBuilder()
                .clearTemplates()
                .setActiveTemplateId(DEFAULT_TEMPLATE_ID)
                .build()
        }
    }
}

// ========== Mappers ==========

fun TechProfile.toProto(): TechProfileProto = TechProfileProto.newBuilder()
    .setName(name)
    .setTitle(title)
    .setDept(dept)
    .build()

fun TechProfile.Companion.fromProto(proto: TechProfileProto): TechProfile = TechProfile(
    name = proto.name,
    title = proto.title,
    dept = proto.dept
)

// Add a companion object to allow extending TechProfile
val TechProfile.Companion.empty: TechProfile get() = TechProfile()


fun Template.toProto(): TemplateProto = TemplateProto.newBuilder()
    .setId(id)
    .setName(name)
    .setContent(content)
    .build()

fun Template.Companion.fromProto(proto: TemplateProto): Template = Template(
    id = proto.id,
    name = proto.name,
    content = proto.content
)
