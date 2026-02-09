package com.kingpaging.qwelcome.ui

import android.content.Context
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kingpaging.qwelcome.R
import com.kingpaging.qwelcome.data.SettingsStore
import com.kingpaging.qwelcome.di.LocalCustomerIntakeViewModel
import com.kingpaging.qwelcome.di.LocalNavigator
import com.kingpaging.qwelcome.di.LocalSoundPlayer
import com.kingpaging.qwelcome.di.LocalTemplateListViewModel
import com.kingpaging.qwelcome.navigation.Navigator
import com.kingpaging.qwelcome.ui.theme.CyberpunkTheme
import com.kingpaging.qwelcome.util.AndroidResourceProvider
import com.kingpaging.qwelcome.util.SoundPlayer
import com.kingpaging.qwelcome.viewmodel.CustomerIntakeViewModel
import com.kingpaging.qwelcome.viewmodel.factory.AppViewModelProvider
import com.kingpaging.qwelcome.viewmodel.templates.TemplateListViewModel
import java.util.concurrent.atomic.AtomicBoolean
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CustomerIntakeScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val settingsOpened = AtomicBoolean(false)
    private lateinit var appContext: Context
    private lateinit var customerIntakeViewModel: CustomerIntakeViewModel
    private lateinit var templateListViewModel: TemplateListViewModel
    private lateinit var navigator: Navigator
    private lateinit var soundPlayer: SoundPlayer

    @Before
    fun setup() {
        AppViewModelProvider.resetForTesting()
        settingsOpened.set(false)

        appContext = ApplicationProvider.getApplicationContext()
        val settingsStore = SettingsStore(appContext)
        customerIntakeViewModel = CustomerIntakeViewModel(
            savedStateHandle = SavedStateHandle(),
            settingsStore = settingsStore,
            resourceProvider = AndroidResourceProvider(appContext)
        )
        templateListViewModel = TemplateListViewModel(settingsStore)

        navigator = object : Navigator {
            override fun openSms(phoneNumber: String, message: String) = Unit
            override fun shareText(message: String, chooserTitle: String) = Unit
            override fun copyToClipboard(label: String, text: String): Boolean = true
        }
        soundPlayer = object : SoundPlayer {
            override fun playBeep() = Unit
            override fun playConfirm() = Unit
        }
    }

    @After
    fun tearDown() {
        AppViewModelProvider.resetForTesting()
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
    fun showQr_becomes_enabled_when_valid_wifi_credentials_entered() {
        setScreenContent()
        val context = appContext
        val ssidLabel = context.getString(R.string.label_wifi_ssid)
        val passwordLabel = context.getString(R.string.label_wifi_password)

        composeRule.onNode(editableFieldWithLabel(ssidLabel), useUnmergedTree = true)
            .performTextInput("QWelcome-Test-Network")
        composeRule.onNode(editableFieldWithLabel(passwordLabel), useUnmergedTree = true)
            .performTextInput("password123")

        composeRule.onNodeWithText(context.getString(R.string.action_show_qr)).assertIsEnabled()
    }

    @Test
    fun settings_button_invokes_callback() {
        setScreenContent()
        val context = appContext

        composeRule.onNodeWithContentDescription(context.getString(R.string.content_desc_settings))
            .performClick()

        assertTrue(settingsOpened.get())
    }

    private fun editableFieldWithLabel(label: String): SemanticsMatcher {
        return hasSetTextAction() and hasText(label)
    }

    private fun setScreenContent() {
        composeRule.setContent {
            CyberpunkTheme {
                CompositionLocalProvider(
                    LocalCustomerIntakeViewModel provides customerIntakeViewModel,
                    LocalTemplateListViewModel provides templateListViewModel,
                    LocalNavigator provides navigator,
                    LocalSoundPlayer provides soundPlayer
                ) {
                    CustomerIntakeScreen(
                        onOpenSettings = { settingsOpened.set(true) },
                        onOpenTemplates = {}
                    )
                }
            }
        }
        composeRule.waitForIdle()
    }
}
