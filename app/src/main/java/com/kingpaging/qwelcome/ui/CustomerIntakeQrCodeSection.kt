@file:Suppress("FunctionNaming")

package com.kingpaging.qwelcome.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kingpaging.qwelcome.R
import com.kingpaging.qwelcome.ui.components.NeonButton
import com.kingpaging.qwelcome.ui.components.NeonButtonStyle
import com.kingpaging.qwelcome.ui.components.NeonPanel
import com.kingpaging.qwelcome.ui.theme.CyberpunkTheme

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CustomerIntakeQrCodeSection(
    uiState: CustomerIntakeUiState,
    enabled: Boolean,
    onShowQrClick: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                stringResource(R.string.header_wifi_qr),
                style = MaterialTheme.typography.titleSmall,
                modifier =
                    Modifier
                        .align(Alignment.CenterVertically)
                        .semantics { heading() },
            )
            NeonButton(
                onClick = onShowQrClick,
                modifier = Modifier.align(Alignment.CenterVertically),
                glowColor = MaterialTheme.colorScheme.tertiary,
                style = NeonButtonStyle.TERTIARY,
                enabled = enabled,
            ) {
                Icon(
                    Icons.Filled.QrCode2,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.action_show_qr))
            }
        }
        if (!enabled) {
            val qrHint =
                when {
                    uiState.ssidError != null -> uiState.ssidError
                    !uiState.isOpenNetwork && uiState.passwordError != null -> uiState.passwordError
                    uiState.isOpenNetwork -> stringResource(R.string.hint_qr_enter_ssid)
                    uiState.ssid.isBlank() && uiState.password.isBlank() -> stringResource(R.string.hint_qr_enter_both)
                    uiState.ssid.isBlank() -> stringResource(R.string.hint_qr_enter_ssid)
                    else -> stringResource(R.string.hint_qr_valid_password)
                }
            Text(
                qrHint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview(name = "Phone", widthDp = 380)
@Preview(name = "Narrow, large text", widthDp = 288, fontScale = 2f)
@Composable
private fun CustomerIntakeQrCodeSectionPreview() {
    CyberpunkTheme(darkTheme = true) {
        NeonPanel {
            CustomerIntakeQrCodeSection(
                uiState = CustomerIntakeUiState(),
                enabled = false,
                onShowQrClick = {},
            )
        }
    }
}
