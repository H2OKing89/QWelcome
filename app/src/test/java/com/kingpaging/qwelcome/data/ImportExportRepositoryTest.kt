package com.kingpaging.qwelcome.data

import com.kingpaging.qwelcome.R
import com.kingpaging.qwelcome.testutil.FakeResourceProvider
import com.kingpaging.qwelcome.viewmodel.factory.AppViewModelProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ImportExportRepositoryTest {

    private val settingsStore = mockk<SettingsStore>(relaxed = true)
    private val encodeDefaultsJson = Json { encodeDefaults = true }

    private val repository = ImportExportRepository(
        settingsStore = settingsStore,
        resourceProvider = FakeResourceProvider()
    )

    @Before
    fun setup() {
        AppViewModelProvider.resetForTesting()
    }

    @After
    fun tearDown() {
        AppViewModelProvider.resetForTesting()
    }

    @Test
    fun `validateImport returns invalid when payload exceeds hard limit by length`() = runTest {
        val oversized = "a".repeat(MAX_IMPORT_SIZE_BYTES + 1)

        val result = repository.validateImport(oversized)

        assertTrue(result is ImportValidationResult.Invalid)
        val message = (result as ImportValidationResult.Invalid).message
        assertTrue(message.startsWith("string_${R.string.error_import_too_large}["))
        assertTrue(message.contains(formatBytesAsMb(MAX_IMPORT_SIZE_BYTES.toLong())))
    }

    @Test
    fun `validateImport returns invalid when utf8 byte size exceeds hard limit`() = runTest {
        val oversizedUtf8 = "€".repeat(MAX_IMPORT_SIZE_BYTES / 2 + 1)

        val result = repository.validateImport(oversizedUtf8)

        assertTrue(result is ImportValidationResult.Invalid)
        val message = (result as ImportValidationResult.Invalid).message
        assertTrue(message.startsWith("string_${R.string.error_import_too_large}["))
        assertTrue(message.contains(formatBytesAsMb(MAX_IMPORT_SIZE_BYTES.toLong())))
    }

    @Test
    fun `exportTemplatePack returns localized no templates message when nothing to export`() = runTest {
        val result = repository.exportTemplatePack()

        assertTrue(result is ExportResult.Error)
        assertEquals(
            "string_${R.string.error_no_templates_to_export}",
            (result as ExportResult.Error).message
        )
    }

    @Test
    fun `exportTemplatePack returns specific message when built in default is explicitly requested`() = runTest {
        coEvery { settingsStore.getAllTemplates() } returns listOf(
            Template(
                id = DEFAULT_TEMPLATE_ID,
                name = "Default",
                content = "Hello {{ customer_name }} {{ ssid }}"
            )
        )

        val result = repository.exportTemplatePack(listOf(DEFAULT_TEMPLATE_ID))

        assertTrue(result is ExportResult.Error)
        assertEquals(
            "string_${R.string.error_export_default_template_not_supported}",
            (result as ExportResult.Error).message
        )
    }

    @Test
    fun `validateImport returns invalid when full backup has empty templates`() = runTest {
        val backup = FullBackup.create(
            techProfile = TechProfile(name = "Tech", title = "Field Tech", dept = "Dept"),
            templates = emptyList(),
            appVersion = "1.0.0"
        )
        val json = encodeDefaultsJson.encodeToString(backup)

        val result = repository.validateImport(json)

        assertTrue(result is ImportValidationResult.Invalid)
        assertEquals(
            "string_${R.string.error_import_template_pack_empty}",
            (result as ImportValidationResult.Invalid).message
        )
    }

    @Test
    fun `legacy full backup with loadout keys validates and applies`() = runTest {
        coEvery { settingsStore.getUserTemplates() } returns emptyList()
        coEvery { settingsStore.getAllTemplates() } returns emptyList()

        val legacyJson = """
            {
              "schemaVersion": 1,
              "kind": "full-backup",
              "exportedAt": "2026-02-10T08:00:00Z",
              "appVersion": "2.4.0",
              "techProfile": {
                "name": "Tech",
                "title": "Field Tech",
                "dept": "Network Services"
              },
              "templates": [
                {
                  "id": "template-1",
                  "name": "Install Welcome",
                  "content": "Hello {{ customer_name }}, SSID: {{ ssid }}",
                  "createdAt": "2026-02-01T00:00:00Z",
                  "modifiedAt": "2026-02-01T00:00:00Z",
                  "slug": "install-welcome",
                  "sortOrder": 1,
                  "tags": ["Install"]
                }
              ],
              "loadouts": [
                { "id": "loadout-1", "name": "Morning", "templateId": "template-1" }
              ],
              "activeLoadoutId": "loadout-1",
              "defaults": { "defaultTemplateId": "template-1" }
            }
        """.trimIndent()

        val validation = repository.validateImport(legacyJson)
        assertTrue(validation is ImportValidationResult.ValidFullBackup)

        val backup = (validation as ImportValidationResult.ValidFullBackup).backup
        val applyResult = repository.applyFullBackup(backup)

        assertTrue(applyResult is ImportApplyResult.Success)
        coVerify { settingsStore.saveTemplates(any()) }
    }
}

