package com.example.allowelcome.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Palette pulled from your HTML theme
private val CyberBg = Color(0xFF05030A)        // --bg
private val CyberCyan = Color(0xFF00E5FF)      // --cyan
private val CyberMagenta = Color(0xFFFF2BD6)   // --magenta
private val CyberPurple = Color(0xFFA371F7)    // --purple
private val CyberLime = Color(0xFF9BFF00)      // --lime

private val CyberSurface = Color(0xFF120A1C)   // close to --bg-secondary (opaque-ish)
private val CyberSurface2 = Color(0xFF1E1030)  // close to --bg-tertiary

private val CyberText = Color(0xFFF0F5FF)
private val CyberMuted = Color(0xFFBEC8DC)

val CyberScheme = darkColorScheme(
    primary = CyberCyan,
    secondary = CyberMagenta,
    tertiary = CyberPurple,

    background = CyberBg,
    surface = CyberSurface,
    surfaceVariant = CyberSurface2,

    onPrimary = Color(0xFF001014),
    onSecondary = Color(0xFF14000F),
    onTertiary = Color(0xFF080010),

    onBackground = CyberText,
    onSurface = CyberText,
    onSurfaceVariant = CyberMuted,

    error = Color(0xFFFF4D6D) // matches your --red vibe
)

@Composable
fun CyberpunkTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CyberScheme,
        typography = CyberTypography,
        content = content
    )
}
