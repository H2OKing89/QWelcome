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

@Composable
fun CyberpunkBackdrop(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = LocalDarkTheme.current

    Box(modifier = modifier.fillMaxSize()) {
        // Deep space gradient background (adapts to theme)
        Canvas(Modifier.matchParentSize()) {
            if (isDark) {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF120A1C),
                            Color(0xFF05030A)
                        ),
                        center = center,
                        radius = size.maxDimension * 0.85f
                    )
                )
            } else {
                // Light mode: subtle gradient background
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFFFFFF),
                            Color(0xFFF0EBF5)
                        ),
                        center = center,
                        radius = size.maxDimension * 0.85f
                    )
                )
            }

            // Subtle grid (lighter in light mode)
            val spacing = 28.dp.toPx()
            val gridColor = if (isDark) {
                Color.White.copy(alpha = 0.04f)
            } else {
                Color(0xFF7C4DFF).copy(alpha = 0.06f) // Purple tint for light mode
            }
            for (x in 0..(size.width / spacing).toInt()) {
                val px = x * spacing
                drawLine(gridColor, Offset(px, 0f), Offset(px, size.height), strokeWidth = 1f)
            }
            for (y in 0..(size.height / spacing).toInt()) {
                val py = y * spacing
                drawLine(gridColor, Offset(0f, py), Offset(size.width, py), strokeWidth = 1f)
            }
        }

        // Moving scanline overlay
        ScanlineOverlay(Modifier.matchParentSize(), isDark = isDark)

        // Your app UI
        content()
    }
}

@Composable
private fun ScanlineOverlay(modifier: Modifier = Modifier, isDark: Boolean = true) {
    val t = rememberInfiniteTransition(label = "scanline")
    val y by t.animateFloat(
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
        val lineH = 4.dp.toPx()
        val yPx = size.height * y

        val brush = Brush.horizontalGradient(
            listOf(
                Color.Transparent,
                colorScheme.primary.copy(alpha = if (isDark) 0.40f else 0.25f),
                Color.Transparent
            )
        )

        drawRect(
            brush = brush,
            topLeft = Offset(0f, yPx),
            size = Size(size.width, lineH),
            alpha = if (isDark) 0.55f else 0.35f
        )
    }
}
