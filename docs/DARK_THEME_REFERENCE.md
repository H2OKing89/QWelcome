# ALLOWelcome Dark Theme Reference

A comprehensive guide to the Cyberpunk Dark Theme implementation in our Android Jetpack Compose app.

---

## 🎨 Design Philosophy

**Goal:** Create an immersive neon cyberpunk experience with glowing elements against deep space backgrounds.

**The Vibe:** Think Blade Runner, Cyberpunk 2077, Tron - neon lights cutting through darkness. The dark theme is where the cyberpunk aesthetic lives most naturally.

**Approach:**

- **Neon glows** on text and button borders (cyan, magenta, purple)
- **Deep space backgrounds** (purples and blacks, never pure black)
- **Animated effects** (pulsing glows, moving scanlines, gradient borders)
- **Vibrant color accents** against muted dark surfaces
- **Semi-transparent overlays** for depth and layering

---

## 🌌 Signature Dark Mode Effects

**Dark mode is where cyberpunk shines - literally.**

### 1. Neon Text Glow

Headers and titles have vibrant glow shadows that make text appear to emit light:

```kotlin
// Cyan neon glow for headers
private val CyanGlowDark = Shadow(
    color = Color(0xFF00E5FF).copy(alpha = 0.6f),
    offset = Offset(0f, 0f),  // Centered glow (no directional shadow)
    blurRadius = 12f          // Wide blur for soft glow
)

// Magenta accent glow for section titles
private val MagentaGlowDark = Shadow(
    color = Color(0xFFFF2BD6).copy(alpha = 0.5f),
    offset = Offset(0f, 0f),
    blurRadius = 10f
)
```

### 2. Pulsing Button Borders

PRIMARY buttons have animated pulsing borders that breathe:

```kotlin
// Animation: 0.4 → 0.8 alpha over 1.5 seconds, reversing
val infiniteTransition = rememberInfiniteTransition(label = "neonPulse")
val glowAlpha by infiniteTransition.animateFloat(
    initialValue = 0.4f,
    targetValue = 0.8f,
    animationSpec = infiniteRepeatable(
        animation = tween(1500, easing = FastOutSlowInEasing),
        repeatMode = RepeatMode.Reverse
    ),
    label = "glowAlpha"
)
```

### 3. Outer Glow Effect on Buttons

Dark mode PRIMARY buttons have a soft outer glow drawn behind:

```kotlin
// Draw outer glow for PRIMARY buttons in dark mode
if (enabled && isDark && style == NeonButtonStyle.PRIMARY) {
    Modifier.drawBehind {
        drawRoundRect(
            color = glowColor.copy(alpha = glowAlpha * 0.25f),
            cornerRadius = CornerRadius(8.dp.toPx()),
            style = Stroke(width = 3.dp.toPx()),
            topLeft = Offset(-1.5.dp.toPx(), -1.5.dp.toPx()),
            size = size.copy(
                width = size.width + 3.dp.toPx(),
                height = size.height + 3.dp.toPx()
            )
        )
    }
}
```

### 4. Deep Space Background Gradient

The backdrop uses a radial gradient from dark purple to near-black:

```kotlin
// Dark mode: Deep space gradient (purple-black)
drawRect(
    brush = Brush.radialGradient(
        colors = listOf(
            Color(0xFF120A1C),  // Dark purple center
            Color(0xFF05030A)   // Near black edges
        ),
        center = center,
        radius = size.maxDimension * 0.85f
    )
)
```

### 5. Animated Scanline

A horizontal neon line that continuously moves down the screen:

```kotlin
// 8-second cycle, linear movement
val yPosition by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
        animation = tween(8000, easing = LinearEasing),
        repeatMode = RepeatMode.Restart
    ),
    label = "scanlineY"
)

// Scanline: 40% alpha primary color, 55% overall alpha
val brush = Brush.horizontalGradient(
    listOf(
        Color.Transparent,
        colorScheme.primary.copy(alpha = 0.40f),
        Color.Transparent
    )
)
drawRect(brush = brush, topLeft = Offset(0f, yPx), alpha = 0.55f)
```

### 6. Subtle Grid Overlay

Very faint white grid lines create a "control panel" feel:

```kotlin
// White grid at 4% alpha
val gridColor = Color.White.copy(alpha = 0.04f)
val spacing = 28.dp.toPx()

// Draw vertical and horizontal lines
for (x in 0..(size.width / spacing).toInt()) {
    val px = x * spacing
    drawLine(gridColor, Offset(px, 0f), Offset(px, size.height), strokeWidth = 1f)
}
```

---

## 📁 File Structure

```text
app/src/main/java/com/example/allowelcome/ui/
├── theme/
│   ├── CyberpunkTheme.kt    # Theme setup, color schemes, CompositionLocals
│   ├── Color.kt             # Semantic color constants
│   └── Type.kt              # Typography with neon glow shadows
└── components/
    ├── NeonComponents.kt    # Reusable UI components (panels, buttons, fields)
    ├── CyberpunkBackdrop.kt # Animated background with grid and scanlines
    └── GradientHeader.kt    # Q WELCOME header with gradient text
```

---

## 🎨 Dark Mode Color Palette

### Primary Colors (CyberpunkTheme.kt)

```kotlin
// ============== DARK MODE PALETTE ==============
// Palette pulled from HTML theme - neon cyberpunk aesthetic
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
```

### Semantic Status Colors

```kotlin
private val CyberErrorDark = Color(0xFFFF4D6D)     // Neon red-pink
private val CyberSuccessDark = Color(0xFF00E676)   // Neon green
private val CyberWarningDark = Color(0xFFFFD600)   // Bright yellow
```

---

## 🎨 Complete Dark Color Scheme

```kotlin
val CyberDarkScheme = darkColorScheme(
    // PRIMARY (Electric Cyan - main actions, neon glow)
    primary = CyberCyan,                 // #00E5FF
    onPrimary = Color(0xFF001014),       // Near-black for contrast
    primaryContainer = Color(0xFF004D5A),
    onPrimaryContainer = Color(0xFF97F0FF),
    
    // SECONDARY (Hot Pink/Magenta - alternate actions)
    secondary = CyberMagenta,            // #FF2BD6
    onSecondary = Color(0xFF14000F),
    secondaryContainer = Color(0xFF5C1049),
    onSecondaryContainer = Color(0xFFFFD8EE),
    
    // TERTIARY (Soft Purple - accents)
    tertiary = CyberPurple,              // #A371F7
    onTertiary = Color(0xFF080010),
    tertiaryContainer = Color(0xFF4A3362),
    onTertiaryContainer = Color(0xFFE9DDFF),

    // BACKGROUND & SURFACE (Deep Space)
    background = CyberBg,                // #05030A (near black)
    onBackground = CyberText,            // #F0F5FF (blue-white)
    
    surface = CyberSurface,              // #120A1C (dark purple)
    onSurface = CyberText,
    surfaceVariant = CyberSurface2,      // #1E1030 (lighter purple)
    onSurfaceVariant = CyberMuted,       // #BEC8DC (muted text)
    surfaceContainerHighest = CyberSurfaceContainer,
    surfaceContainerHigh = CyberSurface2,
    surfaceContainer = CyberSurface,
    surfaceContainerLow = Color(0xFF0D0812),
    surfaceContainerLowest = CyberBg,
    
    // OUTLINE (Muted for dark backgrounds)
    outline = Color(0xFF938F99),
    outlineVariant = Color(0xFF49454F),
    
    // ERROR (Neon Red-Pink)
    error = CyberErrorDark,              // #FF4D6D
    onError = Color(0xFF140008),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    
    // INVERSE (For light-on-dark elements)
    inverseSurface = CyberText,
    inverseOnSurface = CyberBg,
    inversePrimary = Color(0xFF006877),
    
    scrim = Color.Black
)
```

---

## ✍️ Typography (Type.kt)

### Fonts Used

```kotlin
// Display font: Futuristic, techy headers
val DisplayFont = FontFamily(
    Font(R.font.orbitron_regular, FontWeight.Normal),
    Font(R.font.orbitron_medium, FontWeight.Medium),
    Font(R.font.orbitron_bold, FontWeight.Bold)
)

// Body font: Clean, readable body text
val BodyFont = FontFamily(
    Font(R.font.exo2_regular, FontWeight.Normal),
    Font(R.font.exo2_medium, FontWeight.Medium),
    Font(R.font.exo2_bold, FontWeight.Bold)
)
```

