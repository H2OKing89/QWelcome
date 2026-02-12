package com.kingpaging.qwelcome.viewmodel.templates

import app.cash.turbine.test
import com.kingpaging.qwelcome.data.DEFAULT_TEMPLATE_ID
import com.kingpaging.qwelcome.data.SettingsStore
import com.kingpaging.qwelcome.data.Template
import com.kingpaging.qwelcome.testutil.MainDispatcherRule
import com.kingpaging.qwelcome.viewmodel.factory.AppViewModelProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TemplateListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val mockStore = mockk<SettingsStore>(relaxed = true)
    private lateinit var vm: TemplateListViewModel

    private val defaultTemplate = Template(
        id = DEFAULT_TEMPLATE_ID,
        name = "Default Welcome",
        content = "Hello {{ customer_name }}, SSID: {{ ssid }}"
    )
    private val userTemplate = Template(
        id = "650e8400-e29b-41d4-a716-446655440001",
        name = "Custom Template",
        content = "Hi {{ customer_name }}, your SSID is {{ ssid }}"
    )

    @Before
    fun setup() {
        every { mockStore.allTemplatesFlow } returns flowOf(listOf(defaultTemplate, userTemplate))
        every { mockStore.activeTemplateIdFlow } returns flowOf(DEFAULT_TEMPLATE_ID)
        every { mockStore.defaultTemplateContent } returns defaultTemplate.content
        vm = TemplateListViewModel(mockStore)
    }

    @After
    fun tearDown() {
        AppViewModelProvider.resetForTesting()
    }

    @Test
    fun `init collects templates from store`() = runTest {
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(2, state.templates.size)
        assertEquals(DEFAULT_TEMPLATE_ID, state.activeTemplateId)
        assertEquals(false, state.isLoading)
    }

    @Test
    fun `setActiveTemplate calls store and emits event`() = runTest {
        coEvery { mockStore.getTemplate("650e8400-e29b-41d4-a716-446655440001") } returns userTemplate

        vm.events.test {
            vm.setActiveTemplate("650e8400-e29b-41d4-a716-446655440001")
            advanceUntilIdle()

            coVerify { mockStore.setActiveTemplate("650e8400-e29b-41d4-a716-446655440001") }

            val event = awaitItem()
            assertTrue(event is TemplateListEvent.ActiveTemplateChanged)
            assertEquals("Custom Template", (event as TemplateListEvent.ActiveTemplateChanged).template.name)
        }
    }

    @Test
    fun `createTemplate saves and emits TemplateCreated event`() = runTest {
        vm.events.test {
            vm.createTemplate("New Template", "Hello {{ customer_name }}, SSID: {{ ssid }}", emptyList())
            advanceUntilIdle()

            coVerify { mockStore.saveTemplate(match { it.name == "New Template" }) }

            val event = awaitItem()
            assertTrue(event is TemplateListEvent.TemplateCreated)
            assertEquals("New Template", (event as TemplateListEvent.TemplateCreated).template.name)
        }
    }

    @Test
    fun `createTemplate trims whitespace from name`() = runTest {
        vm.events.test {
            vm.createTemplate("  Spaced Name  ", "Hello {{ customer_name }}, your SSID is {{ ssid }}", emptyList())
            advanceUntilIdle()

            coVerify { mockStore.saveTemplate(match { it.name == "Spaced Name" }) }

            awaitItem() // TemplateCreated event
        }
    }

    @Test
    fun `updateTemplate saves and emits TemplateUpdated event`() = runTest {
        coEvery { mockStore.getTemplate("650e8400-e29b-41d4-a716-446655440001") } returns userTemplate

        vm.events.test {
            vm.updateTemplate(
                "650e8400-e29b-41d4-a716-446655440001",
                "Updated Name",
                "Hi {{ customer_name }}, SSID: {{ ssid }}",
                emptyList()
            )
            advanceUntilIdle()

            coVerify { mockStore.saveTemplate(match { it.name == "Updated Name" && it.content == "Hi {{ customer_name }}, SSID: {{ ssid }}" }) }

            val event = awaitItem()
            assertTrue(event is TemplateListEvent.TemplateUpdated)
            assertEquals("Updated Name", (event as TemplateListEvent.TemplateUpdated).template.name)
        }
    }

    @Test
    fun `deleteTemplate removes and emits TemplateDeleted event`() = runTest {
        coEvery { mockStore.getTemplate("650e8400-e29b-41d4-a716-446655440001") } returns userTemplate

        vm.events.test {
            vm.deleteTemplate("650e8400-e29b-41d4-a716-446655440001")
            advanceUntilIdle()

            coVerify { mockStore.deleteTemplate("650e8400-e29b-41d4-a716-446655440001") }

            val event = awaitItem()
            assertTrue(event is TemplateListEvent.TemplateDeleted)
            assertEquals("Custom Template", (event as TemplateListEvent.TemplateDeleted).name)
        }
    }

    @Test
    fun `delete active template switches to default first`() = runTest {
        // Set up so 650e8400-e29b-41d4-a716-446655440001 is active
        every { mockStore.activeTemplateIdFlow } returns flowOf("650e8400-e29b-41d4-a716-446655440001")
        vm = TemplateListViewModel(mockStore)
        advanceUntilIdle()

        coEvery { mockStore.getTemplate("650e8400-e29b-41d4-a716-446655440001") } returns userTemplate

        vm.deleteTemplate("650e8400-e29b-41d4-a716-446655440001")
        advanceUntilIdle()

        // Should switch to default before deleting
        coVerify { mockStore.setActiveTemplate(DEFAULT_TEMPLATE_ID) }
        coVerify { mockStore.deleteTemplate("650e8400-e29b-41d4-a716-446655440001") }
    }

    @Test
    fun `duplicateTemplate saves copy and emits TemplateDuplicated event`() = runTest {
        vm.events.test {
            vm.duplicateTemplate(userTemplate)
            advanceUntilIdle()

            coVerify { mockStore.saveTemplate(match { it.id != userTemplate.id }) }

            val event = awaitItem()
            assertTrue(event is TemplateListEvent.TemplateDuplicated)
        }
    }

    @Test
    fun `duplicateAndEdit saves copy emits event and sets editingTemplate`() = runTest {
        vm.events.test {
            vm.duplicateAndEdit(userTemplate)
            advanceUntilIdle()

            coVerify { mockStore.saveTemplate(match { it.id != userTemplate.id }) }

            val event = awaitItem()
            assertTrue(event is TemplateListEvent.TemplateDuplicated)
        }

        val editingTemplate = vm.uiState.value.editingTemplate
        assertNotNull(editingTemplate)
        assertTrue(editingTemplate!!.id != userTemplate.id)
        assertTrue(vm.uiState.value.validationError == null)
    }

    @Test
    fun `duplicateAndEdit emits navigateToEditor for rapid requests`() = runTest {
        vm.navigateToEditor.test {
            repeat(3) {
                vm.duplicateAndEdit(userTemplate)
            }
            advanceUntilIdle()

            repeat(3) {
                awaitItem()
            }
        }
    }

    @Test
    fun `startEditing sets editingTemplate in state`() {
        vm.startEditing(userTemplate)
        assertEquals(userTemplate, vm.uiState.value.editingTemplate)
    }

    @Test
    fun `startEditing emits navigateToEditor when template is non-null`() = runTest {
        vm.navigateToEditor.test {
            vm.startEditing(userTemplate)
            awaitItem()
        }
    }

    @Test
    fun `startEditing does not emit navigateToEditor when template is null`() = runTest {
        vm.navigateToEditor.test {
            vm.startEditing(null)
            expectNoEvents()
        }
    }

    @Test
    fun `cancelEditing clears editingTemplate`() {
        vm.startEditing(userTemplate)
        assertNotNull(vm.uiState.value.editingTemplate)

        vm.cancelEditing()
        assertNull(vm.uiState.value.editingTemplate)
    }

    @Test
    fun `showDeleteConfirmation sets template in state`() {
        vm.showDeleteConfirmation(userTemplate)
        assertEquals(userTemplate, vm.uiState.value.showDeleteConfirmation)
    }

    @Test
    fun `dismissDeleteConfirmation clears confirmation`() {
        vm.showDeleteConfirmation(userTemplate)
        assertNotNull(vm.uiState.value.showDeleteConfirmation)

        vm.dismissDeleteConfirmation()
        assertNull(vm.uiState.value.showDeleteConfirmation)
    }

    @Test
    fun `getDefaultTemplateContent returns store default`() {
        assertEquals(defaultTemplate.content, vm.getDefaultTemplateContent())
    }
    
    @Test
    fun `createTemplate rejects content without required placeholders`() = runTest {
        vm.events.test {
            vm.createTemplate("Invalid Template", "Content without placeholders", emptyList())
            advanceUntilIdle()

            // Should emit an error event instead of creating
            val event = awaitItem()
            assertTrue(event is TemplateListEvent.Error)
            assertTrue((event as TemplateListEvent.Error).message.contains("Required placeholders missing"))
            
            // UI state should have validation error
            assertTrue(vm.uiState.value.validationError?.contains("Required placeholders missing") == true)
            
            // Store should NOT have been called to save
            coVerify(exactly = 0) { mockStore.saveTemplate(any()) }
        }
    }

    @Test
    fun `createTemplate normalizes and deduplicates tags`() = runTest {
        vm.events.test {
            vm.createTemplate(
                "Tagged Template",
                "Hello {{ customer_name }}, SSID: {{ ssid }}",
                listOf(" Install ", "install", "Repair")
            )
            advanceUntilIdle()

            coVerify {
                mockStore.saveTemplate(
                    match { saved ->
                        saved.tags == listOf("Install", "Repair")
                    }
                )
            }
            awaitItem()
        }
    }
    
    @Test
    fun `updateTemplate rejects content without required placeholders`() = runTest {
        coEvery { mockStore.getTemplate("650e8400-e29b-41d4-a716-446655440001") } returns userTemplate

        vm.events.test {
            vm.updateTemplate(
                "650e8400-e29b-41d4-a716-446655440001",
                "Updated Name",
                "Missing placeholders",
                emptyList()
            )
            advanceUntilIdle()

            // Should emit an error event instead of updating
            val event = awaitItem()
            assertTrue(event is TemplateListEvent.Error)
            assertTrue((event as TemplateListEvent.Error).message.contains("Required placeholders missing"))
            
            // Store should NOT have been called to save
            coVerify(exactly = 0) { mockStore.saveTemplate(any()) }
        }
    }

    @Test
    fun `updateTagFilter toggles selected tag`() {
        vm.updateTagFilter("Install")
        assertEquals(setOf("Install"), vm.uiState.value.selectedTags)

        vm.updateTagFilter("Install")
        assertTrue(vm.uiState.value.selectedTags.isEmpty())
    }
}
