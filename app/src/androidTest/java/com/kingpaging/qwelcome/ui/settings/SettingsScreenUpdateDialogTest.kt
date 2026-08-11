package com.kingpaging.qwelcome.ui.settings

import android.content.Context
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kingpaging.qwelcome.R
import com.kingpaging.qwelcome.data.PrivacySettings
import com.kingpaging.qwelcome.data.SettingsStore
import com.kingpaging.qwelcome.data.TechProfile
import com.kingpaging.qwelcome.data.Template
import com.kingpaging.qwelcome.data.UpdateCheckResult
import com.kingpaging.qwelcome.di.LocalSettingsViewModel
import com.kingpaging.qwelcome.di.LocalSoundPlayer
import com.kingpaging.qwelcome.testutil.FakeAppUpdater
import com.kingpaging.qwelcome.testutil.FakeSoundPlayer
import com.kingpaging.qwelcome.ui.theme.CyberpunkTheme
import com.kingpaging.qwelcome.util.AndroidResourceProvider
import com.kingpaging.qwelcome.viewmodel.settings.SettingsViewModel
import com.kingpaging.qwelcome.viewmodel.settings.UpdateState
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsScreenUpdateDialogTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var appContext: Context
    private lateinit var vm: SettingsViewModel
    private lateinit var fakeAppUpdater: FakeAppUpdater

    @Before
    fun setup() {
        appContext = ApplicationProvider.getApplicationContext()
        val settingsStore = SettingsStore(appContext)
        fakeAppUpdater = FakeAppUpdater().apply {
            checkForUpdateResult = UpdateCheckResult.UpdateAvailable(
                latestVersion = "9.9.9",
                downloadUrl = "https://github.com/H2OKing89/QWelcome/releases/download/v9.9.9/QWelcome-v9.9.9.apk",
                releaseNotes = "test",
                assetName = "QWelcome-v9.9.9.apk",
                assetSizeBytes = 1234L,
                sha256Hex = "f".repeat(64)
            )
        }
        vm = SettingsViewModel(
            store = settingsStore,
            resourceProvider = AndroidResourceProvider(appContext),
            appUpdater = fakeAppUpdater
        )
    }

    @Test
    fun updateConfirmationDialog_visibility_follows_viewmodel_flag() {
        setScreenContent()
        val title = appContext.getString(R.string.title_update_available)

        composeRule.runOnIdle { vm.checkForUpdate() }
        composeRule.waitForIdle()

        composeRule.onAllNodesWithText(title).assertCountEquals(0)

        composeRule.runOnIdle { vm.requestDownloadConfirmation() }
        composeRule.waitForIdle()
        composeRule.onNodeWithText(title).assertIsDisplayed()

        composeRule.runOnIdle { vm.dismissDownloadConfirmation() }
        composeRule.waitForIdle()
        composeRule.onAllNodesWithText(title).assertCountEquals(0)
    }

    @Test
    fun privacySwitches_have_title_content_descriptions() {
        setScreenContent()

        composeRule.onNodeWithContentDescription(
            appContext.getString(R.string.label_crash_reporting)
        ).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(
            appContext.getString(R.string.label_screen_capture_protection)
        ).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun screen_rendersSuppliedStateWithoutViewModel() {
        composeRule.setContent {
            CyberpunkTheme {
                SettingsScreen(
                    uiState = SettingsUiState(
                        profile = TechProfile(name = "Route-free Tech"),
                        privacySettings = PrivacySettings(),
                        activeTemplate = Template(name = "Plain State", content = "Message"),
                        updateState = UpdateState.Idle,
                        showDownloadConfirmDialog = false,
                        currentVersion = "1.0.0"
                    ),
                    onBack = {},
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
                    onOpenProjectPage = {}
                )
            }
        }

        composeRule.onNodeWithText(appContext.getString(R.string.title_settings)).assertIsDisplayed()
        composeRule.onNodeWithText("Route-free Tech").assertIsDisplayed()
    }

    private fun setScreenContent() {
        composeRule.setContent {
            CyberpunkTheme {
                CompositionLocalProvider(
                    LocalSettingsViewModel provides vm,
                    LocalSoundPlayer provides FakeSoundPlayer()
                ) {
                    SettingsRoute(onBack = {})
                }
            }
        }
        composeRule.waitForIdle()
    }
}
