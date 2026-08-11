package com.kingpaging.qwelcome.ui.import_pkg

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kingpaging.qwelcome.R
import com.kingpaging.qwelcome.ui.theme.CyberpunkTheme
import java.util.concurrent.atomic.AtomicBoolean
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ImportCompleteStepTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun doneAction_invokesCallback() {
        val doneInvoked = AtomicBoolean(false)

        composeRule.setContent {
            CyberpunkTheme {
                ImportCompleteStep(onDone = { doneInvoked.set(true) })
            }
        }

        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        composeRule.onNodeWithText(context.getString(R.string.action_done)).performClick()

        assertTrue(doneInvoked.get())
    }
}
