package com.kingpaging.qwelcome.ui.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.kingpaging.qwelcome.ui.theme.LocalDarkTheme

/**
 * Cyberpunk-styled dropdown menu box wrapping [ExposedDropdownMenuBox].
 *
 * Confines @OptIn(ExperimentalMaterial3Api::class) to this component so that
 * screens using the dropdown do not need the annotation themselves.
 *
 * The anchor text field uses the same neon-secondary color scheme as
 * [NeonOutlinedField] for visual consistency.
 *
 * @param expanded          Whether the dropdown menu is currently expanded.
 * @param onExpandedChange  Called with the desired new expanded state.
 * @param selectedText      The text displayed in the read-only anchor field.
 * @param label             Composable label for the anchor field.
 * @param modifier          Modifier applied to the outer [ExposedDropdownMenuBox].
 * @param menuContent       Content of the dropdown menu (typically [DropdownMenuItem]s).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NeonDropdownMenuBox(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    selectedText: String,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    menuContent: @Composable ColumnScope.() -> Unit
) {
    val isDark = LocalDarkTheme.current

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedText,
            onValueChange = {},
            readOnly = true,
            label = label,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.secondary,
                unfocusedBorderColor = MaterialTheme.colorScheme.secondary.copy(
                    alpha = if (isDark) 0.45f else 0.28f
                ),
                focusedLabelColor = MaterialTheme.colorScheme.secondary,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
            content = menuContent
        )
    }
}
