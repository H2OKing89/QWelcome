package com.kingpaging.qwelcome.ui.templates

import android.content.Context
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kingpaging.qwelcome.R
import com.kingpaging.qwelcome.data.NEW_TEMPLATE_ID
import com.kingpaging.qwelcome.data.SettingsStore
import com.kingpaging.qwelcome.data.Template
import com.kingpaging.qwelcome.data.UserPreferences
import com.kingpaging.qwelcome.data.protoDataStore
import com.kingpaging.qwelcome.di.LocalNavigator
import com.kingpaging.qwelcome.di.LocalSoundPlayer
import com.kingpaging.qwelcome.di.LocalTemplateListViewModel
import com.kingpaging.qwelcome.testutil.FakeNavigator
import com.kingpaging.qwelcome.testutil.FakeSoundPlayer
import com.kingpaging.qwelcome.ui.theme.CyberpunkTheme
import com.kingpaging.qwelcome.viewmodel.factory.AppViewModelProvider
import com.kingpaging.qwelcome.viewmodel.templates.TemplateListViewModel
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TemplateEditorScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val backInvoked = AtomicBoolean(false)
    private lateinit var appContext: Context
    private lateinit var templateListViewModel: TemplateListViewModel
    private lateinit var navigator: FakeNavigator
    private lateinit var soundPlayer: FakeSoundPlayer

    @Before
    fun setup() {
        AppViewModelProvider.resetForTesting()
        backInvoked.set(false)

        appContext = ApplicationProvider.getApplicationContext()
        val settingsStore = SettingsStore(appContext)
        templateListViewModel = TemplateListViewModel(settingsStore)

        navigator = FakeNavigator()
        soundPlayer = FakeSoundPlayer()
    }

    @After
    fun tearDown() = runBlocking {
        AppViewModelProvider.resetForTesting()
        if (::appContext.isInitialized) {
            appContext.protoDataStore.updateData { UserPreferences.getDefaultInstance() }
        }
    }

    // ── New template mode ────────────────────────────────────────────

    @Test
    fun newTemplate_showsCreateTitleAndButton() {
        setScreenContentNewTemplate()

        composeRule.onNodeWithText(appContext.getString(R.string.title_create_template))
            .assertIsDisplayed()
        composeRule.onNodeWithText(appContext.getString(R.string.action_create))
            .assertIsDisplayed()
    }

    @Test
    fun newTemplate_nameFieldIsEmpty() {
        setScreenContentNewTemplate()

        composeRule.onNodeWithText(appContext.getString(R.string.label_name))
            .assertIsDisplayed()
    }

    @Test
    fun newTemplate_createButtonDisabledWhenNameBlank() {
        setScreenContentNewTemplate()

        composeRule.onNodeWithText(appContext.getString(R.string.action_create))
            .assertIsNotEnabled()
    }

    @Test
    fun newTemplate_createButtonEnabledAfterEnteringName() {
        setScreenContentNewTemplate()

        composeRule.onNode(editableFieldWithLabel(appContext.getString(R.string.label_name)))
            .performTextInput("My Template")

        composeRule.waitForIdle()

        composeRule.onNodeWithText(appContext.getString(R.string.action_create))
            .assertIsEnabled()
    }

    // ── Edit template mode ───────────────────────────────────────────

    @Test
    fun editTemplate_showsEditTitleAndSaveButton() {
        setScreenContentExistingTemplate()

        composeRule.onNodeWithText(appContext.getString(R.string.title_edit_template))
            .assertIsDisplayed()
        composeRule.onNodeWithText(appContext.getString(R.string.action_save))
            .assertIsDisplayed()
    }

    @Test
    fun editTemplate_prefillsName() {
        setScreenContentExistingTemplate(name = "Existing Template")

        composeRule.onNodeWithText("Existing Template").assertIsDisplayed()
    }

    @Test
    fun editTemplate_saveButtonEnabledWithValidState() {
        setScreenContentExistingTemplate()

        composeRule.onNodeWithText(appContext.getString(R.string.action_save))
            .assertIsEnabled()
    }

    // ── Core UI elements ─────────────────────────────────────────────

    @Test
    fun coreFields_areDisplayed() {
        setScreenContentNewTemplate()

        composeRule.onNodeWithText(appContext.getString(R.string.label_name))
            .assertIsDisplayed()
        composeRule.onNodeWithText(appContext.getString(R.string.label_tags))
            .assertIsDisplayed()
        composeRule.onNodeWithText(appContext.getString(R.string.label_message))
            .assertIsDisplayed()
        composeRule.onNodeWithText(appContext.getString(R.string.label_insert))
            .assertIsDisplayed()
    }

    @Test
    fun backButton_isDisplayed() {
        setScreenContentNewTemplate()

        composeRule.onNodeWithContentDescription(appContext.getString(R.string.content_desc_back))
            .assertIsDisplayed()
    }

    @Test
    fun cancelButton_isDisplayed() {
        setScreenContentNewTemplate()

        composeRule.onNodeWithText(appContext.getString(R.string.action_cancel))
            .assertIsDisplayed()
    }

    // ── Tags ─────────────────────────────────────────────────────────

    @Test
    fun tagSuggestions_areDisplayed() {
        setScreenContentNewTemplate()

        composeRule.onNodeWithText(appContext.getString(R.string.tag_residential))
            .assertIsDisplayed()
        composeRule.onNodeWithText(appContext.getString(R.string.tag_business))
            .assertIsDisplayed()
        composeRule.onNodeWithText(appContext.getString(R.string.tag_install))
            .assertIsDisplayed()
        composeRule.onNodeWithText(appContext.getString(R.string.tag_repair))
            .assertIsDisplayed()
        composeRule.onNodeWithText(appContext.getString(R.string.tag_troubleshooting))
            .assertIsDisplayed()
    }

    @Test
    fun clickingSuggestionTag_addsItAsChip() {
        setScreenContentNewTemplate()
        val residentialTag = appContext.getString(R.string.tag_residential)

        composeRule.onNodeWithText(residentialTag).performClick()
        composeRule.waitForIdle()

        // The tag should now appear as an InputChip with a remove icon
        composeRule.onNodeWithContentDescription(
            appContext.getString(R.string.content_desc_remove_tag, residentialTag)
        ).assertIsDisplayed()
    }

    @Test
    fun editTemplate_prefillsTags() {
        setScreenContentExistingTemplate(tags = listOf("Custom", "VIP"))

        composeRule.onNodeWithText("Custom").assertIsDisplayed()
        composeRule.onNodeWithText("VIP").assertIsDisplayed()
    }

    // ── Placeholder chips ────────────────────────────────────────────

    @Test
    fun placeholderChips_areDisplayed() {
        setScreenContentNewTemplate()

        composeRule.onNodeWithText("customer_name").assertIsDisplayed()
        composeRule.onNodeWithText("ssid").assertIsDisplayed()
        composeRule.onNodeWithText("password").assertIsDisplayed()
        composeRule.onNodeWithText("account_number").assertIsDisplayed()
        composeRule.onNodeWithText("tech_signature").assertIsDisplayed()
    }

    @Test
    fun requiredPlaceholderHint_isDisplayed() {
        setScreenContentNewTemplate()

        composeRule.onNodeWithText(
            appContext.getString(R.string.hint_template_required_placeholders)
        ).assertIsDisplayed()
    }

    // ── Message content ──────────────────────────────────────────────

    @Test
    fun editButton_isDisplayed() {
        setScreenContentNewTemplate()

        composeRule.onNodeWithContentDescription(appContext.getString(R.string.action_edit))
            .assertIsDisplayed()
    }

    @Test
    fun clickingEditButton_opensContentEditorDialog() {
        setScreenContentNewTemplate()

        composeRule.onNodeWithContentDescription(appContext.getString(R.string.action_edit))
            .performClick()
        composeRule.waitForIdle()

        // The content editor dialog shows a "Done" button
        composeRule.onNodeWithText(appContext.getString(R.string.action_done))
            .assertIsDisplayed()
    }

    // ── Content validation ───────────────────────────────────────────

    @Test
    fun newTemplate_withDefaultContent_hasNoContentError() {
        // The default template content already has required placeholders
        setScreenContentNewTemplate()

        // Create button should be disabled only because name is empty,
        // not because of content errors. Enter a name and verify enabled.
        composeRule.onNode(editableFieldWithLabel(appContext.getString(R.string.label_name)))
            .performTextInput("Test Template")
        composeRule.waitForIdle()

        composeRule.onNodeWithText(appContext.getString(R.string.action_create))
            .assertIsEnabled()
    }

    @Test
    fun editTemplate_missingPlaceholders_showsContentError() {
        // Template with content missing required placeholders
        setScreenContentExistingTemplate(content = "Hello, welcome to our service!")

        val errorText = appContext.getString(
            R.string.error_template_missing_placeholders,
            "customer_name, ssid"
        )
        composeRule.onNodeWithText(errorText).assertIsDisplayed()
    }

    @Test
    fun editTemplate_missingPlaceholders_disablesSaveButton() {
        setScreenContentExistingTemplate(content = "Hello, no placeholders here")

        composeRule.onNodeWithText(appContext.getString(R.string.action_save))
            .assertIsNotEnabled()
    }

    // ── Name validation ──────────────────────────────────────────────

    @Test
    fun savingWithBlankName_showsNameError() {
        setScreenContentNewTemplate()

        // Content has default placeholders (valid), but name is blank
        // Force save attempt by clearing name and pressing Create
        composeRule.onNodeWithText(appContext.getString(R.string.action_create))
            .performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText(appContext.getString(R.string.error_name_required))
            .assertIsDisplayed()
    }

    @Test
    fun typingAfterNameError_clearsError() {
        setScreenContentNewTemplate()

        // Trigger name error
        composeRule.onNodeWithText(appContext.getString(R.string.action_create))
            .performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText(appContext.getString(R.string.error_name_required))
            .assertIsDisplayed()

        // Now type a name — error should clear
        composeRule.onNode(editableFieldWithLabel(appContext.getString(R.string.label_name)))
            .performTextInput("Fixed Name")
        composeRule.waitForIdle()

        composeRule.onNodeWithText(appContext.getString(R.string.error_name_required))
            .assertDoesNotExist()
    }

    // ── Discard dialog ───────────────────────────────────────────────

    @Test
    fun cancelWithChanges_showsDiscardDialog() {
        setScreenContentExistingTemplate()

        // Make a change
        composeRule.onNode(
            editableFieldWithLabel(appContext.getString(R.string.label_name)),
            useUnmergedTree = true
        ).performTextInput(" Modified")
        composeRule.waitForIdle()

        // Press cancel
        composeRule.onNodeWithText(appContext.getString(R.string.action_cancel))
            .performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText(appContext.getString(R.string.dialog_discard_changes_title))
            .assertIsDisplayed()
        composeRule.onNodeWithText(appContext.getString(R.string.action_discard))
            .assertIsDisplayed()
        composeRule.onNodeWithText(appContext.getString(R.string.action_keep_editing))
            .assertIsDisplayed()
    }

    @Test
    fun discardDialog_keepEditing_dismissesDialog() {
        setScreenContentExistingTemplate()

        // Make a change then cancel
        composeRule.onNode(
            editableFieldWithLabel(appContext.getString(R.string.label_name)),
            useUnmergedTree = true
        ).performTextInput(" Modified")
        composeRule.waitForIdle()

        composeRule.onNodeWithText(appContext.getString(R.string.action_cancel))
            .performClick()
        composeRule.waitForIdle()

        // Keep editing
        composeRule.onNodeWithText(appContext.getString(R.string.action_keep_editing))
            .performClick()
        composeRule.waitForIdle()

        // Dialog should be gone, editor still visible
        composeRule.onNodeWithText(appContext.getString(R.string.dialog_discard_changes_title))
            .assertDoesNotExist()
        composeRule.onNodeWithText(appContext.getString(R.string.title_edit_template))
            .assertIsDisplayed()
    }

    @Test
    fun cancelWithNoChanges_doesNotShowDiscardDialog() {
        setScreenContentExistingTemplate()

        // Press cancel without making changes
        composeRule.onNodeWithText(appContext.getString(R.string.action_cancel))
            .performClick()
        composeRule.waitForIdle()

        // No dialog should appear
        composeRule.onNodeWithText(appContext.getString(R.string.dialog_discard_changes_title))
            .assertDoesNotExist()

        // Should have navigated back
        assertTrue(backInvoked.get())
    }

    // ── Name max length ──────────────────────────────────────────────

    @Test
    fun nameField_enforcesMaxLength() {
        setScreenContentNewTemplate()

        val longName = "A".repeat(60) // exceeds 50 char limit
        composeRule.onNode(editableFieldWithLabel(appContext.getString(R.string.label_name)))
            .performTextInput(longName)
        composeRule.waitForIdle()

        // The name displayed should be truncated to 50 chars
        val expected = "A".repeat(50)
        composeRule.onNodeWithText(expected).assertIsDisplayed()
    }

    // ── Content editor dialog ────────────────────────────────────────

    @Test
    fun contentEditorDialog_showsMessageLabelAndDoneButton() {
        setScreenContentNewTemplate()

        composeRule.onNodeWithContentDescription(appContext.getString(R.string.action_edit))
            .performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText(appContext.getString(R.string.action_done))
            .assertIsDisplayed()
    }

    @Test
    fun contentEditorDialog_showsPlaceholderChips() {
        setScreenContentNewTemplate()

        composeRule.onNodeWithContentDescription(appContext.getString(R.string.action_edit))
            .performClick()
        composeRule.waitForIdle()

        // Placeholder chips should be visible inside the dialog too
        composeRule.onNodeWithText("customer_name").assertIsDisplayed()
        composeRule.onNodeWithText("ssid").assertIsDisplayed()
    }

    @Test
    fun contentEditorDialog_doneButton_closesDialog() {
        setScreenContentNewTemplate()

        composeRule.onNodeWithContentDescription(appContext.getString(R.string.action_edit))
            .performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText(appContext.getString(R.string.action_done))
            .performClick()
        composeRule.waitForIdle()

        // Dialog should be dismissed — "Done" button no longer visible
        composeRule.onNodeWithText(appContext.getString(R.string.action_done))
            .assertDoesNotExist()
    }

    // ── Helpers ──────────────────────────────────────────────────────

    private fun editableFieldWithLabel(label: String): SemanticsMatcher {
        return hasSetTextAction() and hasText(label)
    }

    private fun setScreenContentNewTemplate() {
        val newTemplate = Template(
            id = NEW_TEMPLATE_ID,
            name = "",
            content = templateListViewModel.getDefaultTemplateContent()
        )
        setScreenContent(newTemplate)
    }

    private fun setScreenContentExistingTemplate(
        name: String = "Test Template",
        content: String = "Hello {{ customer_name }}, your SSID is {{ ssid }}",
        tags: List<String> = emptyList()
    ) {
        val template = Template(
            id = "test-template-id",
            name = name,
            content = content,
            tags = tags
        )
        setScreenContent(template)
    }

    private fun setScreenContent(template: Template) {
        templateListViewModel.startEditing(template)
        composeRule.setContent {
            CyberpunkTheme {
                CompositionLocalProvider(
                    LocalTemplateListViewModel provides templateListViewModel,
                    LocalNavigator provides navigator,
                    LocalSoundPlayer provides soundPlayer
                ) {
                    TemplateEditorScreen(
                        onBack = { backInvoked.set(true) }
                    )
                }
            }
        }
        composeRule.waitForIdle()
    }
}
