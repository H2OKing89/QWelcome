package com.kingpaging.qwelcome.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * CYBERPUNK THEME ARCHITECTURE
 * ============================
 * 
 * This theme follows Android/Material3 best practices:
 * 
 * 1. COLOR ACCESS HIERARCHY:
 *    - MaterialTheme.colorScheme.primary (PREFERRED - standard Material3 colors)
 *    - LocalCyberColors.current.success (EXTENDED - non-Material colors like success/warning)
 *    - LocalDarkTheme.current (CHECK - when you need to know the theme mode)
 * 
 * 2. WHAT'S IN MaterialTheme.colorScheme (USE THESE):
 *    - primary/onPrimary, secondary/onSecondary, tertiary/onTertiary
 *    - background/onBackground, surface/onSurface
 *    - error/onError, outline, scrim, etc.
 * 
 * 3. WHAT'S IN LocalCyberColors.current (CUSTOM EXTRAS):
 *    - success/onSuccess, warning/onWarning (not in Material3)
 *    - lime, info (brand-specific neon colors)
 * 
 * MIGRATION NOTE: CyberScheme is deprecated - use MaterialTheme.colorScheme instead.
 */

// ============== DARK MODE PALETTE ==============
// Palette pulled from your HTML theme - neon cyberpunk aesthetic
private val CyberBg = Color(0xFF05030A)        // --bg (deep space black)
private val CyberCyan = Color(0xFF00E5FF)      // --cyan (electric cyan)
private val CyberMagenta = Color(0xFFFF2BD6)   // --magenta (hot pink)
private val CyberPurple = Color(0xFFA371F7)    // --purple (soft purple)
private val CyberLime = Color(0xFF9BFF00)      // --lime (neon green - for accents)

private val CyberSurface = Color(0xFF120A1C)   // --bg-secondary (dark purple tint)
private val CyberSurface2 = Color(0xFF1E1030)  // --bg-tertiary (lighter purple)
private val CyberSurfaceContainer = Color(0xFF1A0F28) // For elevated surfaces

private val CyberText = Color(0xFFF0F5FF)      // Light blue-white text
private val CyberMuted = Color(0xFFBEC8DC)     // Muted text

// ============== LIGHT MODE PALETTE ==============
// Light mode maintaining cyberpunk aesthetic - high contrast, vibrant but readable
private val CyberLightBg = Color(0xFFFAF8FC)           // Very light purple tint
private val CyberLightCyan = Color(0xFF00838F)         // Deep teal (high contrast cyan)
private val CyberLightMagenta = Color(0xFFC2185B)      // Deep magenta (readable pink)
private val CyberLightPurple = Color(0xFF6200EA)       // Deep purple (vibrant but readable)
private val CyberLightLime = Color(0xFF558B2F)         // Dark lime (readable green)

private val CyberLightSurface = Color(0xFFFFFFFF)      // Pure white cards
private val CyberLightSurface2 = Color(0xFFF3EDF7)     // Light purple tint surface
private val CyberLightSurfaceContainer = Color(0xFFEDE7F2) // Elevated surfaces

private val CyberLightText = Color(0xFF1C1B1F)         // Near black text
private val CyberLightMuted = Color(0xFF49454F)        // Muted gray-purple

// ============== ADDITIONAL SEMANTIC COLORS ==============
// For both themes - status and feedback colors
private val CyberErrorDark = Color(0xFFFF4D6D)         // Neon red-pink
private val CyberErrorLight = Color(0xFFBA1A1A)        // Deep red
private val CyberSuccessDark = Color(0xFF00E676)       // Neon green
private val CyberSuccessLight = Color(0xFF2E7D32)      // Forest green
private val CyberWarningDark = Color(0xFFFFD600)       // Bright yellow
private val CyberWarningLight = Color(0xFFE65100)      // Deep orange

val CyberDarkScheme = darkColorScheme(
    primary = CyberCyan,
    onPrimary = Color(0xFF001014),
    primaryContainer = Color(0xFF004D5A),
    onPrimaryContainer = Color(0xFF97F0FF),
    
    secondary = CyberMagenta,
    onSecondary = Color(0xFF14000F),
    secondaryContainer = Color(0xFF5C1049),
    onSecondaryContainer = Color(0xFFFFD8EE),
    
    tertiary = CyberPurple,
    onTertiary = Color(0xFF080010),
    tertiaryContainer = Color(0xFF4A3362),
    onTertiaryContainer = Color(0xFFE9DDFF),

    background = CyberBg,
    onBackground = CyberText,
    
    surface = CyberSurface,
    onSurface = CyberText,
    surfaceVariant = CyberSurface2,
    onSurfaceVariant = CyberMuted,
    surfaceContainerHighest = CyberSurfaceContainer,
    surfaceContainerHigh = CyberSurface2,
    surfaceContainer = CyberSurface,
    surfaceContainerLow = Color(0xFF0D0812),
    surfaceContainerLowest = CyberBg,
    
    outline = Color(0xFF938F99),
    outlineVariant = Color(0xFF49454F),
    
    error = CyberErrorDark,
    onError = Color(0xFF140008),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    
    inverseSurface = CyberText,
    inverseOnSurface = CyberBg,
    inversePrimary = Color(0xFF006877),
    
    scrim = Color.Black
)

