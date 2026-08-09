package com.kingpaging.qwelcome.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kingpaging.qwelcome.R
import com.kingpaging.qwelcome.ui.theme.DisplayFont

/**
 * Header with Q logo and "WELCOME" text with gradient.
 * Adapts gradient colors for dark/light mode readability.
 */
@Composable
fun QWelcomeHeader(
    modifier: Modifier = Modifier
) {
    val textGradient = Brush.verticalGradient(
        listOf(
            MaterialTheme.colorScheme.secondary,
            MaterialTheme.colorScheme.tertiary
        )
    )
    
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
    val textGradient = Brush.verticalGradient(
        listOf(
            MaterialTheme.colorScheme.secondary,
            MaterialTheme.colorScheme.tertiary
        )
    )
    
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
