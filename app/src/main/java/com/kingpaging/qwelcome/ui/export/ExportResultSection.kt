@file:Suppress("FunctionNaming")

package com.kingpaging.qwelcome.ui.export

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kingpaging.qwelcome.R
import com.kingpaging.qwelcome.ui.components.NeonButton
import com.kingpaging.qwelcome.ui.components.NeonButtonStyle
import com.kingpaging.qwelcome.ui.components.NeonMagentaButton
import com.kingpaging.qwelcome.ui.components.NeonPanel
import com.kingpaging.qwelcome.viewmodel.export.ExportType
import com.kingpaging.qwelcome.viewmodel.export.RecentShareTargetUi

@Composable
internal fun ExportResultSection(
    exportedJson: String?,
    exportType: ExportType?,
    templateCount: Int,
    recentShareTargets: List<RecentShareTargetUi>,
    onShareToPackageRequested: (String) -> Unit,
    onCopy: () -> Unit,
    onShareRequested: () -> Unit,
    onSaveToFileRequested: () -> Unit
) {
    AnimatedVisibility(
        visible = exportedJson != null,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        ExportResultContent(
            exportedJson = exportedJson.orEmpty(),
            exportType = exportType,
            templateCount = templateCount,
            recentShareTargets = recentShareTargets,
            onShareToPackageRequested = onShareToPackageRequested,
            onCopy = onCopy,
            onShareRequested = onShareRequested,
            onSaveToFileRequested = onSaveToFileRequested
        )
    }
}

@Composable
private fun ExportResultContent(
    exportedJson: String,
    exportType: ExportType?,
    templateCount: Int,
    recentShareTargets: List<RecentShareTargetUi>,
    onShareToPackageRequested: (String) -> Unit,
    onCopy: () -> Unit,
    onShareRequested: () -> Unit,
    onSaveToFileRequested: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        HorizontalDivider(
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
            modifier = Modifier.padding(vertical = 8.dp)
        )
        ExportResultHeader(exportType, templateCount)
        RecentShareTargets(recentShareTargets, onShareToPackageRequested)
        ExportCopyShareActions(onCopy, onShareRequested)
        ExportSaveToFileAction(onSaveToFileRequested)
        ExportJsonPreview(exportedJson)
    }
}

@Composable
private fun ExportResultHeader(exportType: ExportType?, templateCount: Int) {
    val typeName = when (exportType) {
        ExportType.TEMPLATE_PACK -> stringResource(R.string.export_type_template_pack)
        ExportType.FULL_BACKUP -> stringResource(R.string.export_type_full_backup)
        null -> ""
    }
    val templateCountText = pluralStringResource(
        R.plurals.template_count,
        templateCount,
        templateCount
    )
    Text(
        stringResource(R.string.status_export_ready, typeName, templateCountText),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.secondary
    )
}

@Composable
private fun RecentShareTargets(
    targets: List<RecentShareTargetUi>,
    onShareToPackageRequested: (String) -> Unit
) {
    if (targets.isEmpty()) return

    Text(
        stringResource(R.string.label_recent_shares),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        targets.forEach { target ->
            RecentShareTargetButton(
                target = target,
                onClick = { onShareToPackageRequested(target.packageName) }
            )
        }
    }
}

@Composable
private fun RecentShareTargetButton(target: RecentShareTargetUi, onClick: () -> Unit) {
    NeonButton(
        onClick = onClick,
        glowColor = MaterialTheme.colorScheme.tertiary,
        style = NeonButtonStyle.TERTIARY,
        modifier = Modifier.width(120.dp)
    ) {
        if (target.icon != null) {
            Image(
                bitmap = target.icon,
                contentDescription = stringResource(
                    R.string.content_desc_share_to_app,
                    target.appName
                ),
                modifier = Modifier.size(18.dp)
            )
        } else {
            Icon(
                Icons.Default.Share,
                contentDescription = stringResource(
                    R.string.content_desc_share_to_app,
                    target.appName
                ),
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(Modifier.width(6.dp))
        Text(
            text = target.appName,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ExportCopyShareActions(onCopy: () -> Unit, onShareRequested: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        NeonMagentaButton(
            onClick = onCopy,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                Icons.Default.ContentCopy,
                contentDescription = stringResource(R.string.content_desc_copy_clipboard),
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.action_copy))
        }
        NeonButton(
            onClick = onShareRequested,
            glowColor = MaterialTheme.colorScheme.tertiary,
            style = NeonButtonStyle.SECONDARY,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                Icons.Default.Share,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.action_share))
        }
    }
}

@Composable
private fun ExportSaveToFileAction(onSaveToFileRequested: () -> Unit) {
    NeonButton(
        onClick = onSaveToFileRequested,
        glowColor = MaterialTheme.colorScheme.secondary,
        style = NeonButtonStyle.SECONDARY,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            Icons.Default.SaveAlt,
            contentDescription = stringResource(R.string.content_desc_save_to_file),
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.action_save_to_file))
    }
}

@Composable
private fun ExportJsonPreview(exportedJson: String) {
    Text(
        stringResource(R.string.label_preview),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
    )
    NeonPanel {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 100.dp, max = 200.dp)
                .horizontalScroll(rememberScrollState())
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = exportedJson,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    lineHeight = 14.sp
                ),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
        }
    }
}
