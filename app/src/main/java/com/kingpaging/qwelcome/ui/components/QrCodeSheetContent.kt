@file:Suppress("FunctionNaming")

package com.kingpaging.qwelcome.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kingpaging.qwelcome.R
import com.kingpaging.qwelcome.util.WifiQrGenerator

private const val ARTWORK_ASPECT_RATIO = 3508f / 4961f
private const val LARGE_FONT_SCALE = 1.3f
private val ArtworkPlaceholderInk = Color(0xFF34323D)

internal data class QrCodeNetworkDetails(
    val ssid: String,
    val isOpenNetwork: Boolean,
    val securityType: WifiQrGenerator.SecurityType,
    val password: String = "",
) {
    val effectiveSecurityType: WifiQrGenerator.SecurityType
        get() = if (isOpenNetwork) WifiQrGenerator.SecurityType.OPEN else securityType
}

internal data class QrCodeSheetActions(
    val onRequestSave: () -> Unit,
    val onShare: () -> Unit,
    val onPrint: () -> Unit = {},
)

@Composable
internal fun QrCodeScrollableContent(
    qrPainter: Painter?,
    network: QrCodeNetworkDetails,
    isSaving: Boolean,
    isSharing: Boolean,
    actions: QrCodeSheetActions,
    modifier: Modifier = Modifier,
    previewFailed: Boolean = false,
    isPrinting: Boolean = false,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        QrCodeSheetContent(qrPainter, network, isSaving, isSharing, actions, previewFailed, isPrinting)
    }
}

@Composable
internal fun ColumnScope.QrCodeSheetContent(
    qrPainter: Painter?,
    network: QrCodeNetworkDetails,
    isSaving: Boolean,
    isSharing: Boolean,
    actions: QrCodeSheetActions,
    previewFailed: Boolean = false,
    isPrinting: Boolean = false,
) {
    QrCodeSheetHeader()
    Spacer(Modifier.height(16.dp))
    QrCodePreview(qrPainter, network, previewFailed)
    Spacer(Modifier.height(12.dp))
    QrCodeSecuritySummary(network)
    Spacer(Modifier.height(16.dp))
    QrCodeActionRow(isSaving, isSharing, isPrinting, actions)
    Spacer(Modifier.height(8.dp))
    QrPrintAction(isPrinting, !isSaving && !isSharing && !isPrinting, actions.onPrint)
    Spacer(Modifier.height(16.dp))
}

@Composable
private fun QrCodeSheetHeader() {
    Text(
        text = stringResource(R.string.title_wifi_qr_code),
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun QrCodePreview(
    qrPainter: Painter?,
    network: QrCodeNetworkDetails,
    previewFailed: Boolean,
) {
    val windowHeightPixels = LocalWindowInfo.current.containerSize.height
    val windowHeight = with(LocalDensity.current) { windowHeightPixels.toDp() }
    val previewWidth = (windowHeight * 0.5f * ARTWORK_ASPECT_RATIO).coerceIn(160.dp, 320.dp)

    Box(
        modifier =
            Modifier
                .widthIn(max = previewWidth)
                .fillMaxWidth()
                .aspectRatio(ARTWORK_ASPECT_RATIO)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center,
    ) {
        if (qrPainter != null) {
            Image(
                painter = qrPainter,
                contentDescription =
                    stringResource(
                        R.string.strings_qr_artwork_accessibility,
                        network.ssid,
                        if (network.effectiveSecurityType == WifiQrGenerator.SecurityType.OPEN) {
                            stringResource(R.string.strings_qr_artwork_open_password)
                        } else {
                            network.password
                        },
                    ),
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            QrArtworkPlaceholder(previewFailed)
        }
    }
}

@Composable
private fun QrArtworkPlaceholder(failed: Boolean) {
    Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (!failed) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        Text(
            stringResource(
                if (failed) R.string.strings_qr_artwork_failed else R.string.strings_qr_artwork_loading,
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = ArtworkPlaceholderInk,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun QrCodeSecuritySummary(network: QrCodeNetworkDetails) {
    Text(
        stringResource(R.string.label_security),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(
        text = stringResource(network.effectiveSecurityType.displayLabelResource()),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun QrCodeActionRow(
    isSaving: Boolean,
    isSharing: Boolean,
    isPrinting: Boolean,
    actions: QrCodeSheetActions,
) {
    val enabled = !isSaving && !isSharing && !isPrinting
    if (LocalDensity.current.fontScale > LARGE_FONT_SCALE) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            QrSaveAction(isSaving, enabled, actions.onRequestSave, Modifier.fillMaxWidth())
            QrShareAction(isSharing, enabled, actions.onShare, Modifier.fillMaxWidth())
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            QrSaveAction(isSaving, enabled, actions.onRequestSave, Modifier.weight(1f))
            QrShareAction(isSharing, enabled, actions.onShare, Modifier.weight(1f))
        }
    }
}

@Composable
private fun QrPrintAction(
    isPrinting: Boolean,
    enabled: Boolean,
    onPrint: () -> Unit,
) {
    NeonButton(
        onClick = onPrint,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        style = NeonButtonStyle.TERTIARY,
    ) {
        if (isPrinting) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary,
            )
        } else {
            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.qr_print_action))
    }
}

@Composable
private fun QrSaveAction(
    isSaving: Boolean,
    enabled: Boolean,
    onRequestSave: () -> Unit,
    modifier: Modifier,
) {
    NeonCyanButton(
        onClick = onRequestSave,
        enabled = enabled,
        modifier = modifier,
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
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.action_save))
    }
}

@Composable
private fun QrShareAction(
    isSharing: Boolean,
    enabled: Boolean,
    onShare: () -> Unit,
    modifier: Modifier,
) {
    NeonMagentaButton(
        onClick = onShare,
        enabled = enabled,
        modifier = modifier,
    ) {
        if (isSharing) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.secondary,
            )
        } else {
            Icon(
                Icons.Default.Share,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.action_share))
    }
}
