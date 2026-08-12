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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kingpaging.qwelcome.ui.theme.LocalDarkTheme

private val ButtonShape = RoundedCornerShape(8.dp)

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
    val isDark = LocalDarkTheme.current
    val colorScheme = MaterialTheme.colorScheme
    val glowAlpha = 0.6f
    val visuals = neonButtonVisuals(style, isDark, enabled, glowColor, colorScheme, glowAlpha)

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
                disabledContentColor =
                    if (isDark) {
                        glowColor.copy(alpha = 0.38f)
                    } else {
                        colorScheme.onSurface.copy(alpha = 0.38f)
                    },
            ),
        border = neonButtonBorder(glowColor, visuals.borderWidth, visuals.borderAlpha),
        elevation = neonButtonElevation(style, isDark),
        modifier =
            modifier
                .heightIn(min = 48.dp)
                .neonPrimaryGlow(enabled, isDark, style, glowColor, glowAlpha),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        content = content,
    )
}

@Composable
private fun neonButtonBorder(
    glowColor: Color,
    borderWidth: Dp,
    borderAlpha: Float,
): BorderStroke? =
    if (borderWidth > 0.dp) {
        BorderStroke(
            width = borderWidth,
            brush =
                Brush.linearGradient(
                    colors =
                        listOf(
                            glowColor.copy(alpha = borderAlpha),
                            glowColor.copy(alpha = borderAlpha * 0.7f),
                        ),
                ),
        )
    } else {
        null
    }

@Composable
private fun neonButtonElevation(
    style: NeonButtonStyle,
    isDark: Boolean,
) = when {
    !isDark && style == NeonButtonStyle.PRIMARY ->
        ButtonDefaults.buttonElevation(
            defaultElevation = 2.dp,
            pressedElevation = 4.dp,
            disabledElevation = 0.dp,
        )
    else -> ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp)
}

private fun Modifier.neonPrimaryGlow(
    enabled: Boolean,
    isDark: Boolean,
    style: NeonButtonStyle,
    glowColor: Color,
    glowAlpha: Float,
): Modifier =
    if (!enabled || !isDark || style != NeonButtonStyle.PRIMARY) {
        this
    } else {
        drawBehind {
            drawRoundRect(
                color = glowColor.copy(alpha = glowAlpha * 0.25f),
                cornerRadius = CornerRadius(8.dp.toPx()),
                style = Stroke(width = 2.dp.toPx()),
            )
        }
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
