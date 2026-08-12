package com.kingpaging.qwelcome.ui.components

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal data class NeonButtonVisuals(
    val containerColor: Color,
    val contentColor: Color,
    val borderWidth: Dp,
    val borderAlpha: Float,
    val disabledContainerColor: Color,
)

private const val SECONDARY_DARK_BORDER_ALPHA = 0.7f
private const val SECONDARY_LIGHT_BORDER_ALPHA = 0.6f
private const val DISABLED_SECONDARY_BORDER_ALPHA = 0.3f

internal fun neonButtonVisuals(
    style: NeonButtonStyle,
    isDark: Boolean,
    enabled: Boolean,
    glowColor: Color,
    colorScheme: ColorScheme,
    glowAlpha: Float,
) = NeonButtonVisuals(
    containerColor = buttonContainerColor(style, isDark, glowColor, colorScheme),
    contentColor = buttonContentColor(style, isDark, glowColor, colorScheme),
    borderWidth = buttonBorderWidth(style, isDark),
    borderAlpha = buttonBorderAlpha(style, isDark, enabled, glowAlpha),
    disabledContainerColor = buttonDisabledContainerColor(style, isDark, glowColor, colorScheme),
)

private fun buttonContainerColor(
    style: NeonButtonStyle,
    isDark: Boolean,
    glowColor: Color,
    colorScheme: ColorScheme,
): Color =
    when (style) {
        NeonButtonStyle.PRIMARY -> if (isDark) glowColor.copy(alpha = 0.20f) else glowColor
        NeonButtonStyle.SECONDARY -> Color.Transparent
        NeonButtonStyle.TERTIARY ->
            if (isDark) {
                colorScheme.surfaceVariant.copy(alpha = 0.35f)
            } else {
                colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
    }

private fun buttonContentColor(
    style: NeonButtonStyle,
    isDark: Boolean,
    glowColor: Color,
    colorScheme: ColorScheme,
): Color =
    when (style) {
        NeonButtonStyle.PRIMARY -> if (isDark) glowColor else colorScheme.onPrimary
        NeonButtonStyle.SECONDARY -> glowColor
        NeonButtonStyle.TERTIARY -> glowColor.copy(alpha = if (isDark) 0.8f else 1f)
    }

private fun buttonBorderWidth(
    style: NeonButtonStyle,
    isDark: Boolean,
): Dp =
    when (style) {
        NeonButtonStyle.PRIMARY -> if (isDark) 1.dp else 0.dp
        NeonButtonStyle.SECONDARY -> 1.dp
        NeonButtonStyle.TERTIARY -> 0.dp
    }

private fun buttonBorderAlpha(
    style: NeonButtonStyle,
    isDark: Boolean,
    enabled: Boolean,
    glowAlpha: Float,
): Float =
    when (style) {
        NeonButtonStyle.PRIMARY -> if (enabled && isDark) glowAlpha else 0f
        NeonButtonStyle.SECONDARY ->
            if (enabled) {
                if (isDark) SECONDARY_DARK_BORDER_ALPHA else SECONDARY_LIGHT_BORDER_ALPHA
            } else {
                DISABLED_SECONDARY_BORDER_ALPHA
            }
        NeonButtonStyle.TERTIARY -> 0f
    }

private fun buttonDisabledContainerColor(
    style: NeonButtonStyle,
    isDark: Boolean,
    glowColor: Color,
    colorScheme: ColorScheme,
): Color =
    when (style) {
        NeonButtonStyle.PRIMARY ->
            if (isDark) {
                glowColor.copy(alpha = 0.08f)
            } else {
                colorScheme.onSurface.copy(alpha = 0.12f)
            }
        NeonButtonStyle.SECONDARY -> Color.Transparent
        NeonButtonStyle.TERTIARY ->
            if (isDark) {
                Color.Transparent
            } else {
                colorScheme.surfaceVariant.copy(alpha = 0.3f)
            }
    }
