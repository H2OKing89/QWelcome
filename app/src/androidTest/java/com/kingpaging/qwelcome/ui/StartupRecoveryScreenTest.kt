package com.kingpaging.qwelcome.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.kingpaging.qwelcome.R
import com.kingpaging.qwelcome.data.AppStartupState
import com.kingpaging.qwelcome.ui.theme.CyberpunkTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class StartupRecoveryScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test
    fun legacyRecoveryRequiresConfirmationAndSupportsCancel() {
        var recoveries = 0
        composeRule.setContent {
            CyberpunkTheme {
                StartupRecoveryScreen(
                    state = AppStartupState.Failed(true),
                    onRetry = {},
                    onContinueWithoutTemplates = { recoveries++ },
                )
            }
        }
        composeRule.onNodeWithText(context.getString(R.string.startup_skip_templates)).performClick()
        composeRule.onNodeWithText(context.getString(R.string.startup_confirm_title)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.startup_cancel)).performClick()
        composeRule.runOnIdle { assertEquals(0, recoveries) }
        composeRule.onNodeWithText(context.getString(R.string.startup_skip_templates)).performClick()
        composeRule.onNodeWithText(context.getString(R.string.startup_confirm_continue)).performClick()
        composeRule.runOnIdle { assertEquals(1, recoveries) }
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
}