### Dark Mode Neon Glows

```kotlin
// Cyan glow: Wide blur, high alpha, centered
private val CyanGlowDark = Shadow(
    color = Color(0xFF00E5FF).copy(alpha = 0.6f),
    offset = Offset(0f, 0f),
    blurRadius = 12f
)

// Magenta glow: Slightly tighter for accent text
private val MagentaGlowDark = Shadow(
    color = Color(0xFFFF2BD6).copy(alpha = 0.5f),
    offset = Offset(0f, 0f),
    blurRadius = 10f
)
```

### Which Text Gets Glows

```kotlin
@Composable
fun cyberTypography(isDark: Boolean = LocalDarkTheme.current): Typography {
    // Dark mode: Headers get neon glows
    val headlineGlow = if (isDark) CyanGlowDark else null
    val titleGlow = if (isDark) CyanGlowDark else null
    val accentGlow = if (isDark) MagentaGlowDark else null
    
    return Typography(
        // GLOWING (dark mode only)
        headlineLarge = CyberTypography.headlineLarge.copy(shadow = headlineGlow),
        titleLarge = CyberTypography.titleLarge.copy(shadow = titleGlow),
        titleMedium = CyberTypography.titleMedium.copy(shadow = accentGlow),
        
        // NON-GLOWING (readability)
        bodyLarge = CyberTypography.bodyLarge,    // No shadow
        bodyMedium = CyberTypography.bodyMedium,  // No shadow
        labelLarge = CyberTypography.labelLarge,  // No shadow
        // ...
    )
}
```

---

## 🧩 Component Dark Mode Styling

### NeonPanel (Card Container)

```kotlin
@Composable
fun NeonPanel(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = LocalDarkTheme.current
    val colorScheme = MaterialTheme.colorScheme

    Column(
        modifier = modifier
            .clip(PanelShape)
            // DARK MODE: Subtle white border
            .border(
                1.dp,
                Color.White.copy(alpha = 0.10f),
                PanelShape
            )
            // DARK MODE: Colorful gradient background
            .background(
                Brush.linearGradient(
                    listOf(
                        colorScheme.secondary.copy(alpha = 0.10f),  // Magenta tint
                        colorScheme.primary.copy(alpha = 0.06f),   // Cyan tint
                        colorScheme.tertiary.copy(alpha = 0.08f)   // Purple tint
                    )
                )
            )
            .padding(16.dp),
        // ...
    )
}
```

### NeonOutlinedField (Text Input)

```kotlin
OutlinedTextField(
    // ...
    colors = OutlinedTextFieldDefaults.colors(
        // Focused: Vibrant primary color
        focusedBorderColor = colorScheme.primary,        // Cyan
        focusedLabelColor = colorScheme.primary,
        cursorColor = colorScheme.primary,
        
        // Unfocused: Subtle white border
        unfocusedBorderColor = Color.White.copy(alpha = 0.18f),
        unfocusedLabelColor = colorScheme.onSurfaceVariant,
        
        // Container: Transparent (shows panel gradient behind)
        focusedContainerColor = Color.Transparent,
        unfocusedContainerColor = Color.Transparent,
        
        // Error: Neon red-pink
        errorBorderColor = colorScheme.error,            // #FF4D6D
    )
)
```

### NeonButton (Button Hierarchy)

```kotlin
enum class NeonButtonStyle {
    PRIMARY,    // Pulsing glow + animated border + outer glow
    SECONDARY,  // Animated gradient border
    TERTIARY    // Subtle/no border
}
```

#### Dark Mode Button Styling

