# ALLOWelcome Light Theme Reference

A comprehensive guide to the Cyberpunk Light Theme implementation in our Android Jetpack Compose app.

---

## 🎨 Design Philosophy

**Goal:** Maintain cyberpunk aesthetic in light mode while prioritizing readability and contrast.

**The Hard Truth:** If you keep the same neon intensity as dark mode in light mode, it becomes a "highlighter explosion" and readability dies. The solution: *keep the cyberpunk roles and shapes, but swap glow-for-shadow and neon-for-deep-saturated inks.*

**Approach:**

- Replace neon glows with subtle drop shadows
- Use deep, saturated variants of cyberpunk colors (not pastels)
- Clean white surfaces with **signature cyberpunk motifs** (neon edge, grid, etc.)
- Keep the same color roles (cyan=primary, magenta=secondary, purple=tertiary)

---

## 🎯 Cyberpunk Light Mode Motifs

**This is where cyberpunk lives in light mode: motifs, not darkness.**

The theme doesn't feel "plain" because of colors—it's the **surface treatment** that defines cyberpunk. We use these signature elements:

### 1. Neon Top-Edge Accent (Primary Motif)

Every `NeonPanel` has a 2dp gradient line across the top with rounded ends (`StrokeCap.Round`):

```kotlin
// Neon gradient edge at ~35% alpha with rounded caps
drawLine(
    brush = Brush.horizontalGradient(
        colors = listOf(
            accentColor.copy(alpha = 0f),
            accentColor.copy(alpha = 0.35f),
            secondary.copy(alpha = 0.28f),
            accentColor.copy(alpha = 0f)
        )
    ),
    start = Offset(16.dp, 0f),
    end = Offset(width - 16.dp, 0f),
    strokeWidth = 2.dp,
    cap = StrokeCap.Round  // Intentional, polished look
)
```

### 2. HUD Grid Background (Optional)

For screens that need more "control panel" vibe:

```kotlin
// Usage: Modifier.cyberGrid()
// Very faint grid lines (3% alpha in light mode)
```

### 3. Scanlines (Optional)

Horizontal lines only—simpler, cleaner:

```kotlin
// Usage: Modifier.cyberScanlines()
// Subtle horizontal lines (2% alpha in light mode)
```

### 4. Precision Over Puffiness

- **Thin lines** (0.5dp borders, not thick frames)
- **Sharp highlights** (neon edge, not full rectangle border)
- **One bold focal element** per section (the PRIMARY button)

---

## 📁 File Structure

```text
app/src/main/java/com/example/allowelcome/ui/
├── theme/
│   ├── CyberpunkTheme.kt    # Theme setup, color schemes, CompositionLocals
│   ├── Color.kt             # Semantic color constants
│   └── Type.kt              # Typography with theme-aware shadows
└── components/
    └── NeonComponents.kt    # Reusable UI components (panels, buttons, fields)
```

---

## 🎨 Light Mode Color Palette

### Primary Colors (CyberpunkTheme.kt)

```kotlin
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
```

### Semantic Status Colors

```kotlin
private val CyberErrorLight = Color(0xFFBA1A1A)        // Deep red
private val CyberSuccessLight = Color(0xFF2E7D32)      // Forest green
private val CyberWarningLight = Color(0xFFE65100)      // Deep orange
```

---

## 🎨 Complete Light Color Scheme

