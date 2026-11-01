package com.kingpaging.qwelcome.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kingpaging.qwelcome.R
import com.kingpaging.qwelcome.ui.theme.CyberpunkTheme
import com.kingpaging.qwelcome.util.WifiQrGenerator
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicBoolean

@RunWith(AndroidJUnit4::class)
class QrCodeSheetContentTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun saveAction_invokesCallback() {
        val saveRequested = AtomicBoolean(false)

        composeRule.setContent {
            CyberpunkTheme {
                Column {
                    QrCodeSheetContent(
                        qrPainter = ColorPainter(Color.Black),
                        network =
                            QrCodeNetworkDetails(
                                ssid = "Technician WiFi",
                                isOpenNetwork = false,
                                securityType = WifiQrGenerator.SecurityType.WPA2_PSK,
                            ),
                        isSaving = false,
                        isSharing = false,
                        actions =
                            QrCodeSheetActions(
                                onRequestSave = { saveRequested.set(true) },
                                onShare = {},
                            ),
                    )
                }
            }
        }

        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        composeRule
            .onNodeWithText(context.getString(R.string.action_save))
            .performClick()

        assertTrue(saveRequested.get())
    }

    @Test
    fun compactHeightWithLargeFontsKeepsActionsReachable() {
        val saveRequested = AtomicBoolean(false)
        val printRequested = AtomicBoolean(false)
        composeRule.setContent {
            CyberpunkTheme {
                val density = LocalDensity.current.density
                CompositionLocalProvider(LocalDensity provides Density(density, fontScale = 2f)) {
                    QrCodeScrollableContent(
                        qrPainter = ColorPainter(Color.Black),
                        network = QrCodeNetworkDetails("Office WiFi", false, WifiQrGenerator.SecurityType.WPA2_PSK),
                        isSaving = false,
                        isSharing = false,
                        actions =
                            QrCodeSheetActions(
                                onRequestSave = { saveRequested.set(true) },
                                onShare = {},
                                onPrint = { printRequested.set(true) },
                            ),
                        modifier = Modifier.width(320.dp).height(280.dp),
                    )
                }
            }
        }

        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        composeRule
            .onNodeWithText(context.getString(R.string.action_save))
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        assertTrue(saveRequested.get())
        composeRule
            .onNodeWithText(context.getString(R.string.action_share))
            .performScrollTo()
            .assertIsDisplayed()
        composeRule
            .onNodeWithText(context.getString(R.string.qr_print_action))
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        assertTrue(printRequested.get())
    }

    @Test
    fun savingSharingOrPrinting_disablesAllOutputActions() {
        val busyAction = mutableStateOf(0)
        composeRule.setContent {
            CyberpunkTheme {
                QrCodeScrollableContent(
                    qrPainter = ColorPainter(Color.White),
                    network = QrCodeNetworkDetails("Guest WiFi", true, WifiQrGenerator.SecurityType.WPA2_PSK),
                    isSaving = busyAction.value == 0,
                    isSharing = busyAction.value == 1,
                    actions = QrCodeSheetActions({}, {}, {}),
                    isPrinting = busyAction.value == 2,
                )
            }
        }

        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        repeat(3) { action ->
            composeRule.runOnIdle { busyAction.value = action }
            listOf(R.string.action_save, R.string.action_share, R.string.qr_print_action).forEach { label ->
                composeRule.onNodeWithText(context.getString(label)).assertIsNotEnabled()
            }
        }
    }

    @Test
    fun protectedWpa3Network_describesVisibleArtworkCredentials() {
        composeRule.setContent {
            CyberpunkTheme {
                Column {
                    QrCodeSheetContent(
                        qrPainter = ColorPainter(Color.Black),
                        network =
                            QrCodeNetworkDetails(
                                ssid = "Technician WiFi",
                                isOpenNetwork = false,
                                securityType = WifiQrGenerator.SecurityType.WPA3_SAE,
                                password = "CaseSensitive | p@ss 42!",
                            ),
                        isSaving = false,
                        isSharing = false,
                        actions =
                            QrCodeSheetActions(
                                onRequestSave = {},
                                onShare = {},
                            ),
                    )
                }
            }
        }

        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        composeRule.onNodeWithText(context.getString(R.string.security_wpa3_sae)).assertIsDisplayed()
        composeRule
            .onNodeWithContentDescription(
                context.getString(
                    R.string.strings_qr_artwork_accessibility,
                    "Technician WiFi",
                    "CaseSensitive | p@ss 42!",
                ),
            ).assertIsDisplayed()
        composeRule.onNodeWithText("********").assertDoesNotExist()
    }

    @Test
    fun securitySummaryShowsEverySelectedModeAndEnumOpenHidesStalePassword() {
        val selected = mutableStateOf(WifiQrGenerator.DEFAULT_SECURITY_TYPE)
        composeRule.setContent {
            CyberpunkTheme {
                QrCodeScrollableContent(
                    qrPainter = ColorPainter(Color.White),
                    network =
                        QrCodeNetworkDetails(
                            ssid = "Mode test",
                            isOpenNetwork = false,
                            securityType = selected.value,
                            password = "password123",
                        ),
                    isSaving = false,
                    isSharing = false,
                    actions = QrCodeSheetActions({}, {}),
                )
            }
        }
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val expected =
            mapOf(
                WifiQrGenerator.SecurityType.OPEN to R.string.security_off,
                WifiQrGenerator.SecurityType.WPA2_PSK to R.string.security_wpa2,
                WifiQrGenerator.SecurityType.WPA_WPA2_PERSONAL to R.string.security_wpa_wpa2_personal,
                WifiQrGenerator.SecurityType.WPA3_SAE to R.string.security_wpa3_sae,
                WifiQrGenerator.SecurityType.WPA2_WPA3_PERSONAL to R.string.security_wpa2_wpa3_personal,
            )
        composeRule
            .onNodeWithText(context.getString(R.string.security_wpa2_wpa3_personal))
            .performScrollTo()
            .assertIsDisplayed()
        expected.forEach { (mode, label) ->
            composeRule.runOnIdle { selected.value = mode }
            composeRule.onNodeWithText(context.getString(label)).performScrollTo().assertIsDisplayed()
        }
        composeRule.runOnIdle { selected.value = WifiQrGenerator.SecurityType.OPEN }
        composeRule
            .onNodeWithContentDescription(
                context.getString(
                    R.string.strings_qr_artwork_accessibility,
                    "Mode test",
                    context.getString(R.string.strings_qr_artwork_open_password),
                ),
            ).performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun sheetOmitsRemovedInstructionsAndPrintHint() {
        composeRule.setContent {
            CyberpunkTheme {
                QrCodeScrollableContent(
                    qrPainter = ColorPainter(Color.White),
                    network = QrCodeNetworkDetails("Guest WiFi", true, WifiQrGenerator.SecurityType.WPA2_PSK),
                    isSaving = false,
                    isSharing = false,
                    actions = QrCodeSheetActions({}, {}),
                )
            }
        }

        composeRule.onNodeWithText("Scan to connect automatically").assertDoesNotExist()
        composeRule.onNodeWithText("High-resolution PNG · A3 / 11×17 print quality").assertDoesNotExist()
    }

    @Test
    fun openNetwork_artworkDescribesNoPassword() {
        composeRule.setContent {
            CyberpunkTheme {
                Column {
                    QrCodeSheetContent(
                        qrPainter = ColorPainter(Color.White),
                        network = QrCodeNetworkDetails("Guest WiFi", true, WifiQrGenerator.SecurityType.WPA2_PSK),
                        isSaving = false,
                        isSharing = false,
                        actions = QrCodeSheetActions({}, {}),
                    )
                }
            }
        }

        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        composeRule
            .onNodeWithContentDescription(
                context.getString(
                    R.string.strings_qr_artwork_accessibility,
                    "Guest WiFi",
                    context.getString(R.string.strings_qr_artwork_open_password),
                ),
            ).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.security_off)).assertIsDisplayed()
        composeRule.onNodeWithText("********").assertDoesNotExist()
    }
}
