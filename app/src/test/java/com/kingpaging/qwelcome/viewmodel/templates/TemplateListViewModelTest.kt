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
    fun `renameTemplate caps persisted names at shared limit`() = runTest {
        val cappedName = "a".repeat(MAX_TEMPLATE_NAME_LENGTH)
        val overlongName = cappedName + "b"
        coEvery { mockStore.getTemplate(userTemplate.id) } returns userTemplate

        vm.renameTemplate(userTemplate.id, overlongName)
        advanceUntilIdle()

        coVerify(exactly = 1) { mockStore.saveTemplate(match { it.name == cappedName }) }
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
    fun `duplicateAndEdit emits duplicate event flagged to open editor`() = runTest {
        vm.eventsFor(TemplateListEventOwner.LIBRARY).test {
            vm.duplicateAndEdit(userTemplate)
            advanceUntilIdle()

            coVerify { mockStore.saveTemplate(match { it.id != userTemplate.id }) }
            val event = awaitItem() as TemplateListEvent.TemplateDuplicated
            assertTrue(event.openEditor)
            assertTrue(event.template.id != userTemplate.id)
        }
    }

    @Test
    fun `duplicateAndEdit emits editor events for rapid requests`() = runTest {
        vm.eventsFor(TemplateListEventOwner.LIBRARY).test {
            repeat(3) {
                vm.duplicateAndEdit(userTemplate)
            }
            advanceUntilIdle()

            repeat(3) {
                assertTrue((awaitItem() as TemplateListEvent.TemplateDuplicated).openEditor)
            }
        }
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
