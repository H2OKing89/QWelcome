@file:OptIn(ExperimentalMaterial3Api::class)

package com.kingpaging.qwelcome.ui.export

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.kingpaging.qwelcome.R
import com.kingpaging.qwelcome.data.Template
import com.kingpaging.qwelcome.di.LocalExportViewModel
import com.kingpaging.qwelcome.di.LocalNavigator
import com.kingpaging.qwelcome.ui.components.CyberpunkBackdrop
import com.kingpaging.qwelcome.ui.components.NeonButton
import com.kingpaging.qwelcome.ui.components.NeonButtonStyle
import com.kingpaging.qwelcome.ui.components.NeonMagentaButton
import com.kingpaging.qwelcome.ui.components.NeonPanel
import com.kingpaging.qwelcome.ui.theme.LocalDarkTheme
import com.kingpaging.qwelcome.util.rememberHapticFeedback
import com.kingpaging.qwelcome.di.LocalSoundPlayer
import com.kingpaging.qwelcome.viewmodel.export.ExportEvent
import com.kingpaging.qwelcome.viewmodel.export.ExportType
import java.io.IOException

@Suppress("LocalContextGetResourceValueCall")
@Composable
fun ExportScreen(
    onBack: () -> Unit
) {
    val vm = LocalExportViewModel.current
    val soundPlayer = LocalSoundPlayer.current
    val context = LocalContext.current
    val navigator = LocalNavigator.current
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val haptic = rememberHapticFeedback()

    // Reset ViewModel state when entering the screen to clear any stale events
    LaunchedEffect(Unit) {
        vm.reset()
    }

    // File picker launcher for saving
    val saveFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            // Write JSON to the selected file
            val json = vm.getPendingFileExportContent()
            if (json != null) {
                try {
                    val outputStream = context.contentResolver.openOutputStream(uri)
                        ?: throw IOException("Could not open output stream")
                    outputStream.use { stream ->
                        stream.write(json.toByteArray(Charsets.UTF_8))
                    }
                    vm.onFileSaveComplete()
                } catch (e: SecurityException) {
                    soundPlayer.playBeep()
                    Toast.makeText(context, context.getString(R.string.toast_failed_save_file, e.message), Toast.LENGTH_LONG).show()
                    vm.onFileSaveCancelled()
                } catch (e: IOException) {
                    soundPlayer.playBeep()
                    Toast.makeText(context, context.getString(R.string.toast_failed_save_file, e.message), Toast.LENGTH_LONG).show()
                    vm.onFileSaveCancelled()
                }
            }
        } else {
            vm.onFileSaveCancelled()
        }
    }

    // Handle system back button
    BackHandler { onBack() }

    // Handle one-shot events
    LaunchedEffect(Unit) {
        vm.events.collect { event ->
            when (event) {
                is ExportEvent.ExportSuccess -> {
                    // Success is shown in UI state
                }
                is ExportEvent.ExportError -> {
                    soundPlayer.playBeep()
                    Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
                }
                is ExportEvent.CopiedToClipboard -> {
                    val typeName = context.getString(
                        when (event.type) {
                            ExportType.TEMPLATE_PACK -> R.string.export_type_template_pack
                            ExportType.FULL_BACKUP -> R.string.export_type_full_backup
                        }
                    )
                    Toast.makeText(context, context.getString(R.string.toast_type_copied, typeName), Toast.LENGTH_SHORT).show()
                }
                is ExportEvent.ShareReady -> {
                    val typeName = context.getString(
                        when (event.type) {
                            ExportType.TEMPLATE_PACK -> R.string.export_type_template_pack
                            ExportType.FULL_BACKUP -> R.string.export_type_full_backup
                        }
                    )
                    navigator.shareText(
                        message = event.json,
                        chooserTitle = context.getString(R.string.chooser_share_type, typeName),
                        subject = context.getString(R.string.subject_qwelcome_export, typeName)
                    )
                }
                is ExportEvent.ShareToAppReady -> {
                    val typeName = context.getString(
                        when (event.type) {
                            ExportType.TEMPLATE_PACK -> R.string.export_type_template_pack
                            ExportType.FULL_BACKUP -> R.string.export_type_full_backup
                        }
                    )
                    navigator.shareToApp(
                        packageName = event.packageName,
                        message = event.json,
                        subject = context.getString(R.string.subject_qwelcome_export, typeName),
                        chooserTitle = context.getString(R.string.chooser_share_type, typeName)
                    )
                }
                is ExportEvent.RequestFileSave -> {
                    saveFileLauncher.launch(event.suggestedName)
                }
                is ExportEvent.FileSaved -> {
                    val typeName = context.getString(
                        when (event.type) {
                            ExportType.TEMPLATE_PACK -> R.string.export_type_template_pack
                            ExportType.FULL_BACKUP -> R.string.export_type_full_backup
                        }
                    )
                    Toast.makeText(context, context.getString(R.string.toast_type_saved, typeName), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    CyberpunkBackdrop {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.title_export), color = MaterialTheme.colorScheme.primary) },
                    navigationIcon = {
                        IconButton(onClick = { haptic(); onBack() }) {
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
                    onClick = { vm.onTemplatePackRequested() }
                )

                // Full Backup Card
                ExportOptionCard(
                    title = stringResource(R.string.export_type_full_backup),
                    description = stringResource(R.string.text_export_full_backup_description),
                    icon = Icons.Default.Backup,
                    iconTint = MaterialTheme.colorScheme.tertiary,
                    isLoading = uiState.isExporting && uiState.currentlyExportingType == ExportType.FULL_BACKUP,
                    onClick = { vm.exportFullBackup() }
                )

                // Show export result if available
                AnimatedVisibility(
                    visible = uiState.lastExportedJson != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )

                        // Result Header
                        val typeName = when (uiState.lastExportType) {
                            ExportType.TEMPLATE_PACK -> stringResource(R.string.export_type_template_pack)
                            ExportType.FULL_BACKUP -> stringResource(R.string.export_type_full_backup)
                            null -> ""
                        }
                        val templateCountText = pluralStringResource(
                            R.plurals.template_count,
                            uiState.templateCount,
                            uiState.templateCount
                        )
                        Text(
                            stringResource(R.string.status_export_ready, typeName, templateCountText),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )

                        if (uiState.recentShareTargets.isNotEmpty()) {
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
                                uiState.recentShareTargets.forEach { target ->
                                    NeonButton(
                                        onClick = { vm.onShareToPackageRequested(target.packageName) },
                                        glowColor = MaterialTheme.colorScheme.tertiary,
                                        style = NeonButtonStyle.TERTIARY,
                                        modifier = Modifier
                                            .width(120.dp)
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
                            }
                        }

                        // Action buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            NeonMagentaButton(
                                onClick = {
                                    uiState.lastExportedJson?.let { json ->
                                        copyToClipboard(context, json)
                                        vm.onCopiedToClipboard()
                                    }
                                },
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
                                onClick = { vm.onShareRequested() },
                                glowColor = MaterialTheme.colorScheme.tertiary,
                                style = NeonButtonStyle.SECONDARY,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    Icons.Default.Share,
                                    contentDescription = stringResource(R.string.action_share),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.action_share))
                            }
                        }

                        // Save to File button
                        NeonButton(
                            onClick = { vm.onSaveToFileRequested() },
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

                        // JSON Preview
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
                                    text = uiState.lastExportedJson ?: "",
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
                }

                Spacer(Modifier.height(32.dp))
            }
        }

        // Template Selection Dialog
        if (uiState.showTemplateSelectionDialog) {
            TemplateSelectionDialog(
                templates = uiState.availableTemplates,
                selectedIds = uiState.selectedTemplateIds,
                onToggleTemplate = { vm.toggleTemplateSelection(it) },
                onToggleSelectAll = { vm.toggleSelectAll() },
                onDismiss = { vm.dismissTemplateSelection() },
                onExport = { vm.exportSelectedTemplates() }
            )
        }
    }
}