```kotlin
val CyberLightScheme = lightColorScheme(
    // PRIMARY (Deep Teal - main actions)
    primary = CyberLightCyan,            // #00838F
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB2EBF2),
    onPrimaryContainer = Color(0xFF002022),
    
    // SECONDARY (Deep Magenta - alternate actions)
    secondary = CyberLightMagenta,       // #C2185B
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFCE4EC),
    onSecondaryContainer = Color(0xFF31111D),
    
    // TERTIARY (Deep Purple - accents)
    tertiary = CyberLightPurple,         // #6200EA
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE8DEF8),
    onTertiaryContainer = Color(0xFF21005D),

    // BACKGROUND & SURFACE
    background = CyberLightBg,           // #FAF8FC (light purple tint)
    onBackground = CyberLightText,       // #1C1B1F
    
    surface = CyberLightSurface,         // #FFFFFF
    onSurface = CyberLightText,
    surfaceVariant = CyberLightSurface2, // #F3EDF7
    onSurfaceVariant = CyberLightMuted,  // #49454F
    surfaceContainerHighest = Color(0xFFE6E0E9),
    surfaceContainerHigh = CyberLightSurfaceContainer,
    surfaceContainer = Color(0xFFF3EDF7),
    surfaceContainerLow = CyberLightSurface,
    surfaceContainerLowest = Color.White,
    
    // OUTLINE
    outline = Color(0xFF79747E),
    outlineVariant = Color(0xFFCAC4D0),
    
    // ERROR
    error = CyberErrorLight,             // #BA1A1A
    onError = Color.White,
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    
    // INVERSE
    inverseSurface = Color(0xFF313033),
    inverseOnSurface = Color(0xFFF4EFF4),
    inversePrimary = CyberCyan,          // Original neon cyan for inverse
    
    scrim = Color.Black
)
```

---

## ✍️ Typography (Type.kt)

### Fonts Used

```kotlin
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
```

### Light Mode Shadow Strategy

**Key Decision:** Light mode gets NO text shadows on body/labels - only headers.

```kotlin
// Light mode: subtle drop shadow (NOT glow)
private val CyanGlowLight = Shadow(
    color = Color(0xFF0097A7).copy(alpha = 0.3f),
    offset = Offset(1f, 2f),  // Offset for drop shadow effect
    blurRadius = 4f           // Much smaller than dark mode's 12f
)
```

### Theme-Aware Typography Function

```kotlin
@Composable
fun cyberTypography(isDark: Boolean = LocalDarkTheme.current): Typography {
    // Light mode: Only headlineLarge gets a very subtle shadow
    // Dark mode: Headers get vibrant neon glows
    val headlineGlow = if (isDark) CyanGlowDark else null  // null = no shadow
    val titleGlow = if (isDark) CyanGlowDark else null
    val accentGlow = if (isDark) MagentaGlowDark else null
    
    return Typography(
        headlineLarge = CyberTypography.headlineLarge.copy(shadow = headlineGlow),
        titleLarge = CyberTypography.titleLarge.copy(shadow = titleGlow),
        titleMedium = CyberTypography.titleMedium.copy(shadow = accentGlow),
        // Body and label styles have NO shadows in either mode
        bodyLarge = CyberTypography.bodyLarge,
        bodyMedium = CyberTypography.bodyMedium,
        labelLarge = CyberTypography.labelLarge,
        // ... etc
    )
}
```

---

## 🧩 Component Light Mode Styling

### NeonPanel (Card Container with Neon Edge)

```kotlin
@Composable
fun NeonPanel(
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = LocalDarkTheme.current
    val colorScheme = MaterialTheme.colorScheme

    Column(
        modifier = modifier
            .clip(PanelShape)
            .then(
                if (isDark) {
                    Modifier.border(1.dp, Color.White.copy(alpha = 0.10f), PanelShape)
                } else {
                    // LIGHT MODE: clean shadow, very thin border (0.5dp max)
                    Modifier
                        .shadow(elevation = 2.dp, shape = PanelShape)
                        .border(0.5.dp, colorScheme.outlineVariant.copy(alpha = 0.15f), PanelShape)
                }
            )
            .background(if (isDark) gradientBrush else solidWhite)
            // CYBERPUNK SIGNATURE: Neon top-edge accent line
            .drawBehind {
                if (!isDark) {
                    drawLine(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                accentColor.copy(alpha = 0f),
                                accentColor.copy(alpha = 0.35f),
                                colorScheme.secondary.copy(alpha = 0.28f),
                                accentColor.copy(alpha = 0f)
                            )
                        ),
                        start = Offset(16.dp.toPx(), 0f),
                        end = Offset(size.width - 16.dp.toPx(), 0f),
                        strokeWidth = 2.dp.toPx()
                    )
                }
            }
            .padding(16.dp),
        // ...
    )
}
```

### NeonOutlinedField (Text Input)

