package com.kingpaging.qwelcome.ui.settings

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kingpaging.qwelcome.R
import com.kingpaging.qwelcome.data.TechProfile
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

                SettingsProfileSection(
                    activeTemplateName = "Welcome",
                    name = name,
                    title = title,
                    department = department,
                    hasUnsavedChanges = true,
                    onNameChange = { name = it },
                    onTitleChange = { title = it },
                    onDepartmentChange = { department = it },
                    onOpenTemplates = {},
                    onSaveProfile = { savedProfiles += TechProfile(name, title, department) }
                )
            }
        }

        composeRule.onNodeWithText("Avery").performTextReplacement("Jordan")
        composeRule.onNodeWithText("Fiber Technician").performTextReplacement("Installation Specialist")
        composeRule.onNodeWithText("Field Operations").performTextReplacement("Network Services")
        composeRule.onNodeWithText(appContext.getString(R.string.action_save_profile)).performClick()

        composeRule.runOnIdle {
            assertEquals(
                listOf(TechProfile("Jordan", "Installation Specialist", "Network Services")),
                savedProfiles
            )
        }
    }
}
