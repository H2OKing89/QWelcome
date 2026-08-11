@file:Suppress("FunctionNaming")

package com.kingpaging.qwelcome.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kingpaging.qwelcome.R
import com.kingpaging.qwelcome.ui.components.NeonButton
import com.kingpaging.qwelcome.ui.components.NeonButtonStyle
import com.kingpaging.qwelcome.ui.components.NeonMagentaButton
import com.kingpaging.qwelcome.ui.components.NeonPanel

@Composable
internal fun SettingsDataManagementSection(
    hasUnsavedChanges: Boolean,
    onOpenExport: () -> Unit,
    onOpenImport: () -> Unit
) {
    Text(
        stringResource(R.string.header_data_management),
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.primary
    )
    NeonPanel {
        Text(
            stringResource(R.string.text_data_management_description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        if (hasUnsavedChanges) {
            Text(
                stringResource(R.string.text_export_disabled_unsaved),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.tertiary
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            NeonMagentaButton(
                onClick = onOpenExport,
                modifier = Modifier.weight(1f),
                enabled = !hasUnsavedChanges,
                style = NeonButtonStyle.SECONDARY
            ) {
                Text(
                    if (hasUnsavedChanges) {
                        stringResource(R.string.action_save_first)
                    } else {
                        stringResource(R.string.action_export)
                    }
                )
            }
            NeonButton(
                onClick = onOpenImport,
                modifier = Modifier.weight(1f),
                style = NeonButtonStyle.SECONDARY
            ) {
                Text(stringResource(R.string.action_import))
            }
        }
    }

    Spacer(Modifier.height(32.dp))
}
