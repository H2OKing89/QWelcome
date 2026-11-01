@file:Suppress("FunctionNaming")

package com.kingpaging.qwelcome.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun NeonOutlinedField(
    value: String,
    onValueChange: (String) -> Unit,
    label: @Composable (() -> Unit),
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    isError: Boolean = false,
    supportingText: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    val colorScheme = MaterialTheme.colorScheme

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        enabled = enabled,
        singleLine = singleLine,
        isError = isError,
        textStyle = MaterialTheme.typography.bodyLarge,
        shape = RoundedCornerShape(12.dp),
        supportingText =
            if (supportingText != null) {
                {
                    Box(
                        modifier =
                            Modifier.semantics {
                                liveRegion = if (isError) LiveRegionMode.Assertive else LiveRegionMode.Polite
                            },
                    ) {
                        supportingText()
                    }
                }
            } else {
                null
            },
        trailingIcon = trailingIcon,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        modifier = modifier.fillMaxWidth(),
        colors =
            OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accentColor,
                focusedLabelColor = accentColor,
                focusedTextColor = colorScheme.onSurface,
                cursorColor = accentColor,
                unfocusedBorderColor = colorScheme.outline,
                unfocusedLabelColor = colorScheme.onSurfaceVariant,
                unfocusedTextColor = colorScheme.onSurface,
                focusedContainerColor = colorScheme.surfaceContainerLow,
                unfocusedContainerColor = colorScheme.surfaceContainerLow,
                disabledBorderColor = colorScheme.outlineVariant,
                disabledLabelColor = colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                disabledTextColor = colorScheme.onSurface.copy(alpha = 0.5f),
                disabledContainerColor = colorScheme.surfaceContainer,
                disabledSupportingTextColor = colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                errorBorderColor = colorScheme.error,
                errorLabelColor = colorScheme.error,
                errorCursorColor = colorScheme.error,
                errorContainerColor = colorScheme.errorContainer,
            ),
    )
}
