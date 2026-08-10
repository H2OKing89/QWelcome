package com.kingpaging.qwelcome.ui.templates

import android.content.Context
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kingpaging.qwelcome.R
import com.kingpaging.qwelcome.data.DEFAULT_TEMPLATE_ID
import com.kingpaging.qwelcome.data.NEW_TEMPLATE_ID
import com.kingpaging.qwelcome.data.SettingsStore
import com.kingpaging.qwelcome.data.Template
import com.kingpaging.qwelcome.data.UserPreferences
import com.kingpaging.qwelcome.data.protoDataStore
import com.kingpaging.qwelcome.di.LocalSoundPlayer
import com.kingpaging.qwelcome.di.LocalTemplateListViewModel
import com.kingpaging.qwelcome.testutil.FakeSoundPlayer
import com.kingpaging.qwelcome.ui.theme.CyberpunkTheme
import com.kingpaging.qwelcome.util.AndroidResourceProvider
import com.kingpaging.qwelcome.viewmodel.factory.AppViewModelProvider
import com.kingpaging.qwelcome.viewmodel.templates.TemplateListViewModel
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TemplateListScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val editorOpened = AtomicBoolean(false)
    private lateinit var appContext: Context
    private lateinit var viewModel: TemplateListViewModel
    private lateinit var soundPlayer: FakeSoundPlayer

    private val englishTemplate = Template(
        id = "english-template",
        name = "English Install",
        content = "Welcome {{ customer_name }} to your new network {{ ssid }}.",
        tags = listOf("English", "Install")
    )
    private val spanishTemplate = Template(
        id = "spanish-template",
        name = "Spanish Repair",
        content = "Hola {{ customer_name }}, repararemos {{ ssid }}.",
        tags = listOf("Spanish", "Repair")
    )

    @Before
    fun setup() {
        AppViewModelProvider.resetForTesting()
        editorOpened.set(false)
        appContext = ApplicationProvider.getApplicationContext()
        soundPlayer = FakeSoundPlayer()
    }

    @After
    fun tearDown() = runBlocking {
        AppViewModelProvider.resetForTesting()
        if (::appContext.isInitialized) {
            appContext.protoDataStore.updateData { UserPreferences.getDefaultInstance() }
        }
    }

    @Test
    fun newTemplate_isTopBarActionAndOpensEditor() {
        setScreenContent()

        composeRule.onNodeWithContentDescription(appContext.getString(R.string.action_new_template))
            .assertIsDisplayed()
            .performClick()

        composeRule.waitUntil { editorOpened.get() }
        assertEquals(NEW_TEMPLATE_ID, viewModel.uiState.value.editingTemplate?.id)
        composeRule.onNodeWithText(appContext.getString(R.string.action_new_template))
            .assertDoesNotExist()
    }

    @Test
    fun activeTemplate_isPinnedFirstAndLabeled() {
        setScreenContent(activeTemplateId = englishTemplate.id)

        val activeTop = composeRule.onNodeWithText(englishTemplate.name)
            .fetchSemanticsNode().boundsInRoot.top
        val defaultTop = composeRule.onNodeWithText("Default")
            .fetchSemanticsNode().boundsInRoot.top

        assertTrue(activeTop < defaultTop)
        composeRule.onNodeWithContentDescription(appContext.getString(R.string.content_desc_active_template))
            .assertIsDisplayed()
    }

    @Test
    fun wholeCardTap_activatesInactiveTemplate() {
        setScreenContent(activeTemplateId = englishTemplate.id)

        composeRule.onNodeWithContentDescription(
            appContext.getString(R.string.content_desc_use_named_template, "Default")
        ).performClick()

        composeRule.waitUntil { viewModel.uiState.value.activeTemplateId == DEFAULT_TEMPLATE_ID }
        composeRule.onNodeWithContentDescription(
            appContext.getString(R.string.content_desc_use_named_template, "Default")
        ).assertIsSelected()
    }

    @Test
    fun editOverflowAction_opensTheEditor() {
        setScreenContent()

        composeRule.onNodeWithContentDescription(
            appContext.getString(R.string.content_desc_template_actions_named, englishTemplate.name)
        ).performClick()
        composeRule.onNodeWithText(appContext.getString(R.string.action_edit_template)).performClick()
        composeRule.waitUntil { editorOpened.get() }

        assertEquals(englishTemplate.id, viewModel.uiState.value.editingTemplate?.id)
    }

    @Test
    fun userTemplateOverflow_exposesAllSecondaryActions() {
        setScreenContent(activeTemplateId = englishTemplate.id)

        composeRule.onNodeWithContentDescription(
            appContext.getString(R.string.content_desc_template_actions_named, englishTemplate.name)
        ).performClick()

        composeRule.onNodeWithText(appContext.getString(R.string.action_edit_template))
            .assertIsDisplayed()
        composeRule.onNodeWithText(appContext.getString(R.string.action_rename_template))
            .assertIsDisplayed()
        composeRule.onNodeWithText(appContext.getString(R.string.action_preview_template))
            .assertIsDisplayed()
        composeRule.onNodeWithText(appContext.getString(R.string.action_duplicate_template))
            .assertIsDisplayed()
        composeRule.onNodeWithText(appContext.getString(R.string.action_delete_template))
            .assertIsDisplayed()
    }

    @Test
    fun defaultOverflow_onlyExposesCustomizeAndPreview() {
        setScreenContent()

        composeRule.onNodeWithContentDescription(
            appContext.getString(R.string.content_desc_template_actions_named, "Default")
        ).performClick()

        composeRule.onNodeWithText(appContext.getString(R.string.action_customize_copy))
            .assertIsDisplayed()
        composeRule.onNodeWithText(appContext.getString(R.string.action_preview_template))
            .assertIsDisplayed()
        composeRule.onNodeWithText(appContext.getString(R.string.action_rename_template))
            .assertDoesNotExist()
        composeRule.onNodeWithText(appContext.getString(R.string.action_delete_template))
            .assertDoesNotExist()
    }

    @Test
    fun searchAndClear_restoreTemplateResults() {
        setScreenContent()
        val query = "No matching template"
        val searchField = composeRule.onNode(
            hasSetTextAction() and hasContentDescription(
                appContext.getString(R.string.content_desc_search_templates)
            )
        )

        searchField.performTextInput(query)
        composeRule.onNodeWithText(appContext.getString(R.string.text_no_templates_match, query))
            .assertIsDisplayed()

        composeRule.onNodeWithContentDescription(appContext.getString(R.string.content_desc_clear_search))
            .performClick()

        composeRule.onNodeWithText(englishTemplate.name).assertIsDisplayed()
        composeRule.onNodeWithText(appContext.getString(R.string.text_no_templates_match, query))
            .assertDoesNotExist()
    }

    @Test
    fun searchImeAction_clearsFocus() {
        setScreenContent()
        val searchField = composeRule.onNode(
            hasSetTextAction() and hasContentDescription(
                appContext.getString(R.string.content_desc_search_templates)
            )
        )

        searchField.performClick().assertIsFocused()
        searchField.performTextInput("English")
        searchField.performImeAction()

        searchField.assertIsNotFocused()
    }

    @Test
    fun tagSheet_appliesMultipleFiltersWithOrSemanticsAndClears() {
        setScreenContent()

        composeRule.onNodeWithText(appContext.getString(R.string.label_tags)).performClick()
        composeRule.onNodeWithText("Install").performClick()
        composeRule.onNodeWithText("Repair").performClick()
        composeRule.onNodeWithContentDescription(appContext.getString(R.string.action_close))
            .performClick()

        composeRule.onNodeWithText(appContext.getString(R.string.label_tags_selected, 2))
            .assertIsDisplayed()
        composeRule.onNodeWithText(englishTemplate.name).assertIsDisplayed()
        composeRule.onNodeWithText(spanishTemplate.name).assertIsDisplayed()

        composeRule.onNodeWithText(appContext.getString(R.string.label_tags_selected, 2))
            .performClick()
        composeRule.onNodeWithText(appContext.getString(R.string.label_all_tags)).performClick()
        composeRule.onNodeWithContentDescription(appContext.getString(R.string.action_close))
            .performClick()
        assertTrue(viewModel.uiState.value.selectedTags.isEmpty())
    }

    @Test
    fun overflowTap_doesNotActivateInactiveTemplateOrRecordUse() {
        setScreenContent()

        composeRule.onNodeWithContentDescription(
            appContext.getString(R.string.content_desc_template_actions_named, englishTemplate.name)
        ).performClick()

        assertEquals(DEFAULT_TEMPLATE_ID, viewModel.uiState.value.activeTemplateId)
        assertTrue(viewModel.uiState.value.templateLastUsedAt.isEmpty())
    }

    @Test
    fun activeCardTap_isNoOp() {
        setScreenContent(activeTemplateId = englishTemplate.id)
        val usageBefore = viewModel.uiState.value.templateLastUsedAt

        composeRule.onNodeWithContentDescription(
            appContext.getString(R.string.content_desc_use_named_template, englishTemplate.name)
        ).performClick()
        composeRule.waitForIdle()

        assertEquals(usageBefore, viewModel.uiState.value.templateLastUsedAt)
    }

    @Test
    fun longPressCard_opensFullPreview() {
        setScreenContent()

        composeRule.onNodeWithContentDescription(
            appContext.getString(R.string.content_desc_use_named_template, englishTemplate.name)
        ).performTouchInput { longClick() }

        composeRule.onNodeWithText(appContext.getString(R.string.action_close)).assertIsDisplayed()
    }

    @Test
    fun renameDialog_validatesAndRenamesWithoutChangingContent() {
        setScreenContent()

        composeRule.onNodeWithContentDescription(
            appContext.getString(R.string.content_desc_template_actions_named, englishTemplate.name)
        ).performClick()
        composeRule.onNodeWithText(appContext.getString(R.string.action_rename_template)).performClick()

        val nameDescription = appContext.getString(R.string.content_desc_rename_template_name)
        composeRule.onAllNodesWithContentDescription(nameDescription).assertCountEquals(1)
        val nameField = composeRule.onNodeWithContentDescription(nameDescription)
        nameField.performTextClearance()
        composeRule.onNodeWithText(appContext.getString(R.string.action_rename_template))
            .assertIsNotEnabled()
        nameField.performTextInput("Priority Install")
        composeRule.onNodeWithText(appContext.getString(R.string.action_rename_template))
            .performClick()

        composeRule.waitUntil {
            viewModel.uiState.value.templates.any {
                it.id == englishTemplate.id && it.name == "Priority Install" &&
                    it.content == englishTemplate.content
            }
        }
    }

    @Test
    fun selectionSnackbar_undoRestoresPreviousTemplate() {
        setScreenContent(activeTemplateId = englishTemplate.id)

        composeRule.onNodeWithContentDescription(
            appContext.getString(R.string.content_desc_use_named_template, "Default")
        ).performClick()
        val undoNodes = composeRule.onAllNodesWithText(appContext.getString(R.string.action_undo))
        composeRule.waitUntil { undoNodes.fetchSemanticsNodes().size == 1 }
        undoNodes.assertCountEquals(1).onFirst().performClick()

        composeRule.waitUntil { viewModel.uiState.value.activeTemplateId == englishTemplate.id }
    }

    @Test
    fun editorError_duringTransition_isNotConsumedByLibrary() {
        setScreenContent()
        val invalidContent = "Missing required variables"
        val expectedMessage = appContext.getString(
            R.string.error_template_required_placeholders_missing,
            Template.findMissingPlaceholders(invalidContent).joinToString(", ")
        )

        composeRule.runOnIdle {
            viewModel.createTemplate("Invalid", invalidContent, emptyList())
        }
        composeRule.waitUntil { viewModel.uiState.value.validationError == expectedMessage }
        composeRule.waitForIdle()

        composeRule.onNodeWithText(expectedMessage).assertDoesNotExist()
        assertEquals(0, soundPlayer.beepCount)
    }

    @Test
    fun invalidCard_isBlockedAndFixOpensEditor() {
        val invalidTemplate = Template(
            id = "invalid-template",
            name = "Needs Fix",
            content = "Missing required variables"
        )
        setScreenContent(templates = listOf(englishTemplate, invalidTemplate))

        composeRule.onNodeWithContentDescription(appContext.getString(R.string.content_desc_invalid_template))
            .assertIsDisplayed()
        composeRule.onNodeWithContentDescription(
            appContext.getString(R.string.content_desc_use_named_template, invalidTemplate.name)
        ).performClick()
        composeRule.onNodeWithText(appContext.getString(R.string.action_fix)).performClick()

        composeRule.waitUntil { editorOpened.get() }
        assertEquals(invalidTemplate.id, viewModel.uiState.value.editingTemplate?.id)
        assertEquals(DEFAULT_TEMPLATE_ID, viewModel.uiState.value.activeTemplateId)
        assertTrue(invalidTemplate.id !in viewModel.uiState.value.templateLastUsedAt)
    }

    @Test
    fun cardTags_showCompactOverflowMetadata() {
        val taggedTemplate = englishTemplate.copy(
            tags = listOf("English", "Install", "Residential", "Priority")
        )
        setScreenContent(templates = listOf(taggedTemplate, spanishTemplate))

        composeRule.onNodeWithText("+2", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Residential").assertDoesNotExist()
    }

    @Test
    fun deletingActiveTemplate_explainsDefaultFallback() {
        setScreenContent(activeTemplateId = englishTemplate.id)

        composeRule.onNodeWithContentDescription(
            appContext.getString(R.string.content_desc_template_actions_named, englishTemplate.name)
        ).performClick()
        composeRule.onNodeWithText(appContext.getString(R.string.action_delete_template))
            .performClick()

        composeRule.onNodeWithText(
            appContext.getString(R.string.text_delete_active_template_confirm, englishTemplate.name)
        ).assertIsDisplayed()
    }

    private fun setScreenContent(
        templates: List<Template> = listOf(englishTemplate, spanishTemplate),
        activeTemplateId: String = DEFAULT_TEMPLATE_ID
    ) {
        val store = runBlocking {
            appContext.protoDataStore.updateData { UserPreferences.getDefaultInstance() }
            SettingsStore(appContext).also { settingsStore ->
                settingsStore.saveTemplates(templates)
                settingsStore.setActiveTemplate(activeTemplateId)
            }
        }
        viewModel = TemplateListViewModel(
            store,
            AndroidResourceProvider(appContext)
        )

        composeRule.setContent {
            CyberpunkTheme {
                CompositionLocalProvider(
                    LocalTemplateListViewModel provides viewModel,
                    LocalSoundPlayer provides soundPlayer
                ) {
                    TemplateListScreen(
                        onBack = {},
                        onOpenEditor = { editorOpened.set(true) }
                    )
                }
            }
        }
        composeRule.waitUntil { !viewModel.uiState.value.isLoading }
        composeRule.waitForIdle()
    }
}