package com.kingpaging.qwelcome.ui.settings

import android.content.Context
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kingpaging.qwelcome.R
import com.kingpaging.qwelcome.data.TechProfile
import com.kingpaging.qwelcome.ui.components.CenteredFormColumn
import com.kingpaging.qwelcome.ui.theme.CyberpunkTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsProfileSectionTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val appContext: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun saveAction_usesEditedProfileValues() {
        val savedProfiles = mutableListOf<TechProfile>()

        composeRule.setContent {
            CyberpunkTheme {
                var name by remember { mutableStateOf("Avery") }
                var title by remember { mutableStateOf("Fiber Technician") }
                var department by remember { mutableStateOf("Field Operations") }

                CenteredFormColumn(padding = PaddingValues()) {
                    SettingsProfileSection(
                        name = name,
                        title = title,
                        department = department,
                        hasUnsavedChanges = true,
                        onNameChange = { name = it },
                        onTitleChange = { title = it },
                        onDepartmentChange = { department = it },
                        onSaveProfile = { savedProfiles += TechProfile(name, title, department) },
                    )
                }
            }
        }

        composeRule.onNode(hasText("Avery") and hasSetTextAction()).performTextReplacement("Jordan")
        composeRule
            .onNode(hasText("Fiber Technician") and hasSetTextAction())
            .performTextReplacement("Installation Specialist")
        composeRule
            .onNode(hasText("Field Operations") and hasSetTextAction())
            .performTextReplacement("Network Services")
        composeRule
            .onNodeWithText(appContext.getString(R.string.action_save_profile))
            .performScrollTo()
            .performClick()

        composeRule.runOnIdle {
            assertEquals(
                listOf(TechProfile("Jordan", "Installation Specialist", "Network Services")),
                savedProfiles,
            )
        }
    }

    @Test
    fun signatureExample_tracksDraft_andSaveUpdatesStatus() {
        composeRule.setContent {
            CyberpunkTheme {
                var persisted by remember { mutableStateOf(TechProfile("Avery", "", "Field Operations")) }
                var name by remember { mutableStateOf(persisted.name) }
                var title by remember { mutableStateOf(persisted.title) }
                var department by remember { mutableStateOf(persisted.dept) }
                val edited = TechProfile(name, title, department)
                CenteredFormColumn(padding = PaddingValues()) {
                    SettingsProfileSection(
                        name = name,
                        title = title,
                        department = department,
                        hasUnsavedChanges = edited != persisted,
                        onNameChange = { name = it },
                        onTitleChange = { title = it },
                        onDepartmentChange = { department = it },
                        onSaveProfile = { persisted = edited },
                    )
                }
            }
        }

        composeRule.onNodeWithText(appContext.getString(R.string.settings_profile_saved_status)).assertExists()
        composeRule.onNodeWithText(appContext.getString(R.string.action_save_profile)).assertDoesNotExist()

        composeRule.onNode(hasText("Avery") and hasSetTextAction()).performTextReplacement("Jordan")

        composeRule.onNodeWithText("Jordan\nField Operations").assertExists()
        composeRule.onNodeWithText(appContext.getString(R.string.settings_profile_signature_draft)).assertExists()
        composeRule.onNodeWithText(appContext.getString(R.string.settings_profile_unsaved_status)).assertExists()
        composeRule
            .onNodeWithText(appContext.getString(R.string.action_save_profile))
            .performScrollTo()
            .performClick()

        composeRule.onNodeWithText(appContext.getString(R.string.settings_profile_saved_status)).assertExists()
        composeRule.onNodeWithText(appContext.getString(R.string.settings_profile_signature_saved)).assertExists()
        composeRule.onNodeWithText(appContext.getString(R.string.settings_profile_unsaved_status)).assertDoesNotExist()
        composeRule.onNodeWithText(appContext.getString(R.string.action_save_profile)).assertDoesNotExist()
        composeRule.onNodeWithText("Jordan\nField Operations").assertExists()
    }
}
