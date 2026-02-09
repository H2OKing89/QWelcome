package com.kingpaging.qwelcome.viewmodel.import_pkg

import app.cash.turbine.test
import com.kingpaging.qwelcome.data.ImportApplyResult
import com.kingpaging.qwelcome.data.ImportExportRepository
import com.kingpaging.qwelcome.data.ImportValidationResult
import com.kingpaging.qwelcome.data.Template
import com.kingpaging.qwelcome.data.TemplatePack
import com.kingpaging.qwelcome.testutil.FakeResourceProvider
import com.kingpaging.qwelcome.testutil.MainDispatcherRule
import com.kingpaging.qwelcome.viewmodel.factory.AppViewModelProvider
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ImportViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val mockRepo = mockk<ImportExportRepository>(relaxed = true)
    private lateinit var vm: ImportViewModel

    private val validPack = TemplatePack(
        templates = listOf(
            Template(
                id = "t1",
                name = "Template 1",
                content = "Hello {{ customer_name }}"
            )
        )
    )

    private val validResult = ImportValidationResult.ValidTemplatePack(
        pack = validPack,
        conflicts = emptyList(),
        warnings = emptyList()
    )

    @Before
    fun setup() {
        AppViewModelProvider.resetForTesting()
        vm = ImportViewModel(mockRepo, FakeResourceProvider())
    }

    @After
    fun tearDown() {
        AppViewModelProvider.resetForTesting()
    }

    @Test
    fun `initial state is Idle with no errors`() {
        assertEquals(ImportStep.Idle, vm.uiState.value.step)
        assertNull(vm.uiState.value.error)
        assertEquals(false, vm.uiState.value.isImporting)
    }

    @Test
    fun `valid JSON transitions to Validated step`() = runTest {
        coEvery { mockRepo.validateImport(any()) } returns validResult

        vm.onJsonContentReceived("{}")
        advanceUntilIdle()

        val step = vm.uiState.value.step
        assertTrue(step is ImportStep.Validated)
        assertEquals(false, vm.uiState.value.isImporting)
    }

    @Test
    fun `invalid JSON sets error and stays Idle`() = runTest {
        coEvery { mockRepo.validateImport(any()) } returns
                ImportValidationResult.Invalid("Invalid JSON format")

        vm.onJsonContentReceived("bad json")
        advanceUntilIdle()

        assertEquals(ImportStep.Idle, vm.uiState.value.step)
        assertEquals("Invalid JSON format", vm.uiState.value.error)
    }

    @Test
    fun `confirm import transitions to Complete and emits success event`() = runTest {
        coEvery { mockRepo.validateImport(any()) } returns validResult
        coEvery { mockRepo.applyTemplatePack(any(), any()) } returns
                ImportApplyResult.Success(templatesImported = 1, techProfileImported = false)

        vm.onJsonContentReceived("{}")
        advanceUntilIdle()

        vm.events.test {
            vm.onImportConfirmed()
            advanceUntilIdle()

            assertEquals(ImportStep.Complete, vm.uiState.value.step)

            val event = awaitItem()
            assertTrue(event is ImportEvent.ImportSuccess)
            assertEquals(1, (event as ImportEvent.ImportSuccess).templatesImported)
            assertEquals(false, event.techProfileImported)
        }
    }

    @Test
    fun `apply error resets to Idle and emits failure event`() = runTest {
        coEvery { mockRepo.validateImport(any()) } returns validResult
        coEvery { mockRepo.applyTemplatePack(any(), any()) } returns
                ImportApplyResult.Error("Write failed")

        vm.onJsonContentReceived("{}")
        advanceUntilIdle()

        vm.events.test {
            vm.onImportConfirmed()
            advanceUntilIdle()

            assertEquals(ImportStep.Idle, vm.uiState.value.step)
            assertEquals("Write failed", vm.uiState.value.error)

            val event = awaitItem()
            assertTrue(event is ImportEvent.ImportFailed)
            assertEquals("Write failed", (event as ImportEvent.ImportFailed).message)
        }
    }

    @Test
    fun `reset cancels in-flight import and returns to Idle`() = runTest {
        coEvery { mockRepo.validateImport(any()) } returns validResult

        vm.onJsonContentReceived("{}")
        advanceUntilIdle()

        vm.reset()

        assertEquals(ImportStep.Idle, vm.uiState.value.step)
        assertNull(vm.uiState.value.error)
        assertEquals(false, vm.uiState.value.isImporting)
    }

    @Test
    fun `duplicate submit while importing is ignored`() = runTest {
        coEvery { mockRepo.validateImport(any()) } returns validResult

        // First call starts import
        vm.onJsonContentReceived("{}")
        // Second call while still importing should be ignored
        vm.onJsonContentReceived("{}")

        advanceUntilIdle()

        // Should only have processed once
        val step = vm.uiState.value.step
        assertTrue(step is ImportStep.Validated)
    }

    @Test
    fun `onPasteContent delegates to onJsonContentReceived`() = runTest {
        coEvery { mockRepo.validateImport(any()) } returns validResult

        vm.onPasteContent("{}")
        advanceUntilIdle()

        assertTrue(vm.uiState.value.step is ImportStep.Validated)
    }

    @Test
    fun `onOpenFileRequest emits RequestFileOpen event`() = runTest {
        vm.events.test {
            vm.onOpenFileRequest()
            advanceUntilIdle()

            val event = awaitItem()
            assertTrue(event is ImportEvent.RequestFileOpen)
        }
    }
}
