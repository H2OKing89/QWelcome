package com.kingpaging.qwelcome.viewmodel.templates

import app.cash.turbine.test
import com.kingpaging.qwelcome.R
import com.kingpaging.qwelcome.data.DEFAULT_TEMPLATE_ID
import com.kingpaging.qwelcome.data.SettingsStore
import com.kingpaging.qwelcome.data.Template
import com.kingpaging.qwelcome.data.TemplateSelectionChange
import com.kingpaging.qwelcome.data.TemplateSelectionResult
import com.kingpaging.qwelcome.testutil.FakeResourceProvider
import com.kingpaging.qwelcome.testutil.MainDispatcherRule
import com.kingpaging.qwelcome.viewmodel.factory.AppViewModelProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
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
    private val resourceProvider = FakeResourceProvider()
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
        every { mockStore.templateLastUsedFlow } returns flowOf(emptyMap())
        every { mockStore.defaultTemplateContent } returns defaultTemplate.content
        coEvery { mockStore.setActiveTemplate(any()) } answers {
            val templateId = firstArg<String>()
            val template = listOf(defaultTemplate, userTemplate).firstOrNull { it.id == templateId }
                ?: return@answers TemplateSelectionResult.NotFound(templateId)
            TemplateSelectionResult.Selected(
                template = template,
                change = TemplateSelectionChange(
                    selectedTemplateId = templateId,
                    selectedLastUsedAt = 100L,
                    previousActiveTemplateId = DEFAULT_TEMPLATE_ID,
                    previousSelectedLastUsedAt = null
                )
            )
        }
        vm = TemplateListViewModel(mockStore, resourceProvider)
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
    fun `init propagates non-empty template recency`() = runTest {
        val recency = mapOf(userTemplate.id to 123L)
        every { mockStore.templateLastUsedFlow } returns flowOf(recency)
        vm = TemplateListViewModel(mockStore, resourceProvider)

        advanceUntilIdle()

        assertEquals(recency, vm.uiState.value.templateLastUsedAt)
    }

    @Test
    fun `updateName keeps 50 characters and truncates 51`() {
        val maxLengthName = "a".repeat(MAX_TEMPLATE_NAME_LENGTH)

        vm.updateName(maxLengthName)
        assertEquals(maxLengthName, vm.templateEditorUiState.value.name)

        vm.updateName(maxLengthName + "b")
        assertEquals(maxLengthName, vm.templateEditorUiState.value.name)
    }

    @Test
    fun `create update and rename cap persisted names at shared limit`() = runTest {
        val cappedName = "a".repeat(MAX_TEMPLATE_NAME_LENGTH)
        val overlongName = cappedName + "b"
        coEvery { mockStore.getTemplate(userTemplate.id) } returns userTemplate

        vm.createTemplate(overlongName, defaultTemplate.content, emptyList())
        vm.updateTemplate(userTemplate.id, overlongName, userTemplate.content, emptyList())
        vm.renameTemplate(userTemplate.id, overlongName)
        advanceUntilIdle()

        coVerify(exactly = 3) { mockStore.saveTemplate(match { it.name == cappedName }) }
    }

    @Test
    fun `setActiveTemplate calls store and emits event`() = runTest {
        vm.eventsFor(TemplateListEventOwner.LIBRARY).test {
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
        vm.eventsFor(TemplateListEventOwner.EDITOR).test {
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
        vm.eventsFor(TemplateListEventOwner.EDITOR).test {
            vm.createTemplate("  Spaced Name  ", "Hello {{ customer_name }}, your SSID is {{ ssid }}", emptyList())
            advanceUntilIdle()

            coVerify { mockStore.saveTemplate(match { it.name == "Spaced Name" }) }

            awaitItem() // TemplateCreated event
        }
    }

    @Test
    fun `updateTemplate saves and emits TemplateUpdated event`() = runTest {
        coEvery { mockStore.getTemplate("650e8400-e29b-41d4-a716-446655440001") } returns userTemplate

        vm.eventsFor(TemplateListEventOwner.EDITOR).test {
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

        vm.eventsFor(TemplateListEventOwner.LIBRARY).test {
            vm.deleteTemplate("650e8400-e29b-41d4-a716-446655440001")
            advanceUntilIdle()

            coVerify { mockStore.deleteTemplate("650e8400-e29b-41d4-a716-446655440001") }

            val event = awaitItem()
            assertTrue(event is TemplateListEvent.TemplateDeleted)
            assertEquals("Custom Template", (event as TemplateListEvent.TemplateDeleted).name)
        }
    }

    @Test
    fun `delete active template delegates atomic fallback to store`() = runTest {
        // Set up so 650e8400-e29b-41d4-a716-446655440001 is active
        every { mockStore.activeTemplateIdFlow } returns flowOf("650e8400-e29b-41d4-a716-446655440001")
        vm = TemplateListViewModel(mockStore, resourceProvider)
        advanceUntilIdle()

        coEvery { mockStore.getTemplate("650e8400-e29b-41d4-a716-446655440001") } returns userTemplate

        vm.deleteTemplate("650e8400-e29b-41d4-a716-446655440001")
        advanceUntilIdle()

        coVerify(exactly = 0) { mockStore.setActiveTemplate(DEFAULT_TEMPLATE_ID) }
        coVerify { mockStore.deleteTemplate("650e8400-e29b-41d4-a716-446655440001") }
    }

    @Test
    fun `duplicateTemplate saves copy and emits TemplateDuplicated event`() = runTest {
        vm.eventsFor(TemplateListEventOwner.LIBRARY).test {
            vm.duplicateTemplate(userTemplate)
            advanceUntilIdle()

            coVerify { mockStore.saveTemplate(match { it.id != userTemplate.id }) }

            val event = awaitItem()
            assertTrue(event is TemplateListEvent.TemplateDuplicated)
        }
    }

    @Test
    fun `duplicateAndEdit saves copy emits event and sets editingTemplate`() = runTest {
        vm.eventsFor(TemplateListEventOwner.LIBRARY).test {
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
        val eventsCollector = backgroundScope.launch {
            vm.eventsFor(TemplateListEventOwner.LIBRARY)
                .collect { /* drain duplicate events so emit never blocks */ }
        }

        try {
            vm.navigateToEditor.test {
                repeat(3) {
                    vm.duplicateAndEdit(userTemplate)
                }
                advanceUntilIdle()

                repeat(3) {
                    awaitItem()
                }
            }
        } finally {
            eventsCollector.cancel()
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
        val content = "Content without placeholders"
        val expectedMessage = resourceProvider.getString(
            R.string.error_template_required_placeholders_missing,
            Template.findMissingPlaceholders(content).joinToString(", ")
        )

        vm.eventsFor(TemplateListEventOwner.EDITOR).test {
            vm.createTemplate("Invalid Template", content, emptyList())
            advanceUntilIdle()

            // Should emit an error event instead of creating
            val event = awaitItem()
            assertTrue(event is TemplateListEvent.Error)
            assertEquals(expectedMessage, (event as TemplateListEvent.Error).message)
            
            // UI state should have validation error
            assertEquals(expectedMessage, vm.uiState.value.validationError)
            
            // Store should NOT have been called to save
            coVerify(exactly = 0) { mockStore.saveTemplate(any()) }
        }
    }

    @Test
    fun `createTemplate normalizes and deduplicates tags`() = runTest {
        vm.eventsFor(TemplateListEventOwner.EDITOR).test {
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
        val content = "Missing placeholders"
        val expectedMessage = resourceProvider.getString(
            R.string.error_template_required_placeholders_missing,
            Template.findMissingPlaceholders(content).joinToString(", ")
        )

        vm.eventsFor(TemplateListEventOwner.EDITOR).test {
            vm.updateTemplate(
                "650e8400-e29b-41d4-a716-446655440001",
                "Updated Name",
                content,
                emptyList()
            )
            advanceUntilIdle()

            // Should emit an error event instead of updating
            val event = awaitItem()
            assertTrue(event is TemplateListEvent.Error)
            assertEquals(expectedMessage, (event as TemplateListEvent.Error).message)
            
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

    @Test
    fun `filterAndOrderTemplates moves matching active template first and preserves remaining order`() {
        val templates = listOf(
            defaultTemplate,
            userTemplate,
            Template(id = "third", name = "Third", content = "Third message")
        )

        val result = filterAndOrderTemplates(
            templates = templates,
            activeTemplateId = userTemplate.id,
            searchQuery = "",
            selectedTags = emptySet()
        )

        assertEquals(listOf(userTemplate, defaultTemplate, templates[2]), result)
    }

    @Test
    fun `filterAndOrderTemplates does not reinsert active template excluded by search`() {
        val result = filterAndOrderTemplates(
            templates = listOf(defaultTemplate, userTemplate),
            activeTemplateId = userTemplate.id,
            searchQuery = "Default",
            selectedTags = emptySet()
        )

        assertEquals(listOf(defaultTemplate), result)
    }

    @Test
    fun `filterAndOrderTemplates searches name and content but not tags ignoring case`() {
        val taggedTemplate = userTemplate.copy(tags = listOf("Residential"))
        val templates = listOf(defaultTemplate, taggedTemplate)

        assertEquals(
            listOf(taggedTemplate),
            filterAndOrderTemplates(templates, DEFAULT_TEMPLATE_ID, "CUSTOM TEMPLATE", emptySet())
        )
        assertEquals(
            listOf(taggedTemplate),
            filterAndOrderTemplates(templates, DEFAULT_TEMPLATE_ID, "your ssid", emptySet())
        )
        assertEquals(
            emptyList<Template>(),
            filterAndOrderTemplates(templates, DEFAULT_TEMPLATE_ID, "residential", emptySet())
        )
    }

    @Test
    fun `filterAndOrderTemplates orders active then recent then stable`() {
        val neverUsed = userTemplate.copy(id = "never-used", name = "Never used")
        val older = userTemplate.copy(id = "older", name = "Older")
        val newer = userTemplate.copy(id = "newer", name = "Newer")

        val result = filterAndOrderTemplates(
            templates = listOf(neverUsed, older, defaultTemplate, newer),
            activeTemplateId = defaultTemplate.id,
            searchQuery = "",
            selectedTags = emptySet(),
            templateLastUsedAt = mapOf(older.id to 100L, newer.id to 200L)
        )

        assertEquals(listOf(defaultTemplate, newer, older, neverUsed), result)
    }

    @Test
    fun `setActiveTemplate emits blocked event for invalid template`() = runTest {
        val invalidTemplate = userTemplate.copy(content = "Missing variables")
        coEvery { mockStore.setActiveTemplate(invalidTemplate.id) } returns
            TemplateSelectionResult.Blocked(
                invalidTemplate,
                listOf("{{ customer_name }}", "{{ ssid }}")
            )

        vm.eventsFor(TemplateListEventOwner.LIBRARY).test {
            vm.setActiveTemplate(invalidTemplate.id)
            advanceUntilIdle()

            val event = awaitItem()
            assertTrue(event is TemplateListEvent.TemplateSelectionBlocked)
            assertEquals(invalidTemplate, (event as TemplateListEvent.TemplateSelectionBlocked).template)
        }
    }

    @Test
    fun `undoTemplateSelection delegates token without emitting another selection event`() = runTest {
        val change = TemplateSelectionChange(
            selectedTemplateId = userTemplate.id,
            selectedLastUsedAt = 200L,
            previousActiveTemplateId = DEFAULT_TEMPLATE_ID,
            previousSelectedLastUsedAt = 50L
        )
        coEvery { mockStore.undoTemplateSelection(change) } returns true

        vm.eventsFor(TemplateListEventOwner.LIBRARY).test {
            vm.undoTemplateSelection(change)
            advanceUntilIdle()

            coVerify { mockStore.undoTemplateSelection(change) }
            expectNoEvents()
        }
    }

    @Test
    fun `renameTemplate preserves content and tags and emits renamed event`() = runTest {
        coEvery { mockStore.getTemplate(userTemplate.id) } returns userTemplate.copy(tags = listOf("Install"))

        vm.eventsFor(TemplateListEventOwner.LIBRARY).test {
            vm.renameTemplate(userTemplate.id, "  Priority Install  ")
            advanceUntilIdle()

            coVerify {
                mockStore.saveTemplate(
                    match {
                        it.name == "Priority Install" &&
                            it.content == userTemplate.content &&
                            it.tags == listOf("Install")
                    }
                )
            }
            val event = awaitItem()
            assertTrue(event is TemplateListEvent.TemplateRenamed)
        }
    }

    @Test
    fun `editor events are isolated from library collectors`() = runTest {
        vm.eventsFor(TemplateListEventOwner.LIBRARY).test {
            vm.eventsFor(TemplateListEventOwner.EDITOR).test {
                vm.createTemplate("New Template", defaultTemplate.content, emptyList())
                advanceUntilIdle()

                assertTrue(awaitItem() is TemplateListEvent.TemplateCreated)
            }

            expectNoEvents()
        }
    }

    @Test
    fun `filterAndOrderTemplates uses OR semantics for multiple case-insensitive tags`() {
        val install = userTemplate.copy(id = "install", tags = listOf("Install"))
        val repair = userTemplate.copy(id = "repair", tags = listOf("REPAIR"))
        val unrelated = userTemplate.copy(id = "business", tags = listOf("Business"))

        val result = filterAndOrderTemplates(
            templates = listOf(defaultTemplate, install, repair, unrelated),
            activeTemplateId = DEFAULT_TEMPLATE_ID,
            searchQuery = "",
            selectedTags = setOf("install", "Repair")
        )

        assertEquals(listOf(install, repair), result)
    }

    @Test
    fun `filterAndOrderTemplates requires both query and tag filters to match`() {
        val englishInstall = userTemplate.copy(
            id = "english-install",
            name = "English Install",
            tags = listOf("Install")
        )
        val spanishInstall = userTemplate.copy(
            id = "spanish-install",
            name = "Spanish Install",
            tags = listOf("Install")
        )
        val englishRepair = userTemplate.copy(
            id = "english-repair",
            name = "English Repair",
            tags = listOf("Repair")
        )

        val result = filterAndOrderTemplates(
            templates = listOf(englishInstall, spanishInstall, englishRepair),
            activeTemplateId = englishRepair.id,
            searchQuery = "English",
            selectedTags = setOf("Install")
        )

        assertEquals(listOf(englishInstall), result)
    }
}
