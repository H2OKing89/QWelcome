@file:OptIn(ExperimentalMaterial3Api::class)

package com.kingpaging.qwelcome.ui.import_pkg

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FileCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.UploadFile
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kingpaging.qwelcome.data.ImportValidationResult
import com.kingpaging.qwelcome.di.LocalImportViewModel
import com.kingpaging.qwelcome.ui.components.CyberpunkBackdrop
import com.kingpaging.qwelcome.ui.components.NeonButton
import com.kingpaging.qwelcome.ui.components.NeonButtonStyle
import com.kingpaging.qwelcome.ui.components.NeonPanel
import com.kingpaging.qwelcome.ui.theme.LocalCyberColors
import com.kingpaging.qwelcome.viewmodel.import_pkg.ImportEvent
import com.kingpaging.qwelcome.viewmodel.import_pkg.ImportStep

@Composable
fun ImportScreen(
    onBack: () -> Unit,
    onImportComplete: () -> Unit
) {
    val vm = LocalImportViewModel.current
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val uiState by vm.uiState.collectAsState()

    // Reset ViewModel state when entering the screen to clear any stale events
    LaunchedEffect(Unit) {
        vm.reset()
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val json = inputStream.bufferedReader().use { it.readText() }
                    vm.onJsonContentReceived(json)
                } ?: Toast.makeText(context, "Could not open file", Toast.LENGTH_LONG).show()
            } catch (e: SecurityException) {
                Toast.makeText(context, "Permission denied to read file", Toast.LENGTH_LONG).show()
            } catch (e: java.io.IOException) {
                Toast.makeText(context, "Error reading file: ${e.message}", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Unexpected error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    LaunchedEffect(Unit) {
        vm.events.collect { event ->
            when (event) {
                is ImportEvent.ImportSuccess -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
                }
                is ImportEvent.ImportFailed -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
                }
                is ImportEvent.RequestFileOpen -> {
                    filePickerLauncher.launch("application/json")
                }
            }
        }
    }

    BackHandler { onBack() }

    CyberpunkBackdrop {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("Import", color = MaterialTheme.colorScheme.primary) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { padding ->
            AnimatedContent(
                targetState = uiState.step,
                label = "ImportStepAnimation"
            ) { step ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (step) {
                        is ImportStep.Idle -> {
                            IdleStep(
                                isLoading = uiState.isImporting,
                                error = uiState.error,
                                onOpenFile = { vm.onOpenFileRequest() },
                                onPaste = {
                                    try {
                                        clipboardManager.getText()?.let {
                                            vm.onPasteContent(it.text)
                                        } ?: Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_SHORT).show()
                                    } catch (e: SecurityException) {
                                        Toast.makeText(context, "Cannot access clipboard", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                        is ImportStep.Validated -> {
                            ConfirmStep(
                                isLoading = uiState.isImporting,
                                validationResult = step.validationResult,
                                onConfirm = { vm.onImportConfirmed() },
                                onCancel = { vm.reset() }
                            )
                        }
                        is ImportStep.Complete -> {
                            CompleteStep(onDone = onImportComplete)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IdleStep(
    isLoading: Boolean,
    error: String?,
    onOpenFile: () -> Unit,
    onPaste: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = Icons.Default.UploadFile,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.secondary
        )
        Text(
            "Import Templates or a Full Backup",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            "You can import from a file or paste the backup text directly.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        )

        Spacer(Modifier.height(16.dp))
        
        if (isLoading) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
            Text(
                "Decoding...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 8.dp)
            )
        } else {
            NeonButton(
                onClick = onOpenFile,
                modifier = Modifier.fillMaxWidth(0.8f),
                glowColor = MaterialTheme.colorScheme.secondary
            ) {
                Icon(
                    Icons.Default.UploadFile,
                    contentDescription = "Select file to import",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Select File")
            }

            NeonButton(
                onClick = onPaste,
                modifier = Modifier.fillMaxWidth(0.8f),
                style = NeonButtonStyle.SECONDARY,
                glowColor = MaterialTheme.colorScheme.tertiary
            ) {
                Icon(
                    Icons.Default.FileCopy,
                    contentDescription = "Paste from clipboard",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Paste from Clipboard")
            }
        }

        AnimatedVisibility(visible = error != null) {
            NeonPanel(
                modifier = Modifier.padding(top = 24.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = "Error",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun ConfirmStep(
    isLoading: Boolean,
    validationResult: ImportValidationResult,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Ready to Import", style = MaterialTheme.typography.titleLarge)

        NeonPanel {
            Text(
                "The backup contains the following data. This will overwrite any existing data with the same name.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            when (validationResult) {
                is ImportValidationResult.ValidTemplatePack -> {
                    InfoRow("Backup Type", "Template Pack")
                    InfoRow("Template Count", validationResult.pack.templates.size.toString())
                    InfoRow("Tech Profile", "Not Included")
                    if (validationResult.hasConflicts) {
                        InfoRow("Conflicts", "${validationResult.conflicts.size} template(s) will be replaced")
                    }
                }
                is ImportValidationResult.ValidFullBackup -> {
                    InfoRow("Backup Type", "Full Backup")
                    InfoRow("Template Count", validationResult.backup.templates.size.toString())
                    InfoRow("Tech Profile", "Included")
                    if (validationResult.hasConflicts) {
                        InfoRow("Conflicts", "${validationResult.conflicts.size} template(s) will be replaced")
                    }
                }
                is ImportValidationResult.Invalid -> {
                    // This shouldn't happen in this screen, but handle it gracefully
                    Text(
                        "Invalid import data",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        if (isLoading) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
            Text(
                "Importing...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 8.dp)
            )
        } else {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                NeonButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                    style = NeonButtonStyle.TERTIARY,
                    glowColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                ) {
                    Text("Cancel")
                }
                NeonButton(
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f),
                    glowColor = LocalCyberColors.current.success
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Confirm import",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Confirm")
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun CompleteStep(
    onDone: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 64.dp) // Push content up
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(128.dp)
                .clip(CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Success",
                tint = LocalCyberColors.current.success,
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(Modifier.height(24.dp))
        Text("Import Complete!", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Your data has been successfully restored.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
            modifier = Modifier.padding(top = 8.dp)
        )
        Spacer(Modifier.height(32.dp))
        NeonButton(
            onClick = onDone,
            modifier = Modifier.fillMaxWidth(0.7f),
            glowColor = MaterialTheme.colorScheme.secondary
        ) {
            Text("Done")
        }
    }
}
