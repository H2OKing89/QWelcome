package com.kingpaging.qwelcome.ui.export

import android.app.Activity
import android.app.Instrumentation.ActivityResult
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.intent.Intents.init
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.Intents.release
import androidx.test.espresso.intent.VerificationModes.times
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kingpaging.qwelcome.QWelcomeApplication
import com.kingpaging.qwelcome.R
import com.kingpaging.qwelcome.di.LocalNavigator
import com.kingpaging.qwelcome.di.LocalSoundPlayer
import com.kingpaging.qwelcome.testutil.FakeNavigator
import com.kingpaging.qwelcome.testutil.FakeSoundPlayer
import com.kingpaging.qwelcome.ui.theme.CyberpunkTheme
import com.kingpaging.qwelcome.viewmodel.export.ExportViewModel
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExportRouteActivityResultTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var exportViewModel: ExportViewModel

    @Before
    fun setUp() {
        val application = ApplicationProvider.getApplicationContext<QWelcomeApplication>()
        val container = application.appContainer
        exportViewModel =
            ExportViewModel(
                repository = container.importExportRepository,
                settingsStore = container.settingsStore,
                packageManager = container.packageManager,
                contentResolver = container.contentResolver,
            )
        init()

        composeRule.setContent {
            CyberpunkTheme {
                CompositionLocalProvider(
                    LocalNavigator provides FakeNavigator(),
                    LocalSoundPlayer provides FakeSoundPlayer(),
                ) {
                    ExportRoute(
                        viewModel = exportViewModel,
                        onBack = {},
                    )
                }
            }
        }
    }

    @After
    fun tearDown() {
        release()
    }

    @Test
    fun cancellingDocumentPicker_emitsExactlyOnePickerLaunchPerSaveRequest() {
        intending(hasAction(Intent.ACTION_CREATE_DOCUMENT))
            .respondWith(ActivityResult(Activity.RESULT_CANCELED, null))

        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        composeRule.onNodeWithText(context.getString(R.string.export_type_full_backup)).performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule
                .onAllNodesWithText(context.getString(R.string.action_save_to_file))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeRule.onNodeWithText(context.getString(R.string.action_save_to_file)).assertIsDisplayed().performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText(context.getString(R.string.action_save_to_file)).performClick()
        composeRule.waitForIdle()

        intended(hasAction(Intent.ACTION_CREATE_DOCUMENT), times(2))
    }

    @Test
    fun stoppedRoute_consumesPendingPickerRequestOnlyOnceAfterRestart() {
        intending(hasAction(Intent.ACTION_CREATE_DOCUMENT))
            .respondWith(ActivityResult(Activity.RESULT_CANCELED, null))

        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        composeRule.onNodeWithText(context.getString(R.string.export_type_full_backup)).performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule
                .onAllNodesWithText(context.getString(R.string.action_save_to_file))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        composeRule.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        exportViewModel.onSaveToFileRequested()
        composeRule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        composeRule.waitForIdle()

        composeRule.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        composeRule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        composeRule.waitForIdle()

        intended(hasAction(Intent.ACTION_CREATE_DOCUMENT), times(1))
    }
}
