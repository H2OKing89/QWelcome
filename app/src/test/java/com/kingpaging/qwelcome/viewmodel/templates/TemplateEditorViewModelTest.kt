package com.kingpaging.qwelcome.viewmodel.templates

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.kingpaging.qwelcome.R
import com.kingpaging.qwelcome.data.NEW_TEMPLATE_ID
import com.kingpaging.qwelcome.data.SettingsStore
import com.kingpaging.qwelcome.data.Template
import com.kingpaging.qwelcome.testutil.FakeResourceProvider
import com.kingpaging.qwelcome.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TemplateEditorViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val settingsStore = mockk<SettingsStore>(relaxed = true)
    private val resourceProvider = FakeResourceProvider()
    private val template = Template(
        id = "template-id",
        name = "Existing",
        content = "Hello {{ customer_name }}, SSID: {{ ssid }}",
        tags = listOf("Install")
    )

    @Before
    fun setup() {
        every { settingsStore.defaultTemplateContent } returns template.content
        coEvery { settingsStore.getTemplate(template.id) } returns template
    }

    @Test
    fun `existing template route loads persisted template`() = runTest {
        val viewModel = createViewModel(template.id)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(template, state.template)
        assertEquals(template.name, state.name)
        assertEquals(template.content, state.contentText)
        assertEquals(template.tags, state.tags)
    }

    @Test
    fun `new template route uses default content`() = runTest {
        val viewModel = createViewModel(NEW_TEMPLATE_ID)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(NEW_TEMPLATE_ID, state.template?.id)
        assertEquals("", state.name)
        assertEquals(template.content, state.contentText)
    }

    @Test
    fun `missing template route finishes loading without template`() = runTest {
        coEvery { settingsStore.getTemplate("missing") } returns null

        val viewModel = createViewModel("missing")
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.template)
    }

    @Test
    fun `saved draft replaces persisted template fields after recreation`() = runTest {
        val savedStateHandle = SavedStateHandle(
            mapOf(
                "templateId" to template.id,
                "editorName" to "Restored name",
                "editorTags" to listOf("Repair"),
                "editorNewTagInput" to "Priority",
                "editorContent" to "Restored {{ customer_name }} on {{ ssid }}",
                "editorShowDiscardDialog" to true
            )
        )

        val viewModel = TemplateEditorViewModel(savedStateHandle, settingsStore, resourceProvider)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Restored name", state.name)
        assertEquals(listOf("Repair"), state.tags)
        assertEquals("Priority", state.newTagInput)
        assertEquals("Restored {{ customer_name }} on {{ ssid }}", state.contentText)
        assertTrue(state.showDiscardDialog)
    }

    @Test
    fun `draft mutations are written to SavedStateHandle`() = runTest {
        val savedStateHandle = SavedStateHandle(mapOf("templateId" to template.id))
        val viewModel = TemplateEditorViewModel(savedStateHandle, settingsStore, resourceProvider)
        advanceUntilIdle()

        viewModel.updateName("Draft")
        viewModel.updateTags(listOf(" Install ", "install", "Repair"))
        viewModel.updateNewTagInput("Priority")
        viewModel.updateContent("Draft {{ customer_name }} {{ ssid }}")
        viewModel.toggleDiscardDialog(true)

        assertEquals("Draft", savedStateHandle.get<String>("editorName"))
        assertEquals(listOf("Install", "Repair"), savedStateHandle.get<List<String>>("editorTags"))
        assertEquals("Priority", savedStateHandle.get<String>("editorNewTagInput"))
        assertEquals("Draft {{ customer_name }} {{ ssid }}", savedStateHandle.get<String>("editorContent"))
        assertEquals(true, savedStateHandle.get<Boolean>("editorShowDiscardDialog"))
    }

    @Test
    fun `create saves sanitized template and emits created event`() = runTest {
        val viewModel = createViewModel(NEW_TEMPLATE_ID)
        advanceUntilIdle()

        viewModel.events.test {
            viewModel.createTemplate(
                "  New template  ",
                template.content,
                listOf(" Install ", "install", "Repair")
            )
            advanceUntilIdle()

            val event = awaitItem() as TemplateEditorEvent.TemplateCreated
            assertEquals("New template", event.template.name)
            assertEquals(listOf("Install", "Repair"), event.template.tags)
            assertTrue(viewModel.uiState.value.isSaveCompleted)
            coVerify { settingsStore.saveTemplate(event.template) }
        }
    }

    @Test
    fun `update saves existing template and emits updated event`() = runTest {
        val viewModel = createViewModel(template.id)
        advanceUntilIdle()

        viewModel.events.test {
            viewModel.updateTemplate(
                "Updated",
                "Hi {{customer_name}}, SSID: {{ssid}}",
                listOf("Repair")
            )
            advanceUntilIdle()

            val event = awaitItem() as TemplateEditorEvent.TemplateUpdated
            assertEquals("Updated", event.template.name)
            assertEquals("Hi {{ customer_name }}, SSID: {{ ssid }}", event.template.content)
            assertTrue(viewModel.uiState.value.isSaveCompleted)
            coVerify { settingsStore.saveTemplate(event.template) }
        }
    }

    @Test
    fun `update emits error when persisted template is missing`() = runTest {
        coEvery { settingsStore.getTemplate("missing-template") } returns null
        val viewModel = createViewModel("missing-template")
        advanceUntilIdle()
        val expectedMessage = resourceProvider.getString(R.string.error_template_update_failed)

        viewModel.events.test {
            viewModel.updateTemplate(
                "Missing",
                template.content,
                emptyList()
            )
            advanceUntilIdle()

            assertEquals(expectedMessage, (awaitItem() as TemplateEditorEvent.Error).message)
            coVerify(exactly = 0) { settingsStore.saveTemplate(any()) }
        }
    }

    @Test
    fun `invalid content emits error without saving`() = runTest {
        val viewModel = createViewModel(NEW_TEMPLATE_ID)
        advanceUntilIdle()
        val expectedMessage = resourceProvider.getString(
            R.string.error_template_required_placeholders_missing,
            "{{ customer_name }}, {{ ssid }}"
        )

        viewModel.events.test {
            viewModel.createTemplate("Invalid", "Missing placeholders", emptyList())
            advanceUntilIdle()

            assertEquals(expectedMessage, (awaitItem() as TemplateEditorEvent.Error).message)
            assertEquals("customer_name, ssid", viewModel.uiState.value.contentError)
            coVerify(exactly = 0) { settingsStore.saveTemplate(any()) }
        }
    }

    private fun createViewModel(templateId: String): TemplateEditorViewModel =
        TemplateEditorViewModel(
            SavedStateHandle(mapOf("templateId" to templateId)),
            settingsStore,
            resourceProvider
        )
}
