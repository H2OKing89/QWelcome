package com.kingpaging.qwelcome.ui

import android.content.Context
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kingpaging.qwelcome.R
import com.kingpaging.qwelcome.data.SettingsStore
import com.kingpaging.qwelcome.data.UserPreferences
import com.kingpaging.qwelcome.data.protoDataStore
import com.kingpaging.qwelcome.di.LocalCustomerIntakeViewModel
import com.kingpaging.qwelcome.di.LocalNavigator
import com.kingpaging.qwelcome.di.LocalSoundPlayer
import com.kingpaging.qwelcome.di.LocalTemplateSelectionViewModel
import com.kingpaging.qwelcome.testutil.FakeNavigator
import com.kingpaging.qwelcome.testutil.FakeSoundPlayer
import com.kingpaging.qwelcome.ui.theme.CyberpunkTheme
import com.kingpaging.qwelcome.util.AndroidResourceProvider
import com.kingpaging.qwelcome.util.WifiQrGenerator
import com.kingpaging.qwelcome.viewmodel.CustomerIntakeViewModel
import com.kingpaging.qwelcome.viewmodel.templates.TemplateSelectionUiState
import com.kingpaging.qwelcome.viewmodel.templates.TemplateSelectionViewModel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicBoolean

@RunWith(AndroidJUnit4::class)
class CustomerIntakeScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val settingsOpened = AtomicBoolean(false)
    private lateinit var appContext: Context
    private lateinit var customerIntakeViewModel: CustomerIntakeViewModel
    private lateinit var templateSelectionViewModel: TemplateSelectionViewModel
    private lateinit var navigator: FakeNavigator
    private lateinit var soundPlayer: FakeSoundPlayer

    @Before
    fun setup() {
        settingsOpened.set(false)

        appContext = ApplicationProvider.getApplicationContext()
        val settingsStore = SettingsStore(appContext)
        customerIntakeViewModel =
            CustomerIntakeViewModel(
                savedStateHandle = SavedStateHandle(),
                settingsStore = settingsStore,
                resourceProvider = AndroidResourceProvider(appContext),
            )
        templateSelectionViewModel =
            TemplateSelectionViewModel(
                settingsStore,
                AndroidResourceProvider(appContext),
            )

        navigator = FakeNavigator()
        soundPlayer = FakeSoundPlayer()
    }

    @After
    fun tearDown() =
        runBlocking {
            if (::appContext.isInitialized) {
                appContext.protoDataStore.updateData { UserPreferences.getDefaultInstance() }
            }
            Unit
        }

    @Test
    fun customerIntake_renders_core_fields_and_qr_disabled_by_default() {
        setScreenContent()
        val context = appContext

        composeRule.onNodeWithText(context.getString(R.string.label_customer_name)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.label_customer_phone)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.label_wifi_ssid)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.label_wifi_password)).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(context.getString(R.string.content_desc_settings)).assertIsDisplayed()

        composeRule.onNodeWithText(context.getString(R.string.action_show_qr)).assertIsNotEnabled()
    }

    @Test
    fun screen_rendersSuppliedStateWithoutViewModels() {
        composeRule.setContent {
            CyberpunkTheme {
                CustomerIntakeScreen(
                    uiState = CustomerIntakeUiState(customerName = "Supplied customer"),
                    templateUiState = TemplateSelectionUiState(isLoading = false),
                    snackbarHostState = remember { SnackbarHostState() },
                    formFocusTargets = remember { CustomerFormFocusTargets() },
                    copySuccess = false,
                    actions = noOpCustomerIntakeActions(),
                    onOpenSettings = {},
                    onOpenTemplates = {},
                )
            }
        }

        composeRule.onNodeWithText("Supplied customer").assertIsDisplayed()
    }

    @Test
    fun showQr_becomes_enabled_when_valid_wifi_credentials_entered() {
        setScreenContent()
        val context = appContext
        val ssidLabel = context.getString(R.string.label_wifi_ssid)
        val passwordLabel = context.getString(R.string.label_wifi_password)

        composeRule
            .onNode(editableFieldWithLabel(ssidLabel))
            .performTextInput("QWelcome-Test-Network")
        composeRule
            .onNode(editableFieldWithLabel(passwordLabel))
            .performTextInput("password123")

        composeRule.onNodeWithText(context.getString(R.string.action_show_qr)).assertIsEnabled()
    }

    @Test
    fun advancedWifiOptions_areCollapsedByDefaultAndExpandOnDemand() {
        setScreenContent()
        val context = appContext

        composeRule.onAllNodesWithText(context.getString(R.string.label_open_network)).assertCountEquals(0)
        composeRule.onAllNodesWithText(context.getString(R.string.label_hidden_network)).assertCountEquals(0)
        composeRule.onAllNodesWithText(context.getString(R.string.label_wifi_security)).assertCountEquals(0)

        composeRule
            .onNodeWithText(context.getString(R.string.label_advanced_wifi_options))
            .performClick()

        composeRule.onAllNodesWithText(context.getString(R.string.label_open_network)).assertCountEquals(1)
        composeRule.onAllNodesWithText(context.getString(R.string.label_hidden_network)).assertCountEquals(1)
        composeRule.onAllNodesWithText(context.getString(R.string.label_wifi_security)).assertCountEquals(1)
    }

    @Test
    fun failedValidation_focusesTheFirstInvalidField() {
        setScreenContent()
        val context = appContext

        composeRule
            .onNodeWithText(context.getString(R.string.action_sms))
            .performScrollTo()
            .performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule
                .onAllNodes(hasSetTextAction(), useUnmergedTree = true)
                .fetchSemanticsNodes()
                .firstOrNull()
                ?.config
                ?.getOrElse(SemanticsProperties.Focused) { false } == true
        }

        composeRule.onAllNodes(hasSetTextAction(), useUnmergedTree = true)[0].assertIsFocused()
    }

    @Test
    fun settings_button_invokes_callback() {
        setScreenContent()
        val context = appContext

        composeRule
            .onNodeWithContentDescription(context.getString(R.string.content_desc_settings))
            .performClick()

        assertTrue(settingsOpened.get())
    }

    private fun editableFieldWithLabel(label: String): SemanticsMatcher = hasSetTextAction() and hasText(label)

    private fun setScreenContent() {
        composeRule.setContent {
            CyberpunkTheme {
                CompositionLocalProvider(
                    LocalCustomerIntakeViewModel provides customerIntakeViewModel,
                    LocalTemplateSelectionViewModel provides templateSelectionViewModel,
                    LocalNavigator provides navigator,
                    LocalSoundPlayer provides soundPlayer,
                ) {
                    CustomerIntakeRoute(
                        onOpenSettings = { settingsOpened.set(true) },
                        onOpenTemplates = {},
                    )
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun noOpCustomerIntakeActions() =
        CustomerIntakeActions(
            onDismissQr = {},
            onClearForm = {},
            onTemplateSelected = {},
            onCustomerNameChanged = {},
            onCustomerPhoneChanged = {},
            onSsidChanged = {},
            onSecurityTypeChanged = { _: WifiQrGenerator.SecurityType -> },
            onHiddenNetworkChanged = {},
            onOpenNetworkChanged = {},
            onPasswordChanged = {},
            onAccountNumberChanged = {},
            onSmsClick = {},
            onShareClick = {},
            onCopyClick = {},
            onShowQr = {},
        )
}
