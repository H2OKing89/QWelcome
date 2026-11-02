@file:Suppress("FunctionNaming")

package com.kingpaging.qwelcome.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kingpaging.qwelcome.R
import com.kingpaging.qwelcome.ui.components.NeonButton
import com.kingpaging.qwelcome.ui.components.NeonButtonStyle
import com.kingpaging.qwelcome.ui.theme.CyberpunkTheme
import com.kingpaging.qwelcome.ui.theme.LocalCyberColors

/** Persistent message actions; validation and external handoff remain owned by the route. */
@Composable
internal fun CustomerIntakeActionBar(
    copySuccess: Boolean,
    onSmsClick: () -> Unit,
    onShareClick: () -> Unit,
    onCopyClick: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Column {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Box(
                modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
                contentAlignment = Alignment.TopCenter,
            ) {
                CustomerIntakeActionButtons(
                    modifier =
                        Modifier
                            .widthIn(max = 720.dp)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                    copySuccess = copySuccess,
                    onSmsClick = onSmsClick,
                    onShareClick = onShareClick,
                    onCopyClick = onCopyClick,
                )
            }
        }
    }
}

@Composable
private fun CustomerIntakeActionButtons(
    modifier: Modifier,
    copySuccess: Boolean,
    onSmsClick: () -> Unit,
    onShareClick: () -> Unit,
    onCopyClick: () -> Unit,
) {
    Column(modifier = modifier) {
        NeonButton(
            onClick = onSmsClick,
            modifier = Modifier.fillMaxWidth(),
            style = NeonButtonStyle.PRIMARY,
        ) {
            val contentColor = LocalContentColor.current
            Icon(
                Icons.AutoMirrored.Filled.Send,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = contentColor,
            )
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.action_sms), color = contentColor)
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
        ) {
            TextButton(
                onClick = onShareClick,
                modifier = Modifier.sizeIn(minHeight = 48.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
            ) {
                Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.action_share))
            }
            TextButton(
                onClick = onCopyClick,
                modifier = Modifier.sizeIn(minHeight = 48.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                colors =
                    ButtonDefaults.textButtonColors(
                        contentColor =
                            if (copySuccess) {
                                LocalCyberColors.current.success
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                    ),
            ) {
                Icon(
                    if (copySuccess) Icons.Filled.Check else Icons.Filled.ContentCopy,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(stringResource(if (copySuccess) R.string.action_copied else R.string.action_copy))
            }
        }
    }
}

@Preview(name = "Phone", widthDp = 411)
@Preview(name = "Narrow, large text", widthDp = 320, fontScale = 2f)
@Composable
private fun CustomerIntakeActionBarPreview() {
    CyberpunkTheme(darkTheme = true) {
        CustomerIntakeActionBar(
            copySuccess = false,
            onSmsClick = {},
            onShareClick = {},
            onCopyClick = {},
        )
    }
}
