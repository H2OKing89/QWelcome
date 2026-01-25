package com.example.allowelcome.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.allowelcome.R

val DisplayFont = FontFamily(
    Font(R.font.orbitron_regular, FontWeight.Normal),
    Font(R.font.orbitron_medium, FontWeight.Medium),
    Font(R.font.orbitron_bold, FontWeight.Bold)
)

val BodyFont = FontFamily(
    Font(R.font.exo2_regular, FontWeight.Normal),
    Font(R.font.exo2_medium, FontWeight.Medium),
    Font(R.font.exo2_bold, FontWeight.Bold)
)

// ============== DARK MODE GLOW EFFECTS ==============
// Neon glow shadows for dark mode - vibrant and visible against dark backgrounds
private val CyanGlowDark = Shadow(
    color = Color(0xFF00E5FF).copy(alpha = 0.6f),
    offset = Offset(0f, 0f),
    blurRadius = 12f
)

private val MagentaGlowDark = Shadow(
    color = Color(0xFFFF2BD6).copy(alpha = 0.5f),
    offset = Offset(0f, 0f),
    blurRadius = 10f
)

// ============== LIGHT MODE GLOW EFFECTS ==============
// Subtle drop shadows for light mode - maintains cyberpunk feel without overwhelming
private val CyanGlowLight = Shadow(
    color = Color(0xFF0097A7).copy(alpha = 0.3f),
    offset = Offset(1f, 2f),
    blurRadius = 4f
)

private val MagentaGlowLight = Shadow(
    color = Color(0xFFD81B60).copy(alpha = 0.25f),
    offset = Offset(1f, 2f),
    blurRadius = 4f
)

// Base typography without shadows (used as foundation)
val CyberTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = DisplayFont,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        letterSpacing = 1.2.sp
    ),
    titleLarge = TextStyle(
        fontFamily = DisplayFont,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        letterSpacing = 1.0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = DisplayFont,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        letterSpacing = 0.8.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = BodyFont,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = BodyFont,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp
    ),
    labelLarge = TextStyle(
        fontFamily = BodyFont,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        letterSpacing = 0.5.sp
    )
)

/**
 * Returns theme-aware typography with appropriate glow/shadow effects.
 * 
 * Dark mode: Vibrant neon glows on headers (headlineLarge, titleLarge, titleMedium)
 * Light mode: NO shadows on general typography - keeps it crisp and readable.
 *             The cyberpunk vibe comes from colors/panels, not blurry text.
 */
@Composable
fun cyberTypography(isDark: Boolean = LocalDarkTheme.current): Typography {
    // Light mode: Only headlineLarge gets a very subtle shadow (for Q WELCOME header)
    // Dark mode: Headers get neon glows
    val headlineGlow = if (isDark) CyanGlowDark else null
    val titleGlow = if (isDark) CyanGlowDark else null
    val accentGlow = if (isDark) MagentaGlowDark else null
    
    return Typography(
        headlineLarge = CyberTypography.headlineLarge.copy(shadow = headlineGlow),
        headlineMedium = CyberTypography.headlineMedium,
        headlineSmall = CyberTypography.headlineSmall,
        titleLarge = CyberTypography.titleLarge.copy(shadow = titleGlow),
        titleMedium = CyberTypography.titleMedium.copy(shadow = accentGlow),
        titleSmall = CyberTypography.titleSmall,
        bodyLarge = CyberTypography.bodyLarge,
        bodyMedium = CyberTypography.bodyMedium,
        bodySmall = CyberTypography.bodySmall,
        labelLarge = CyberTypography.labelLarge,
        labelMedium = CyberTypography.labelMedium,
        labelSmall = CyberTypography.labelSmall
    )
}
