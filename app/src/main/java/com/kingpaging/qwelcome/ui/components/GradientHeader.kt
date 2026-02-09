package com.kingpaging.qwelcome.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kingpaging.qwelcome.R
import com.kingpaging.qwelcome.ui.theme.DisplayFont
import com.kingpaging.qwelcome.ui.theme.LocalDarkTheme

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

// ============== LIGHT MODE GRADIENT ==============
// Deeper colors for better readability on light backgrounds
private val LightGradientColors = listOf(
    Color(0xFFC2185B), // Deep pink
    Color(0xFFAB47BC), // Medium purple
    Color(0xFF7B1FA2), // Purple
    Color(0xFF6200EA), // Deep purple
    Color(0xFF4527A0), // Indigo purple
    Color(0xFF311B92)  // Deep indigo
)

/**
 * Header with Q logo and "WELCOME" text with gradient.
 * Adapts gradient colors for dark/light mode readability.
 */
@Composable
fun QWelcomeHeader(
    modifier: Modifier = Modifier
) {
    val isDark = LocalDarkTheme.current
    val gradientColors = if (isDark) DarkGradientColors else LightGradientColors
    val textGradient = Brush.verticalGradient(gradientColors)
    
    Row(
        modifier = modifier.padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Vector Q logo
        Image(
            painter = painterResource(id = R.drawable.q_logo),
            contentDescription = stringResource(R.string.content_desc_q_logo),
            modifier = Modifier.size(40.dp)
        )
        
        // "WELCOME" text with gradient
        Text(
            text = stringResource(R.string.label_welcome),
            style = TextStyle(
                fontFamily = DisplayFont,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 3.sp,
                brush = textGradient
            )
        )
    }
}

/**
 * Large header variant for splash screens.
 * Adapts gradient colors for dark/light mode readability.
 */
@Composable
fun QWelcomeHeaderLarge(
    modifier: Modifier = Modifier
) {
    val isDark = LocalDarkTheme.current
    val gradientColors = if (isDark) DarkGradientColors else LightGradientColors
    val textGradient = Brush.verticalGradient(gradientColors)
    
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Large Q logo
        Image(
            painter = painterResource(id = R.drawable.q_logo),
            contentDescription = stringResource(R.string.content_desc_q_logo),
            modifier = Modifier.size(80.dp)
        )
        
        // "WELCOME" text
        Text(
            text = stringResource(R.string.label_welcome),
            style = TextStyle(
                fontFamily = DisplayFont,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp,
                brush = textGradient
            )
        )
    }
}
