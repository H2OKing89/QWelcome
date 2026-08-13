@file:Suppress("PackageNaming")

package com.kingpaging.qwelcome.ui.import_pkg

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
import com.kingpaging.qwelcome.di.LocalSoundPlayer
import com.kingpaging.qwelcome.testutil.FakeSoundPlayer
import com.kingpaging.qwelcome.testutil.ImportFixtureProvider
import com.kingpaging.qwelcome.ui.theme.CyberpunkTheme
import com.kingpaging.qwelcome.viewmodel.import_pkg.ImportViewModel
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ImportRouteActivityResultTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var importViewModel: ImportViewModel

    @Before
    fun setUp() {
        val application = ApplicationProvider.getApplicationContext<QWelcomeApplication>()
        val container = application.appContainer
        importViewModel =
            ImportViewModel(
                repository = container.importExportRepository,
                resourceProvider = container.resourceProvider,
            )
        init()

        composeRule.setContent {
            CyberpunkTheme {
                CompositionLocalProvider(LocalSoundPlayer provides FakeSoundPlayer()) {
                    ImportRoute(
                        viewModel = importViewModel,
                        onBack = {},
                        onImportComplete = {},
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
    fun selectedFileUri_isReadAndPresentedForConfirmation() {
        intending(hasAction(Intent.ACTION_GET_CONTENT))
            .respondWith(
                ActivityResult(
                    Activity.RESULT_OK,
                    Intent().setData(ImportFixtureProvider.CONTENT_URI),
                ),
            )

        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        composeRule.onNodeWithText(context.getString(R.string.action_select_file)).performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule
                .onAllNodesWithText(context.getString(R.string.title_ready_to_import))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        composeRule.onNodeWithText(context.getString(R.string.title_ready_to_import)).assertIsDisplayed()
        intended(hasAction(Intent.ACTION_GET_CONTENT), times(1))
    }
}