@Composable
private fun TemplateSelectionDialog(
    templates: List<Template>,
    selectedIds: Set<String>,
    onToggleTemplate: (String) -> Unit,
    onToggleSelectAll: () -> Unit,
    onDismiss: () -> Unit,
    onExport: () -> Unit
) {
    val isDark = LocalDarkTheme.current
    val allSelected = templates.isNotEmpty() && selectedIds.size == templates.size
    val selectedCount = selectedIds.size
    val haptic = rememberHapticFeedback()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = if (isDark) 0.dp else 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Header
                Text(
                    text = stringResource(R.string.title_select_templates),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(Modifier.height(16.dp))

                // Select All row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { haptic(); onToggleSelectAll() }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = allSelected,
                        onCheckedChange = { haptic(); onToggleSelectAll() },
                        colors = CheckboxDefaults.colors(
                            checkedColor = MaterialTheme.colorScheme.primary,
                            uncheckedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.action_select_all),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                // Template list
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                ) {
                    items(templates, key = { it.id }) { template ->
                        TemplateSelectionItem(
                            template = template,
                            isSelected = template.id in selectedIds,
                            onToggle = { onToggleTemplate(template.id) }
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { haptic(); onDismiss() }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                    Spacer(Modifier.width(8.dp))
                    NeonButton(
                        onClick = onExport,
                        enabled = selectedCount > 0,
                        glowColor = MaterialTheme.colorScheme.primary,
                        style = NeonButtonStyle.PRIMARY
                    ) {
                        val templateCountText = pluralStringResource(
                            R.plurals.template_count,
                            selectedCount,
                            selectedCount
                        )
                        Text(stringResource(R.string.action_export_count, templateCountText))
                    }
                }
            }
        }
    }
}

@Composable
private fun TemplateSelectionItem(
    template: Template,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    val haptic = rememberHapticFeedback()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { haptic(); onToggle() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isSelected,
            onCheckedChange = { haptic(); onToggle() },
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.secondary,
                uncheckedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        )
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = template.name,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = formatPreview(template.content),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ExportOptionCard(
    title: String,
    description: String,
    icon: ImageVector,
    iconTint: Color,
    isLoading: Boolean,
    onClick: () -> Unit
) {
    val isDark = LocalDarkTheme.current
    val haptic = rememberHapticFeedback()
    
    Card(
        onClick = {
            haptic()
            onClick()
        },
        enabled = !isLoading,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) MaterialTheme.colorScheme.surface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isDark) 0.dp else 2.dp
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(48.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = iconTint,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = iconTint,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(Modifier.width(16.dp))

            // Text content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Copy JSON to system clipboard.
 */
private fun copyToClipboard(context: Context, json: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(context.getString(R.string.title_export), json)
    clipboard.setPrimaryClip(clip)
}

/**
 * Format content for preview display.
 * Normalizes newlines to spaces first, then truncates to maxChars.
 * Does not append ellipsis - let TextOverflow.Ellipsis handle that.
 */
private fun formatPreview(content: String, maxChars: Int = 60): String {
    return content.replace("\n", " ").take(maxChars)
}

