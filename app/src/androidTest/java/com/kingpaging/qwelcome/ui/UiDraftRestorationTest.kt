package com.kingpaging.qwelcome.ui

import android.content.Context
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kingpaging.qwelcome.R
import com.kingpaging.qwelcome.data.TechProfile
import com.kingpaging.qwelcome.data.Template
import com.kingpaging.qwelcome.ui.settings.SettingsScreen
import com.kingpaging.qwelcome.ui.settings.SettingsUiState
import com.kingpaging.qwelcome.ui.templates.TEMPLATE_LIST_TEST_TAG
import com.kingpaging.qwelcome.ui.templates.TemplateListScreen
import com.kingpaging.qwelcome.ui.theme.CyberpunkTheme
import com.kingpaging.qwelcome.viewmodel.settings.UpdateState
import com.kingpaging.qwelcome.viewmodel.templates.TemplateListUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UiDraftRestorationTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun profileDraftSurvivesSavedStateRestoration() {
        val restoration = StateRestorationTester(composeRule)
        val profile = TechProfile("Persisted technician", "Title", "Department")
        val template = Template(name = "Default", content = "{{ customer_name }} {{ ssid }}")
        restoration.setContent {
            CyberpunkTheme {
                SettingsScreen(
                    uiState = SettingsUiState(profile, null, template, UpdateState.Idle, false, "1.0"),
                    onBack = {},
                    onOpenExport = {},
                    onOpenImport = {},
                    onOpenTemplates = {},
                    onSaveProfile = {},
                    onSetCrashReportingEnabled = {},
                    onSetScreenCaptureProtectionEnabled = {},
                    onDismissDownloadConfirmation = {},
                    onConfirmDownload = {},
                    onRequestDownloadConfirmation = {},
                    onDismissUpdate = {},
                    onRetryInstall = {},
                    onOpenInstallSettings = {},
                    onCheckForUpdate = {},
                    onOpenProjectPage = {},
                )
            }
        }

        val context = ApplicationProvider.getApplicationContext<Context>()
        composeRule
            .onNodeWithText(context.getString(R.string.settings_profile_edit))
            .performClick()
        composeRule.onNodeWithText(profile.name).performTextReplacement("Unsaved technician")
        restoration.emulateSavedInstanceStateRestore()
        composeRule.onNodeWithText("Unsaved technician").assertIsDisplayed()
    }

    @Test
    fun templateListKeepsScrollPositionWhenRestored() {
        val restoration = StateRestorationTester(composeRule)
        val templates =
            List(40) { index ->
                Template(
                    id = "template-$index",
                    name = "Template $index",
                    content = "{{ customer_name }} {{ ssid }}",
                )
            }
        restoration.setContent {
            CyberpunkTheme {
                TemplateListScreen(
                    uiState =
                        TemplateListUiState(
                            templates = templates,
                            activeTemplateId = templates.first().id,
                            isLoading = false,
                        ),
                    snackbarHostState = SnackbarHostState(),
                    onBack = {},
                    onOpenEditor = {},
                    onDeleteTemplate = {},
                    onDismissDeleteConfirmation = {},
                    onUpdateTagFilter = {},
                    onClearTagFilter = {},
                    onRenameTemplate = { _, _ -> },
                    onUpdateSearchQuery = {},
                    onDismissTemplateLimitWarning = {},
                    onSetActiveTemplate = {},
                    onDuplicateAndEdit = {},
                    onDuplicateTemplate = {},
                    onShowDeleteConfirmation = {},
                )
            }
        }

        composeRule.onNodeWithTag(TEMPLATE_LIST_TEST_TAG).performScrollToNode(hasText("Template 20"))
        composeRule.onNodeWithText("Template 20").assertIsDisplayed()
        restoration.emulateSavedInstanceStateRestore()
        composeRule.onNodeWithText("Template 20").assertIsDisplayed()
    }
}