val CyberLightScheme = lightColorScheme(
    primary = CyberLightCyan,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB2EBF2),
    onPrimaryContainer = Color(0xFF002022),
    
    secondary = CyberLightMagenta,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFCE4EC),
    onSecondaryContainer = Color(0xFF31111D),
    
    tertiary = CyberLightPurple,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE8DEF8),
    onTertiaryContainer = Color(0xFF21005D),

    background = CyberLightBg,
    onBackground = CyberLightText,
    
    surface = CyberLightSurface,
    onSurface = CyberLightText,
    surfaceVariant = CyberLightSurface2,
    onSurfaceVariant = CyberLightMuted,
    surfaceContainerHighest = Color(0xFFE6E0E9),
    surfaceContainerHigh = CyberLightSurfaceContainer,
    surfaceContainer = Color(0xFFF3EDF7),
    surfaceContainerLow = CyberLightSurface,
    surfaceContainerLowest = Color.White,
    
    outline = Color(0xFF79747E),
    outlineVariant = Color(0xFFCAC4D0),
    
    error = CyberErrorLight,
    onError = Color.White,
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    
    inverseSurface = Color(0xFF313033),
    inverseOnSurface = Color(0xFFF4EFF4),
    inversePrimary = CyberCyan,
    
    scrim = Color.Black
)

// ============== EXTENDED COLORS ==============
// For colors not part of Material3's ColorScheme (success, warning, info, etc.)
// Access via LocalCyberColors.current
data class CyberExtendedColors(
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val warning: Color,
    val onWarning: Color,
    val warningContainer: Color,
    val info: Color,
    val lime: Color
)

private val DarkExtendedColors = CyberExtendedColors(
    success = CyberSuccessDark,
    onSuccess = Color(0xFF003910),
    successContainer = Color(0xFF005221),
    warning = CyberWarningDark,
    onWarning = Color(0xFF3D2600),
    warningContainer = Color(0xFF5C3D00),
    info = CyberCyan,
    lime = CyberLime
)

private val LightExtendedColors = CyberExtendedColors(
    success = CyberSuccessLight,
    onSuccess = Color.White,
    successContainer = Color(0xFFC8E6C9),
    warning = CyberWarningLight,
    onWarning = Color.White,
    warningContainer = Color(0xFFFFE0B2),
    info = CyberLightCyan,
    lime = CyberLightLime
)

val LocalCyberColors = compositionLocalOf<CyberExtendedColors> {
    error("No CyberExtendedColors provided. Ensure CyberpunkTheme is applied.")
}

// Composition local for dark theme state
val LocalDarkTheme = compositionLocalOf<Boolean> {
    error("No LocalDarkTheme provided. Ensure CyberpunkTheme is applied.")
}

/**
 * DEPRECATED: Use MaterialTheme.colorScheme instead.
 * Kept for backward compatibility during migration.
 * 
 * Example migration:
 *   Before: CyberScheme.primary
 *   After:  MaterialTheme.colorScheme.primary
 */
@Deprecated(
    message = "Use MaterialTheme.colorScheme instead for proper theme integration",
    replaceWith = ReplaceWith("MaterialTheme.colorScheme", "androidx.compose.material3.MaterialTheme")
)
val CyberScheme: ColorScheme
    @Composable
    get() = MaterialTheme.colorScheme

/**
 * Cyberpunk-themed Material3 theme that adapts to system dark/light mode.
 * 
 * Usage:
 *   - Standard colors: MaterialTheme.colorScheme.primary, .secondary, .surface, etc.
 *   - Extended colors: LocalCyberColors.current.success, .warning, .lime, etc.
 *   - Check dark mode: LocalDarkTheme.current
 * 
 * Dark mode: Neon glows, deep purple/black backgrounds, vibrant cyan/magenta accents
 * Light mode: High contrast, vibrant but readable colors, subtle shadows instead of glows
 */
@Composable
fun CyberpunkTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) CyberDarkScheme else CyberLightScheme
    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors
    val typography = cyberTypography(isDark = darkTheme)

    CompositionLocalProvider(
        LocalDarkTheme provides darkTheme,
        LocalCyberColors provides extendedColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            content = content
        )
    }
}
