package com.kingpaging.qwelcome.data

import android.util.Log
import com.kingpaging.qwelcome.BuildConfig
import com.kingpaging.qwelcome.R
import com.kingpaging.qwelcome.util.ResourceProvider
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val TAG = "ExportService"
private const val MAX_EXPORT_SIZE_BYTES = 10 * 1024 * 1024

internal class ExportService(
    private val settingsStore: SettingsStore,
    private val resourceProvider: ResourceProvider,
    private val json: Json
) {

    suspend fun exportTemplatePack(templateIds: List<String> = emptyList()): ExportResult {
        return try {
            val allTemplates = settingsStore.getAllTemplates()
            val templatesToExport = if (templateIds.isEmpty()) {
                // Export all user templates (exclude built-in default)
                allTemplates.filter { it.id != DEFAULT_TEMPLATE_ID }
            } else {
                allTemplates.filter { it.id in templateIds && it.id != DEFAULT_TEMPLATE_ID }
            }

            if (templatesToExport.isEmpty()) {
                return ExportResult.Error(
                    resourceProvider.getString(R.string.error_no_templates_to_export)
                )
            }

            // First use cheap String.length estimate, then compute UTF-8 size if near limit.
            val cheapEstimate = templatesToExport.sumOf { it.content.length + it.name.length + 200 }
            if (cheapEstimate > MAX_EXPORT_SIZE_BYTES / 2) {
                val preciseSize = templatesToExport.sumOf {
                    it.content.toByteArray(Charsets.UTF_8).size +
                        it.name.toByteArray(Charsets.UTF_8).size + 200
                }
                if (preciseSize > MAX_EXPORT_SIZE_BYTES) {
                    return ExportResult.Error(
                        resourceProvider.getString(
                            R.string.error_export_too_large,
                            formatBytesAsMb(MAX_EXPORT_SIZE_BYTES.toLong())
                        )
                    )
                }
            }

            val pack = TemplatePack.create(
                templates = templatesToExport,
                appVersion = getAppVersion()
            )

            val jsonString = json.encodeToString(pack)
            ExportResult.Success(jsonString, templatesToExport.size)
        } catch (e: kotlin.coroutines.cancellation.CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to export template pack", e)
            ExportResult.Error(
                resourceProvider.getString(R.string.error_export_failed, e.message ?: "")
            )
        }
    }

    suspend fun exportFullBackup(): ExportResult {
        return try {
            val techProfile = settingsStore.getTechProfile()
            val allTemplates = settingsStore.getAllTemplates()
            val userTemplates = allTemplates.filter { it.id != DEFAULT_TEMPLATE_ID }
            val activeTemplateId = settingsStore.getActiveTemplateId()

            // First use cheap String.length estimate, then compute UTF-8 size if near limit.
            val cheapEstimate = userTemplates.sumOf { it.content.length + it.name.length + 200 } +
                techProfile.name.length + techProfile.title.length + techProfile.dept.length + 500
            if (cheapEstimate > MAX_EXPORT_SIZE_BYTES / 2) {
                val preciseSize = userTemplates.sumOf {
                    it.content.toByteArray(Charsets.UTF_8).size +
                        it.name.toByteArray(Charsets.UTF_8).size + 200
                } + techProfile.name.toByteArray(Charsets.UTF_8).size +
                    techProfile.title.toByteArray(Charsets.UTF_8).size +
                    techProfile.dept.toByteArray(Charsets.UTF_8).size + 500
                if (preciseSize > MAX_EXPORT_SIZE_BYTES) {
                    return ExportResult.Error(
                        resourceProvider.getString(
                            R.string.error_export_too_large,
                            formatBytesAsMb(MAX_EXPORT_SIZE_BYTES.toLong())
                        )
                    )
                }
            }

            val backup = FullBackup.create(
                techProfile = techProfile,
                templates = userTemplates,
                defaultTemplateId = activeTemplateId.takeIf { it != DEFAULT_TEMPLATE_ID },
                appVersion = getAppVersion()
            )

            val jsonString = json.encodeToString(backup)
            ExportResult.Success(jsonString, userTemplates.size)
        } catch (e: kotlin.coroutines.cancellation.CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to export full backup", e)
            ExportResult.Error(
                resourceProvider.getString(R.string.error_export_failed, e.message ?: "")
            )
        }
    }

    private fun getAppVersion(): String {
        return BuildConfig.VERSION_NAME.ifEmpty { "1.0" }
    }
}
