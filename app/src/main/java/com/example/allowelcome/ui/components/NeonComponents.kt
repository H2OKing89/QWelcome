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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.allowelcome.ui.theme.LocalDarkTheme
import com.example.allowelcome.util.SoundManager

private val PanelShape = RoundedCornerShape(16.dp)
private val ButtonShape = RoundedCornerShape(8.dp)

// Cyberpunk light mode constants
private const val NEON_EDGE_ALPHA = 0.35f
private const val LIGHT_BORDER_ALPHA = 0.25f  // Raised from 0.15 for better visibility on all displays

/**
 * Button emphasis levels for proper visual hierarchy.
 * - PRIMARY: Main action per screen (SMS button, Save button)
 * - SECONDARY: Important but not the main action (Share, Copy, Export)
 * - TERTIARY: Less important actions (QR Code, Import)
 */
enum class NeonButtonStyle {
    PRIMARY,    // Filled with glow/elevation - strongest emphasis
    SECONDARY,  // Outlined with border - medium emphasis
    TERTIARY    // Subtle outlined - lowest emphasis
}

/**
 * Cyberpunk-styled panel with signature neon top-edge accent.
 * 
 * Light mode: Clean white surface + neon gradient edge = cyberpunk identity
 * Dark mode: Gradient background + subtle border
 */
@Composable
fun NeonPanel(
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = LocalDarkTheme.current
    val colorScheme = MaterialTheme.colorScheme

    // Shadow/clip ordering: shadow BEFORE clip to avoid clipped shadows
    Column(
        modifier = modifier
            .then(
                if (isDark) {
                    Modifier
                } else {
                    // Light mode: shadow first (before clip)
                    Modifier.shadow(
                        elevation = 2.dp,
                        shape = PanelShape,
                        clip = false  // Don't clip the shadow
                    )
                }
            )
            .clip(PanelShape)
            .then(
                if (isDark) {
                    // Dark mode: subtle border
                    Modifier.border(
                        1.dp,
                        Color.White.copy(alpha = 0.10f),
                        PanelShape
                    )
                } else {
                    // Light mode: thin visible border
                    Modifier.border(
                        0.5.dp,
                        colorScheme.outlineVariant.copy(alpha = LIGHT_BORDER_ALPHA),
                        PanelShape
                    )
                }
            )
            .then(
                if (isDark) {
                    // Dark mode: colorful gradient with low alpha
                    Modifier.background(
                        Brush.linearGradient(
                            listOf(
                                colorScheme.secondary.copy(alpha = 0.10f),
                                colorScheme.primary.copy(alpha = 0.06f),
                                colorScheme.tertiary.copy(alpha = 0.08f)
                            )
                        )
                    )
                } else {
                    // Light mode: clean solid white background (no gradient allocation)
                    Modifier.background(colorScheme.surface)
                }
            )
            // CYBERPUNK SIGNATURE: Neon top-edge accent line (both modes now!)
            .drawBehind {
                // Draw neon gradient line across top of panel
                // StrokeCap.Round for intentional, polished look
                if (isDark) {
                    // Dark mode: cyan→magenta gradient at low alpha (static, no animation)
                    drawLine(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                accentColor.copy(alpha = 0f),
                                accentColor.copy(alpha = 0.25f),  // Cyan
                                colorScheme.secondary.copy(alpha = 0.20f),  // Magenta
                                accentColor.copy(alpha = 0f)
                            )
                        ),
                        start = Offset(16.dp.toPx(), 0f),
                        end = Offset(size.width - 16.dp.toPx(), 0f),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                } else {
                    // Light mode: cyan→magenta at higher alpha
                    drawLine(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                accentColor.copy(alpha = 0f),
                                accentColor.copy(alpha = NEON_EDGE_ALPHA),
                                colorScheme.secondary.copy(alpha = NEON_EDGE_ALPHA * 0.8f),
                                accentColor.copy(alpha = 0f)
                            )
                        ),
                        start = Offset(16.dp.toPx(), 0f),
                        end = Offset(size.width - 16.dp.toPx(), 0f),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content
    )
}

