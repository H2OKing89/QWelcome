package com.kingpaging.qwelcome.ui

import android.net.Uri
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.FileProvider
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.core.app.ApplicationProvider
import com.kingpaging.qwelcome.R
import com.kingpaging.qwelcome.data.AppStartupState
import com.kingpaging.qwelcome.data.readLegacyTemplatesForRecovery
import com.kingpaging.qwelcome.data.tempPreferencesDataStore
import com.kingpaging.qwelcome.ui.theme.CyberpunkTheme
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File

class StartupRecoveryScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val rawTemplates = "{invalid-json"
    private var pendingRequestCode: Int? = null
    private val registry =
        object : ActivityResultRegistry() {
            override fun <I, O> onLaunch(
                requestCode: Int,
                contract: ActivityResultContract<I, O>,
                input: I,
                options: ActivityOptionsCompat?,
            ) {
                pendingRequestCode = requestCode
            }
        }

    @Before
    fun seedLegacyTemplates() {
        runBlocking {
            context.tempPreferencesDataStore.edit { prefs ->
                prefs.clear()
                prefs[stringPreferencesKey("templates_json")] = rawTemplates
            }
        }
    }

    @After
    fun clearLegacyTemplates() {
        runBlocking {
            context.tempPreferencesDataStore.edit { it.clear() }
        }
    }

    @Test
    fun legacyRecoveryRequiresSavedCopyAndConfirmationAndSupportsCancel() {
        var recoveries = 0
        setRecoveryContent { recoveries++ }
        composeRule.onNodeWithText(context.getString(R.string.startup_skip_templates)).assertIsNotEnabled()
        val directory = File(context.cacheDir, "qr_codes").apply { mkdirs() }
        val savedCopy = File.createTempFile("recovery-", ".json", directory)
        try {
            saveCopy(FileProvider.getUriForFile(context, "${context.packageName}.provider", savedCopy))
            waitForStatus(R.string.startup_copy_saved)
            assertEquals(rawTemplates, savedCopy.readText())

            composeRule.onNodeWithText(context.getString(R.string.startup_skip_templates)).performClick()
            composeRule.onNodeWithText(context.getString(R.string.startup_confirm_title)).assertIsDisplayed()
            composeRule.onNodeWithText(context.getString(R.string.startup_cancel)).performClick()
            composeRule.runOnIdle { assertEquals(0, recoveries) }
            composeRule.onNodeWithText(context.getString(R.string.startup_skip_templates)).performClick()
            composeRule.onNodeWithText(context.getString(R.string.startup_confirm_continue)).performClick()
            composeRule.runOnIdle { assertEquals(1, recoveries) }
        } finally {
            savedCopy.delete()
        }
    }

    @Test
    fun failedCopyKeepsContinueDisabledAndPreservesLegacyTemplates() {
        var recoveries = 0
        setRecoveryContent { recoveries++ }

        saveCopy(Uri.parse("content://missing.recovery.provider/templates.json"))
        waitForStatus(R.string.startup_copy_failed)

        composeRule.onNodeWithText(context.getString(R.string.startup_skip_templates)).assertIsNotEnabled()
        composeRule.runOnIdle { assertEquals(0, recoveries) }
        runBlocking { assertEquals(rawTemplates, readLegacyTemplatesForRecovery(context)) }
    }

    @Test
    fun cancelledCopyKeepsContinueDisabledAndPreservesLegacyTemplates() {
        var recoveries = 0
        setRecoveryContent { recoveries++ }

        saveCopy(null)

        composeRule.onNodeWithText(context.getString(R.string.startup_skip_templates)).assertIsNotEnabled()
        composeRule.runOnIdle { assertEquals(0, recoveries) }
        runBlocking { assertEquals(rawTemplates, readLegacyTemplatesForRecovery(context)) }
    }

    @Test
    fun ordinaryStorageFailureOffersRetryWithoutSkippingTemplates() {
        var retries = 0
        composeRule.setContent {
            CyberpunkTheme {
                StartupRecoveryScreen(
                    state = AppStartupState.Failed(false),
                    onRetry = { retries++ },
                    onContinueWithoutTemplates = {},
                )
            }
        }
        composeRule.onNodeWithText(context.getString(R.string.startup_skip_templates)).assertDoesNotExist()
        composeRule.onNodeWithText(context.getString(R.string.startup_retry)).performClick()
        composeRule.runOnIdle { assertEquals(1, retries) }
    }

    private fun setRecoveryContent(onContinue: () -> Unit) {
        val owner =
            object : ActivityResultRegistryOwner {
                override val activityResultRegistry = registry
            }
        composeRule.setContent {
            CompositionLocalProvider(LocalActivityResultRegistryOwner provides owner) {
                CyberpunkTheme {
                    StartupRecoveryScreen(
                        state = AppStartupState.Failed(true),
                        onRetry = {},
                        onContinueWithoutTemplates = onContinue,
                    )
                }
            }
        }
    }

    private fun saveCopy(uri: Uri?) {
        composeRule.onNodeWithText(context.getString(R.string.startup_save_copy)).performClick()
        composeRule.runOnIdle { registry.dispatchResult(checkNotNull(pendingRequestCode), uri) }
    }

    private fun waitForStatus(messageRes: Int) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(context.getString(messageRes)).fetchSemanticsNodes().isNotEmpty()
        }
    }
}
