@file:Suppress("FunctionNaming")

package com.kingpaging.qwelcome.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kingpaging.qwelcome.R
import com.kingpaging.qwelcome.ui.theme.LocalCyberColors

private val PanelShape = RoundedCornerShape(16.dp)

@Composable
fun NeonPanel(
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val borderColor =
        if (accentColor == colorScheme.error) {
            colorScheme.error.copy(alpha = 0.5f)
        } else {
            colorScheme.outlineVariant
        }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = PanelShape,
        color = colorScheme.surface,
        contentColor = colorScheme.onSurface,
        border = BorderStroke(1.dp, borderColor),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

@Composable
fun NeonWarningBanner(
    text: String,
    modifier: Modifier = Modifier,
    onDismiss: (() -> Unit)? = null,
) {
    val cyberColors = LocalCyberColors.current

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = cyberColors.warningContainer,
        contentColor = cyberColors.warning,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = stringResource(R.string.content_desc_warning),
                tint = cyberColors.warning,
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = cyberColors.warning,
                modifier = Modifier.weight(1f),
            )
            if (onDismiss != null) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.action_dismiss),
                        tint = cyberColors.warning,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}
