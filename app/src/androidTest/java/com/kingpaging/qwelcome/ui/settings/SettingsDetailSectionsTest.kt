package com.kingpaging.qwelcome.ui.settings

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kingpaging.qwelcome.R
import com.kingpaging.qwelcome.data.PrivacySettings
import com.kingpaging.qwelcome.ui.components.CenteredFormColumn
import com.kingpaging.qwelcome.ui.theme.CyberpunkTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsDetailSectionsTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val appContext: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun privacyLabels_toggleSuppliedValuesAtDoubleTextSize() {
        val crashChanges = mutableListOf<Boolean>()
        val captureChanges = mutableListOf<Boolean>()
        setPrivacyContent(
            settings = PrivacySettings(crashReportingEnabled = false, screenCaptureProtectionEnabled = true),
            onCrashChange = { crashChanges += it },
            onCaptureChange = { captureChanges += it },
            fontScale = 2f,
        )
        val crashRow = composeRule.onNodeWithText(appContext.getString(R.string.label_crash_reporting))
        val captureRow = composeRule.onNodeWithText(appContext.getString(R.string.label_screen_capture_protection))

        crashRow.performScrollTo().assertIsOff().performClick()
        captureRow.performScrollTo().assertIsOn().performClick()

        composeRule.runOnIdle {
            assertEquals(listOf(true), crashChanges)
            assertEquals(listOf(false), captureChanges)
        }
        captureRow.assertIsOn()
    }

    @Test
    fun privacyLoading_disablesBothControls() {
        setPrivacyContent(settings = null)

        composeRule
            .onNodeWithText(appContext.getString(R.string.label_crash_reporting))
            .assertIsNotEnabled()
        composeRule
            .onNodeWithText(appContext.getString(R.string.label_screen_capture_protection))
            .assertIsNotEnabled()
    }

    @Test
    fun unsavedProfile_blocksExportAndKeepsImportAvailable() {
        var exports = 0
        var imports = 0
        setBackupContent(
            hasUnsavedChanges = true,
            onExport = { exports++ },
            onImport = { imports++ },
        )

        composeRule
            .onNodeWithText(appContext.getString(R.string.settings_backup_export_title))
            .assertIsNotEnabled()
            .performTouchInput { click() }
        composeRule
            .onNodeWithText(appContext.getString(R.string.settings_backup_save_profile_first))
            .assertIsDisplayed()
        composeRule
            .onNodeWithText(appContext.getString(R.string.settings_backup_import_title))
            .assertIsEnabled()
            .performClick()

        composeRule.runOnIdle {
            assertEquals(0, exports)
            assertEquals(1, imports)
        }
    }

    @Test
    fun savedProfile_allowsExport() {
        var exports = 0
        setBackupContent(hasUnsavedChanges = false, onExport = { exports++ })

        composeRule
            .onNodeWithText(appContext.getString(R.string.settings_backup_export_title))
            .assertIsEnabled()
            .performClick()

        composeRule.runOnIdle { assertEquals(1, exports) }
    }

    private fun setPrivacyContent(
        settings: PrivacySettings?,
        onCrashChange: (Boolean) -> Unit = {},
        onCaptureChange: (Boolean) -> Unit = {},
        fontScale: Float = 1f,
    ) {
        composeRule.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, fontScale)) {
                CyberpunkTheme {
                    Box(modifier = Modifier.width(320.dp).height(480.dp)) {
                        CenteredFormColumn(padding = PaddingValues()) {
                            SettingsPrivacySection(
                                privacySettings = settings,
                                onSetCrashReportingEnabled = onCrashChange,
                                onSetScreenCaptureProtectionEnabled = onCaptureChange,
                            )
                        }
                    }
                }
            }
        }
    }

    private fun setBackupContent(
        hasUnsavedChanges: Boolean,
        onExport: () -> Unit = {},
        onImport: () -> Unit = {},
    ) {
        composeRule.setContent {
            CyberpunkTheme {
                CenteredFormColumn(padding = PaddingValues()) {
                    SettingsDataManagementSection(
                        hasUnsavedChanges = hasUnsavedChanges,
                        onOpenExport = onExport,
                        onOpenImport = onImport,
                    )
                }
            }
        }
    }
}
