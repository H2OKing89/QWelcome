package com.kingpaging.qwelcome.ui.export

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kingpaging.qwelcome.R
import com.kingpaging.qwelcome.ui.theme.CyberpunkTheme
import com.kingpaging.qwelcome.viewmodel.export.ExportUiState
import java.util.concurrent.atomic.AtomicBoolean
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExportScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun screen_rendersSuppliedStateWithoutViewModel() {
        composeRule.setContent {
            CyberpunkTheme {
                ExportScreen(
                    uiState = ExportUiState(),
                    onBack = {},
                    onTemplatePackRequested = {},
                    onFullBackupRequested = {},
                    onShareToPackageRequested = {},
                    onCopy = {},
                    onShareRequested = {},
                    onSaveToFileRequested = {},
                    onToggleTemplateSelection = {},
                    onToggleSelectAll = {},
                    onDismissTemplateSelection = {},
                    onExportSelectedTemplates = {}
                )
            }
        }

        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        composeRule.onNodeWithText(context.getString(R.string.title_export)).assertIsDisplayed()
    }

    @Test
    fun templatePackCard_invokesCallback() {
        val callbackInvoked = AtomicBoolean(false)
        composeRule.setContent {
            CyberpunkTheme {
                ExportScreen(
                    uiState = ExportUiState(),
                    onBack = {},
                    onTemplatePackRequested = { callbackInvoked.set(true) },
                    onFullBackupRequested = {},
                    onShareToPackageRequested = {},
                    onCopy = {},
                    onShareRequested = {},
                    onSaveToFileRequested = {},
                    onToggleTemplateSelection = {},
                    onToggleSelectAll = {},
                    onDismissTemplateSelection = {},
                    onExportSelectedTemplates = {}
                )
            }
        }

        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        composeRule.onNodeWithText(context.getString(R.string.export_type_template_pack)).performClick()

        assertTrue(callbackInvoked.get())
    }
}