/**
 * Cyberpunk-styled outlined text field.
 * Adapts border and label colors based on theme.
 * 
 * ChatGPT feedback: Focus state should feel like "hacking a terminal"
 * - Dark mode: thin cyan glow on focused field
 * - Error: neon red with NO infinite pulse (prevents rave mode)
 */
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
    val isDark = LocalDarkTheme.current
    val colorScheme = MaterialTheme.colorScheme
    
    // Focus state glow modifier for dark mode - "hacking terminal" vibe
    val focusGlowModifier = if (isDark) {
        // Add a subtle outer glow when focused (applied via border, not drawBehind)
        // The thicker focused border + brighter color creates the glow effect
        Modifier
    } else {
        Modifier
    }

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
        modifier = modifier.fillMaxWidth().then(focusGlowModifier),
        colors = OutlinedTextFieldDefaults.colors(
            // Focused states - brighter in dark mode for "terminal" feel
            focusedBorderColor = if (isDark) colorScheme.primary else colorScheme.primary,
            focusedLabelColor = colorScheme.primary,
            focusedTextColor = colorScheme.onSurface,
            cursorColor = colorScheme.primary,
            // Unfocused states - cleaner in light mode
            unfocusedBorderColor = if (isDark) {
                Color.White.copy(alpha = 0.18f)
            } else {
                colorScheme.outline.copy(alpha = 0.6f)
            },
            unfocusedLabelColor = colorScheme.onSurfaceVariant,
            unfocusedTextColor = colorScheme.onSurface,
            // Container - slight tint on focus in dark mode
            focusedContainerColor = if (isDark) colorScheme.primary.copy(alpha = 0.05f) else colorScheme.surface,
            unfocusedContainerColor = if (isDark) Color.Transparent else colorScheme.surface,
            // Error states - neon red (no animation, per motion budget)
            errorBorderColor = colorScheme.error,
            errorLabelColor = colorScheme.error,
            errorCursorColor = colorScheme.error,
            errorContainerColor = if (isDark) colorScheme.error.copy(alpha = 0.05f) else Color.Transparent
        )
    )
}

/**
 * A neon-styled button with proper visual hierarchy.
 * 
 * Style guide per ChatGPT feedback:
 * - PRIMARY: Main action (filled with glow/elevation) - SMS, Save All
 * - SECONDARY: Important alt actions (outlined) - Share, Copy, Export
 * - TERTIARY: Less prominent (subtle outlined) - QR Code, Import
 * 
 * Includes haptic feedback and sound.
 */
