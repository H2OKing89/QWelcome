package com.kingpaging.qwelcome.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.dp
import com.kingpaging.qwelcome.ui.theme.*

/**
 * Cyberpunk-styled backdrop with a subtle grid effect.
 * Adapts to dark/light theme with appropriate styling:
 * - Dark mode: Deep space gradient with subtle grid
 * - Light mode: Clean white/purple gradient with subtle grid
 */
@Composable
fun CyberpunkBackdrop(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val isDark = LocalDarkTheme.current
    val colorScheme = MaterialTheme.colorScheme

    Box(modifier = modifier.fillMaxSize()) {
        // Background gradient layer
        Canvas(Modifier.matchParentSize()) {
            if (isDark) {
                // Dark mode: Deep space gradient (purple-black)
                drawRect(
                    brush =
                        Brush.radialGradient(
                            colors =
                                listOf(
                                    Color(0xFF120A1C), // Dark purple center
                                    Color(0xFF05030A), // Near black edges
                                ),
                            center = center,
                            radius = size.maxDimension * 0.85f,
                        ),
                )
            } else {
                // Light mode: Clean white with subtle purple tint
                drawRect(
                    brush =
                        Brush.radialGradient(
                            colors =
                                listOf(
                                    Color(0xFFFFFFFF), // Pure white center
                                    Color(0xFFF5F0FA), // Subtle purple tint edges
                                ),
                            center = center,
                            radius = size.maxDimension * 0.85f,
                        ),
                )
            }

            // Cyberpunk grid overlay
            val spacing = GRID_SPACING_DP.toPx()
            val gridColor =
                if (isDark) {
                    Color.White.copy(alpha = 0.04f)
                } else {
                    colorScheme.primary.copy(alpha = 0.04f) // Theme-aware grid for light mode
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

        // App content
        content()
    }
}

/**
 * Visual tuning constants for cyberpunk backdrop effects.
 * Centralized for easier adjustment of grid spacing, line sizes, and animation speed.
 */
private val GRID_SPACING_DP = 28.dp
