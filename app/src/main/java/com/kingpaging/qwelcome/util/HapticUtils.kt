package com.kingpaging.qwelcome.util

import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

/**
 * Provides a lambda function that triggers a haptic feedback effect.
 *
 * This composable remembers the view and creates a lambda that can be called
 * to perform a standard haptic click effect. It's useful for providing
 * tactile feedback on user interactions like button clicks.
 *
 * @return A () -> Unit function that triggers haptic feedback when called.
 */
@Composable
fun rememberHapticFeedback(): () -> Unit {
    val view = LocalView.current
    return remember(view) {
        {
            view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }
}
