# ALLOWelcome Dark Theme Reference

A comprehensive guide to the Cyberpunk Dark Theme implementation in our Android Jetpack Compose app.

---

## 🎨 Design Philosophy

**Goal:** Create an immersive neon cyberpunk experience with glowing elements against deep space backgrounds.

**The Vibe:** Think Blade Runner, Cyberpunk 2077, Tron - neon lights cutting through darkness. The dark theme is where the cyberpunk aesthetic lives most naturally.

**Approach:**

- **Neon glows** on text headers (cyan, magenta, purple) - NOT on everything
- **Deep space backgrounds** (purples and blacks, never pure black)
- **One continuous animation** per screen (scanline only - motion budget)
- **State-driven feedback** (success states, focus states) - not always-on effects
- **Vibrant color accents** against muted dark surfaces
- **Semi-transparent overlays** for depth and layering

---

## 🎛️ Motion Budget (Critical!)

**Rule:** Only ONE continuous animation per screen to avoid "casino UI."

| Animation Type | Status | Notes |
| --------------- | -------- | ------- |
| **Background scanline** | ✅ Continuous | The ONE allowed always-on animation |
| **Button glow pulse** | ❌ Removed | Was competing with scanline |
| **Button outer glow** | ✅ Static | Static 0.6 alpha, no pulse |
| **Grid overlay** | ✅ Static | Decorative, always visible |
| **Copy success feedback** | ✅ State-driven | 1.5s then resets |
| **Focus state glow** | ✅ State-driven | Only when field focused |

**Everything else is STATE-DRIVEN (focus/press/success), not always-on.**

---

## 🌌 Signature Dark Mode Effects

### 1. Neon Text Glow (Headers Only)

Headers and titles have vibrant glow shadows that make text appear to emit light.
**Blur varies by text size** to prevent haziness on smaller text:

```kotlin
// Wide glow for headlineLarge (big text can handle it)
private val CyanGlowDark = Shadow(
    color = Color(0xFF00E5FF).copy(alpha = 0.6f),
    offset = Offset(0f, 0f),
    blurRadius = 12f  // Wide blur for large headlines
)

// Tighter glow for titleLarge (prevents haziness)
private val CyanGlowTightDark = Shadow(
    color = Color(0xFF00E5FF).copy(alpha = 0.5f),
    offset = Offset(0f, 0f),
    blurRadius = 8f   // Tighter blur for titles
)

// Magenta accent glow for titleMedium
private val MagentaGlowDark = Shadow(
    color = Color(0xFFFF2BD6).copy(alpha = 0.5f),
    offset = Offset(0f, 0f),
    blurRadius = 8f   // Tighter blur
)
```

### 2. Static Button Glow (No Animation)

PRIMARY buttons have a static outer glow - no pulsing (motion budget):

```kotlin
// MOTION BUDGET: Scanline is the ONE continuous animation.
// Button glow is STATIC (0.6 alpha) - no infinite pulse.
val glowAlpha = 0.6f  // Static glow - no animation

// Outer glow drawn inside bounds (with padding to avoid clipping)
if (enabled && isDark && style == NeonButtonStyle.PRIMARY) {
    Modifier
        .padding(4.dp)  // Glow padding (prevents clip)
        .drawBehind {
            drawRoundRect(
                color = glowColor.copy(alpha = glowAlpha * 0.30f),
                cornerRadius = CornerRadius(10.dp.toPx()),
                style = Stroke(width = 2.dp.toPx())
            )
        }
}
```

### 3. Deep Space Background Gradient

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

### 4. Animated Scanline (The ONE Animation)

A horizontal neon line that continuously moves down the screen:

```kotlin
// 8-second cycle, linear movement - THE ONE continuous animation
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

### 5. Subtle Grid Overlay (Decorative)

Very faint white grid lines create a "control panel" feel.
**Decorative elements don't need 3:1 contrast** - they should be barely visible:

```kotlin
// White grid at 4% alpha - decorative, not structural
val gridColor = Color.White.copy(alpha = 0.04f)
val spacing = 28.dp.toPx()

// Draw vertical and horizontal lines
for (x in 0..(size.width / spacing).toInt()) {
    val px = x * spacing
    drawLine(gridColor, Offset(px, 0f), Offset(px, size.height), strokeWidth = 1f)
}
```

### 6. Top-Edge Neon Line (Signature Motif)

Both light and dark mode now have the neon top-edge accent on panels:

```kotlin
// Dark mode: cyan→magenta gradient at low alpha (static, no animation)
drawLine(
    brush = Brush.horizontalGradient(
        colors = listOf(
            accentColor.copy(alpha = 0f),
            accentColor.copy(alpha = 0.25f),      // Cyan
            colorScheme.secondary.copy(alpha = 0.20f),  // Magenta
            accentColor.copy(alpha = 0f)
        )
    ),
    start = Offset(16.dp.toPx(), 0f),
    end = Offset(size.width - 16.dp.toPx(), 0f),
    strokeWidth = 2.dp.toPx(),
    cap = StrokeCap.Round
)
```

---

## 🖱️ State-Driven Feedback

### Focus States ("Hacking Terminal" Vibe)

Text fields get a subtle cyan tint when focused:

```kotlin
// Container - slight tint on focus in dark mode
focusedContainerColor = colorScheme.primary.copy(alpha = 0.05f)
unfocusedContainerColor = Color.Transparent