```kotlin
// Container: Semi-transparent with color tint
val containerColor = when (style) {
    NeonButtonStyle.PRIMARY -> glowColor.copy(alpha = 0.15f)   // Cyan tint
    NeonButtonStyle.SECONDARY -> Color.Transparent
    NeonButtonStyle.TERTIARY -> Color.Transparent
}

// Content: Neon color text (same as glow)
val contentColor = when (style) {
    NeonButtonStyle.PRIMARY -> glowColor       // Cyan text
    NeonButtonStyle.SECONDARY -> glowColor     // Cyan text
    NeonButtonStyle.TERTIARY -> glowColor.copy(alpha = 0.8f)
}

// Border: Animated pulsing gradient
val borderWidth = when (style) {
    NeonButtonStyle.PRIMARY -> 1.dp    // Animated alpha (0.4 → 0.8)
    NeonButtonStyle.SECONDARY -> 1.dp  // Static 0.7 alpha
    NeonButtonStyle.TERTIARY -> 0.dp   // No border
}

// Border uses gradient brush for extra depth
border = BorderStroke(
    width = borderWidth,
    brush = Brush.linearGradient(
        colors = listOf(
            glowColor.copy(alpha = glowAlpha),       // Brighter
            glowColor.copy(alpha = glowAlpha * 0.7f) // Slightly darker
        )
    )
)

// Disabled: Very faint version
disabledContainerColor = glowColor.copy(alpha = 0.08f)
disabledContentColor = glowColor.copy(alpha = 0.38f)
```

---

## 🌈 Header Gradient (GradientHeader.kt)

The Q WELCOME header uses a vibrant pink-to-purple gradient:

```kotlin
// ============== DARK MODE GRADIENT ==============
// Vibrant pink to purple gradient (matches Q logo)
private val DarkGradientColors = listOf(
    Color(0xFFFF10F0), // Hot pink
    Color(0xFFEC4899), // Pink
    Color(0xFFC19EE0), // Light purple
    Color(0xFF9D4EDD), // Medium purple
    Color(0xFF7C3AED), // Purple
    Color(0xFF6D28D9)  // Deep purple
)

// Apply as vertical gradient to text
val textGradient = Brush.verticalGradient(DarkGradientColors)

Text(
    text = "WELCOME",
    style = TextStyle(
        fontFamily = DisplayFont,
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 3.sp,
        brush = textGradient  // Gradient fill
    )
)
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

private val DarkExtendedColors = CyberExtendedColors(
    success = CyberSuccessDark,       // #00E676 (neon green)
    onSuccess = Color(0xFF003910),
    successContainer = Color(0xFF005221),
    warning = CyberWarningDark,       // #FFD600 (bright yellow)
    onWarning = Color(0xFF3D2600),
    warningContainer = Color(0xFF5C3D00),
    info = CyberCyan,                 // #00E5FF (cyan)
    lime = CyberLime                  // #9BFF00 (neon lime)
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
    val primary = MaterialTheme.colorScheme.primary      // #00E5FF
    val secondary = MaterialTheme.colorScheme.secondary  // #FF2BD6
    val surface = MaterialTheme.colorScheme.surface      // #120A1C
    val error = MaterialTheme.colorScheme.error          // #FF4D6D
    
    // Extended colors (for success, warning, etc.)
    val success = LocalCyberColors.current.success       // #00E676
    val warning = LocalCyberColors.current.warning       // #FFD600
    val lime = LocalCyberColors.current.lime             // #9BFF00
    
    // Check if dark mode
    val isDark = LocalDarkTheme.current  // true in dark mode
}
```

### Button Hierarchy Example

```kotlin
// Screen with proper button hierarchy
Column {
    // PRIMARY - main action (cyan glow, pulsing border, outer glow)
    NeonButton(
        onClick = { /* send SMS */ },
        style = NeonButtonStyle.PRIMARY,
        glowColor = MaterialTheme.colorScheme.primary  // Cyan
    ) {
        Text("Send SMS")
    }
    
    // SECONDARY - important alternate (magenta, static border)
    NeonButton(
        onClick = { /* share */ },
        style = NeonButtonStyle.SECONDARY,
        glowColor = MaterialTheme.colorScheme.secondary  // Magenta
    ) {
        Text("Share")
    }
    
    // TERTIARY - less prominent (purple, no border)
    NeonButton(
        onClick = { /* show QR */ },
        style = NeonButtonStyle.TERTIARY,
        glowColor = MaterialTheme.colorScheme.tertiary  // Purple
    ) {
        Text("Show QR")
    }
}
```

---

## 🔑 Key Dark Mode Decisions

