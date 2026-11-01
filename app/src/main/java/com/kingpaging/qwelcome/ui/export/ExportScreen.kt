package com.kingpaging.qwelcome.ui.export

import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.kingpaging.qwelcome.R
import com.kingpaging.qwelcome.ui.components.CenteredFormColumn
import com.kingpaging.qwelcome.ui.components.CyberpunkBackdrop
import com.kingpaging.qwelcome.ui.components.NeonTopAppBar
import com.kingpaging.qwelcome.util.rememberHapticFeedback
import com.kingpaging.qwelcome.viewmodel.export.ExportType
import com.kingpaging.qwelcome.viewmodel.export.ExportUiState

@Suppress("FunctionNaming", "LongMethod")
@Composable
fun ExportScreen(
    uiState: ExportUiState,
    actions: ExportActions,
) {
    val haptic = rememberHapticFeedback()

    BackHandler { actions.onBack() }

    CyberpunkBackdrop {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                NeonTopAppBar(
                    title = { Text(stringResource(R.string.title_export)) },
                    navigationIcon = {
                        IconButton(onClick = {
                            haptic()
                            actions.onBack()
                        }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.content_desc_back),
                            )
                        }
                    },
                )
            },
        ) { padding ->
            CenteredFormColumn(padding = padding) {
                // Header explanation
                Text(
                    stringResource(R.string.text_export_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                // Export Options
                Text(
                    stringResource(R.string.header_export_options),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                // Template Pack Card
                ExportOptionCard(
                    title = stringResource(R.string.export_type_template_pack),
                    description = stringResource(R.string.text_export_template_pack_description),
                    icon = Icons.Default.Description,
                    iconTint = MaterialTheme.colorScheme.secondary,
                    isLoading = uiState.isExporting && uiState.currentlyExportingType == ExportType.TEMPLATE_PACK,
                    onClick = actions.onTemplatePackRequested,
                )

                // Full Backup Card
                ExportOptionCard(
                    title = stringResource(R.string.export_type_full_backup),
                    description = stringResource(R.string.text_export_full_backup_description),
                    icon = Icons.Default.Backup,
                    iconTint = MaterialTheme.colorScheme.tertiary,
                    isLoading = uiState.isExporting && uiState.currentlyExportingType == ExportType.FULL_BACKUP,
                    onClick = actions.onFullBackupRequested,
                )

                ExportResultSection(
                    exportedJson = uiState.lastExportedJson,
                    exportType = uiState.lastExportType,
                    templateCount = uiState.templateCount,
                    recentShareTargets = uiState.recentShareTargets,
                    onShareToPackageRequested = actions.onShareToPackageRequested,
                    onCopy = actions.onCopy,
                    onShareRequested = actions.onShareRequested,
                    onSaveToFileRequested = actions.onSaveToFileRequested,
                )
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
                onExport = actions.onExportSelectedTemplates,
            )
        }
    }
}