```kotlin
@Composable
fun NeonOutlinedField(/* params */) {
    val isDark = LocalDarkTheme.current
    
    OutlinedTextField(
        // ...
        colors = OutlinedTextFieldDefaults.colors(
            // Focused states - same in both modes
            focusedBorderColor = colorScheme.primary,
            focusedLabelColor = colorScheme.primary,
            cursorColor = colorScheme.primary,
            
            // UNFOCUSED - different per mode
            unfocusedBorderColor = if (isDark) {
                Color.White.copy(alpha = 0.18f)
            } else {
                colorScheme.outline.copy(alpha = 0.6f)  // Subtle gray outline
            },
            
            // Container background
            focusedContainerColor = if (isDark) Color.Transparent else colorScheme.surface,
            unfocusedContainerColor = if (isDark) Color.Transparent else colorScheme.surface,
            // ...
        )
    )
}
```

### NeonButton (Button Hierarchy)

We use 3 button styles for visual hierarchy:

```kotlin
enum class NeonButtonStyle {
    PRIMARY,    // Filled button - main action (solid fill, elevation)
    SECONDARY,  // Outlined button - important alternate (border only)
    TERTIARY    // Text/Tonal button - less prominent (subtle or no border)
}
```

#### Light Mode Button Styling (DECISIVE Hierarchy)

```kotlin
// Container: PRIMARY = solid fill, others = transparent/subtle
val containerColor = when (style) {
    NeonButtonStyle.PRIMARY -> if (isDark) {
        glowColor.copy(alpha = 0.15f)
    } else {
        glowColor  // Solid fill - shape + elevation do the work
    }
    NeonButtonStyle.SECONDARY -> Color.Transparent
    NeonButtonStyle.TERTIARY -> if (isDark) Color.Transparent else surfaceVariant.copy(alpha = 0.5f)
}

// Content: Light PRIMARY = white text (onPrimary)
val contentColor = when (style) {
    NeonButtonStyle.PRIMARY -> if (isDark) glowColor else colorScheme.onPrimary
    NeonButtonStyle.SECONDARY -> glowColor
    NeonButtonStyle.TERTIARY -> glowColor.copy(alpha = if (isDark) 0.8f else 1f)
}

// Border: Light mode thinner (0.5dp max unless deliberate)
val borderWidth = when (style) {
    NeonButtonStyle.PRIMARY -> if (isDark) 1.dp else 0.dp  // NO border on filled
    NeonButtonStyle.SECONDARY -> 1.dp  // Deliberate outline
    NeonButtonStyle.TERTIARY -> 0.dp   // Text button - no border
}

// Disabled states: readable but clearly inactive
disabledContainerColor = when (style) {
    NeonButtonStyle.PRIMARY -> if (isDark) glowColor.copy(alpha = 0.08f) 
                               else colorScheme.onSurface.copy(alpha = 0.12f)
    NeonButtonStyle.TERTIARY -> surfaceVariant.copy(alpha = 0.3f)
    else -> Color.Transparent
}
disabledContentColor = if (isDark) glowColor.copy(alpha = 0.38f) 
                       else colorScheme.onSurface.copy(alpha = 0.38f)
```

---

## 🏗️ Theme Architecture

### CompositionLocals

```kotlin
// Check current theme mode
val LocalDarkTheme = staticCompositionLocalOf { true }

// Access extended colors (success, warning, lime - not in Material3)
val LocalCyberColors = staticCompositionLocalOf { DarkExtendedColors }
```

### Extended Colors (Non-Material3)

```kotlin
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

private val LightExtendedColors = CyberExtendedColors(
    success = CyberSuccessLight,      // #2E7D32 (forest green)
    onSuccess = Color.White,
    successContainer = Color(0xFFC8E6C9),
    warning = CyberWarningLight,      // #E65100 (deep orange)
    onWarning = Color.White,
    warningContainer = Color(0xFFFFE0B2),
    info = CyberLightCyan,            // #00838F
    lime = CyberLightLime             // #558B2F
)
```

### Main Theme Composable

```kotlin
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
```

---

## ✅ Usage Examples

### Accessing Colors in Components

