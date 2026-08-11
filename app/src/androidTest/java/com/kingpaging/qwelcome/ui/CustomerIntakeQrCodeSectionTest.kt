package com.kingpaging.qwelcome.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kingpaging.qwelcome.R
import com.kingpaging.qwelcome.ui.theme.CyberpunkTheme
import java.util.concurrent.atomic.AtomicBoolean
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CustomerIntakeQrCodeSectionTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun enabledShowQrAction_invokesCallback() {
        val callbackInvoked = AtomicBoolean(false)
        composeRule.setContent {
            CyberpunkTheme {
                CustomerIntakeQrCodeSection(
                    uiState = CustomerIntakeUiState(
                        ssid = "QWelcome-Test-Network",
                        password = "password123"
                    ),
                    enabled = true,
                    onShowQrClick = { callbackInvoked.set(true) }
                )
            }
        }

        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        composeRule.onNodeWithText(context.getString(R.string.action_show_qr)).performClick()

        assertTrue(callbackInvoked.get())
    }
}