// Error states get subtle red tint (no infinite animation!)
errorContainerColor = colorScheme.error.copy(alpha = 0.05f)
```

### Copy Success Feedback

Copy button shows brief success state - animate meaning, not screensaver:

```kotlin
// Success state: 1.5 seconds, then reset
var copySuccess by remember { mutableStateOf(false) }

// On copy success event:
copySuccess = true
delay(1500L)
copySuccess = false

// Button shows success color + check icon
NeonButton(
    glowColor = if (copySuccess) cyberColors.success else colorScheme.primary
) {
    Icon(if (copySuccess) Icons.Filled.Check else Icons.Filled.ContentCopy)
    Text(if (copySuccess) "Copied!" else "Copy")
}
```

---

## 📁 File Structure

```text
app/src/main/java/com/example/allowelcome/ui/
├── theme/
│   ├── CyberpunkTheme.kt    # Theme setup, color schemes, CompositionLocals
│   ├── Color.kt             # Semantic color constants
│   └── Type.kt              # Typography with tiered neon glow shadows
└── components/
    ├── NeonComponents.kt    # Reusable UI components (panels, buttons, fields)
    ├── CyberpunkBackdrop.kt # Animated background with grid and scanline
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

### Dark Mode Neon Glows (Tiered by Text Size)

```kotlin
// Wide glow for headlineLarge (big text can handle wide blur)
private val CyanGlowDark = Shadow(
    color = Color(0xFF00E5FF).copy(alpha = 0.6f),
    offset = Offset(0f, 0f),
    blurRadius = 12f  // Wide blur
)

// Tighter glow for titleLarge (prevents haziness on smaller text)
private val CyanGlowTightDark = Shadow(
    color = Color(0xFF00E5FF).copy(alpha = 0.5f),
    offset = Offset(0f, 0f),
    blurRadius = 8f   // Tighter blur
)

// Magenta glow for titleMedium/accent
private val MagentaGlowDark = Shadow(
    color = Color(0xFFFF2BD6).copy(alpha = 0.5f),
    offset = Offset(0f, 0f),
    blurRadius = 8f   // Tighter blur
)
```

### Which Text Gets Glows

```kotlin
@Composable
fun cyberTypography(isDark: Boolean = LocalDarkTheme.current): Typography {
    // Dark mode: Headers get neon glows (tiered by size)
    val headlineGlow = if (isDark) CyanGlowDark else null      // blur 12f
    val titleGlow = if (isDark) CyanGlowTightDark else null    // blur 8f
    val accentGlow = if (isDark) MagentaGlowDark else null     // blur 8f
    
    return Typography(
        // GLOWING (dark mode only, tiered blur)
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

### NeonOutlinedField (Text Input with Focus Glow)

```kotlin
OutlinedTextField(
    // ...
    colors = OutlinedTextFieldDefaults.colors(
        // Focused: "Hacking terminal" vibe - bright border + subtle container tint
        focusedBorderColor = colorScheme.primary,        // Cyan
        focusedLabelColor = colorScheme.primary,
        cursorColor = colorScheme.primary,
        focusedContainerColor = colorScheme.primary.copy(alpha = 0.05f),  // Subtle tint!
        
        // Unfocused: Subtle white border, transparent container
        unfocusedBorderColor = Color.White.copy(alpha = 0.18f),
        unfocusedLabelColor = colorScheme.onSurfaceVariant,
        unfocusedContainerColor = Color.Transparent,
        
        // Error: Neon red-pink with subtle tint (no infinite animation!)
        errorBorderColor = colorScheme.error,            // #FF4D6D
        errorContainerColor = colorScheme.error.copy(alpha = 0.05f),  // Subtle tint
    )
)
```

### NeonButton (Button Hierarchy)

```kotlin
enum class NeonButtonStyle {
    PRIMARY,    // Static outer glow + filled container - strongest emphasis
    SECONDARY,  // Static border (0.7 alpha) - medium emphasis
    TERTIARY    // No border - lowest emphasis
}
```

#### Dark Mode Button Styling (Motion Budget Compliant)

```kotlin
// MOTION BUDGET: No button animation - scanline is the ONE continuous animation
val glowAlpha = 0.6f  // Static - no pulse!