```kotlin
@Composable
fun MyComponent() {
    // Standard Material3 colors (PREFERRED)
    val primary = MaterialTheme.colorScheme.primary
    val surface = MaterialTheme.colorScheme.surface
    val error = MaterialTheme.colorScheme.error
    
    // Extended colors (for success, warning, etc.)
    val success = LocalCyberColors.current.success
    val warning = LocalCyberColors.current.warning
    
    // Check if dark mode
    val isDark = LocalDarkTheme.current
}
```

### Button Hierarchy Example

```kotlin
// Screen with proper button hierarchy
Column {
    // PRIMARY - main action (solid cyan button)
    NeonButton(
        onClick = { /* send SMS */ },
        style = NeonButtonStyle.PRIMARY,
        glowColor = MaterialTheme.colorScheme.primary
    ) {
        Text("Send SMS")
    }
    
    // SECONDARY - important alternate (outlined magenta)
    NeonButton(
        onClick = { /* share */ },
        style = NeonButtonStyle.SECONDARY,
        glowColor = MaterialTheme.colorScheme.secondary
    ) {
        Text("Share")
    }
    
    // TERTIARY - less prominent (subtle outlined purple)
    NeonButton(
        onClick = { /* show QR */ },
        style = NeonButtonStyle.TERTIARY,
        glowColor = MaterialTheme.colorScheme.tertiary
    ) {
        Text("Show QR")
    }
}
```

---

## 🔑 Key Light Mode Decisions

| Element | Dark Mode | Light Mode | Reason |
| ------- | --------- | ---------- | ------ |
| **Text shadows** | Neon glow (blur=12f) | None | Glows look muddy on white |
| **Panel background** | Gradient | Solid white + neon edge | Clean surface, cyberpunk motif |
| **Panel border** | 1dp white@10% | 0.5dp @15% (thinner!) | Precision over puffiness |
| **Primary button** | Semi-transparent + glow | Solid fill + elevation | Shape does the work |
| **Secondary button** | 1dp animated border | 1dp static border | Deliberate outline |
| **Tertiary button** | 0.5dp border | No border, tonal bg | Text button style |
| **Colors** | Neon (#00E5FF) | Deep (#00838F) | High contrast for readability |

---

## 📐 Typography Shadow Rules (Explicit)

**Body/Labels:** NEVER have shadows in either mode

**Titles (titleLarge, titleMedium):** Dark mode only (neon glow)

**Headlines (headlineLarge):** Dark mode only (neon glow)

Light mode typography gets its "cyberpunk" from the **Orbitron/Exo2 fonts** and **colors**, not shadows.

---

## 📱 Color Comparison

| Role | Dark Mode | Light Mode |
| ---- | --------- | ---------- |
| Primary (Cyan) | `#00E5FF` (Neon) | `#00838F` (Deep Teal) |
| Secondary (Magenta) | `#FF2BD6` (Hot Pink) | `#C2185B` (Deep Magenta) |
| Tertiary (Purple) | `#A371F7` (Soft Purple) | `#6200EA` (Deep Purple) |
| Background | `#05030A` (Space Black) | `#FAF8FC` (Light Purple Tint) |
| Surface | `#120A1C` (Dark Purple) | `#FFFFFF` (White) |
| Text | `#F0F5FF` (Blue-White) | `#1C1B1F` (Near Black) |
| Error | `#FF4D6D` (Neon Red) | `#BA1A1A` (Deep Red) |
| Success | `#00E676` (Neon Green) | `#2E7D32` (Forest Green) |

---

## 🎯 Summary: What Makes It Cyberpunk in Light Mode

1. **Neon top-edge accent** on panels (the signature motif)
2. **Deep saturated colors** instead of neon (readable but vibrant)
3. **Precision styling** - thin lines, sharp highlights, not puffy borders
4. **Orbitron + Exo2 fonts** - techy typography
5. **Optional HUD grid/scanlines** for screens that need more "control panel" vibe
6. **Decisive button hierarchy** - PRIMARY pops, others recede

The cyberpunk is in the **system**, not just the colors.

---

- *Last updated: January 25, 2026*
