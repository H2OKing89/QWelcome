package com.kingpaging.qwelcome.ui.export

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kingpaging.qwelcome.R
import com.kingpaging.qwelcome.data.Template
import com.kingpaging.qwelcome.ui.theme.CyberpunkTheme
import com.kingpaging.qwelcome.viewmodel.export.ExportUiState
import com.kingpaging.qwelcome.viewmodel.export.ExportType
import java.util.concurrent.atomic.AtomicBoolean
import org.junit.Assert.assertEquals
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
                    actions = exportActions()
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
                    actions = exportActions(
                        onTemplatePackRequested = { callbackInvoked.set(true) }
                    )
                )
            }
        }

        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        composeRule.onNodeWithText(context.getString(R.string.export_type_template_pack)).performClick()

        assertTrue(callbackInvoked.get())
    }

    @Test
    fun exportOptionCard_isDisabledWhileLoading() {
        composeRule.setContent {
            CyberpunkTheme {
                ExportOptionCard(
                    title = "Template Pack",
                    description = "Export selected templates",
                    icon = Icons.Default.Backup,
                    iconTint = Color.Red,
                    isLoading = true,
                    onClick = {}
                )
            }
        }

        composeRule.onNodeWithText("Template Pack").assertIsNotEnabled()
    }

    @Test
    fun templateSelectionDialog_invokesSelectionCallback() {
        val selectedTemplateIds = mutableListOf<String>()

        composeRule.setContent {
            CyberpunkTheme {
                ExportTemplateSelectionDialog(
                    templates = listOf(
                        Template(
                            id = "service-welcome",
                            name = "Service Welcome",
                            content = "Welcome to your new service"
                        )
                    ),
                    selectedIds = emptySet(),
                    onToggleTemplate = { selectedTemplateIds += it },
                    onToggleSelectAll = {},
                    onDismiss = {},
                    onExport = {}
                )
            }
        }

        composeRule.onNodeWithText("Service Welcome").performClick()

        composeRule.runOnIdle {
            assertEquals(listOf("service-welcome"), selectedTemplateIds)
        }
    }

    @Test
    fun exportResultSection_copyActionInvokesCallback() {
        val copyInvoked = AtomicBoolean(false)

        composeRule.setContent {
            CyberpunkTheme {
                ExportResultSection(
                    exportedJson = "{\"templates\":[]}",
                    exportType = ExportType.TEMPLATE_PACK,
                    templateCount = 0,
                    recentShareTargets = emptyList(),
                    onShareToPackageRequested = {},
                    onCopy = { copyInvoked.set(true) },
                    onShareRequested = {},
                    onSaveToFileRequested = {}
                )
            }
        }

        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        composeRule.onNodeWithText(context.getString(R.string.action_copy)).performClick()

        assertTrue(copyInvoked.get())
    }

    private fun exportActions(
        onTemplatePackRequested: () -> Unit = {}
    ) = ExportActions(
        onBack = {},
        onTemplatePackRequested = onTemplatePackRequested,
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
