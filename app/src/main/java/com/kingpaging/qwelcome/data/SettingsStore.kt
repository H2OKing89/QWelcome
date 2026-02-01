package com.kingpaging.qwelcome.data

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import androidx.datastore.preferences.core.booleanPreferencesKey
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
private const val MIGRATION_COMPLETED_KEY = "proto_migration_completed"

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
                    val prefs = context.tempPreferencesDataStore.data.first()
                    // Check for explicit migration marker first (most reliable)
                    val migrationCompleted = prefs[booleanPreferencesKey(MIGRATION_COMPLETED_KEY)] == true
                    if (migrationCompleted) {
                        return false // Already migrated
                    }
                    // Check if there's any legacy data to migrate
                    // Look for specific keys rather than just checking if map is non-empty
                    val hasLegacyData = prefs[stringPreferencesKey("tech_name")] != null ||
                            prefs[stringPreferencesKey("templates_json")] != null ||
                            prefs[stringPreferencesKey("active_template_id")] != null
                    return hasLegacyData
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
                            // Log error with full context for debugging
                            Log.e(TAG, "Error decoding templates from preferences. JSON length: ${templatesJson.length}", e)
                            // Attempt partial recovery by trying to decode individual templates
                            logCorruptedTemplatesDiagnostics(templatesJson)
                        }
                    }

                    return UserPreferences.newBuilder()
                        .setActiveTemplateId(prefs[stringPreferencesKey("active_template_id")] ?: DEFAULT_TEMPLATE_ID)
                        .setTechProfile(techProfile)
                        .addAllTemplates(templates)
                        .build()
                }

                /**
                 * Logs diagnostic information about corrupted template data.
                 * Note: This function only logs diagnostics and does NOT attempt recovery,
                 * as partial JSON recovery is unreliable and could introduce data corruption.
                 * Always returns an empty list.
                 */
                private fun logCorruptedTemplatesDiagnostics(templatesJson: String): List<TemplateProto> {
                    Log.w(TAG, "Logging corrupted template data for diagnostics...")
                    // Try to identify potential template fragments for logging purposes
                    return try {
                        val templateRegex = """\{[^{}]*"id"\s*:\s*"[^"]+""".toRegex()
                        val matchList = templateRegex.findAll(templatesJson).toList()
                        val matchCount = matchList.size
                        if (matchCount == 0) {
                            Log.w(TAG, "No template fragments found in corrupted data")
                        } else {
                            Log.w(TAG, "Found $matchCount template fragment(s) in corrupted data - manual recovery may be needed")
                        }
                        // Return empty list as we can't reliably reconstruct full objects
                        emptyList()
                    } catch (e: java.util.regex.PatternSyntaxException) {
                        // Handle regex compilation errors (shouldn't happen with static pattern)
                        Log.e(TAG, "Regex pattern error while analyzing corrupted data", e)
                        emptyList()
                    } catch (e: IllegalArgumentException) {
                        // Handle invalid regex arguments
                        Log.e(TAG, "Invalid argument while analyzing corrupted data", e)
                        emptyList()
                    }
                }

                override suspend fun cleanUp() {
                    // Mark migration as completed and clear the old data
                    context.tempPreferencesDataStore.edit { prefs ->
                        prefs[booleanPreferencesKey(MIGRATION_COMPLETED_KEY)] = true
                        // Remove legacy keys but keep the migration marker
                        prefs.remove(stringPreferencesKey("tech_name"))
                        prefs.remove(stringPreferencesKey("tech_title"))
                        prefs.remove(stringPreferencesKey("tech_dept"))
                        prefs.remove(stringPreferencesKey("templates_json"))
                        prefs.remove(stringPreferencesKey("active_template_id"))
                    }
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
 * Catches [IOException] on a [Flow] of [UserPreferences], logging the error and emitting a default instance.
 * Other exceptions are rethrown. Reduces duplication across DataStore flows.
 */
private fun Flow<UserPreferences>.catchIoException(
    errorMessage: String
): Flow<UserPreferences> = catch { exception ->
    if (exception is IOException) {
        Log.e(TAG, errorMessage, exception)
        emit(UserPreferences.getDefaultInstance())
    } else {
        throw exception
    }
}

/**
 * Manages app settings and template storage using Proto DataStore.
 */
class SettingsStore(private val context: Context) {

    /** The default template that ships with the app */
    private val builtInDefaultTemplate: Template by lazy {
        Template(
            id = DEFAULT_TEMPLATE_ID,
            name = "Default",
            content = context.getString(R.string.welcome_template),
            sortOrder = Int.MIN_VALUE  // Pin built-in default at top
        )
    }

    /** Public accessor for the default template content */
    val defaultTemplateContent: String
        get() = builtInDefaultTemplate.content

    private val dataStore = context.protoDataStore

    // ========== Tech Profile ==========

    val techProfileFlow: Flow<TechProfile> = dataStore.data
        .catchIoException("Error reading tech profile.")
        .map { preferences -> TechProfile.fromProto(preferences.techProfile) }

    suspend fun saveTechProfile(profile: TechProfile) {
        dataStore.updateData { preferences ->
            preferences.toBuilder()
                .setTechProfile(profile.toProto())
                .build()
        }
    }

    suspend fun getTechProfile(): TechProfile = techProfileFlow.first()

    // ========== Templates ==========

    val userTemplatesFlow: Flow<List<Template>> = dataStore.data
        .catchIoException("Error reading user templates.")
        .map { prefs -> prefs.templatesList.map { Template.fromProto(it) } }

    val allTemplatesFlow: Flow<List<Template>> = userTemplatesFlow.map { userTemplates ->
        listOf(builtInDefaultTemplate) + userTemplates
    }

    val activeTemplateIdFlow: Flow<String> = dataStore.data
        .catchIoException("Error reading active template ID.")
        .map { prefs -> prefs.activeTemplateId.ifEmpty { DEFAULT_TEMPLATE_ID } }

    val activeTemplateFlow: Flow<Template> = dataStore.data
        .catchIoException("Error reading active template.")
        .map { prefs ->
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
            // Preserve order by starting from existing list and updating/appending
            val existingList = prefs.templatesList.toMutableList()
            for (newTemplate in validTemplates) {
                val existingIndex = existingList.indexOfFirst { it.id == newTemplate.id }
                if (existingIndex >= 0) {
                    existingList[existingIndex] = newTemplate.toProto()
                } else {
                    existingList.add(newTemplate.toProto())
                }
            }
            prefs.toBuilder().clearTemplates().addAllTemplates(existingList).build()
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

/** Maximum length for TechProfile string fields to prevent protobuf encoding issues */
private const val MAX_PROFILE_FIELD_LENGTH = 500

fun TechProfile.toProto(): TechProfileProto {
    val truncatedName = name.take(MAX_PROFILE_FIELD_LENGTH)
    val truncatedTitle = title.take(MAX_PROFILE_FIELD_LENGTH)
    val truncatedDept = dept.take(MAX_PROFILE_FIELD_LENGTH)

    if (name.length > MAX_PROFILE_FIELD_LENGTH) {
        Log.w(TAG, "TechProfile name truncated from ${name.length} to $MAX_PROFILE_FIELD_LENGTH chars")
    }
    if (title.length > MAX_PROFILE_FIELD_LENGTH) {
        Log.w(TAG, "TechProfile title truncated from ${title.length} to $MAX_PROFILE_FIELD_LENGTH chars")
    }
    if (dept.length > MAX_PROFILE_FIELD_LENGTH) {
        Log.w(TAG, "TechProfile dept truncated from ${dept.length} to $MAX_PROFILE_FIELD_LENGTH chars")
    }

    return TechProfileProto.newBuilder()
        .setName(truncatedName)
        .setTitle(truncatedTitle)
        .setDept(truncatedDept)
        .build()
}

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
    .setCreatedAt(createdAt)
    .setModifiedAt(modifiedAt)
    .setSlug(slug ?: "")
    .build()

fun Template.Companion.fromProto(proto: TemplateProto): Template = Template(
    id = proto.id,
    name = proto.name,
    content = proto.content,
    // Use epoch as deterministic default for missing timestamps (not Instant.now())
    createdAt = proto.createdAt.ifEmpty { "1970-01-01T00:00:00Z" },
    modifiedAt = proto.modifiedAt.ifEmpty { "1970-01-01T00:00:00Z" },
    slug = proto.slug.ifEmpty { null }
)
