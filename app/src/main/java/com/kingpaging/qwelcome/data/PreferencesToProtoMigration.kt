package com.kingpaging.qwelcome.data

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

private const val TAG = "PreferencesToProtoMigration"
private const val DATA_STORE_FILE_NAME = "user_preferences.pb"
private const val MIGRATION_COMPLETED_KEY = "proto_migration_completed"

private const val TECH_NAME_KEY = "tech_name"
private const val TECH_TITLE_KEY = "tech_title"
private const val TECH_DEPT_KEY = "tech_dept"
private const val TEMPLATES_JSON_KEY = "templates_json"
private const val ACTIVE_TEMPLATE_ID_KEY = "active_template_id"

private val Context.tempPreferencesDataStore by preferencesDataStore(name = "settings")

val Context.protoDataStore: DataStore<UserPreferences> by dataStore(
    fileName = DATA_STORE_FILE_NAME,
    serializer = UserPreferencesSerializer,
    produceMigrations = { context ->
        listOf(PreferencesToProtoMigration(context))
    }
)

private class PreferencesToProtoMigration(
    private val context: Context
) : DataMigration<UserPreferences> {

    override suspend fun shouldMigrate(currentData: UserPreferences): Boolean {
        val prefs = context.tempPreferencesDataStore.data.first()

        val migrationCompleted = prefs[booleanPreferencesKey(MIGRATION_COMPLETED_KEY)] == true
        if (migrationCompleted) return false

        return prefs[stringPreferencesKey(TECH_NAME_KEY)] != null ||
            prefs[stringPreferencesKey(TEMPLATES_JSON_KEY)] != null ||
            prefs[stringPreferencesKey(ACTIVE_TEMPLATE_ID_KEY)] != null
    }

    override suspend fun migrate(currentData: UserPreferences): UserPreferences {
        val prefs = context.tempPreferencesDataStore.data.first()
        val json = Json { ignoreUnknownKeys = true }

        val techProfile = TechProfileProto.newBuilder()
            .setName(prefs[stringPreferencesKey(TECH_NAME_KEY)].orEmpty())
            .setTitle(prefs[stringPreferencesKey(TECH_TITLE_KEY)].orEmpty())
            .setDept(prefs[stringPreferencesKey(TECH_DEPT_KEY)].orEmpty())
            .build()

        val templatesJson = prefs[stringPreferencesKey(TEMPLATES_JSON_KEY)]
        val templates = parseTemplateProtos(templatesJson, json)

        return UserPreferences.newBuilder()
            .setActiveTemplateId(
                prefs[stringPreferencesKey(ACTIVE_TEMPLATE_ID_KEY)] ?: DEFAULT_TEMPLATE_ID
            )
            .setTechProfile(techProfile)
            .addAllTemplates(templates)
            .build()
    }

    override suspend fun cleanUp() {
        context.tempPreferencesDataStore.edit { prefs ->
            prefs[booleanPreferencesKey(MIGRATION_COMPLETED_KEY)] = true
            prefs.remove(stringPreferencesKey(TECH_NAME_KEY))
            prefs.remove(stringPreferencesKey(TECH_TITLE_KEY))
            prefs.remove(stringPreferencesKey(TECH_DEPT_KEY))
            prefs.remove(stringPreferencesKey(TEMPLATES_JSON_KEY))
            prefs.remove(stringPreferencesKey(ACTIVE_TEMPLATE_ID_KEY))
        }
    }

    private fun parseTemplateProtos(
        templatesJson: String?,
        json: Json
    ): List<TemplateProto> {
        if (templatesJson.isNullOrBlank()) return emptyList()
        return try {
            json.decodeFromString<List<Template>>(templatesJson).map { it.toProto() }
        } catch (e: SerializationException) {
            Log.e(
                TAG,
                "Error decoding templates from preferences. JSON length: ${templatesJson.length}",
                e
            )
            logCorruptedTemplatesDiagnostics(templatesJson)
        }
    }

    /**
     * Logs diagnostics only. We intentionally avoid partial recovery because it can
     * silently corrupt template objects.
     */
    private fun logCorruptedTemplatesDiagnostics(templatesJson: String): List<TemplateProto> {
        Log.w(TAG, "Logging corrupted template data for diagnostics...")
        return try {
            val templateRegex = """\{[^{}]*"id"\s*:\s*"[^"]+""".toRegex()
            val matchCount = templateRegex.findAll(templatesJson).toList().size
            if (matchCount == 0) {
                Log.w(TAG, "No template fragments found in corrupted data")
            } else {
                Log.w(
                    TAG,
                    "Found $matchCount template fragment(s) in corrupted data - manual recovery may be needed"
                )
            }
            emptyList()
        } catch (e: java.util.regex.PatternSyntaxException) {
            Log.e(TAG, "Regex pattern error while analyzing corrupted data", e)
            emptyList()
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Invalid argument while analyzing corrupted data", e)
            emptyList()
        }
    }
}
