package com.kingpaging.qwelcome.ui.import_pkg

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.kingpaging.qwelcome.viewmodel.import_pkg.ImportStep
import com.kingpaging.qwelcome.viewmodel.import_pkg.ImportUiState

@Suppress("FunctionNaming", "LongMethod", "LongParameterList")
@Composable
fun ImportScreen(
    uiState: ImportUiState,
    onBack: () -> Unit,
    onImportComplete: () -> Unit,
    onOpenFile: () -> Unit,
    onPaste: () -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    BackHandler { onBack() }

    val haptic = rememberHapticFeedback()

    CyberpunkBackdrop {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                NeonTopAppBar(
                    title = { Text(stringResource(R.string.title_import)) },
                    navigationIcon = {
                        IconButton(onClick = {
                            haptic()
                            onBack()
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
            AnimatedContent(
                targetState = uiState.step,
                label = "ImportStepAnimation",
            ) { step ->
                CenteredFormColumn(padding = padding) {
                    Text(
                        stringResource(
                            when (step) {
                                is ImportStep.Idle -> R.string.ux_import_step_source
                                is ImportStep.Validated -> R.string.ux_import_step_review
                                is ImportStep.Complete -> R.string.ux_import_step_complete
                            },
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    when (step) {
                        is ImportStep.Idle -> {
                            ImportIdleStep(
                                isLoading = uiState.isImporting,
                                error = uiState.error,
                                onOpenFile = onOpenFile,
                                onPaste = onPaste,
                            )
                        }
                        is ImportStep.Validated -> {
                            ImportConfirmStep(
                                isLoading = uiState.isImporting,
                                validationResult = step.validationResult,
                                onConfirm = onConfirm,
                                onCancel = onCancel,
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
