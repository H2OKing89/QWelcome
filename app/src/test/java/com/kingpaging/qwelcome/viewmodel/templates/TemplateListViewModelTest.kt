package com.kingpaging.qwelcome.viewmodel.templates

import app.cash.turbine.test
import com.kingpaging.qwelcome.data.DEFAULT_TEMPLATE_ID
import com.kingpaging.qwelcome.data.SettingsStore
import com.kingpaging.qwelcome.data.Template
import com.kingpaging.qwelcome.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
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
        content = "Hello {{ customer_name }}"
    )
    private val userTemplate = Template(
        id = "user-1",
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
        coEvery { mockStore.getTemplate("user-1") } returns userTemplate

        vm.events.test {
            vm.setActiveTemplate("user-1")
            advanceUntilIdle()

            coVerify { mockStore.setActiveTemplate("user-1") }

            val event = awaitItem()
            assertTrue(event is TemplateListEvent.ActiveTemplateChanged)
            assertEquals("Custom Template", (event as TemplateListEvent.ActiveTemplateChanged).template.name)
        }
    }

    @Test
    fun `createTemplate saves and emits TemplateCreated event`() = runTest {
        vm.events.test {
            vm.createTemplate("New Template", "New content")
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
            vm.createTemplate("  Spaced Name  ", "Content")
            advanceUntilIdle()

            coVerify { mockStore.saveTemplate(match { it.name == "Spaced Name" }) }

            awaitItem() // TemplateCreated event
        }
    }

    @Test
    fun `updateTemplate saves and emits TemplateUpdated event`() = runTest {
        coEvery { mockStore.getTemplate("user-1") } returns userTemplate

        vm.events.test {
            vm.updateTemplate("user-1", "Updated Name", "Updated content")
            advanceUntilIdle()

            coVerify { mockStore.saveTemplate(match { it.name == "Updated Name" && it.content == "Updated content" }) }

            val event = awaitItem()
            assertTrue(event is TemplateListEvent.TemplateUpdated)
            assertEquals("Updated Name", (event as TemplateListEvent.TemplateUpdated).template.name)
        }
    }

    @Test
    fun `deleteTemplate removes and emits TemplateDeleted event`() = runTest {
        coEvery { mockStore.getTemplate("user-1") } returns userTemplate

        vm.events.test {
            vm.deleteTemplate("user-1")
            advanceUntilIdle()

            coVerify { mockStore.deleteTemplate("user-1") }

            val event = awaitItem()
            assertTrue(event is TemplateListEvent.TemplateDeleted)
            assertEquals("Custom Template", (event as TemplateListEvent.TemplateDeleted).name)
        }
    }

    @Test
    fun `delete active template switches to default first`() = runTest {
        // Set up so user-1 is active
        every { mockStore.activeTemplateIdFlow } returns flowOf("user-1")
        vm = TemplateListViewModel(mockStore)
        advanceUntilIdle()

        coEvery { mockStore.getTemplate("user-1") } returns userTemplate

        vm.deleteTemplate("user-1")
        advanceUntilIdle()

        // Should switch to default before deleting
        coVerify { mockStore.setActiveTemplate(DEFAULT_TEMPLATE_ID) }
        coVerify { mockStore.deleteTemplate("user-1") }
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
    fun `startEditing sets editingTemplate in state`() {
        vm.startEditing(userTemplate)
        assertEquals(userTemplate, vm.uiState.value.editingTemplate)
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
}
