package com.kingpaging.qwelcome.data

import android.util.Log
import com.kingpaging.qwelcome.R
import com.kingpaging.qwelcome.util.ResourceProvider

private const val TAG = "ImportApplyService"

internal class ImportApplyService(
    private val settingsStore: SettingsStore,
    private val resourceProvider: ResourceProvider
) {

    suspend fun applyTemplatePack(
        pack: TemplatePack,
        resolutions: Map<String, ConflictResolution> = emptyMap()
    ): ImportApplyResult {
        return try {
            val templatesToSave = resolveTemplates(pack.templates, resolutions)
            settingsStore.saveTemplates(templatesToSave)
            ImportApplyResult.Success(templatesToSave.size)
        } catch (e: kotlin.coroutines.cancellation.CancellationException) {
            throw e
        } catch (e: Exception) {
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
            val templatesToSave = resolveTemplates(backup.templates, resolutions)
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
                val importedIds = templatesToSave.map { it.id }.toSet()
                val targetDefaultId = backup.getEffectiveDefaultTemplateId() ?: DEFAULT_TEMPLATE_ID
                if (targetDefaultId == DEFAULT_TEMPLATE_ID || targetDefaultId in importedIds) {
                    settingsStore.setActiveTemplate(targetDefaultId)
                }
            }

            ImportApplyResult.Success(
                templatesImported = templatesToSave.size,
                techProfileImported = importTechProfile
            )
        } catch (e: kotlin.coroutines.cancellation.CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to apply full backup", e)
            ImportApplyResult.Error(resourceProvider.getString(R.string.error_import_failed, e.message ?: ""))
        }
    }

    private fun resolveTemplates(
        templates: List<Template>,
        resolutions: Map<String, ConflictResolution>
    ): List<Template> {
        return templates.mapNotNull { template ->
            when (resolutions[template.id]) {
                ConflictResolution.KEEP_EXISTING -> null
                ConflictResolution.SAVE_AS_COPY -> template.duplicate()
                ConflictResolution.REPLACE, null -> template
            }
        }
    }
}
