@file:Suppress("FunctionNaming")

package com.kingpaging.qwelcome.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kingpaging.qwelcome.R
import com.kingpaging.qwelcome.ui.components.NeonButton
import com.kingpaging.qwelcome.ui.components.NeonButtonStyle
import com.kingpaging.qwelcome.ui.theme.LocalCyberColors

/** Persistent message actions; validation and external handoff remain owned by the route. */
@Composable
internal fun CustomerIntakeActionButtonRow(
    copySuccess: Boolean,
    onSmsClick: () -> Unit,
    onShareClick: () -> Unit,
    onCopyClick: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        NeonButton(
            onClick = onSmsClick,
            modifier = Modifier.fillMaxWidth(),
            style = NeonButtonStyle.PRIMARY,
        ) {
            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.action_sms))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            NeonButton(
                onClick = onShareClick,
                modifier = Modifier.weight(1f),
                style = NeonButtonStyle.TERTIARY,
            ) {
                Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.action_share))
            }
            NeonButton(
                onClick = onCopyClick,
                modifier = Modifier.weight(1f),
                style = NeonButtonStyle.TERTIARY,
                glowColor = if (copySuccess) LocalCyberColors.current.success else MaterialTheme.colorScheme.primary,
            ) {
                Icon(
                    if (copySuccess) Icons.Filled.Check else Icons.Filled.ContentCopy,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(stringResource(if (copySuccess) R.string.action_copied else R.string.action_copy))
            }
        }
    }
}
