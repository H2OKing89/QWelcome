package com.kingpaging.qwelcome.ui.components

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal data class NeonButtonVisuals(
    val containerColor: Color,
    val contentColor: Color,
    val borderWidth: Dp,
    val borderAlpha: Float,
    val disabledContainerColor: Color,
)

private const val ENABLED_OUTLINE_ALPHA = 0.8f
private const val DISABLED_OUTLINE_ALPHA = 0.24f
private const val LIGHT_ACCENT_LUMINANCE_THRESHOLD = 0.179f

internal fun neonButtonVisuals(
    style: NeonButtonStyle,
    enabled: Boolean,
    accentColor: Color,
    colorScheme: ColorScheme,
) = NeonButtonVisuals(
    containerColor = if (style == NeonButtonStyle.PRIMARY) accentColor else Color.Transparent,
    contentColor = if (style == NeonButtonStyle.PRIMARY) onAccentColor(accentColor, colorScheme) else accentColor,
    borderWidth = if (style == NeonButtonStyle.SECONDARY) 1.dp else 0.dp,
    borderAlpha = if (enabled) ENABLED_OUTLINE_ALPHA else DISABLED_OUTLINE_ALPHA,
    disabledContainerColor =
        if (style == NeonButtonStyle.PRIMARY) {
            colorScheme.surfaceContainerHighest
        } else {
            Color.Transparent
        },
)

private fun onAccentColor(
    accentColor: Color,
    colorScheme: ColorScheme,
): Color =
    when (accentColor) {
        colorScheme.primary -> colorScheme.onPrimary
        colorScheme.secondary -> colorScheme.onSecondary
        colorScheme.tertiary -> colorScheme.onTertiary
        colorScheme.error -> colorScheme.onError
        else -> if (accentColor.luminance() > LIGHT_ACCENT_LUMINANCE_THRESHOLD) Color.Black else Color.White
    }
