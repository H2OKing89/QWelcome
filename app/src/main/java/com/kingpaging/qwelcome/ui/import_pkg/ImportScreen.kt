@file:OptIn(ExperimentalMaterial3Api::class)

package com.kingpaging.qwelcome.ui.import_pkg

import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.kingpaging.qwelcome.R
import com.kingpaging.qwelcome.data.ImportValidationResult
import com.kingpaging.qwelcome.ui.components.CyberpunkBackdrop
import com.kingpaging.qwelcome.ui.components.NeonButton
import com.kingpaging.qwelcome.ui.components.NeonButtonStyle
import com.kingpaging.qwelcome.ui.components.NeonPanel
import com.kingpaging.qwelcome.ui.theme.LocalCyberColors
import com.kingpaging.qwelcome.util.rememberHapticFeedback
import com.kingpaging.qwelcome.viewmodel.import_pkg.ImportUiState
import com.kingpaging.qwelcome.viewmodel.import_pkg.ImportStep

@Suppress("FunctionNaming", "LongMethod", "LongParameterList")
@Composable
fun ImportScreen(
    uiState: ImportUiState,
    onBack: () -> Unit,
    onImportComplete: () -> Unit,
    onOpenFile: () -> Unit,
    onPaste: () -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    BackHandler { onBack() }

    val haptic = rememberHapticFeedback()

    CyberpunkBackdrop {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.title_import), color = MaterialTheme.colorScheme.primary) },
                    navigationIcon = {
                        IconButton(onClick = { haptic(); onBack() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.content_desc_back),
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
                                onOpenFile = onOpenFile,
                                onPaste = onPaste
                            )
                        }
                        is ImportStep.Validated -> {
                            ConfirmStep(
                                isLoading = uiState.isImporting,
                                validationResult = step.validationResult,
                                onConfirm = onConfirm,
                                onCancel = onCancel
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
            contentDescription = stringResource(R.string.content_desc_import_data),
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.secondary
        )
        Text(
            stringResource(R.string.title_import_intro),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            stringResource(R.string.text_import_intro_description),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        )

        Spacer(Modifier.height(16.dp))
        
        if (isLoading) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
            Text(
                stringResource(R.string.status_decoding),
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
                    contentDescription = stringResource(R.string.content_desc_select_file_to_import),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.action_select_file))
            }

            NeonButton(
                onClick = onPaste,
                modifier = Modifier.fillMaxWidth(0.8f),
                style = NeonButtonStyle.SECONDARY,
                glowColor = MaterialTheme.colorScheme.tertiary
            ) {
                Icon(
                    Icons.Default.FileCopy,
                    contentDescription = stringResource(R.string.content_desc_paste_from_clipboard),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.action_paste_from_clipboard))
            }
        }

        AnimatedVisibility(visible = error != null) {
            error?.let { errorMessage ->
                NeonPanel(
                    modifier = Modifier.padding(top = 24.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = stringResource(R.string.content_desc_error),
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
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
        Text(stringResource(R.string.title_ready_to_import), style = MaterialTheme.typography.titleLarge)

        NeonPanel {
            Text(
                stringResource(R.string.text_import_overwrite_warning),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            when (validationResult) {
                is ImportValidationResult.ValidTemplatePack -> {
                    InfoRow(stringResource(R.string.label_backup_type), stringResource(R.string.export_type_template_pack))
                    InfoRow(stringResource(R.string.label_template_count), validationResult.pack.templates.size.toString())
                    InfoRow(stringResource(R.string.label_tech_profile), stringResource(R.string.label_not_included))
                    if (validationResult.hasConflicts) {
                        val count = validationResult.conflicts.size
                        InfoRow(stringResource(R.string.label_conflicts), pluralStringResource(R.plurals.import_conflicts, count, count))
                    }
                }
                is ImportValidationResult.ValidFullBackup -> {
                    InfoRow(stringResource(R.string.label_backup_type), stringResource(R.string.export_type_full_backup))
                    InfoRow(stringResource(R.string.label_template_count), validationResult.backup.templates.size.toString())
                    InfoRow(stringResource(R.string.label_tech_profile), stringResource(R.string.label_included))
                    if (validationResult.hasConflicts) {
                        val count = validationResult.conflicts.size
                        InfoRow(stringResource(R.string.label_conflicts), pluralStringResource(R.plurals.import_conflicts, count, count))
                    }
                }
                is ImportValidationResult.Invalid -> {
                    // This shouldn't happen in this screen, but handle it gracefully
                    Text(
                        stringResource(R.string.error_invalid_import_data),
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
                stringResource(R.string.status_importing),
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
                    Text(stringResource(R.string.action_cancel))
                }
                NeonButton(
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f),
                    glowColor = LocalCyberColors.current.success
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = stringResource(R.string.content_desc_confirm_import),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.action_confirm))
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
                contentDescription = stringResource(R.string.content_desc_success),
                tint = LocalCyberColors.current.success,
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(stringResource(R.string.title_import_complete), style = MaterialTheme.typography.headlineSmall)
        Text(
            stringResource(R.string.text_import_complete_description),
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
            Text(stringResource(R.string.action_done))
        }
    }
}
