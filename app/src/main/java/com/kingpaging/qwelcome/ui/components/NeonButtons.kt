@file:Suppress("FunctionNaming")

package com.kingpaging.qwelcome.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp

private val ButtonShape = RoundedCornerShape(12.dp)
private const val DISABLED_CONTENT_ALPHA = 0.38f

enum class NeonButtonStyle {
    PRIMARY,
    SECONDARY,
    TERTIARY,
}

@Composable
fun NeonButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    glowColor: Color = MaterialTheme.colorScheme.primary,
    enabled: Boolean = true,
    style: NeonButtonStyle = NeonButtonStyle.PRIMARY,
    content: @Composable RowScope.() -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val colorScheme = MaterialTheme.colorScheme
    val visuals = neonButtonVisuals(style, enabled, glowColor, colorScheme)

    Button(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        enabled = enabled,
        shape = ButtonShape,
        colors =
            ButtonDefaults.buttonColors(
                containerColor = visuals.containerColor,
                contentColor = visuals.contentColor,
                disabledContainerColor = visuals.disabledContainerColor,
                disabledContentColor = colorScheme.onSurface.copy(alpha = DISABLED_CONTENT_ALPHA),
            ),
        border =
            if (visuals.borderWidth > 0.dp) {
                BorderStroke(visuals.borderWidth, glowColor.copy(alpha = visuals.borderAlpha))
            } else {
                null
            },
        elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp),
        modifier = modifier.heightIn(min = 48.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        content = content,
    )
}

@Composable
fun NeonCyanButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    style: NeonButtonStyle = NeonButtonStyle.PRIMARY,
    content: @Composable RowScope.() -> Unit,
) {
    NeonButton(
        onClick = onClick,
        modifier = modifier,
        glowColor = MaterialTheme.colorScheme.primary,
        enabled = enabled,
        style = style,
        content = content,
    )
}

@Composable
fun NeonMagentaButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    style: NeonButtonStyle = NeonButtonStyle.SECONDARY,
    content: @Composable RowScope.() -> Unit,
) {
    NeonButton(
        onClick = onClick,
        modifier = modifier,
        glowColor = MaterialTheme.colorScheme.secondary,
        enabled = enabled,
        style = style,
        content = content,
    )
}
