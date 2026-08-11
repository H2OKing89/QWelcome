package com.kingpaging.qwelcome.ui.import_pkg

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kingpaging.qwelcome.R
import com.kingpaging.qwelcome.ui.theme.CyberpunkTheme
import com.kingpaging.qwelcome.viewmodel.import_pkg.ImportUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ImportScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun screen_rendersSuppliedStateWithoutViewModel() {
        composeRule.setContent {
            CyberpunkTheme {
                ImportScreen(
                    uiState = ImportUiState(),
                    onBack = {},
                    onImportComplete = {},
                    onOpenFile = {},
                    onPaste = {},
                    onConfirm = {},
                    onCancel = {}
                )
            }
        }

        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        composeRule.onNodeWithText(context.getString(R.string.title_import)).assertIsDisplayed()
    }
}
