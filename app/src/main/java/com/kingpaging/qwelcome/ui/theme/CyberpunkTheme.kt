package com.kingpaging.qwelcome.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

/** Shared color roles for the Q Welcome field workflow. */
val CyberDarkScheme =
    darkColorScheme(
        primary = Color(0xFFBCA7FF),
        onPrimary = Color(0xFF29164F),
        primaryContainer = Color(0xFF392961),
        onPrimaryContainer = Color(0xFFEADDFF),
        secondary = Color(0xFF85D3E5),
        onSecondary = Color(0xFF003641),
        secondaryContainer = Color(0xFF16434F),
        onSecondaryContainer = Color(0xFFBDECF6),
        tertiary = Color(0xFFB5C4F6),
        onTertiary = Color(0xFF1E2E56),
        tertiaryContainer = Color(0xFF34446E),
        onTertiaryContainer = Color(0xFFDCE3FF),
        background = Color(0xFF0B0F1A),
        onBackground = Color(0xFFF1F3FA),
        surface = Color(0xFF151B2A),
        onSurface = Color(0xFFF1F3FA),
        surfaceVariant = Color(0xFF232C40),
        onSurfaceVariant = Color(0xFFB7C0D3),
        surfaceContainerHighest = Color(0xFF293249),
        surfaceContainerHigh = Color(0xFF20283B),
        surfaceContainer = Color(0xFF1A2233),
        surfaceContainerLow = Color(0xFF121827),
        surfaceContainerLowest = Color(0xFF080C15),
        outline = Color(0xFF8490A8),
        outlineVariant = Color(0xFF364057),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF5A2027),
        onErrorContainer = Color(0xFFFFDAD6),
        inverseSurface = Color(0xFFE3E7F1),
        inverseOnSurface = Color(0xFF202635),
        inversePrimary = Color(0xFF6544B9),
        scrim = Color.Black,
    )

val CyberLightScheme =
    lightColorScheme(
        primary = Color(0xFF6544B9),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFEADDFF),
        onPrimaryContainer = Color(0xFF29164F),
        secondary = Color(0xFF236B7B),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFD1F0F7),
        onSecondaryContainer = Color(0xFF003641),
        tertiary = Color(0xFF4C5D8B),
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFFDCE3FF),
        onTertiaryContainer = Color(0xFF1E2E56),
        background = Color(0xFFF5F6FB),
        onBackground = Color(0xFF1B2130),
        surface = Color.White,
        onSurface = Color(0xFF1B2130),
        surfaceVariant = Color(0xFFE9EDF5),
        onSurfaceVariant = Color(0xFF515C70),
        surfaceContainerHighest = Color(0xFFE1E6F0),
        surfaceContainerHigh = Color(0xFFE8ECF4),
        surfaceContainer = Color(0xFFEEF1F7),
        surfaceContainerLow = Color(0xFFF9FAFD),
        surfaceContainerLowest = Color.White,
        outline = Color(0xFF717D93),
        outlineVariant = Color(0xFFD6DCE8),
        error = Color(0xFFB3261E),
        onError = Color.White,
        errorContainer = Color(0xFFFCE3DF),
        onErrorContainer = Color(0xFF410E0B),
        inverseSurface = Color(0xFF2D3444),
        inverseOnSurface = Color(0xFFF1F3FA),
        inversePrimary = Color(0xFFBCA7FF),
        scrim = Color.Black,
    )

data class CyberExtendedColors(
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val warning: Color,
    val onWarning: Color,
    val warningContainer: Color,
    val info: Color,
    val lime: Color,
    val qrCodeBackground: Color,
)

private val DarkExtendedColors =
    CyberExtendedColors(
        success = CyberColors.SuccessDark,
        onSuccess = Color(0xFF003824),
        successContainer = Color(0xFF174D37),
        warning = CyberColors.WarningDark,
        onWarning = Color(0xFF442B00),
        warningContainer = Color(0xFF503B1D),
        info = CyberColors.InfoDark,
        lime = CyberColors.LimeDark,
        qrCodeBackground = Color.White,
    )

private val LightExtendedColors =
    CyberExtendedColors(
        success = CyberColors.SuccessLight,
        onSuccess = Color.White,
        successContainer = Color(0xFFD4EFDF),
        warning = CyberColors.WarningLight,
        onWarning = Color.White,
        warningContainer = Color(0xFFFFE9C9),
        info = CyberColors.InfoLight,
        lime = CyberColors.LimeLight,
        qrCodeBackground = Color.White,
    )

val LocalCyberColors =
    compositionLocalOf<CyberExtendedColors> {
        error("No CyberExtendedColors provided. Ensure CyberpunkTheme is applied.")
    }

val LocalDarkTheme =
    compositionLocalOf<Boolean> {
        error("No LocalDarkTheme provided. Ensure CyberpunkTheme is applied.")
    }

@Deprecated(
    message = "Use MaterialTheme.colorScheme instead for proper theme integration",
    replaceWith = ReplaceWith("MaterialTheme.colorScheme", "androidx.compose.material3.MaterialTheme"),
)
val CyberScheme: ColorScheme
    @Composable
    get() = MaterialTheme.colorScheme

/** Applies Q Welcome's purple brand, calm surfaces and readable typography. */
@Composable
fun CyberpunkTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalDarkTheme provides darkTheme,
        LocalCyberColors provides if (darkTheme) DarkExtendedColors else LightExtendedColors,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) CyberDarkScheme else CyberLightScheme,
            typography = cyberTypography(isDark = darkTheme),
            content = content,
        )
    }
}
