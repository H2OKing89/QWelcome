@file:OptIn(ExperimentalMaterial3Api::class)

package com.kingpaging.qwelcome.ui.import_pkg

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
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
                            ImportIdleStep(
                                isLoading = uiState.isImporting,
                                error = uiState.error,
                                onOpenFile = onOpenFile,
                                onPaste = onPaste
                            )
                        }
                        is ImportStep.Validated -> {
                            ImportConfirmStep(
                                isLoading = uiState.isImporting,
                                validationResult = step.validationResult,
                                onConfirm = onConfirm,
                                onCancel = onCancel
                            )
                        }
                        is ImportStep.Complete -> {
                            ImportCompleteStep(onDone = onImportComplete)
                        }
                    }
                }
            }
        }
    }
}
