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
import androidx.compose.ui.unit.dp
import com.kingpaging.qwelcome.R
import com.kingpaging.qwelcome.ui.components.NeonButton
import com.kingpaging.qwelcome.ui.components.NeonButtonStyle
import com.kingpaging.qwelcome.util.WifiQrGenerator

@Composable
internal fun CustomerIntakeQrCodeSection(
    uiState: CustomerIntakeUiState,
    enabled: Boolean,
    onShowQrClick: () -> Unit
) {
    val qrHint = when {
        uiState.ssidError != null -> uiState.ssidError
        !uiState.isOpenNetwork && uiState.passwordError != null -> uiState.passwordError
        uiState.isOpenNetwork && uiState.ssid.isBlank() -> stringResource(R.string.hint_qr_enter_ssid_open)
        uiState.isOpenNetwork && uiState.ssid.isNotBlank() ->
            stringResource(R.string.hint_qr_open_network, uiState.ssid)
        uiState.ssid.isBlank() && uiState.password.isBlank() -> stringResource(R.string.hint_qr_enter_both)
        uiState.ssid.isBlank() -> stringResource(R.string.hint_qr_enter_ssid)
        uiState.password.length < WifiQrGenerator.MIN_PASSWORD_LENGTH ->
            stringResource(R.string.hint_qr_password_length, uiState.password.length)
        else -> uiState.ssid
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.semantics(mergeDescendants = true) {}) {
            Text(
                stringResource(R.string.header_wifi_qr),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.semantics { heading() }
            )
            Text(
                qrHint,
                style = MaterialTheme.typography.bodySmall,
                color = if (enabled) {
                    MaterialTheme.colorScheme.tertiary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                }
            )
        }
        NeonButton(
            onClick = onShowQrClick,
            glowColor = MaterialTheme.colorScheme.tertiary,
            style = NeonButtonStyle.TERTIARY,
            enabled = enabled
        ) {
            Icon(
                Icons.Filled.QrCode2,
                contentDescription = stringResource(R.string.content_desc_show_qr),
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.action_show_qr))
        }
    }
}
