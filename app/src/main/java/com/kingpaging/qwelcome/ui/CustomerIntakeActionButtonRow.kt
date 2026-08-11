@file:Suppress("FunctionNaming")

package com.kingpaging.qwelcome.ui

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.kingpaging.qwelcome.R
import com.kingpaging.qwelcome.ui.components.NeonButton
import com.kingpaging.qwelcome.ui.components.NeonButtonStyle
import com.kingpaging.qwelcome.ui.components.NeonCyanButton
import com.kingpaging.qwelcome.ui.theme.LocalCyberColors

@Composable
internal fun CustomerIntakeActionButtonRow(
    copySuccess: Boolean,
    onSmsClick: () -> Unit,
    onShareClick: () -> Unit,
    onCopyClick: () -> Unit
) {
    Text(
        stringResource(R.string.header_send),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.semantics { heading() }
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        NeonCyanButton(
            onClick = onSmsClick,
            modifier = Modifier.weight(1f),
            style = NeonButtonStyle.PRIMARY
        ) {
            Icon(
                Icons.AutoMirrored.Filled.Send,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.action_sms))
        }
        NeonCyanButton(
            onClick = onShareClick,
            modifier = Modifier.weight(1f),
            style = NeonButtonStyle.SECONDARY
        ) {
            Icon(
                Icons.Filled.Share,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(stringResource(R.string.action_share))
        }
        val cyberColors = LocalCyberColors.current
        NeonButton(
            onClick = onCopyClick,
            modifier = Modifier.weight(1f),
            style = NeonButtonStyle.TERTIARY,
            glowColor = if (copySuccess) cyberColors.success else MaterialTheme.colorScheme.primary
        ) {
            Icon(
                if (copySuccess) Icons.Filled.Check else Icons.Filled.ContentCopy,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                if (copySuccess) {
                    stringResource(R.string.action_copied)
                } else {
                    stringResource(R.string.action_copy)
                }
            )
        }
    }
}
