@file:OptIn(ExperimentalMaterial3Api::class)

package com.kingpaging.qwelcome.ui.export

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kingpaging.qwelcome.di.LocalExportViewModel
import com.kingpaging.qwelcome.ui.components.CyberpunkBackdrop
import com.kingpaging.qwelcome.ui.components.NeonButton
import com.kingpaging.qwelcome.ui.components.NeonButtonStyle
import com.kingpaging.qwelcome.ui.components.NeonMagentaButton
import com.kingpaging.qwelcome.ui.components.NeonPanel
import com.kingpaging.qwelcome.ui.theme.LocalDarkTheme
import com.kingpaging.qwelcome.viewmodel.export.ExportEvent
import com.kingpaging.qwelcome.viewmodel.export.ExportType

@Composable
fun ExportScreen(
    onBack: () -> Unit
) {
    val vm = LocalExportViewModel.current
    val context = LocalContext.current
    val uiState by vm.uiState.collectAsState()

    // File picker launcher for saving
    val saveFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            // Write JSON to the selected file
            val json = vm.getPendingFileExportContent()
            if (json != null) {
                try {
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        outputStream.write(json.toByteArray(Charsets.UTF_8))
                    }
                    vm.onFileSaveComplete()
                } catch (e: Exception) {
                    Toast.makeText(context, "Failed to save file: ${e.message}", Toast.LENGTH_LONG).show()
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
                    Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
                }
                is ExportEvent.CopiedToClipboard -> {
                    val typeName = when (event.type) {
                        ExportType.TEMPLATE_PACK -> "Template Pack"
                        ExportType.FULL_BACKUP -> "Full Backup"
                    }
                    Toast.makeText(context, "$typeName copied to clipboard", Toast.LENGTH_SHORT).show()
                }
                is ExportEvent.ShareReady -> {
                    shareJson(context, event.json, event.type)
                }
                is ExportEvent.RequestFileSave -> {
                    saveFileLauncher.launch(event.suggestedName)
                }
                is ExportEvent.FileSaved -> {
                    val typeName = when (event.type) {
                        ExportType.TEMPLATE_PACK -> "Template Pack"
                        ExportType.FULL_BACKUP -> "Full Backup"
                    }
                    Toast.makeText(context, "$typeName saved to file", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    CyberpunkBackdrop {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("Export", color = MaterialTheme.colorScheme.primary) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
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
                    "Share your templates with teammates or create a personal backup.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )

                Spacer(Modifier.height(8.dp))

                // Export Options
                Text(
                    "Export Options",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                // Template Pack Card
                ExportOptionCard(
                    title = "Template Pack",
                    description = "Share templates with your team. Does NOT include your personal signature info.",
                    icon = Icons.Default.Description,
                    iconTint = MaterialTheme.colorScheme.secondary,
                    isLoading = uiState.isExporting && uiState.currentlyExportingType == ExportType.TEMPLATE_PACK,
                    onClick = { vm.exportTemplatePack() }
                )

                // Full Backup Card
                ExportOptionCard(
                    title = "Full Backup",
                    description = "Export everything including your tech profile. Use for personal backup/restore.",
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
                            ExportType.TEMPLATE_PACK -> "Template Pack"
                            ExportType.FULL_BACKUP -> "Full Backup"
                            null -> ""
                        }
                        Text(
                            "✅ $typeName Ready (${uiState.templateCount} template${if (uiState.templateCount != 1) "s" else ""})",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )

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
                                    contentDescription = "Copy to clipboard",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Copy")
                            }

                            NeonButton(
                                onClick = { vm.onShareRequested() },
                                glowColor = MaterialTheme.colorScheme.tertiary,
                                style = NeonButtonStyle.SECONDARY,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    Icons.Default.Share,
                                    contentDescription = "Share",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Share")
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
                                contentDescription = "Save to file",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Save to File")
                        }

                        // JSON Preview
                        Text(
                            "Preview",
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

                        // Export new button - reset action, subtle styling
                        NeonButton(
                            onClick = { vm.clearExport() },
                            glowColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            style = NeonButtonStyle.TERTIARY,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Export Another")
                        }
                    }
                }

                Spacer(Modifier.height(32.dp))
            }
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
    
    Card(
        onClick = onClick,
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
                        contentDescription = null,
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
    val clip = ClipData.newPlainText("Q Welcome Export", json)
    clipboard.setPrimaryClip(clip)
}

/**
 * Share JSON via Android share sheet.
 */
private fun shareJson(context: Context, json: String, type: ExportType) {
    val typeName = when (type) {
        ExportType.TEMPLATE_PACK -> "Template Pack"
        ExportType.FULL_BACKUP -> "Full Backup"
    }
    
    val intent = Intent(Intent.ACTION_SEND).apply {
        this.type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Q Welcome $typeName")
        putExtra(Intent.EXTRA_TEXT, json)
    }
    
    context.startActivity(
        Intent.createChooser(intent, "Share $typeName")
    )
}