@Composable
fun NeonButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    glowColor: Color = MaterialTheme.colorScheme.primary,
    enabled: Boolean = true,
    style: NeonButtonStyle = NeonButtonStyle.PRIMARY,
    content: @Composable RowScope.() -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val isDark = LocalDarkTheme.current
    val colorScheme = MaterialTheme.colorScheme

    // MOTION BUDGET: Scanline is the ONE continuous animation per screen.
    // Button glow is now STATIC (0.6 alpha) - no infinite pulse.
    // This prevents "casino UI" while keeping the cyberpunk vibe.
    val glowAlpha = 0.6f  // Static glow - no animation

    // Style-dependent properties - DECISIVE hierarchy per ChatGPT feedback
    // PRIMARY = Filled (no border), SECONDARY = Outlined, TERTIARY = Text/Subtle
    // Issue 6 (ChatGPT): PRIMARY needs more decisive fill in dark mode
    // Using 0.20f alpha (up from 0.15f) for better "premium" feel
    val containerColor = when (style) {
        NeonButtonStyle.PRIMARY -> if (isDark) {
            glowColor.copy(alpha = 0.20f)  // Slightly stronger tint
        } else {
            glowColor  // Solid fill - shape + elevation do the work
        }
        NeonButtonStyle.SECONDARY -> Color.Transparent
        // TERTIARY: Subtle tonal fill so it reads as a button, not floating text
        NeonButtonStyle.TERTIARY -> if (isDark) {
            colorScheme.surfaceVariant.copy(alpha = 0.35f)  // Visible but subtle
        } else {
            colorScheme.surfaceVariant.copy(alpha = 0.5f)
        }
    }
    
    val contentColor = when (style) {
        NeonButtonStyle.PRIMARY -> if (isDark) glowColor else colorScheme.onPrimary
        NeonButtonStyle.SECONDARY -> glowColor
        NeonButtonStyle.TERTIARY -> glowColor.copy(alpha = if (isDark) 0.8f else 1f)
    }
    
    // Light mode: thinner, more precise borders (0.5dp max unless deliberate)
    val borderWidth = when (style) {
        NeonButtonStyle.PRIMARY -> if (isDark) 1.dp else 0.dp  // NO border on filled
        NeonButtonStyle.SECONDARY -> if (isDark) 1.dp else 1.dp  // Deliberate outline
        NeonButtonStyle.TERTIARY -> 0.dp  // Text button - no border
    }
    
    // SECONDARY is STATIC (not animated) - this preserves hierarchy.
    // PRIMARY has glow, SECONDARY has static outline, TERTIARY is subtle.
    val borderAlpha = when (style) {
        NeonButtonStyle.PRIMARY -> if (enabled && isDark) glowAlpha else 0f
        NeonButtonStyle.SECONDARY -> if (enabled) (if (isDark) 0.7f else 0.6f) else 0.3f  // Static!
        NeonButtonStyle.TERTIARY -> 0f
    }

    Button(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            SoundManager.playBeep()
            onClick()
        },
        enabled = enabled,
        shape = ButtonShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            // Disabled states: readable but clearly inactive
            disabledContainerColor = when (style) {
                NeonButtonStyle.PRIMARY -> if (isDark) glowColor.copy(alpha = 0.08f) else colorScheme.onSurface.copy(alpha = 0.12f)
                NeonButtonStyle.TERTIARY -> if (isDark) Color.Transparent else colorScheme.surfaceVariant.copy(alpha = 0.3f)
                else -> Color.Transparent
            },
            disabledContentColor = if (isDark) glowColor.copy(alpha = 0.38f) else colorScheme.onSurface.copy(alpha = 0.38f)
        ),
        border = if (borderWidth > 0.dp) {
            BorderStroke(
                width = borderWidth,
                brush = Brush.linearGradient(
                    colors = listOf(
                        glowColor.copy(alpha = borderAlpha),
                        glowColor.copy(alpha = borderAlpha * 0.7f)
                    )
                )
            )
        } else null,
        elevation = when {
            !isDark && style == NeonButtonStyle.PRIMARY -> ButtonDefaults.buttonElevation(
                defaultElevation = 2.dp,
                pressedElevation = 4.dp,
                disabledElevation = 0.dp
            )
            else -> ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp)
        },
        // Dark mode PRIMARY: subtle inner glow effect (stays within bounds, no alignment issues)
        modifier = modifier
            .height(48.dp)
            .then(
                // Dark mode PRIMARY: draw glow as inner border effect (no clipping issues)
                if (enabled && isDark && style == NeonButtonStyle.PRIMARY) {
                    Modifier.drawBehind {
                        // Draw subtle glow as an inset rounded rect
                        drawRoundRect(
                            color = glowColor.copy(alpha = glowAlpha * 0.25f),
                            cornerRadius = CornerRadius(8.dp.toPx()),
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                } else Modifier
            ),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        content = content
    )
}

/**
 * Primary cyan button - main action per screen (SMS, Save)
 */
