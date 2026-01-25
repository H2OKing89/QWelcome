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
    Box(modifier = modifier.fillMaxSize()) {
        // Deep space gradient background
        Canvas(Modifier.matchParentSize()) {
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

            // Subtle grid
            val spacing = 28.dp.toPx()
            val gridColor = Color.White.copy(alpha = 0.04f)
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
        ScanlineOverlay(Modifier.matchParentSize())

        // Your app UI
        content()
    }
}

@Composable
private fun ScanlineOverlay(modifier: Modifier = Modifier) {
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

    Canvas(modifier) {
        val lineH = 4.dp.toPx()
        val yPx = size.height * y

        val brush = Brush.horizontalGradient(
            listOf(
                Color.Transparent,
                CyberScheme.primary.copy(alpha = 0.40f),
                Color.Transparent
            )
        )

        drawRect(
            brush = brush,
            topLeft = Offset(0f, yPx),
            size = Size(size.width, lineH),
            alpha = 0.55f
        )
    }
}
