package com.kingpaging.qwelcome.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kingpaging.qwelcome.R
import com.kingpaging.qwelcome.data.MessageTemplate
import com.kingpaging.qwelcome.util.rememberHapticFeedback
import kotlinx.coroutines.launch

/**
 * Bottom sheet displaying available template placeholders.
 *
 * Shows a list of all available variables that can be used in message templates,
 * with a copy button for each to allow easy insertion.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateVariablesSheet(onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val clipboardManager = LocalClipboard.current
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        dragHandle = {
            Surface(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                shape = RoundedCornerShape(2.dp),
            ) {
                Box(Modifier.size(width = 32.dp, height = 4.dp))
            }
        },
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.title_template_variables),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.action_close))
                }
            }

            Spacer(Modifier.height(4.dp))

            Text(
                text = stringResource(R.string.help_tap_copy),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(16.dp))

            // Variable list
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MessageTemplate.PLACEHOLDERS.forEach { (key, _) ->
                    val description = getPlaceholderDescription(key)
                    VariableItem(
                        placeholder = key,
                        description = description,
                        onCopy = {
                            scope.launch {
                                clipboardManager.setClipEntry(
                                    androidx.compose.ui.platform.ClipEntry(
                                        android.content.ClipData.newPlainText("Template Variable", key),
                                    ),
                                )
                            }
                        },
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun VariableItem(
    placeholder: String,
    description: String,
    onCopy: () -> Unit,
) {
    val haptic = rememberHapticFeedback()
    NeonPanel(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = placeholder,
                    style =
                        MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium,
                        ),
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = {
                haptic()
                onCopy()
            }) {
                Icon(
                    Icons.Default.ContentCopy,
                    contentDescription = stringResource(R.string.action_copy_placeholder, placeholder),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

/**
 * Gets the localized description for a placeholder key.
 */
@Composable
private fun getPlaceholderDescription(key: String): String =
    when (key) {
        MessageTemplate.KEY_CUSTOMER_NAME -> stringResource(R.string.template_var_customer_name)
        MessageTemplate.KEY_SSID -> stringResource(R.string.template_var_ssid)
        MessageTemplate.KEY_PASSWORD -> stringResource(R.string.template_var_password)
        MessageTemplate.KEY_ACCOUNT_NUMBER -> stringResource(R.string.template_var_account_number)
        MessageTemplate.KEY_TECH_SIGNATURE -> stringResource(R.string.template_var_tech_signature)
        else -> ""
    }
