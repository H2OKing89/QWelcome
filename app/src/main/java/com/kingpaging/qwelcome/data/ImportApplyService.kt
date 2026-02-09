package com.kingpaging.qwelcome.data

import android.util.Log
import com.kingpaging.qwelcome.R
import com.kingpaging.qwelcome.util.ResourceProvider
import java.io.IOException
import kotlinx.serialization.SerializationException

private const val TAG = "ImportApplyService"

@Suppress("TooGenericExceptionCaught")
internal class ImportApplyService(
    private val settingsStore: SettingsStore,
    private val resourceProvider: ResourceProvider
) {

    suspend fun applyTemplatePack(
        pack: TemplatePack,
        resolutions: Map<String, ConflictResolution> = emptyMap()
    ): ImportApplyResult {
        return try {
            val resolved = resolveTemplates(pack.templates, resolutions)
            val templatesToSave = resolved.templates
            settingsStore.saveTemplates(templatesToSave)
            ImportApplyResult.Success(templatesToSave.size)
        } catch (e: kotlin.coroutines.cancellation.CancellationException) {
            throw e
        } catch (e: IOException) {
            Log.e(TAG, "I/O failure while applying template pack", e)
            ImportApplyResult.Error(resourceProvider.getString(R.string.error_import_failed, e.message ?: ""))
        } catch (e: SerializationException) {
            Log.e(TAG, "Serialization failure while applying template pack", e)
            ImportApplyResult.Error(resourceProvider.getString(R.string.error_import_failed, e.message ?: ""))
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Invalid import data while applying template pack", e)
            ImportApplyResult.Error(resourceProvider.getString(R.string.error_import_failed, e.message ?: ""))
        } catch (e: Exception) {
            // Fallback for unexpected runtime failures from persistence/runtime layers.
            Log.e(TAG, "Failed to apply template pack", e)
            ImportApplyResult.Error(resourceProvider.getString(R.string.error_import_failed, e.message ?: ""))
        }
    }

    suspend fun applyFullBackup(
        backup: FullBackup,
        importTechProfile: Boolean = false,
        importDefaultTemplate: Boolean = true,
        resolutions: Map<String, ConflictResolution> = emptyMap()
    ): ImportApplyResult {
        return try {
            val existingIds = settingsStore.getAllTemplates().map { it.id }.toSet()
            val resolved = resolveTemplates(backup.templates, resolutions)
            val templatesToSave = resolved.templates
            settingsStore.saveTemplates(templatesToSave)

            if (importTechProfile) {
                settingsStore.saveTechProfile(
                    TechProfile(
                        name = backup.techProfile.name,
                        title = backup.techProfile.title,
                        dept = backup.techProfile.getDepartment()
                    )
                )
            }

            if (importDefaultTemplate) {
                val requestedDefaultId = backup.getEffectiveDefaultTemplateId() ?: DEFAULT_TEMPLATE_ID
                val resolvedDefaultId = resolved.idMap[requestedDefaultId] ?: DEFAULT_TEMPLATE_ID
                val availableIds = existingIds + templatesToSave.map { it.id }
                if (resolvedDefaultId == DEFAULT_TEMPLATE_ID || resolvedDefaultId in availableIds) {
                    settingsStore.setActiveTemplate(resolvedDefaultId)
                }
            }

            ImportApplyResult.Success(
                templatesImported = templatesToSave.size,
                techProfileImported = importTechProfile
            )
        } catch (e: kotlin.coroutines.cancellation.CancellationException) {
            throw e
        } catch (e: IOException) {
            Log.e(TAG, "I/O failure while applying full backup", e)
            ImportApplyResult.Error(resourceProvider.getString(R.string.error_import_failed, e.message ?: ""))
        } catch (e: SerializationException) {
            Log.e(TAG, "Serialization failure while applying full backup", e)
            ImportApplyResult.Error(resourceProvider.getString(R.string.error_import_failed, e.message ?: ""))
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Invalid import data while applying full backup", e)
            ImportApplyResult.Error(resourceProvider.getString(R.string.error_import_failed, e.message ?: ""))
        } catch (e: Exception) {
            // Fallback for unexpected runtime failures from persistence/runtime layers.
            Log.e(TAG, "Failed to apply full backup", e)
            ImportApplyResult.Error(resourceProvider.getString(R.string.error_import_failed, e.message ?: ""))
        }
    }

    private data class ResolvedTemplates(
        val templates: List<Template>,
        val idMap: Map<String, String>
    )

    private fun resolveTemplates(
        templates: List<Template>,
        resolutions: Map<String, ConflictResolution>
    ): ResolvedTemplates {
        val idMap = mutableMapOf<String, String>()
        val resolvedTemplates = templates.mapNotNull { template ->
            when (resolutions[template.id]) {
                ConflictResolution.KEEP_EXISTING -> {
                    idMap[template.id] = template.id
                    null
                }
                ConflictResolution.SAVE_AS_COPY -> {
                    val copy = template.duplicate()
                    idMap[template.id] = copy.id
                    copy
                }
                ConflictResolution.REPLACE, null -> {
                    idMap[template.id] = template.id
                    template
                }
            }
        }
        return ResolvedTemplates(
            templates = resolvedTemplates,
            idMap = idMap
        )
    }
}
