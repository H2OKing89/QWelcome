package com.kingpaging.qwelcome.ui.settings

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kingpaging.qwelcome.R
import com.kingpaging.qwelcome.data.PrivacySettings
import com.kingpaging.qwelcome.data.TechProfile
import com.kingpaging.qwelcome.data.Template
import com.kingpaging.qwelcome.ui.theme.CyberpunkTheme
import com.kingpaging.qwelcome.viewmodel.settings.UpdateState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

@RunWith(AndroidJUnit4::class)
class SettingsScreenNavigationTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val appContext: Context = ApplicationProvider.getApplicationContext()
    private val externalBacks = AtomicInteger()
    private val templatesOpened = AtomicInteger()
    private val keyboardVisible = AtomicBoolean()
    private val savedProfiles = mutableListOf<TechProfile>()
    private lateinit var suppliedState: MutableState<SettingsUiState>

    @Test
    fun overview_hasNoEditableFieldsAndOpensTemplateLibrary() {
        setScreenContent()

        composeRule.onAllNodes(hasSetTextAction()).assertCountEquals(0)
        composeRule
            .onNodeWithText(appContext.getString(R.string.settings_templates_title))
            .performScrollTo()
            .performClick()

        assertEquals(1, templatesOpened.get())
        assertEquals(0, externalBacks.get())
    }

    @Test
    fun profileBack_keepsDraftOrDiscardsItBeforeReturningToOverview() {
        setScreenContent()
        openProfile()
        nameField().performScrollTo().performTextReplacement("Jordan")

        pressBack()
        composeRule
            .onNodeWithText(appContext.getString(R.string.dialog_discard_changes_title))
            .assertIsDisplayed()
        composeRule.onNodeWithText(appContext.getString(R.string.action_keep_editing)).performClick()
        nameField().assertTextContains("Jordan")

        pressBack()
        composeRule.onNodeWithText(appContext.getString(R.string.action_discard)).performClick()
        composeRule.onAllNodes(hasSetTextAction()).assertCountEquals(0)
        openProfile()

        nameField().assertTextContains("Avery")
        assertEquals(0, externalBacks.get())
        composeRule.runOnIdle { assertEquals(emptyList<TechProfile>(), savedProfiles) }
    }

    @Test
    fun savingProfile_updatesSuppliedStateAndStaysInEditor() {
        setScreenContent()
        openProfile()
        nameField().performScrollTo().performTextReplacement("Jordan")
        composeRule.onNodeWithText(appContext.getString(R.string.action_save_profile)).performClick()

        composeRule
            .onNodeWithText(appContext.getString(R.string.settings_profile_saved_status))
            .assertIsDisplayed()
        composeRule.onAllNodes(hasSetTextAction()).assertCountEquals(3)
        nameField().assertTextContains("Jordan")
        composeRule.onAllNodesWithText(appContext.getString(R.string.action_save_profile)).assertCountEquals(0)
        composeRule.runOnIdle {
            assertEquals(listOf(TechProfile("Jordan", "Fiber Technician", "Field Operations")), savedProfiles)
            assertEquals("Jordan", suppliedState.value.profile.name)
        }
        assertEquals(0, externalBacks.get())
    }

    @Test
    fun privacyBack_returnsToOverviewBeforeCallingExternalBack() {
        setScreenContent()
        composeRule
            .onNodeWithText(appContext.getString(R.string.settings_privacy_title))
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithText(appContext.getString(R.string.settings_privacy_intro)).assertIsDisplayed()

        pressBack()
        composeRule
            .onNodeWithText(appContext.getString(R.string.settings_profile_edit))
            .performScrollTo()
            .assertIsDisplayed()
        assertEquals(0, externalBacks.get())

        pressBack()
        assertEquals(1, externalBacks.get())
    }

    @Test
    fun compactScreenWithDoubleTextSize_keepsSaveAboveKeyboard() {
        setScreenContent(fontScale = 2f)
        openProfile()
        nameField().performScrollTo().performClick().performTextReplacement("Jordan")
        composeRule.waitUntil(timeoutMillis = 5_000) { keyboardVisible.get() }

        nameField().assertIsFocused()
        composeRule
            .onNodeWithText(appContext.getString(R.string.action_save_profile))
            .assertIsDisplayed()
            .assertIsEnabled()
            .performClick()

        composeRule
            .onNodeWithText(appContext.getString(R.string.settings_profile_saved_status))
            .assertIsDisplayed()
        composeRule.runOnIdle { assertEquals("Jordan", savedProfiles.single().name) }
    }

    private fun openProfile() {
        composeRule
            .onNodeWithText(appContext.getString(R.string.settings_profile_edit))
            .performScrollTo()
            .performClick()
    }

    private fun pressBack() {
        composeRule
            .onNodeWithContentDescription(appContext.getString(R.string.content_desc_back))
            .performClick()
    }

    private fun nameField() =
        composeRule.onNode(hasSetTextAction() and hasText(appContext.getString(R.string.label_tech_name)))

    private fun setScreenContent(fontScale: Float = 1f) {
        suppliedState =
            mutableStateOf(
                SettingsUiState(
                    profile = TechProfile("Avery", "Fiber Technician", "Field Operations"),
                    privacySettings = PrivacySettings(),
                    activeTemplate = Template(name = "Welcome", content = "Hello {{ customer_name }}, {{ ssid }}"),
                    updateState = UpdateState.Idle,
                    showDownloadConfirmDialog = false,
                    currentVersion = "1.0.0",
                ),
            )
        composeRule.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, fontScale)) {
                val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
                SideEffect { keyboardVisible.set(imeVisible) }
                CyberpunkTheme {
                    Box(modifier = Modifier.width(320.dp).height(560.dp)) {
                        SettingsScreen(
                            uiState = suppliedState.value,
                            onBack = { externalBacks.incrementAndGet() },
                            onOpenExport = {},
                            onOpenImport = {},
                            onOpenTemplates = { templatesOpened.incrementAndGet() },
                            onSaveProfile = {
                                savedProfiles += it
                                suppliedState.value = suppliedState.value.copy(profile = it)
                            },
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
            }
        }
    }
}
