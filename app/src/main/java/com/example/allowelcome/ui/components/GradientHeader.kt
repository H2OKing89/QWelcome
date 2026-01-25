package com.example.allowelcome.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.allowelcome.R
import com.example.allowelcome.ui.theme.DisplayFont

// Gradient colors from pink to purple
private val GradientColors = listOf(
    Color(0xFFFF10F0), // Hot pink
    Color(0xFFEC4899), // Pink
    Color(0xFFC19EE0), // Light purple
    Color(0xFF9D4EDD), // Medium purple
    Color(0xFF7C3AED), // Purple
    Color(0xFF6D28D9)  // Deep purple
)

private val TextGradient = Brush.verticalGradient(GradientColors)

/**
 * Header with Q logo and "WELCOME" text
 */
@Composable
fun QWelcomeHeader(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Vector Q logo
        Image(
            painter = painterResource(id = R.drawable.q_logo),
            contentDescription = "Q Logo",
            modifier = Modifier.size(40.dp)
        )
        
        // "WELCOME" text with gradient
        Text(
            text = "WELCOME",
            style = TextStyle(
                fontFamily = DisplayFont,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 3.sp,
                brush = TextGradient
            )
        )
    }
}

/**
 * Large header variant for splash screens
 */
@Composable
fun QWelcomeHeaderLarge(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Large Q logo
        Image(
            painter = painterResource(id = R.drawable.q_logo),
            contentDescription = "Q Logo",
            modifier = Modifier.size(80.dp)
        )
        
        // "WELCOME" text
        Text(
            text = "WELCOME",
            style = TextStyle(
                fontFamily = DisplayFont,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp,
                brush = TextGradient
            )
        )
    }
}
