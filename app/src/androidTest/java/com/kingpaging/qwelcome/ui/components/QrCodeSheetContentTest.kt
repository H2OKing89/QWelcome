package com.kingpaging.qwelcome.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kingpaging.qwelcome.R
import com.kingpaging.qwelcome.ui.theme.CyberpunkTheme
import com.kingpaging.qwelcome.util.WifiQrGenerator
import java.util.concurrent.atomic.AtomicBoolean
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class QrCodeSheetContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun saveAction_invokesCallback() {
        val saveRequested = AtomicBoolean(false)

        composeRule.setContent {
            CyberpunkTheme {
                QrCodeSheetContent(
                    qrPainter = ColorPainter(Color.Black),
                    network = QrCodeNetworkDetails(
                        ssid = "Technician WiFi",
                        isOpenNetwork = false,
                        securityType = WifiQrGenerator.SecurityType.WPA2_PSK
                    ),
                    isSaving = false,
                    isSharing = false,
                    actions = QrCodeSheetActions(
                        onRequestSave = { saveRequested.set(true) },
                        onShare = {}
                    )
                )
            }
        }

        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        composeRule.onNodeWithText(context.getString(R.string.action_save)).performClick()

        assertTrue(saveRequested.get())
    }

    @Test
    fun protectedWpa3Network_displaysSecurityAndMasksPassword() {
        composeRule.setContent {
            CyberpunkTheme {
                QrCodeSheetContent(
                    qrPainter = ColorPainter(Color.Black),
                    network = QrCodeNetworkDetails(
                        ssid = "Technician WiFi",
                        isOpenNetwork = false,
                        securityType = WifiQrGenerator.SecurityType.WPA3_SAE
                    ),
                    isSaving = false,
                    isSharing = false,
                    actions = QrCodeSheetActions(
                        onRequestSave = {},
                        onShare = {}
                    )
                )
            }
        }

        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        composeRule.onNodeWithText(context.getString(R.string.security_wpa3_sae)).assertExists()
        composeRule.onNodeWithText(context.getString(R.string.label_wifi_password)).assertExists()
        composeRule.onNodeWithText("********").assertExists()
    }
}
