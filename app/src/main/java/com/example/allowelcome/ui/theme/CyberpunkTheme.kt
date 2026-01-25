package com.example.allowelcome.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ============== DARK MODE PALETTE ==============
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

// ============== LIGHT MODE PALETTE ==============
// Light mode with cyberpunk aesthetic - softer but still futuristic
private val CyberLightBg = Color(0xFFF5F3F8)          // Light grayish purple
private val CyberLightCyan = Color(0xFF0097A7)        // Deeper cyan for readability
private val CyberLightMagenta = Color(0xFFD81B60)     // Deeper magenta
private val CyberLightPurple = Color(0xFF7C4DFF)      // Vibrant purple
private val CyberLightSurface = Color(0xFFFFFFFF)     // White surface
private val CyberLightSurface2 = Color(0xFFE8E4EE)    // Light purple tint
private val CyberLightText = Color(0xFF1A1A2E)        // Dark text
private val CyberLightMuted = Color(0xFF5C5C7A)       // Muted text

val CyberDarkScheme = darkColorScheme(
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

val CyberLightScheme = lightColorScheme(
    primary = CyberLightCyan,
    secondary = CyberLightMagenta,
    tertiary = CyberLightPurple,

    background = CyberLightBg,
    surface = CyberLightSurface,
    surfaceVariant = CyberLightSurface2,

    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,

    onBackground = CyberLightText,
    onSurface = CyberLightText,
    onSurfaceVariant = CyberLightMuted,

    error = Color(0xFFB00020)
)

// Keep CyberScheme as alias for backward compatibility (defaults to dark)
val CyberScheme: ColorScheme
    @Composable
    get() = if (LocalDarkTheme.current) CyberDarkScheme else CyberLightScheme

// Composition local for dark theme state
val LocalDarkTheme = staticCompositionLocalOf { true }

@Composable
fun CyberpunkTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) CyberDarkScheme else CyberLightScheme

    CompositionLocalProvider(LocalDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = CyberTypography,
            content = content
        )
    }
}