| Element | Implementation | Reason |
| ------- | -------------- | ------ |
| **Text shadows** | Neon glow (blur=12f, centered) | Simulate light emission |
| **Panel background** | Multi-color gradient (10% alpha) | Depth without opacity |
| **Panel border** | 1dp white@10% | Subtle separation |
| **Primary button** | Pulsing border + outer glow | Maximum attention |
| **Secondary button** | Static gradient border | Clear but secondary |
| **Background** | Radial gradient purple→black | Deep space feel |
| **Scanline** | 40% primary, 55% overall alpha | Subtle animation |
| **Grid** | White@4% | Barely visible texture |

---

## 📐 Animation Parameters

| Animation | Duration | Easing | Values |
| --------- | -------- | ------ | ------ |
| **Button glow pulse** | 1500ms | FastOutSlowInEasing | 0.4 → 0.8 alpha |
| **Scanline movement** | 8000ms | LinearEasing | 0% → 100% Y position |

---

## 📱 Color Reference Table

| Role | Hex | RGB | Usage |
| ---- | --- | --- | ----- |
| **Primary (Cyan)** | `#00E5FF` | 0, 229, 255 | Main actions, focused fields, glows |
| **Secondary (Magenta)** | `#FF2BD6` | 255, 43, 214 | Alt actions, accent highlights |
| **Tertiary (Purple)** | `#A371F7` | 163, 113, 247 | Less prominent accents |
| **Background** | `#05030A` | 5, 3, 10 | Deep space black |
| **Surface** | `#120A1C` | 18, 10, 28 | Card/panel backgrounds |
| **Surface Variant** | `#1E1030` | 30, 16, 48 | Elevated surfaces |
| **Text** | `#F0F5FF` | 240, 245, 255 | Primary text (blue-white) |
| **Muted Text** | `#BEC8DC` | 190, 200, 220 | Secondary text |
| **Error** | `#FF4D6D` | 255, 77, 109 | Neon red-pink |
| **Success** | `#00E676` | 0, 230, 118 | Neon green |
| **Warning** | `#FFD600` | 255, 214, 0 | Bright yellow |
| **Lime** | `#9BFF00` | 155, 255, 0 | Neon lime accent |

---

## 🎯 Summary: What Makes It Cyberpunk in Dark Mode

1. **Neon text glows** on headers (cyan, magenta shadows with 10-12f blur)
2. **Pulsing animated borders** on PRIMARY buttons (0.4 → 0.8 alpha, 1.5s cycle)
3. **Outer glow effect** on PRIMARY buttons (drawn behind with Stroke)
4. **Deep space gradient background** (radial, purple center → black edges)
5. **Moving scanline** (8s cycle, cyan gradient line)
6. **Subtle grid overlay** (white@4%, 28dp spacing)
7. **Multi-color panel gradients** (magenta/cyan/purple at 6-10% alpha)
8. **Vibrant header gradient** (hot pink → deep purple)
9. **Transparent field containers** (shows panel gradient through)

The cyberpunk is in the **light emission** - everything glows, pulses, and radiates energy.

---

## 📄 XML Resources (values-night/colors.xml)

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- Primary: Electric Cyan -->
    <color name="cyber_primary">#FF00E5FF</color>
    <color name="cyber_on_primary">#FF001014</color>
    
    <!-- Secondary: Hot Pink/Magenta -->
    <color name="cyber_secondary">#FFFF2BD6</color>
    <color name="cyber_on_secondary">#FF14000F</color>
    
    <!-- Tertiary: Soft Purple -->
    <color name="cyber_tertiary">#FFA371F7</color>
    <color name="cyber_on_tertiary">#FF080010</color>
    
    <!-- Background colors (deep space) -->
    <color name="cyber_background">#FF05030A</color>
    <color name="cyber_surface">#FF120A1C</color>
    <color name="cyber_surface_variant">#FF1E1030</color>
    
    <!-- Text colors -->
    <color name="cyber_on_background">#FFF0F5FF</color>
    <color name="cyber_on_surface">#FFF0F5FF</color>
    <color name="cyber_on_surface_variant">#FFBEC8DC</color>
    
    <!-- Error: Neon red-pink -->
    <color name="cyber_error">#FFFF4D6D</color>
    
    <!-- Splash screen -->
    <color name="splash_background">#FF05030A</color>
</resources>
```

---

*Last updated: January 25, 2026*
