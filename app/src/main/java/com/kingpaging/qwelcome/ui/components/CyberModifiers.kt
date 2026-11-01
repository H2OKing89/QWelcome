package com.kingpaging.qwelcome.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Compatibility hooks for older callers; content now uses clean, untextured surfaces. */
@Suppress("UNUSED_PARAMETER")
@Composable
fun Modifier.cyberGrid(
    gridColor: Color = MaterialTheme.colorScheme.primary,
    cellSize: Dp = 24.dp,
): Modifier = this

@Suppress("UNUSED_PARAMETER")
@Composable
fun Modifier.cyberScanlines(
    lineColor: Color = MaterialTheme.colorScheme.primary,
    lineSpacing: Dp = 4.dp,
): Modifier = this