// Container: Semi-transparent with stronger tint (0.20f for "premium" feel)
val containerColor = when (style) {
    NeonButtonStyle.PRIMARY -> glowColor.copy(alpha = 0.20f)   // Stronger cyan tint
    NeonButtonStyle.SECONDARY -> Color.Transparent
    NeonButtonStyle.TERTIARY -> Color.Transparent
}

// Content: Neon color text
val contentColor = when (style) {
    NeonButtonStyle.PRIMARY -> glowColor       // Cyan text
    NeonButtonStyle.SECONDARY -> glowColor     // Cyan text
    NeonButtonStyle.TERTIARY -> glowColor.copy(alpha = 0.8f)
}

// Border: STATIC (not animated!) - this preserves hierarchy
val borderWidth = when (style) {
    NeonButtonStyle.PRIMARY -> 1.dp    // Static 0.6 alpha
    NeonButtonStyle.SECONDARY -> 1.dp  // Static 0.7 alpha
    NeonButtonStyle.TERTIARY -> 0.dp   // No border
}

// Outer glow: drawn INSIDE bounds with padding (prevents clipping)
if (enabled && isDark && style == NeonButtonStyle.PRIMARY) {
    Modifier
        .padding(4.dp)  // Glow fits inside bounds
        .drawBehind {
            drawRoundRect(
                color = glowColor.copy(alpha = glowAlpha * 0.30f),
                cornerRadius = CornerRadius(10.dp.toPx()),
                style = Stroke(width = 2.dp.toPx())
            )
        }
}

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
    // PRIMARY - main action (static outer glow, filled container)
    NeonButton(
        onClick = { /* send SMS */ },
        style = NeonButtonStyle.PRIMARY,
        glowColor = MaterialTheme.colorScheme.primary  // Cyan
    ) {
        Text("Send SMS")
    }
    
    // SECONDARY - important alternate (static border, transparent)
    NeonButton(
        onClick = { /* share */ },
        style = NeonButtonStyle.SECONDARY,
        glowColor = MaterialTheme.colorScheme.secondary  // Magenta
    ) {
        Text("Share")
    }
    
    // TERTIARY - less prominent (no border, can show success state)
    NeonButton(
        onClick = { /* copy */ },
        style = NeonButtonStyle.TERTIARY,
        glowColor = if (copySuccess) cyberColors.success else colorScheme.primary
    ) {
        Icon(if (copySuccess) Icons.Filled.Check else Icons.Filled.ContentCopy)
        Text(if (copySuccess) "Copied!" else "Copy")
    }
}
```

---

## 🔑 Key Dark Mode Decisions

| Element | Implementation | Reason |
| ------- | -------------- | ------ |
| **Text shadows** | Tiered blur (12f headline, 8f title) | Prevents haziness on small text |
| **Panel background** | Multi-color gradient (10% alpha) | Depth without opacity |
| **Panel border** | 1dp white@10% + neon top edge | Signature motif (both modes) |
| **Primary button** | Static outer glow (0.6 alpha) | No animation (motion budget) |
| **Secondary button** | Static border (0.7 alpha) | Clear hierarchy vs PRIMARY |
| **Focus state** | 5% primary tint on container | "Hacking terminal" feedback |
| **Copy success** | State-driven 1.5s feedback | Animate meaning, not screensaver |
| **Background** | Radial gradient purple→black | Deep space feel |
| **Scanline** | THE ONE continuous animation | Motion budget compliance |
| **Grid** | White@4% | Decorative (doesn't need 3:1) |

---

## 📐 Animation Parameters

| Animation | Duration | Easing | Values |
| --------- | -------- | ------ | ------ |
| **Scanline movement** | 8000ms | LinearEasing | 0% → 100% Y position |
| **Copy success feedback** | 1500ms | State-driven | Show → Reset |

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

1. **Tiered neon text glows** on headers (blur 12f headline, 8f titles)
2. **Static outer glow** on PRIMARY buttons (motion budget: no pulse)
3. **Top-edge neon line** on panels (signature motif, both modes)
4. **Deep space gradient background** (radial, purple center → black edges)
5. **Moving scanline** (THE ONE continuous animation, 8s cycle)
6. **Subtle grid overlay** (white@4%, 28dp spacing - decorative)
7. **Multi-color panel gradients** (magenta/cyan/purple at 6-10% alpha)
8. **State-driven feedback** (focus tint, copy success, error tint)
9. **Focus container tint** (5% primary for "hacking terminal" feel)

The cyberpunk is in **controlled light emission** - glow with intention, not chaos.

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

Last updated: January 25, 2026
