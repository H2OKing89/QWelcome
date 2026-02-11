package com.kingpaging.qwelcome.data

import android.content.Context
import android.util.Log
import com.kingpaging.qwelcome.R
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.catch
import java.io.IOException

private const val TAG = "SettingsStore"
private const val MAX_RECENT_SHARE_TARGETS = 3

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

    val recentSharePackagesFlow: Flow<List<String>> = dataStore.data
        .catchIoException("Error reading recent share packages.")
        .map { prefs -> prefs.recentSharePackagesList }

    suspend fun getUserTemplates(): List<Template> = userTemplatesFlow.first()

    suspend fun getAllTemplates(): List<Template> = allTemplatesFlow.first()

    suspend fun getTemplate(id: String): Template? {
        if (id == DEFAULT_TEMPLATE_ID) return builtInDefaultTemplate
        return getUserTemplates().find { it.id == id }
    }

    suspend fun getActiveTemplateId(): String = activeTemplateIdFlow.first()

    suspend fun getActiveTemplate(): Template = activeTemplateFlow.first()

    suspend fun setActiveTemplate(templateId: String) {
        dataStore.updateData {
            it.toBuilder()
                .setActiveTemplateId(templateId)
                .build()
        }
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

    suspend fun recordRecentSharePackage(packageName: String) {
        if (packageName.isBlank()) return
        dataStore.updateData { prefs ->
            val deduped = (listOf(packageName) + prefs.recentSharePackagesList.filterNot { it == packageName })
                .take(MAX_RECENT_SHARE_TARGETS)
            prefs.toBuilder()
                .clearRecentSharePackages()
                .addAllRecentSharePackages(deduped)
                .build()
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
