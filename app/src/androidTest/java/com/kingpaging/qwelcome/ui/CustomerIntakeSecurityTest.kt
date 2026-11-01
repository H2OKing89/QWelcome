package com.kingpaging.qwelcome.ui

import android.content.Context
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kingpaging.qwelcome.R
import com.kingpaging.qwelcome.ui.components.CenteredFormColumn
import com.kingpaging.qwelcome.ui.theme.CyberpunkTheme
import com.kingpaging.qwelcome.util.WifiQrGenerator
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CustomerIntakeSecurityTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun securityDropdownOffersExactlyFiveModesAndKeepsOffSelectable() {
        val state = mutableStateOf(CustomerIntakeUiState(ssid = "Mode test", password = "password123"))
        val expanded = mutableStateOf(false)
        val selections = mutableListOf<WifiQrGenerator.SecurityType>()
        composeRule.setContent {
            CyberpunkTheme {
                CenteredFormColumn(padding = PaddingValues()) {
                    CustomerFormFields(
                        uiState = state.value,
                        focusTargets = remember { CustomerFormFocusTargets() },
                        passwordVisible = false,
                        onCustomerNameChanged = {},
                        onCustomerPhoneChanged = {},
                        onSsidChanged = {},
                        advancedWifiOptionsExpanded = true,
                        onAdvancedWifiOptionsExpandedChange = {},
                        securityType = state.value.securityType,
                        securityDropdownExpanded = expanded.value,
                        onSecurityDropdownExpandedChange = { expanded.value = it },
                        onSecurityTypeChanged = {
                            selections += it
                            state.value = state.value.copy(securityType = it)
                        },
                        isHiddenNetwork = false,
                        onHiddenNetworkChanged = {},
                        onPasswordChanged = {},
                        onPasswordVisibilityToggle = {},
                        onAccountNumberChanged = {},
                    )
                }
            }
        }
        val expected =
            linkedMapOf(
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
            composeRule
                .onNodeWithText(context.getString(R.string.label_wifi_security))
                .performScrollTo()
                .performClick()
            composeRule.onAllNodes(hasAnyAncestor(isPopup()) and hasClickAction()).assertCountEquals(5)
            expected.values.forEach { resource ->
                composeRule
                    .onNode(hasText(context.getString(resource)) and hasAnyAncestor(isPopup()))
                    .assertIsDisplayed()
            }
            composeRule
                .onNode(hasText(context.getString(label)) and hasAnyAncestor(isPopup()))
                .performClick()
            composeRule.onAllNodes(isPopup()).assertCountEquals(0)
            val password = composeRule.onNodeWithText(context.getString(R.string.label_wifi_password))
            if (mode == WifiQrGenerator.SecurityType.OPEN) {
                password.assertIsNotEnabled()
            } else {
                password.assertIsEnabled()
            }
        }
        composeRule.runOnIdle { assertEquals(expected.keys.toList(), selections) }
    }
}
