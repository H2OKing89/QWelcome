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

data class TemplateSelectionChange(
    val selectedTemplateId: String,
    val selectedLastUsedAt: Long,
    val previousActiveTemplateId: String,
    val previousSelectedLastUsedAt: Long?
)

sealed interface TemplateSelectionResult {
    data class Selected(
        val template: Template,
        val change: TemplateSelectionChange
    ) : TemplateSelectionResult

    data class AlreadyActive(val template: Template) : TemplateSelectionResult

    data class Blocked(
        val template: Template,
        val missingPlaceholders: List<String>
    ) : TemplateSelectionResult

    data class NotFound(val templateId: String) : TemplateSelectionResult
}

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

internal fun Flow<UserPreferences>.readPrivacySettings(): Flow<PrivacySettings> = map { preferences ->
    PrivacySettings.fromProto(preferences.privacySettings)
}.catch { exception ->
    if (exception is IOException) {
        Log.e(TAG, "Error reading privacy settings.", exception)
        emit(
            PrivacySettings(
                crashReportingEnabled = false,
                screenCaptureProtectionEnabled = true
            )
        )
    } else {
        throw exception
    }
}

/**
 * Manages app settings and template storage using Proto DataStore.
 */
class SettingsStore(
    private val context: Context,
    private val currentTimeMillis: () -> Long = System::currentTimeMillis
) {

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

    // ========== Privacy Settings ==========

    val privacySettingsFlow: Flow<PrivacySettings> = dataStore.data.readPrivacySettings()

    suspend fun savePrivacySettings(settings: PrivacySettings) {
        dataStore.updateData { preferences ->
            preferences.toBuilder()
                .setPrivacySettings(settings.toProto())
                .build()
        }
    }

    suspend fun updatePrivacySettings(transform: (PrivacySettings) -> PrivacySettings) {
        dataStore.updateData { preferences ->
            preferences.toBuilder()
                .setPrivacySettings(transform(PrivacySettings.fromProto(preferences.privacySettings)).toProto())
                .build()
        }
    }

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

    val templateLastUsedFlow: Flow<Map<String, Long>> = dataStore.data
        .catchIoException("Error reading template usage history.")
        .map { prefs -> prefs.templateLastUsedEpochMillisMap }

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

    suspend fun setActiveTemplate(templateId: String): TemplateSelectionResult {
        var selectionResult: TemplateSelectionResult? = null
        var selectedAt: Long? = null
        dataStore.updateData { preferences ->
            val template = preferences.findTemplate(templateId)
            val missingPlaceholders = template?.let {
                Template.findMissingPlaceholders(it.content)
            }.orEmpty()
            selectionResult = when {
                template == null -> TemplateSelectionResult.NotFound(templateId)
                missingPlaceholders.isNotEmpty() -> {
                    TemplateSelectionResult.Blocked(
                        template = template,
                        missingPlaceholders = missingPlaceholders
                    )
                }
                preferences.activeTemplateId.ifEmpty { DEFAULT_TEMPLATE_ID } == templateId -> {
                    TemplateSelectionResult.AlreadyActive(template)
                }
                else -> {
                    val previousTimestamp = preferences.templateLastUsedEpochMillisMap[templateId]
                    selectedAt = nextTemplateUseTimestamp(
                        now = currentTimeMillis(),
                        existingTimestamps = preferences.templateLastUsedEpochMillisMap.values
                    )
                    val change = TemplateSelectionChange(
                        selectedTemplateId = templateId,
                        selectedLastUsedAt = checkNotNull(selectedAt),
                        previousActiveTemplateId = preferences.activeTemplateId.ifEmpty {
                            DEFAULT_TEMPLATE_ID
                        },
                        previousSelectedLastUsedAt = previousTimestamp
                    )
                    TemplateSelectionResult.Selected(template, change)
                }
            }

            val selected = selectionResult as? TemplateSelectionResult.Selected
                ?: return@updateData preferences
            preferences.toBuilder()
                .setActiveTemplateId(selected.template.id)
                .putTemplateLastUsedEpochMillis(selected.template.id, checkNotNull(selectedAt))
                .build()
        }
        return checkNotNull(selectionResult)
    }

    suspend fun undoTemplateSelection(change: TemplateSelectionChange): Boolean {
        var undone = false
        dataStore.updateData { preferences ->
            val activeTemplateId = preferences.activeTemplateId.ifEmpty { DEFAULT_TEMPLATE_ID }
            val selectedLastUsedAt = preferences.templateLastUsedEpochMillisMap[change.selectedTemplateId]
            if (activeTemplateId != change.selectedTemplateId ||
                selectedLastUsedAt != change.selectedLastUsedAt
            ) {
                return@updateData preferences
            }

            undone = true
            val restoredActiveTemplateId = preferences.resolveValidTemplateId(
                change.previousActiveTemplateId
            )
            preferences.toBuilder()
                .setActiveTemplateId(restoredActiveTemplateId)
                .apply {
                    val previousTimestamp = change.previousSelectedLastUsedAt
                    if (previousTimestamp == null) {
                        removeTemplateLastUsedEpochMillis(change.selectedTemplateId)
                    } else {
                        putTemplateLastUsedEpochMillis(change.selectedTemplateId, previousTimestamp)
                    }
                }
                .build()
        }
        return undone
    }

    suspend fun restoreActiveTemplate(templateId: String): String {
        var restoredTemplateId = DEFAULT_TEMPLATE_ID
        dataStore.updateData { preferences ->
            restoredTemplateId = preferences.resolveValidTemplateId(templateId)
            preferences.toBuilder()
                .setActiveTemplateId(restoredTemplateId)
                .build()
        }
        return restoredTemplateId
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
            builder.removeTemplateLastUsedEpochMillis(templateId)
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
                .clearTemplateLastUsedEpochMillis()
                .setActiveTemplateId(DEFAULT_TEMPLATE_ID)
                .build()
        }
    }

    private fun UserPreferences.findTemplate(templateId: String): Template? = when (templateId) {
        DEFAULT_TEMPLATE_ID -> builtInDefaultTemplate
        else -> templatesList.firstOrNull { it.id == templateId }?.let { Template.fromProto(it) }
    }

    private fun UserPreferences.resolveValidTemplateId(templateId: String): String =
        findTemplate(templateId)
            ?.takeIf { Template.hasRequiredPlaceholders(it.content) }
            ?.id
            ?: DEFAULT_TEMPLATE_ID
}

internal fun nextTemplateUseTimestamp(
    now: Long,
    existingTimestamps: Collection<Long>
): Long {
    val latestTimestamp = existingTimestamps.maxOrNull() ?: 0L
    return if (latestTimestamp == Long.MAX_VALUE) {
        Long.MAX_VALUE
    } else {
        maxOf(now, latestTimestamp + 1L)
    }
}
