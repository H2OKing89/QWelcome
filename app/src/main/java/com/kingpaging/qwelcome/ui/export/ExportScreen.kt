@file:OptIn(ExperimentalMaterial3Api::class)

package com.kingpaging.qwelcome.ui.export

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kingpaging.qwelcome.R
import com.kingpaging.qwelcome.ui.components.CyberpunkBackdrop
import com.kingpaging.qwelcome.ui.components.NeonButton
import com.kingpaging.qwelcome.ui.components.NeonButtonStyle
import com.kingpaging.qwelcome.ui.components.NeonMagentaButton
import com.kingpaging.qwelcome.ui.components.NeonPanel
import com.kingpaging.qwelcome.util.rememberHapticFeedback
import com.kingpaging.qwelcome.viewmodel.export.ExportUiState
import com.kingpaging.qwelcome.viewmodel.export.ExportType

@Suppress("FunctionNaming", "LongMethod")
@Composable
fun ExportScreen(
    uiState: ExportUiState,
    actions: ExportActions
) {
    val haptic = rememberHapticFeedback()

    BackHandler { actions.onBack() }

    CyberpunkBackdrop {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.title_export), color = MaterialTheme.colorScheme.primary) },
                    navigationIcon = {
                        IconButton(onClick = { haptic(); actions.onBack() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.content_desc_back),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header explanation
                Text(
                    stringResource(R.string.text_export_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )

                Spacer(Modifier.height(8.dp))

                // Export Options
                Text(
                    stringResource(R.string.header_export_options),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                // Template Pack Card
                ExportOptionCard(
                    title = stringResource(R.string.export_type_template_pack),
                    description = stringResource(R.string.text_export_template_pack_description),
                    icon = Icons.Default.Description,
                    iconTint = MaterialTheme.colorScheme.secondary,
                    isLoading = uiState.isExporting && uiState.currentlyExportingType == ExportType.TEMPLATE_PACK,
                    onClick = actions.onTemplatePackRequested
                )

                // Full Backup Card
                ExportOptionCard(
                    title = stringResource(R.string.export_type_full_backup),
                    description = stringResource(R.string.text_export_full_backup_description),
                    icon = Icons.Default.Backup,
                    iconTint = MaterialTheme.colorScheme.tertiary,
                    isLoading = uiState.isExporting && uiState.currentlyExportingType == ExportType.FULL_BACKUP,
                    onClick = actions.onFullBackupRequested
                )

                ExportResultSection(
                    exportedJson = uiState.lastExportedJson,
                    exportType = uiState.lastExportType,
                    templateCount = uiState.templateCount,
                    recentShareTargets = uiState.recentShareTargets,
                    onShareToPackageRequested = actions.onShareToPackageRequested,
                    onCopy = actions.onCopy,
                    onShareRequested = actions.onShareRequested,
                    onSaveToFileRequested = actions.onSaveToFileRequested
                )

                Spacer(Modifier.height(32.dp))
            }
        }

        // Template Selection Dialog
        if (uiState.showTemplateSelectionDialog) {
            ExportTemplateSelectionDialog(
                templates = uiState.availableTemplates,
                selectedIds = uiState.selectedTemplateIds,
                onToggleTemplate = actions.onToggleTemplateSelection,
                onToggleSelectAll = actions.onToggleSelectAll,
                onDismiss = actions.onDismissTemplateSelection,
                onExport = actions.onExportSelectedTemplates
            )
        }
    }
}

