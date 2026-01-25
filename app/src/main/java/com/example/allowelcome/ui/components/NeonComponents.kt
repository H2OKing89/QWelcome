package com.example.allowelcome.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.allowelcome.ui.theme.CyberScheme

private val PanelShape = RoundedCornerShape(16.dp)

@Composable
fun NeonPanel(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .clip(PanelShape)
            .border(1.dp, Color.White.copy(alpha = 0.10f), PanelShape)
            .background(
                Brush.linearGradient(
                    listOf(
                        CyberScheme.secondary.copy(alpha = 0.10f),
                        CyberScheme.primary.copy(alpha = 0.06f),
                        CyberScheme.tertiary.copy(alpha = 0.08f)
                    )
                )
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NeonOutlinedField(
    value: String,
    onValueChange: (String) -> Unit,
    label: @Composable (() -> Unit),
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    isError: Boolean = false,
    supportingText: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        singleLine = singleLine,
        isError = isError,
        supportingText = supportingText,
        trailingIcon = trailingIcon,
        visualTransformation = visualTransformation,
        modifier = modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = CyberScheme.primary.copy(alpha = 0.85f),
            unfocusedBorderColor = Color.White.copy(alpha = 0.18f),
            cursorColor = CyberScheme.primary,
            focusedLabelColor = CyberScheme.primary,
        )
    )
}
