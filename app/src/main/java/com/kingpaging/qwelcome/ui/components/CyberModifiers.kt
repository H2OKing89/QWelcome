package com.kingpaging.qwelcome.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kingpaging.qwelcome.ui.theme.LocalDarkTheme
import kotlin.math.max

@Composable
fun Modifier.cyberGrid(
    gridColor: Color = MaterialTheme.colorScheme.primary,
    cellSize: Dp = 24.dp,
): Modifier {
    val isDark = LocalDarkTheme.current
    val alpha = if (isDark) 0.06f else 0.03f

    return this.drawBehind {
        val gridSpacing = max(1f, cellSize.toPx())

        var x = 0f
        while (x < size.width) {
            drawLine(
                color = gridColor.copy(alpha = alpha),
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = 0.5f,
            )
            x += gridSpacing
        }

        var y = 0f
        while (y < size.height) {
            drawLine(
                color = gridColor.copy(alpha = alpha),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 0.5f,
            )
            y += gridSpacing
        }
    }
}

@Composable
fun Modifier.cyberScanlines(
    lineColor: Color = MaterialTheme.colorScheme.primary,
    lineSpacing: Dp = 4.dp,
): Modifier {
    val isDark = LocalDarkTheme.current
    val alpha = if (isDark) 0.04f else 0.02f

    return this.drawBehind {
        val spacing = max(1f, lineSpacing.toPx())
        var y = 0f
        while (y < size.height) {
            drawLine(
                color = lineColor.copy(alpha = alpha),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1f,
            )
            y += spacing
        }
    }
}
