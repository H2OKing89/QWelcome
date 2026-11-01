package com.kingpaging.qwelcome.ui.settings

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
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
        fakeAppUpdater =
            FakeAppUpdater().apply {
                checkForUpdateResult =
                    UpdateCheckResult.UpdateAvailable(
                        latestVersion = "9.9.9",
                        downloadUrl =
                            "https://github.com/H2OKing89/QWelcome/releases/download/v9.9.9/QWelcome-v9.9.9.apk",
                        releaseNotes = "test",
                        assetName = "QWelcome-v9.9.9.apk",
                        assetSizeBytes = 1234L,
                        sha256Hex = "f".repeat(64),
                    )
            }
        vm =
            SettingsViewModel(
                store = settingsStore,
                resourceProvider = AndroidResourceProvider(appContext),
                appUpdater = fakeAppUpdater,
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
    fun privacySwitches_have_readableTitles() {
        setScreenContent()
        composeRule.onNodeWithText(appContext.getString(R.string.settings_privacy_title)).performClick()

        composeRule
            .onNodeWithText(
                appContext.getString(R.string.label_crash_reporting),
            ).assertIsDisplayed()
        composeRule
            .onNodeWithText(
                appContext.getString(R.string.label_screen_capture_protection),
            ).performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun screen_rendersSuppliedStateWithoutViewModel() {
        composeRule.setContent {
            CyberpunkTheme {
                SettingsScreen(
                    uiState =
                        SettingsUiState(
                            profile = TechProfile(name = "Route-free Tech"),
                            privacySettings = PrivacySettings(),
                            activeTemplate = Template(name = "Plain State", content = "Message"),
                            updateState = UpdateState.Idle,
                            showDownloadConfirmDialog = false,
                            currentVersion = "1.0.0",
                        ),
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

        composeRule.onNodeWithText(appContext.getString(R.string.title_settings)).assertIsDisplayed()
        composeRule.onNodeWithText("Route-free Tech").assertIsDisplayed()
    }

    @Test
    fun aboutUpdates_largeText_keepsProgressAndActionsAccessible() {
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
                CyberpunkTheme {
                    Column(
                        modifier =
                            Modifier
                                .width(320.dp)
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        SettingsUpdateSection(
                            currentVersion = "1.0.0",
                            updateState =
                                UpdateState.Downloading(
                                    version = "2.0.0",
                                    bytesDownloaded = 512L,
                                    totalBytes = 1024L,
                                ),
                            downloadActions = UpdateDownloadActions({}, {}, {}, {}),
                            navigationActions = UpdateNavigationActions({}, {}),
                        )
                    }
                }
            }
        }

        composeRule
            .onNodeWithTag(SETTINGS_UPDATE_PROGRESS_TAG)
            .performScrollTo()
            .assertIsDisplayed()
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.ProgressBarRangeInfo,
                    ProgressBarRangeInfo(0.5f, 0f..1f),
                ),
            )
        composeRule
            .onNodeWithText(appContext.getString(R.string.action_check_updates))
            .performScrollTo()
            .assertIsDisplayed()
            .assertIsNotEnabled()
        composeRule
            .onNodeWithText(appContext.getString(R.string.action_view_github))
            .performScrollTo()
            .assertIsDisplayed()
    }

    private fun setScreenContent() {
        composeRule.setContent {
            CyberpunkTheme {
                CompositionLocalProvider(
                    LocalSettingsViewModel provides vm,
                    LocalSoundPlayer provides FakeSoundPlayer(),
                ) {
                    SettingsRoute(onBack = {})
                }
            }
        }
        composeRule.waitForIdle()
    }
}
