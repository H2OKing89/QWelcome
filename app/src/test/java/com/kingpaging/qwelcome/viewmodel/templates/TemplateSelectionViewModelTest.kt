package com.kingpaging.qwelcome.viewmodel.templates

import app.cash.turbine.test
import com.kingpaging.qwelcome.data.DEFAULT_TEMPLATE_ID
import com.kingpaging.qwelcome.data.SettingsStore
import com.kingpaging.qwelcome.data.Template
import com.kingpaging.qwelcome.data.TemplateSelectionResult
import com.kingpaging.qwelcome.testutil.FakeResourceProvider
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TemplateSelectionViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val settingsStore = mockk<SettingsStore>(relaxed = true)
    private val defaultTemplate =
        Template(
            id = DEFAULT_TEMPLATE_ID,
            name = "Default Welcome",
            content = "Hello {{ customer_name }}, SSID: {{ ssid }}",
        )
    private val invalidTemplate =
        Template(
            id = "650e8400-e29b-41d4-a716-446655440002",
            name = "Invalid",
            content = "Hello",
        )
    private lateinit var viewModel: TemplateSelectionViewModel

    @Before
    fun setUp() {
        every { settingsStore.allTemplatesFlow } returns flowOf(listOf(defaultTemplate, invalidTemplate))
        every { settingsStore.activeTemplateIdFlow } returns flowOf(DEFAULT_TEMPLATE_ID)
        viewModel = TemplateSelectionViewModel(settingsStore, FakeResourceProvider())
    }

    @Test
    fun `state exposes templates and active selection`() =
        runTest {
            advanceUntilIdle()

            assertEquals(listOf(defaultTemplate, invalidTemplate), viewModel.uiState.value.templates)
            assertEquals(DEFAULT_TEMPLATE_ID, viewModel.uiState.value.activeTemplateId)
            assertFalse(viewModel.uiState.value.isLoading)
        }

    @Test
    fun `blocked selection emits focused feedback`() =
        runTest {
            coEvery { settingsStore.setActiveTemplate(invalidTemplate.id) } returns
                TemplateSelectionResult.Blocked(
                    template = invalidTemplate,
                    missingPlaceholders = listOf("{{ customer_name }}", "{{ ssid }}"),
                )

            viewModel.events.test {
                viewModel.selectTemplate(invalidTemplate.id)
                advanceUntilIdle()

                val event = awaitItem()
                assertTrue(event is TemplateSelectionEvent.SelectionBlocked)
                assertEquals(invalidTemplate, (event as TemplateSelectionEvent.SelectionBlocked).template)
            }
        }

    @Test
    fun `selection delegates to store`() =
        runTest {
            coEvery { settingsStore.setActiveTemplate(defaultTemplate.id) } returns
                TemplateSelectionResult.AlreadyActive(defaultTemplate)

            viewModel.selectTemplate(defaultTemplate.id)
            advanceUntilIdle()

            coVerify(exactly = 1) { settingsStore.setActiveTemplate(defaultTemplate.id) }
        }
}
