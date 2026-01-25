@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.allowelcome.ui.import_pkg

import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.allowelcome.data.ConflictResolution
import com.example.allowelcome.di.LocalImportViewModel
import com.example.allowelcome.ui.components.CyberpunkBackdrop
import com.example.allowelcome.ui.components.NeonButton
import com.example.allowelcome.ui.components.NeonMagentaButton
import com.example.allowelcome.ui.components.NeonPanel
import com.example.allowelcome.ui.theme.CyberScheme
import com.example.allowelcome.viewmodel.import_pkg.ImportEvent
import com.example.allowelcome.viewmodel.import_pkg.ImportStep
import com.example.allowelcome.viewmodel.import_pkg.TemplateImportStatus
import com.example.allowelcome.viewmodel.import_pkg.TemplatePreviewItem

@Composable
fun ImportScreen(
    onBack: () -> Unit,
    onImportComplete: () -> Unit = {}
) {
    val vm = LocalImportViewModel.current
    val context = LocalContext.current
    val uiState by vm.uiState.collectAsState()

    // Shared back navigation logic
    val handleBack: () -> Unit = {
        when (uiState.step) {
            ImportStep.PREVIEW -> vm.backToInput()
            ImportStep.COMPLETE -> {
                vm.reset()
                onImportComplete()
            }
            else -> onBack()
        }
    }

    // Handle system back button
    BackHandler { handleBack() }

    // Handle one-shot events
    LaunchedEffect(Unit) {
        vm.events.collect { event ->
            when (event) {
                is ImportEvent.ValidationSuccess -> {
                    // Preview shown automatically via state
                }
                is ImportEvent.ValidationError -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
                }
                is ImportEvent.ImportSuccess -> {
                    val msg = buildString {
                        append("Imported ${event.count} template")
                        if (event.count != 1) append("s")
                        if (event.techProfileImported) append(" + tech profile")
                    }
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
                is ImportEvent.ImportError -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    CyberpunkBackdrop {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            when (uiState.step) {
                                ImportStep.INPUT, ImportStep.VALIDATING -> "Import"
                                ImportStep.PREVIEW, ImportStep.APPLYING -> "Import Preview"
                                ImportStep.COMPLETE -> "Import Complete"
                            },
                            color = CyberScheme.primary
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = handleBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = CyberScheme.primary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        ) { padding ->
            AnimatedContent(
                targetState = uiState.step,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "import_step"
            ) { step ->
                when (step) {
                    ImportStep.INPUT, ImportStep.VALIDATING -> {
                        InputStepContent(
                            modifier = Modifier.padding(padding),
                            jsonInput = uiState.jsonInput,
                            onJsonInputChange = vm::updateJsonInput,
                            onValidate = vm::validateInput,
                            onPasteFromClipboard = {
                                pasteFromClipboard(context)?.let { json ->
                                    vm.updateJsonInput(json)
                                }
                            },
                            isValidating = uiState.isProcessing,
                            errorMessage = uiState.errorMessage
                        )
                    }
                    ImportStep.PREVIEW, ImportStep.APPLYING -> {
                        PreviewStepContent(
                            modifier = Modifier.padding(padding),
                            importKind = uiState.importKind ?: "",
                            packName = uiState.packName,
                            templatePreviews = uiState.templatePreviews,
                            warnings = uiState.warnings.map { it.toString() },
                            hasTechProfile = uiState.hasTechProfile,
                            importTechProfile = uiState.importTechProfile,
                            techProfileName = uiState.techProfileName,
                            onToggleTechProfile = vm::toggleImportTechProfile,
                            onToggleTemplate = vm::toggleTemplateSelection,
                            onSetResolution = vm::setConflictResolution,
                            onApply = vm::applyImport,
                            onCancel = vm::backToInput,
                            isApplying = uiState.isProcessing,
                            selectedCount = uiState.selectedCount,
                            canApply = uiState.canApply,
                            errorMessage = uiState.errorMessage
                        )
                    }
                    ImportStep.COMPLETE -> {
                        CompleteStepContent(
                            modifier = Modifier.padding(padding),
                            importedCount = uiState.importedCount,
                            onDone = {
                                vm.reset()
                                onImportComplete()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InputStepContent(
    modifier: Modifier = Modifier,
    jsonInput: String,
    onJsonInputChange: (String) -> Unit,
    onValidate: () -> Unit,
    onPasteFromClipboard: () -> Unit,
    isValidating: Boolean,
    errorMessage: String?
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Instructions
        NeonPanel {
            Text(
                "Paste JSON from a teammate's export below, or tap the paste button to grab it from your clipboard.",
                style = MaterialTheme.typography.bodyMedium,
                color = CyberScheme.onSurface.copy(alpha = 0.8f)
            )
        }

        // JSON Input with paste button
        NeonPanel {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "JSON Input",
                    style = MaterialTheme.typography.titleSmall,
                    color = CyberScheme.primary
                )
                TextButton(onClick = onPasteFromClipboard) {
                    Icon(
                        Icons.Default.ContentPaste,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = CyberScheme.secondary
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Paste", color = CyberScheme.secondary)
                }
            }

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = jsonInput,
                onValueChange = onJsonInputChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 200.dp, max = 350.dp),
                placeholder = {
                    Text(
                        "{ \"schemaVersion\": 1, \"kind\": \"template-pack\", ... }",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                },
                textStyle = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                ),
                minLines = 8,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyberScheme.secondary.copy(alpha = 0.85f),
                    unfocusedBorderColor = CyberScheme.onSurface.copy(alpha = 0.25f),
                    cursorColor = CyberScheme.secondary,
                    focusedLabelColor = CyberScheme.secondary
                )
            )
        }

        // Error message
        AnimatedVisibility(
            visible = errorMessage != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = CyberScheme.error.copy(alpha = 0.15f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Error,
                        contentDescription = null,
                        tint = CyberScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        errorMessage ?: "",
                        color = CyberScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Validate button
        NeonMagentaButton(
            onClick = onValidate,
            modifier = Modifier.fillMaxWidth(),
            enabled = jsonInput.isNotBlank() && !isValidating
        ) {
            if (isValidating) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = CyberScheme.onPrimary,
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(8.dp))
                Text("Validating...")
            } else {
                Text("Validate & Preview")
            }
        }
    }
}

@Composable
private fun PreviewStepContent(
    modifier: Modifier = Modifier,
    importKind: String,
    packName: String?,
    templatePreviews: List<TemplatePreviewItem>,
    warnings: List<String>,
    hasTechProfile: Boolean,
    importTechProfile: Boolean,
    techProfileName: String?,
    onToggleTechProfile: () -> Unit,
    onToggleTemplate: (String) -> Unit,
    onSetResolution: (String, ConflictResolution) -> Unit,
    onApply: () -> Unit,
    onCancel: () -> Unit,
    isApplying: Boolean,
    selectedCount: Int,
    canApply: Boolean,
    errorMessage: String?
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Summary header
        NeonPanel {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        importKind,
                        style = MaterialTheme.typography.titleMedium,
                        color = CyberScheme.primary
                    )
                    if (packName != null) {
                        Text(
                            packName,
                            style = MaterialTheme.typography.bodySmall,
                            color = CyberScheme.secondary
                        )
                    }
                }
                Text(
                    "${templatePreviews.size} template${if (templatePreviews.size != 1) "s" else ""}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CyberScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }

        // Warnings section
        AnimatedVisibility(
            visible = warnings.isNotEmpty(),
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = CyberScheme.tertiary.copy(alpha = 0.15f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = CyberScheme.tertiary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            "Warnings",
                            style = MaterialTheme.typography.titleSmall,
                            color = CyberScheme.tertiary
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    warnings.forEach { warning ->
                        Text(
                            "• $warning",
                            style = MaterialTheme.typography.bodySmall,
                            color = CyberScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }

        // Tech Profile option (full backup only)
        if (hasTechProfile) {
            NeonPanel {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleTechProfile() },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = CyberScheme.tertiary,
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text(
                                "Import Tech Profile",
                                style = MaterialTheme.typography.bodyMedium,
                                color = CyberScheme.onSurface
                            )
                            Text(
                                techProfileName ?: "Unknown",
                                style = MaterialTheme.typography.bodySmall,
                                color = CyberScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                    Checkbox(
                        checked = importTechProfile,
                        onCheckedChange = { onToggleTechProfile() },
                        colors = CheckboxDefaults.colors(
                            checkedColor = CyberScheme.secondary,
                            uncheckedColor = CyberScheme.onSurface.copy(alpha = 0.5f)
                        )
                    )
                }
            }
        }

        // Templates section header
        Text(
            "Templates to Import",
            style = MaterialTheme.typography.titleMedium,
            color = CyberScheme.primary
        )

        // Template list - use LazyColumn for virtualization
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .heightIn(max = 400.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(
                items = templatePreviews,
                key = { it.template.id }
            ) { preview ->
                TemplatePreviewCard(
                    preview = preview,
                    onToggleSelection = { onToggleTemplate(preview.template.id) },
                    onSetResolution = { resolution -> onSetResolution(preview.template.id, resolution) }
                )
            }
        }

        // Error message
        AnimatedVisibility(
            visible = errorMessage != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = CyberScheme.error.copy(alpha = 0.15f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Error,
                        contentDescription = null,
                        tint = CyberScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        errorMessage ?: "",
                        color = CyberScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            NeonButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
                enabled = !isApplying
            ) {
                Text("Cancel")
            }

            NeonMagentaButton(
                onClick = onApply,
                modifier = Modifier.weight(1f),
                enabled = canApply
            ) {
                if (isApplying) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = CyberScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Importing...")
                } else {
                    Text("Import $selectedCount")
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

/**
 * Presentation model for template import status display.
 */
private data class StatusPresentation(
    val color: Color,
    val icon: ImageVector,
    val text: String
)

/**
 * Maps TemplateImportStatus to its visual presentation.
 */
@Composable
private fun TemplateImportStatus.toPresentation(): StatusPresentation = when (this) {
    TemplateImportStatus.NEW -> StatusPresentation(
        color = CyberScheme.secondary,
        icon = Icons.Default.NewReleases,
        text = "New"
    )
    TemplateImportStatus.WILL_REPLACE -> StatusPresentation(
        color = CyberScheme.tertiary,
        icon = Icons.Default.SwapHoriz,
        text = "Will Replace"
    )
    TemplateImportStatus.CONFLICT -> StatusPresentation(
        color = CyberScheme.error,
        icon = Icons.Default.Warning,
        text = "Conflict"
    )
}

@Composable
private fun TemplatePreviewCard(
    preview: TemplatePreviewItem,
    onToggleSelection: () -> Unit,
    onSetResolution: (ConflictResolution) -> Unit
) {
    val presentation = preview.status.toPresentation()

    Card(
        colors = CardDefaults.cardColors(
            containerColor = CyberScheme.surface.copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header row with checkbox, name, and status
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleSelection() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Checkbox(
                        checked = preview.isSelected,
                        onCheckedChange = { onToggleSelection() },
                        colors = CheckboxDefaults.colors(
                            checkedColor = CyberScheme.secondary,
                            uncheckedColor = CyberScheme.onSurface.copy(alpha = 0.5f)
                        )
                    )
                    Column {
                        Text(
                            preview.template.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = CyberScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "ID: ${preview.template.id}",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = CyberScheme.onSurface.copy(alpha = 0.5f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Status pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(presentation.color.copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            presentation.icon,
                            contentDescription = null,
                            tint = presentation.color,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            presentation.text,
                            style = MaterialTheme.typography.labelSmall,
                            color = presentation.color
                        )
                    }
                }
            }

            // Content preview
            Spacer(Modifier.height(8.dp))
            Text(
                preview.template.content.take(120).let {
                    if (preview.template.content.length > 120) "$it..." else it
                },
                style = MaterialTheme.typography.bodySmall,
                color = CyberScheme.onSurface.copy(alpha = 0.6f),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            // Conflict resolution options
            if (preview.status == TemplateImportStatus.CONFLICT && preview.isSelected) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = CyberScheme.onSurface.copy(alpha = 0.1f))
                Spacer(Modifier.height(12.dp))

                Text(
                    "How to handle this conflict:",
                    style = MaterialTheme.typography.labelMedium,
                    color = CyberScheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ConflictResolution.entries.forEach { resolution ->
                        val isSelected = preview.conflictResolution == resolution
                        val label = when (resolution) {
                            ConflictResolution.REPLACE -> "Replace"
                            ConflictResolution.KEEP_EXISTING -> "Skip"
                            ConflictResolution.SAVE_AS_COPY -> "Copy"
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) CyberScheme.secondary.copy(alpha = 0.3f)
                                    else CyberScheme.surface.copy(alpha = 0.3f)
                                )
                                .clickable { onSetResolution(resolution) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                label,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isSelected) CyberScheme.secondary else CyberScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompleteStepContent(
    modifier: Modifier = Modifier,
    importedCount: Int,
    onDone: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            tint = CyberScheme.secondary,
            modifier = Modifier.size(72.dp)
        )

        Spacer(Modifier.height(24.dp))

        Text(
            "Import Complete!",
            style = MaterialTheme.typography.headlineSmall,
            color = CyberScheme.primary
        )

        Spacer(Modifier.height(8.dp))

        Text(
            "Successfully imported $importedCount template${if (importedCount != 1) "s" else ""}",
            style = MaterialTheme.typography.bodyMedium,
            color = CyberScheme.onSurface.copy(alpha = 0.7f)
        )

        Spacer(Modifier.height(32.dp))

        NeonMagentaButton(
            onClick = onDone,
            modifier = Modifier.fillMaxWidth(0.6f)
        ) {
            Text("Done")
        }
    }
}

/**
 * Paste text from the clipboard.
 */
private fun pasteFromClipboard(context: Context): String? {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    return clipboard.primaryClip?.getItemAt(0)?.text?.toString()
}
