package com.example.allowelcome.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.allowelcome.ui.theme.CyberScheme
import com.example.allowelcome.util.SoundManager

private val PanelShape = RoundedCornerShape(16.dp)
private val ButtonShape = RoundedCornerShape(8.dp)

@Composable
fun NeonPanel(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .clip(PanelShape)
            .border(1.dp, Color.White.copy(alpha = 0.10f), PanelShape)
            .background(
                Brush.linearGradient(
                    listOf(
                        CyberScheme.secondary.copy(alpha = 0.10f),
                        CyberScheme.primary.copy(alpha = 0.06f),
                        CyberScheme.tertiary.copy(alpha = 0.08f)
                    )
                )
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NeonOutlinedField(
    value: String,
    onValueChange: (String) -> Unit,
    label: @Composable (() -> Unit),
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    isError: Boolean = false,
    supportingText: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        singleLine = singleLine,
        isError = isError,
        supportingText = supportingText,
        trailingIcon = trailingIcon,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        modifier = modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = CyberScheme.primary.copy(alpha = 0.85f),
            unfocusedBorderColor = Color.White.copy(alpha = 0.18f),
            cursorColor = CyberScheme.primary,
            focusedLabelColor = CyberScheme.primary,
        )
    )
}

/**
 * A neon-styled button with glow effect, haptic feedback, and sound
 */
@Composable
fun NeonButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    glowColor: Color = CyberScheme.primary,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    val haptic = LocalHapticFeedback.current
    
    val infiniteTransition = rememberInfiniteTransition(label = "neonPulse")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Button(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            SoundManager.playBeep()
            onClick()
        },
        enabled = enabled,
        shape = ButtonShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = glowColor.copy(alpha = 0.12f),
            contentColor = glowColor,
            disabledContainerColor = glowColor.copy(alpha = 0.05f),
            disabledContentColor = glowColor.copy(alpha = 0.4f)
        ),
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    glowColor.copy(alpha = if (enabled) glowAlpha else 0.3f),
                    glowColor.copy(alpha = if (enabled) glowAlpha * 0.6f else 0.2f)
                )
            )
        ),
        modifier = modifier
            .height(48.dp)
            .then(
                if (enabled) {
                    Modifier.drawBehind {
                        // Outer glow effect
                        drawRoundRect(
                            color = glowColor.copy(alpha = glowAlpha * 0.3f),
                            cornerRadius = CornerRadius(8.dp.toPx()),
                            style = Stroke(width = 4.dp.toPx()),
                            topLeft = Offset(-2.dp.toPx(), -2.dp.toPx()),
                            size = size.copy(
                                width = size.width + 4.dp.toPx(),
                                height = size.height + 4.dp.toPx()
                            )
                        )
                    }
                } else Modifier
            ),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        content = content
    )
}

/**
 * Cyan neon button variant
 */
@Composable
fun NeonCyanButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    NeonButton(
        onClick = onClick,
        modifier = modifier,
        glowColor = CyberScheme.primary,
        enabled = enabled,
        content = content
    )
}

/**
 * Magenta neon button variant
 */
@Composable
fun NeonMagentaButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    NeonButton(
        onClick = onClick,
        modifier = modifier,
        glowColor = CyberScheme.secondary,
        enabled = enabled,
        content = content
    )
}
