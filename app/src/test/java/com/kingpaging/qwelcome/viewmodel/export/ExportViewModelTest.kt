package com.kingpaging.qwelcome.viewmodel.export

import app.cash.turbine.test
import com.kingpaging.qwelcome.data.ExportResult
import com.kingpaging.qwelcome.data.ImportExportRepository
import com.kingpaging.qwelcome.data.SettingsStore
import com.kingpaging.qwelcome.data.Template
import com.kingpaging.qwelcome.testutil.MainDispatcherRule
import com.kingpaging.qwelcome.viewmodel.factory.AppViewModelProvider
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ExportViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val mockRepo = mockk<ImportExportRepository>(relaxed = true)
    private val mockStore = mockk<SettingsStore>(relaxed = true)
    private lateinit var vm: ExportViewModel

    private val testTemplates = listOf(
        Template(id = "t1", name = "Template 1", content = "Content 1"),
        Template(id = "t2", name = "Template 2", content = "Content 2")
    )

    @Before
    fun setup() {
        coEvery { mockStore.getUserTemplates() } returns testTemplates
        every { mockStore.recentSharePackagesFlow } returns flowOf(emptyList())
        vm = ExportViewModel(mockRepo, mockStore)
    }

    @After
    fun tearDown() {
        AppViewModelProvider.resetForTesting()
    }

    @Test
    fun `initial state is default`() {
        val state = vm.uiState.value
        assertFalse(state.isExporting)
        assertNull(state.lastExportedJson)
        assertNull(state.lastExportType)
        assertFalse(state.showTemplateSelectionDialog)
    }

    @Test
    fun `onTemplatePackRequested loads templates and shows dialog`() = runTest {
        vm.onTemplatePackRequested()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state.showTemplateSelectionDialog)
        assertEquals(2, state.availableTemplates.size)
        assertEquals(setOf("t1", "t2"), state.selectedTemplateIds)
    }

    @Test
    fun `onTemplatePackRequested with no templates emits error`() = runTest {
        coEvery { mockStore.getUserTemplates() } returns emptyList()

        vm.events.test {
            vm.onTemplatePackRequested()
            advanceUntilIdle()

            val event = awaitItem()
            assertTrue(event is ExportEvent.ExportError)
        }
    }

    @Test
    fun `toggleTemplateSelection toggles a template`() = runTest {
        vm.onTemplatePackRequested()
        advanceUntilIdle()

        vm.toggleTemplateSelection("t1")

        assertEquals(setOf("t2"), vm.uiState.value.selectedTemplateIds)
    }

    @Test
    fun `toggleTemplateSelection re-adds removed template`() = runTest {
        vm.onTemplatePackRequested()
        advanceUntilIdle()

        vm.toggleTemplateSelection("t1")
        vm.toggleTemplateSelection("t1")

        assertEquals(setOf("t1", "t2"), vm.uiState.value.selectedTemplateIds)
    }

    @Test
    fun `toggleSelectAll deselects all when all selected`() = runTest {
        vm.onTemplatePackRequested()
        advanceUntilIdle()

        vm.toggleSelectAll()

        assertTrue(vm.uiState.value.selectedTemplateIds.isEmpty())
    }

    @Test
    fun `toggleSelectAll selects all when none selected`() = runTest {
        vm.onTemplatePackRequested()
        advanceUntilIdle()

        vm.toggleSelectAll() // Deselect all
        vm.toggleSelectAll() // Select all again

        assertEquals(setOf("t1", "t2"), vm.uiState.value.selectedTemplateIds)
    }

    @Test
    fun `exportSelectedTemplates shows success state`() = runTest {
        coEvery { mockRepo.exportTemplatePack(any()) } returns
                ExportResult.Success(json = "{\"templates\":[]}", templateCount = 2)

        vm.onTemplatePackRequested()
        advanceUntilIdle()

        vm.exportSelectedTemplates()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isExporting)
        assertFalse(state.showTemplateSelectionDialog)
        assertNotNull(state.lastExportedJson)
        assertEquals(ExportType.TEMPLATE_PACK, state.lastExportType)
        assertEquals(2, state.templateCount)
    }

    @Test
    fun `exportFullBackup shows success state`() = runTest {
        coEvery { mockRepo.exportFullBackup() } returns
                ExportResult.Success(json = "{\"backup\":true}", templateCount = 3)

        vm.exportFullBackup()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isExporting)
        assertEquals(ExportType.FULL_BACKUP, state.lastExportType)
        assertEquals(3, state.templateCount)
    }

    @Test
    fun `export error emits ExportError event`() = runTest {
        coEvery { mockRepo.exportFullBackup() } returns
                ExportResult.Error("Export failed")

        vm.events.test {
            vm.exportFullBackup()
            advanceUntilIdle()

            // First event from ExportResult.Error
            val event = awaitItem()
            assertTrue(event is ExportEvent.ExportError)
            assertEquals("Export failed", (event as ExportEvent.ExportError).message)
        }
    }

    @Test
    fun `onCopiedToClipboard emits CopiedToClipboard event`() = runTest {
        coEvery { mockRepo.exportFullBackup() } returns
                ExportResult.Success(json = "{}", templateCount = 1)

        vm.events.test {
            vm.exportFullBackup()
            advanceUntilIdle()
            // Consume the ExportSuccess from the export operation
            val successEvent = awaitItem()
            assertTrue(successEvent is ExportEvent.ExportSuccess)

            vm.onCopiedToClipboard()
            advanceUntilIdle()

            val event = awaitItem()
            assertTrue(event is ExportEvent.CopiedToClipboard)
            assertEquals(ExportType.FULL_BACKUP, (event as ExportEvent.CopiedToClipboard).type)
        }
    }

    @Test
    fun `onShareRequested emits ShareReady event`() = runTest {
        coEvery { mockRepo.exportFullBackup() } returns
                ExportResult.Success(json = "{\"data\":true}", templateCount = 1)

        vm.events.test {
            vm.exportFullBackup()
            advanceUntilIdle()
            // Consume the ExportSuccess from the export operation
            val successEvent = awaitItem()
            assertTrue(successEvent is ExportEvent.ExportSuccess)

            vm.onShareRequested()
            advanceUntilIdle()

            val event = awaitItem()
            assertTrue(event is ExportEvent.ShareReady)
            assertEquals("{\"data\":true}", (event as ExportEvent.ShareReady).json)
        }
    }

    @Test
    fun `onShareToPackageRequested emits ShareToAppReady event`() = runTest {
        coEvery { mockRepo.exportFullBackup() } returns
            ExportResult.Success(json = "{\"data\":true}", templateCount = 1)

        vm.events.test {
            vm.exportFullBackup()
            advanceUntilIdle()
            val successEvent = awaitItem()
            assertTrue(successEvent is ExportEvent.ExportSuccess)

            vm.onShareToPackageRequested("com.example.app")
            advanceUntilIdle()

            val event = awaitItem()
            assertTrue(event is ExportEvent.ShareToAppReady)
            assertEquals("com.example.app", (event as ExportEvent.ShareToAppReady).packageName)
        }
    }

    @Test
    fun `onSaveToFileRequested emits RequestFileSave event`() = runTest {
        coEvery { mockRepo.exportFullBackup() } returns
                ExportResult.Success(json = "{}", templateCount = 1)

        vm.events.test {
            vm.exportFullBackup()
            advanceUntilIdle()
            // Consume the ExportSuccess from the export operation
            val successEvent = awaitItem()
            assertTrue(successEvent is ExportEvent.ExportSuccess)

            vm.onSaveToFileRequested()
            advanceUntilIdle()

            val event = awaitItem()
            assertTrue(event is ExportEvent.RequestFileSave)
            assertTrue((event as ExportEvent.RequestFileSave).suggestedName.contains("qwelcome_backup"))
        }
    }

    @Test
    fun `getPendingFileExportContent returns and clears content`() = runTest {
        coEvery { mockRepo.exportFullBackup() } returns
                ExportResult.Success(json = "{\"test\":1}", templateCount = 1)

        vm.exportFullBackup()
        advanceUntilIdle()

        vm.onSaveToFileRequested()
        advanceUntilIdle()

        val content = vm.getPendingFileExportContent()
        assertEquals("{\"test\":1}", content)

        // Second call should return null (already consumed)
        assertNull(vm.getPendingFileExportContent())
    }

    @Test
    fun `reset clears all state`() = runTest {
        coEvery { mockRepo.exportFullBackup() } returns
                ExportResult.Success(json = "{}", templateCount = 1)

        vm.exportFullBackup()
        advanceUntilIdle()

        vm.reset()

        val state = vm.uiState.value
        assertNull(state.lastExportedJson)
        assertNull(state.lastExportType)
        assertFalse(state.isExporting)
    }

    @Test
    fun `dismissTemplateSelection hides dialog`() = runTest {
        vm.onTemplatePackRequested()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.showTemplateSelectionDialog)

        vm.dismissTemplateSelection()

        assertFalse(vm.uiState.value.showTemplateSelectionDialog)
    }
}
