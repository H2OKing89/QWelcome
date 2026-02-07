package com.kingpaging.qwelcome.data

import com.kingpaging.qwelcome.viewmodel.factory.AppViewModelProvider
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ImportExportRepositoryTest {

    private val repository = ImportExportRepository(
        settingsStore = mockk(relaxed = true)
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
        assertTrue((result as ImportValidationResult.Invalid).message.contains("max"))
    }

    @Test
    fun `validateImport returns invalid when utf8 byte size exceeds hard limit`() = runTest {
        val oversizedUtf8 = "€".repeat(MAX_IMPORT_SIZE_BYTES / 2 + 1)

        val result = repository.validateImport(oversizedUtf8)

        assertTrue(result is ImportValidationResult.Invalid)
        assertTrue((result as ImportValidationResult.Invalid).message.contains("max"))
    }
}

