package com.example.allowelcome.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Cyberpunk Theme Color Reference
 * ================================
 * 
 * This file provides additional semantic color constants for use throughout the app.
 * Primary color definitions are in CyberpunkTheme.kt.
 * 
 * DARK MODE PALETTE (Neon Cyberpunk):
 * - Primary:   Cyan (#00E5FF) - Electric neon blue
 * - Secondary: Magenta (#FF2BD6) - Hot pink neon
 * - Tertiary:  Purple (#A371F7) - Soft purple accent
 * - Background: Deep space black (#05030A)
 * - Surface: Dark purple (#120A1C)
 * 
 * LIGHT MODE PALETTE (High Contrast Cyberpunk):
 * - Primary:   Deep Teal (#00838F) - Readable cyan variant
 * - Secondary: Deep Magenta (#C2185B) - Readable pink variant
 * - Tertiary:  Deep Purple (#6200EA) - Vibrant purple
 * - Background: Light purple tint (#FAF8FC)
 * - Surface: Pure white (#FFFFFF)
 * 
 * Usage: Use CyberScheme composable getter to get theme-aware colors,
 * or access CyberDarkScheme/CyberLightScheme directly when needed.
 */

// Semantic status colors (can be used directly when needed)
object CyberColors {
    // Success states
    val SuccessDark = Color(0xFF00E676)   // Neon green
    val SuccessLight = Color(0xFF2E7D32)  // Forest green
    
    // Warning states
    val WarningDark = Color(0xFFFFD600)   // Bright yellow
    val WarningLight = Color(0xFFE65100)  // Deep orange
    
    // Info states (uses primary color typically)
    val InfoDark = Color(0xFF00E5FF)      // Cyan
    val InfoLight = Color(0xFF00838F)     // Deep teal
    
    // Accent lime (for special highlights)
    val LimeDark = Color(0xFF9BFF00)      // Neon lime
    val LimeLight = Color(0xFF558B2F)     // Dark lime
}