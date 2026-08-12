@file:Suppress("FunctionNaming")

package com.kingpaging.qwelcome.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kingpaging.qwelcome.R
import com.kingpaging.qwelcome.ui.theme.LocalCyberColors
import com.kingpaging.qwelcome.ui.theme.LocalDarkTheme

private val PanelShape = RoundedCornerShape(16.dp)
private const val NEON_EDGE_ALPHA = 0.35f

@Composable
fun NeonPanel(
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    content: @Composable ColumnScope.() -> Unit,
) {
    val isDark = LocalDarkTheme.current
    val colorScheme = MaterialTheme.colorScheme

    Column(
        modifier =
            modifier
                .neonPanelDecoration(isDark, colorScheme, accentColor)
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

private fun Modifier.neonPanelDecoration(
    isDark: Boolean,
    colorScheme: ColorScheme,
    accentColor: Color,
): Modifier =
    neonPanelShadow(isDark)
        .clip(PanelShape)
        .neonPanelBorder(isDark, colorScheme)
        .neonPanelBackground(isDark, colorScheme)
        .neonPanelTopEdge(isDark, accentColor, colorScheme.secondary)

private fun Modifier.neonPanelShadow(isDark: Boolean): Modifier =
    if (isDark) {
        this
    } else {
        shadow(elevation = 2.dp, shape = PanelShape, clip = false)
    }

private fun Modifier.neonPanelBorder(
    isDark: Boolean,
    colorScheme: ColorScheme,
): Modifier =
    if (isDark) {
        border(1.dp, colorScheme.outline.copy(alpha = 0.10f), PanelShape)
    } else {
        border(0.5.dp, colorScheme.outline, PanelShape)
    }

private fun Modifier.neonPanelBackground(
    isDark: Boolean,
    colorScheme: ColorScheme,
): Modifier =
    if (isDark) {
        background(
            Brush.linearGradient(
                listOf(
                    colorScheme.secondary.copy(alpha = 0.10f),
                    colorScheme.primary.copy(alpha = 0.06f),
                    colorScheme.tertiary.copy(alpha = 0.08f),
                ),
            ),
        )
    } else {
        background(colorScheme.surface)
    }

private fun Modifier.neonPanelTopEdge(
    isDark: Boolean,
    accentColor: Color,
    secondaryColor: Color,
): Modifier =
    drawBehind {
        val edgeColors =
            if (isDark) {
                listOf(
                    accentColor.copy(alpha = 0f),
                    accentColor.copy(alpha = 0.25f),
                    secondaryColor.copy(alpha = 0.20f),
                    accentColor.copy(alpha = 0f),
                )
            } else {
                listOf(
                    accentColor.copy(alpha = 0f),
                    accentColor.copy(alpha = NEON_EDGE_ALPHA),
                    secondaryColor.copy(alpha = NEON_EDGE_ALPHA * 0.8f),
                    accentColor.copy(alpha = 0f),
                )
            }
        drawLine(
            brush = Brush.horizontalGradient(colors = edgeColors),
            start = Offset(16.dp.toPx(), 0f),
            end = Offset(size.width - 16.dp.toPx(), 0f),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }

@Composable
fun NeonWarningBanner(
    text: String,
    modifier: Modifier = Modifier,
    onDismiss: (() -> Unit)? = null,
) {
    val cyberColors = LocalCyberColors.current

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = cyberColors.warning,
        contentColor = cyberColors.onWarning,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = stringResource(R.string.content_desc_warning),
                tint = cyberColors.onWarning,
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = cyberColors.onWarning,
                modifier = Modifier.weight(1f),
            )
            if (onDismiss != null) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.action_dismiss),
                        tint = cyberColors.onWarning,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}
