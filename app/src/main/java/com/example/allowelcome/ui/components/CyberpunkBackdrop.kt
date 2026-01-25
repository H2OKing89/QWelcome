package com.example.allowelcome.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.dp
import com.example.allowelcome.ui.theme.*

/**
 * Cyberpunk-styled backdrop with animated grid and scanline effects.
 * Adapts to dark/light theme with appropriate styling:
 * - Dark mode: Deep space gradient with subtle grid, neon scanline
 * - Light mode: Clean white/purple gradient with subtle grid, softer scanline
 */
@Composable
fun CyberpunkBackdrop(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = LocalDarkTheme.current

    Box(modifier = modifier.fillMaxSize()) {
        // Background gradient layer
        Canvas(Modifier.matchParentSize()) {
            if (isDark) {
                // Dark mode: Deep space gradient (purple-black)
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF120A1C),  // Dark purple center
                            Color(0xFF05030A)   // Near black edges
                        ),
                        center = center,
                        radius = size.maxDimension * 0.85f
                    )
                )
            } else {
                // Light mode: Clean white with subtle purple tint
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFFFFFF),  // Pure white center
                            Color(0xFFF5F0FA)   // Subtle purple tint edges
                        ),
                        center = center,
                        radius = size.maxDimension * 0.85f
                    )
                )
            }

            // Cyberpunk grid overlay
            val spacing = 28.dp.toPx()
            val gridColor = if (isDark) {
                Color.White.copy(alpha = 0.04f)
            } else {
                Color(0xFF6200EA).copy(alpha = 0.04f) // Purple grid for light mode
            }
            
            // Vertical lines
            for (x in 0..(size.width / spacing).toInt()) {
                val px = x * spacing
                drawLine(gridColor, Offset(px, 0f), Offset(px, size.height), strokeWidth = 1f)
            }
            // Horizontal lines
            for (y in 0..(size.height / spacing).toInt()) {
                val py = y * spacing
                drawLine(gridColor, Offset(0f, py), Offset(size.width, py), strokeWidth = 1f)
            }
        }

        // Animated scanline overlay
        ScanlineOverlay(Modifier.matchParentSize(), isDark = isDark)

        // App content
        content()
    }
}

/**
 * Animated horizontal scanline that moves down the screen.
 * Creates a subtle futuristic effect.
 */
@Composable
private fun ScanlineOverlay(modifier: Modifier = Modifier, isDark: Boolean = true) {
    val infiniteTransition = rememberInfiniteTransition(label = "scanline")
    val yPosition by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scanlineY"
    )

    val colorScheme = CyberScheme

    Canvas(modifier) {
        val lineHeight = 4.dp.toPx()
        val yPx = size.height * yPosition

        // Gradient for scanline - fades at edges
        val brush = Brush.horizontalGradient(
            listOf(
                Color.Transparent,
                colorScheme.primary.copy(alpha = if (isDark) 0.40f else 0.20f),
                Color.Transparent
            )
        )

        drawRect(
            brush = brush,
            topLeft = Offset(0f, yPx),
            size = Size(size.width, lineHeight),
            alpha = if (isDark) 0.55f else 0.30f
        )
    }
}
