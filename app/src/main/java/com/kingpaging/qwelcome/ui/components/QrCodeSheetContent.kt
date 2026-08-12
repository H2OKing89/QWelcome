@file:Suppress("FunctionNaming")

package com.kingpaging.qwelcome.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kingpaging.qwelcome.R
import com.kingpaging.qwelcome.ui.theme.LocalCyberColors
import com.kingpaging.qwelcome.util.WifiQrGenerator

private const val MASKED_PASSWORD_DISPLAY = "********"

internal data class QrCodeNetworkDetails(
    val ssid: String,
    val isOpenNetwork: Boolean,
    val securityType: WifiQrGenerator.SecurityType,
)

internal data class QrCodeSheetActions(
    val onRequestSave: () -> Unit,
    val onShare: () -> Unit,
)

@Composable
internal fun ColumnScope.QrCodeSheetContent(
    qrPainter: Painter,
    network: QrCodeNetworkDetails,
    isSaving: Boolean,
    isSharing: Boolean,
    actions: QrCodeSheetActions,
) {
    QrCodeSheetHeader()
    Spacer(Modifier.height(16.dp))
    QrCodePreview(qrPainter)
    Spacer(Modifier.height(16.dp))
    QrCodeNetworkSummary(network)
    Spacer(Modifier.height(16.dp))
    QrCodeActionRow(isSaving, isSharing, actions)
    Spacer(Modifier.height(24.dp))
}

@Composable
private fun QrCodeSheetHeader() {
    Text(
        text = stringResource(R.string.title_wifi_qr_code),
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.primary,
    )
    Spacer(Modifier.height(4.dp))
    Text(
        text = stringResource(R.string.text_scan_to_connect_automatically),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
    )
}

@Composable
private fun QrCodePreview(qrPainter: Painter) {
    val cyberColors = LocalCyberColors.current

    Box(
        modifier =
            Modifier
                .size(240.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(cyberColors.qrCodeBackground)
                .padding(12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = qrPainter,
            contentDescription = stringResource(R.string.content_desc_wifi_qr_code),
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun QrCodeNetworkSummary(network: QrCodeNetworkDetails) {
    NeonPanel(modifier = Modifier.fillMaxWidth()) {
        Text(
            stringResource(R.string.label_network),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        )
        Text(
            network.ssid,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(4.dp))
        QrCodeSecuritySummary(network)
    }
}

@Composable
private fun QrCodeSecuritySummary(network: QrCodeNetworkDetails) {
    Text(
        stringResource(R.string.label_security),
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
    )
    Text(
        text =
            when {
                network.isOpenNetwork -> stringResource(R.string.label_open_no_password)
                network.securityType == WifiQrGenerator.SecurityType.WPA3_SAE -> {
                    stringResource(R.string.security_wpa3_sae)
                }
                else -> stringResource(R.string.security_wpa2)
            },
        color = MaterialTheme.colorScheme.tertiary,
        fontWeight = FontWeight.Medium,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
    if (!network.isOpenNetwork) {
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(R.string.label_wifi_password),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        )
        Text(
            MASKED_PASSWORD_DISPLAY,
            color = MaterialTheme.colorScheme.secondary,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun QrCodeActionRow(
    isSaving: Boolean,
    isSharing: Boolean,
    actions: QrCodeSheetActions,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        QrSaveAction(isSaving, isSharing, actions.onRequestSave)
        QrShareAction(isSaving, isSharing, actions.onShare)
    }
}

@Composable
private fun RowScope.QrSaveAction(
    isSaving: Boolean,
    isSharing: Boolean,
    onRequestSave: () -> Unit,
) {
    NeonCyanButton(
        onClick = onRequestSave,
        enabled = !isSaving && !isSharing,
        modifier = Modifier.weight(1f),
    ) {
        if (isSaving) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        } else {
            Icon(
                Icons.Default.Download,
                contentDescription = stringResource(R.string.action_save),
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.action_save))
    }
}

@Composable
private fun RowScope.QrShareAction(
    isSaving: Boolean,
    isSharing: Boolean,
    onShare: () -> Unit,
) {
    NeonMagentaButton(
        onClick = onShare,
        enabled = !isSaving && !isSharing,
        modifier = Modifier.weight(1f),
    ) {
        if (isSharing) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        } else {
            Icon(
                Icons.Default.Share,
                contentDescription = stringResource(R.string.action_share),
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.action_share))
    }
}
