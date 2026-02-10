package com.kingpaging.qwelcome.ui.settings

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kingpaging.qwelcome.R
import com.kingpaging.qwelcome.data.AppUpdater
import com.kingpaging.qwelcome.data.DownloadEnqueueResult
import com.kingpaging.qwelcome.data.DownloadStatus
import com.kingpaging.qwelcome.data.SettingsStore
import com.kingpaging.qwelcome.data.UpdateCheckResult
import com.kingpaging.qwelcome.data.VerificationResult
import com.kingpaging.qwelcome.di.LocalSettingsViewModel
import com.kingpaging.qwelcome.di.LocalSoundPlayer
import com.kingpaging.qwelcome.testutil.FakeSoundPlayer
import com.kingpaging.qwelcome.ui.theme.CyberpunkTheme
import com.kingpaging.qwelcome.util.AndroidResourceProvider
import com.kingpaging.qwelcome.viewmodel.factory.AppViewModelProvider
import com.kingpaging.qwelcome.viewmodel.settings.SettingsViewModel
import org.junit.After
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

    @Before
    fun setup() {
        AppViewModelProvider.resetForTesting()
        appContext = ApplicationProvider.getApplicationContext()
        val settingsStore = SettingsStore(appContext)
        vm = SettingsViewModel(
            store = settingsStore,
            resourceProvider = AndroidResourceProvider(appContext),
            appUpdater = FakeDialogAppUpdater()
        )
    }

    @After
    fun tearDown() {
        AppViewModelProvider.resetForTesting()
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

    private fun setScreenContent() {
        composeRule.setContent {
            CyberpunkTheme {
                CompositionLocalProvider(
                    LocalSettingsViewModel provides vm,
                    LocalSoundPlayer provides FakeSoundPlayer()
                ) {
                    SettingsScreen(onBack = {})
                }
            }
        }
        composeRule.waitForIdle()
    }
}

private class FakeDialogAppUpdater : AppUpdater {
    override suspend fun checkForUpdate(currentVersionName: String): UpdateCheckResult {
        return UpdateCheckResult.UpdateAvailable(
            latestVersion = "9.9.9",
            downloadUrl = "https://github.com/H2OKing89/QWelcome/releases/download/v9.9.9/QWelcome-v9.9.9.apk",
            releaseNotes = "test",
            assetName = "QWelcome-v9.9.9.apk",
            assetSizeBytes = 1234L,
            sha256Hex = "f".repeat(64)
        )
    }

    override suspend fun enqueueDownload(update: UpdateCheckResult.UpdateAvailable): DownloadEnqueueResult {
        return DownloadEnqueueResult.Failed("unused")
    }

    override suspend fun getDownloadStatus(downloadId: Long): DownloadStatus {
        return DownloadStatus.Failed("unused")
    }

    override suspend fun verifyDownloadedApk(
        apkPath: String,
        update: UpdateCheckResult.UpdateAvailable
    ): VerificationResult {
        return VerificationResult.Failed("unused")
    }

    override fun canRequestPackageInstalls(): Boolean = true

    override fun createUnknownSourcesSettingsIntent(): Intent = Intent("unused")

    override fun createInstallIntent(apkPath: String): Intent? = null
}
