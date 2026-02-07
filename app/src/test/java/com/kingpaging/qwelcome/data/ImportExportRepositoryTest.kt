package com.kingpaging.qwelcome.data

import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class ImportExportRepositoryTest {

    private val repository = ImportExportRepository(
        settingsStore = mockk(relaxed = true)
    )

    @Test
    fun `validateImport returns invalid when payload exceeds hard limit by length`() = runTest {
        val oversized = "a".repeat(MAX_IMPORT_SIZE_BYTES + 1)

        val result = repository.validateImport(oversized)

        assertTrue(result is ImportValidationResult.Invalid)
        assertTrue((result as ImportValidationResult.Invalid).message.contains("max 10MB"))
    }

    @Test
    fun `validateImport returns invalid when utf8 byte size exceeds hard limit`() = runTest {
        val oversizedUtf8 = "€".repeat(MAX_IMPORT_SIZE_BYTES / 2 + 1)

        val result = repository.validateImport(oversizedUtf8)

        assertTrue(result is ImportValidationResult.Invalid)
        assertTrue((result as ImportValidationResult.Invalid).message.contains("max 10MB"))
    }
}

