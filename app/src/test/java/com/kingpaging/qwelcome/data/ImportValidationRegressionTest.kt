package com.kingpaging.qwelcome.data

import com.kingpaging.qwelcome.testutil.FakeResourceProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.coroutines.ContinuationInterceptor

class ImportValidationRegressionTest {
    private val store = mockk<SettingsStore>(relaxed = true)
    private val json = Json { encodeDefaults = true }

    private fun template(id: String = "template-1"): Template =
        Template(
            id = id,
            name = "Welcome",
            content = "Hello {{ customer_name }}, SSID: {{ ssid }}",
        )

    @Test
    fun `default only full backup can be exported validated and restored`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val repository = ImportExportRepository(store, FakeResourceProvider(), dispatcher)
            val profile = TechProfile(name = "Technician", title = "Field Tech", dept = "Install")
            coEvery { store.getTechProfile() } returns profile
            coEvery { store.getAllTemplates() } returns listOf(template(DEFAULT_TEMPLATE_ID))
            coEvery { store.getActiveTemplateId() } returns DEFAULT_TEMPLATE_ID

            val exported = repository.exportFullBackup() as ExportResult.Success
            assertEquals(0, exported.templateCount)
            val validated = repository.validateImport(exported.json) as ImportValidationResult.ValidFullBackup
            val applied = repository.applyFullBackup(validated.backup, importTechProfile = true)

            assertEquals(ImportApplyResult.Success(0, techProfileImported = true), applied)
            coVerify {
                store.restoreFullBackup(
                    templates = emptyList(),
                    techProfile = profile,
                    activeTemplateId = DEFAULT_TEMPLATE_ID,
                )
            }
        }

    @Test
    fun `blank duplicate and reserved ids fail validation and cannot be applied directly`() =
        runTest {
            val repository =
                ImportExportRepository(store, FakeResourceProvider(), StandardTestDispatcher(testScheduler))
            val invalidLists =
                listOf(
                    listOf(template("")),
                    listOf(template("  ")),
                    listOf(template(DEFAULT_TEMPLATE_ID)),
                    listOf(template(NEW_TEMPLATE_ID)),
                    listOf(template("same"), template("same").copy(name = "Different content")),
                )

            invalidLists.forEach { templates ->
                val pack = TemplatePack(templates = templates)
                val backup = FullBackup.create(TechProfile(), templates)
                assertTrue(repository.validateImport(json.encodeToString(pack)) is ImportValidationResult.Invalid)
                assertTrue(repository.validateImport(json.encodeToString(backup)) is ImportValidationResult.Invalid)
                assertTrue(repository.applyTemplatePack(pack) is ImportApplyResult.Error)
                assertTrue(repository.applyFullBackup(backup) is ImportApplyResult.Error)
            }

            coVerify(exactly = 0) { store.saveTemplates(any()) }
            coVerify(exactly = 0) { store.restoreFullBackup(any(), any(), any()) }
        }

    @Test
    fun `blank required fields are structural errors`() =
        runTest {
            val repository =
                ImportExportRepository(store, FakeResourceProvider(), StandardTestDispatcher(testScheduler))
            listOf(template().copy(name = "  "), template().copy(content = "  ")).forEach { invalid ->
                val pack = TemplatePack(templates = listOf(invalid))
                assertTrue(repository.validateImport(json.encodeToString(pack)) is ImportValidationResult.Invalid)
                assertTrue(repository.applyTemplatePack(pack) is ImportApplyResult.Error)
            }
            coVerify(exactly = 0) { store.saveTemplates(any()) }
        }

    @Test
    fun `empty template pack remains invalid`() =
        runTest {
            val repository =
                ImportExportRepository(store, FakeResourceProvider(), StandardTestDispatcher(testScheduler))
            val result = repository.validateImport(json.encodeToString(TemplatePack(templates = emptyList())))
            assertTrue(result is ImportValidationResult.Invalid)
        }

    @Test
    fun `import count reflects conflict resolutions`() =
        runTest {
            val repository =
                ImportExportRepository(store, FakeResourceProvider(), StandardTestDispatcher(testScheduler))
            val pack = TemplatePack(templates = listOf(template("keep"), template("copy"), template("replace")))
            val result =
                repository.applyTemplatePack(
                    pack,
                    mapOf(
                        "keep" to ConflictResolution.KEEP_EXISTING,
                        "copy" to ConflictResolution.SAVE_AS_COPY,
                        "replace" to ConflictResolution.REPLACE,
                    ),
                )

            assertEquals(ImportApplyResult.Success(2), result)
            coVerify {
                store.saveTemplates(
                    match { saved ->
                        saved.size == 2 &&
                            saved.any { it.id == "replace" } &&
                            saved.none { it.id == "keep" || it.id == "copy" }
                    },
                )
            }
        }

    @Test
    fun `non structural import warnings survive validation`() =
        runTest {
            val repository =
                ImportExportRepository(store, FakeResourceProvider(), StandardTestDispatcher(testScheduler))
            val pack =
                TemplatePack(
                    schemaVersion = EXPORT_SCHEMA_VERSION + 1,
                    templates = listOf(template().copy(content = "Message without placeholders")),
                )
            val result =
                repository.validateImport(json.encodeToString(pack)) as ImportValidationResult.ValidTemplatePack

            assertTrue(result.warnings.any { it is ImportWarning.NewerVersion })
            assertTrue(result.warnings.any { it is ImportWarning.MissingPlaceholders })
        }

    @Test
    fun `repository uses injected computation dispatcher for validation and serialization`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val repository = ImportExportRepository(store, FakeResourceProvider(), dispatcher)
            coEvery { store.getUserTemplates() } coAnswers {
                assertEquals(dispatcher, currentCoroutineContext()[ContinuationInterceptor])
                emptyList()
            }
            coEvery { store.getAllTemplates() } coAnswers {
                assertEquals(dispatcher, currentCoroutineContext()[ContinuationInterceptor])
                listOf(template())
            }

            val pack = TemplatePack(templates = listOf(template()))
            assertTrue(repository.validateImport(json.encodeToString(pack)) is ImportValidationResult.ValidTemplatePack)
            assertTrue(repository.exportTemplatePack() is ExportResult.Success)
        }
}
