package com.kingpaging.qwelcome.data

import com.kingpaging.qwelcome.R
import com.kingpaging.qwelcome.testutil.FakeResourceProvider
import com.kingpaging.qwelcome.viewmodel.factory.AppViewModelProvider
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ImportExportRepositoryTest {

    private val repository = ImportExportRepository(
        settingsStore = mockk(relaxed = true),
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
}