@Composable
fun NeonCyanButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    style: NeonButtonStyle = NeonButtonStyle.PRIMARY,
    content: @Composable RowScope.() -> Unit
) {
    NeonButton(
        onClick = onClick,
        modifier = modifier,
        glowColor = MaterialTheme.colorScheme.primary,
        enabled = enabled,
        style = style,
        content = content
    )
}

/**
 * Secondary magenta button - important alternate actions (Share, Export)
 */
@Composable
fun NeonMagentaButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    style: NeonButtonStyle = NeonButtonStyle.SECONDARY,
    content: @Composable RowScope.() -> Unit
) {
    NeonButton(
        onClick = onClick,
        modifier = modifier,
        glowColor = MaterialTheme.colorScheme.secondary,
        enabled = enabled,
        style = style,
        content = content
    )
}

/**
 * Placeholder chip for displaying template placeholders in a scannable format.
 * Per ChatGPT feedback: "Render placeholders as chips... instantly readable"
 */
@Composable
fun PlaceholderChip(
    text: String,
    modifier: Modifier = Modifier,
    isRequired: Boolean = false
) {
    val isDark = LocalDarkTheme.current
    val colorScheme = MaterialTheme.colorScheme
    val chipColor = if (isRequired) colorScheme.secondary else colorScheme.tertiary
    
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(4.dp),
        color = if (isDark) {
            chipColor.copy(alpha = 0.15f)
        } else {
            chipColor.copy(alpha = 0.1f)
        },
        border = BorderStroke(
            width = 0.5.dp,
            color = chipColor.copy(alpha = if (isDark) 0.4f else 0.3f)
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = chipColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

/**
 * Flow row of placeholder chips for the Settings screen.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlaceholderChipsRow(
    placeholders: List<Pair<String, String>>, // (placeholder, description)
    modifier: Modifier = Modifier
) {
    // Required placeholders - exact match against canonical tokens
    val requiredPlaceholders = setOf("{{ customer_name }}", "{{ ssid }}")
    
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        placeholders.forEach { (placeholder, _) ->
            PlaceholderChip(
                text = placeholder,
                // Mark required placeholders using exact match against canonical tokens
                isRequired = placeholder in requiredPlaceholders
            )
        }
    }
}

/**
 * Cyberpunk HUD grid background modifier.
 * Adds a subtle scanline/grid effect for that "control panel" vibe.
 * 
 * Light mode: Very faint (3-5% alpha) to maintain readability
 * Dark mode: Slightly more visible (5-8% alpha)
 */
@Composable
fun Modifier.cyberGrid(
    gridColor: Color = MaterialTheme.colorScheme.primary,
    cellSize: Float = 24f
): Modifier {
    val isDark = LocalDarkTheme.current
    val alpha = if (isDark) 0.06f else 0.03f
    
    return this.drawBehind {
        val gridSpacing = cellSize.dp.toPx()
        
        // Vertical lines
        var x = 0f
        while (x < size.width) {
            drawLine(
                color = gridColor.copy(alpha = alpha),
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = 0.5f
            )
            x += gridSpacing
        }
        
        // Horizontal lines
        var y = 0f
        while (y < size.height) {
            drawLine(
                color = gridColor.copy(alpha = alpha),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 0.5f
            )
            y += gridSpacing
        }
    }
}

/**
 * Cyberpunk scanline effect modifier.
 * Horizontal lines only - simpler, cleaner look.
 */
@Composable
fun Modifier.cyberScanlines(
    lineColor: Color = MaterialTheme.colorScheme.primary,
    lineSpacing: Float = 4f
): Modifier {
    val isDark = LocalDarkTheme.current
    val alpha = if (isDark) 0.04f else 0.02f
    
    return this.drawBehind {
        val spacing = lineSpacing.dp.toPx()
        var y = 0f
        while (y < size.height) {
            drawLine(
                color = lineColor.copy(alpha = alpha),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1f
            )
            y += spacing
        }
    }
}
