package com.kingpaging.qwelcome.ui.templates

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kingpaging.qwelcome.R
import com.kingpaging.qwelcome.data.DEFAULT_TEMPLATE_ID
import com.kingpaging.qwelcome.data.NEW_TEMPLATE_ID
import com.kingpaging.qwelcome.data.Template
import com.kingpaging.qwelcome.ui.components.CyberpunkBackdrop
import com.kingpaging.qwelcome.ui.components.NeonButton
import com.kingpaging.qwelcome.ui.components.NeonButtonStyle
import com.kingpaging.qwelcome.ui.components.NeonWarningBanner
import com.kingpaging.qwelcome.util.rememberHapticFeedback
import com.kingpaging.qwelcome.viewmodel.templates.TemplateListUiState
import com.kingpaging.qwelcome.viewmodel.templates.filterAndOrderTemplates

@Suppress("FunctionNaming", "LongMethod", "LongParameterList")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateListScreen(
    uiState: TemplateListUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onOpenEditor: (String) -> Unit,
    onDeleteTemplate: (String) -> Unit,
    onDismissDeleteConfirmation: () -> Unit,
    onUpdateTagFilter: (String) -> Unit,
    onClearTagFilter: () -> Unit,
    onRenameTemplate: (String, String) -> Unit,
    onUpdateSearchQuery: (String) -> Unit,
    onDismissTemplateLimitWarning: () -> Unit,
    onSetActiveTemplate: (String) -> Unit,
    onDuplicateAndEdit: (Template) -> Unit,
    onDuplicateTemplate: (Template) -> Unit,
    onShowDeleteConfirmation: (Template) -> Unit
) {
    val haptic = rememberHapticFeedback()
    var showTagFilters by remember { mutableStateOf(false) }
    var previewTemplate by remember { mutableStateOf<Template?>(null) }
    var renameTemplate by remember { mutableStateOf<Template?>(null) }

    BackHandler { onBack() }

    uiState.showDeleteConfirmation?.let { template ->
        DeleteConfirmationDialog(
            templateName = template.name,
            isActive = template.id == uiState.activeTemplateId,
            onConfirm = { onDeleteTemplate(template.id) },
            onDismiss = onDismissDeleteConfirmation
        )
    }

    if (showTagFilters) {
        TemplateTagFilterSheet(
            allTags = uiState.allTags,
            selectedTags = uiState.selectedTags,
            onToggle = {
                haptic()
                onUpdateTagFilter(it)
            },
            onClear = {
                haptic()
                onClearTagFilter()
            },
            onDismiss = { showTagFilters = false }
        )
    }

    previewTemplate?.let { template ->
        TemplatePreviewSheet(
            template = template,
            onDismiss = { previewTemplate = null }
        )
    }

    renameTemplate?.let { template ->
        RenameTemplateDialog(
            template = template,
            onRename = { name ->
                onRenameTemplate(template.id, name)
                renameTemplate = null
            },
            onDismiss = { renameTemplate = null }
        )
    }

    CyberpunkBackdrop {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.title_templates),
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            haptic()
                            onBack()
                        }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.content_desc_back),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            haptic()
                            onOpenEditor(NEW_TEMPLATE_ID)
                        }) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = stringResource(R.string.action_new_template),
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { padding ->
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                val filteredTemplates = remember(
                    uiState.templates,
                    uiState.activeTemplateId,
                    uiState.searchQuery,
                    uiState.selectedTags,
                    uiState.templateLastUsedAt
                ) {
                    filterAndOrderTemplates(
                        templates = uiState.templates,
                        activeTemplateId = uiState.activeTemplateId,
                        searchQuery = uiState.searchQuery,
                        selectedTags = uiState.selectedTags,
                        templateLastUsedAt = uiState.templateLastUsedAt
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp)
                ) {
                    TemplateLibraryControls(
                        query = uiState.searchQuery,
                        selectedTagCount = uiState.selectedTags.size,
                        hasTags = uiState.allTags.isNotEmpty(),
                        onQueryChange = onUpdateSearchQuery,
                        onClearSearch = {
                            haptic()
                            onUpdateSearchQuery("")
                        },
                        onOpenTags = {
                            haptic()
                            showTagFilters = true
                        }
                    )

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (uiState.showTemplateLimitWarning && !uiState.warningDismissed) {
                            item(key = "template_limit_warning") {
                                NeonWarningBanner(
                                    text = androidx.compose.ui.res.pluralStringResource(
                                        R.plurals.warning_template_limit,
                                        uiState.templates.size,
                                        uiState.templates.size
                                    ),
                                    onDismiss = { haptic(); onDismissTemplateLimitWarning() }
                                )
                            }
                        }

                        items(items = filteredTemplates, key = { it.id }) { template ->
                            TemplateCard(
                                template = template,
                                isActive = template.id == uiState.activeTemplateId,
                                isDefault = template.id == DEFAULT_TEMPLATE_ID,
                                onSelect = { haptic(); onSetActiveTemplate(template.id) },
                                onPreview = {
                                    haptic()
                                    previewTemplate = template
                                },
                                onEdit = {
                                    haptic()
                                    if (template.id == DEFAULT_TEMPLATE_ID) {
                                        onDuplicateAndEdit(template)
                                    } else {
                                        onOpenEditor(template.id)
                                    }
                                },
                                onRename = {
                                    haptic()
                                    renameTemplate = template
                                },
                                onDuplicate = { haptic(); onDuplicateTemplate(template) },
                                onDelete = { haptic(); onShowDeleteConfirmation(template) },
                                modifier = Modifier.animateItem()
                            )
                        }

                        val shouldShowNoResults = filteredTemplates.isEmpty()
                        if (shouldShowNoResults) {
                            item(key = "no_results") {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Text(
                                        text = when {
                                            uiState.searchQuery.isNotBlank() &&
                                                uiState.selectedTags.isNotEmpty() -> {
                                                stringResource(
                                                    R.string.text_no_templates_match_with_filters,
                                                    uiState.searchQuery
                                                )
                                            }

                                            uiState.searchQuery.isNotBlank() -> {
                                                stringResource(
                                                    R.string.text_no_templates_match,
                                                    uiState.searchQuery
                                                )
                                            }

                                            else -> stringResource(R.string.text_no_templates_for_filters)
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                    NeonButton(
                                        onClick = {
                                            haptic()
                                            onUpdateSearchQuery("")
                                            onClearTagFilter()
                                        },
                                        glowColor = MaterialTheme.colorScheme.secondary,
                                        style = NeonButtonStyle.TERTIARY
                                    ) {
                                        Text(stringResource(R.string.action_clear_filters))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeleteConfirmationDialog(
    templateName: String,
    isActive: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = stringResource(R.string.title_delete_template),
                color = MaterialTheme.colorScheme.error
            )
        },
        text = {
            Text(
                text = stringResource(
                    if (isActive) R.string.text_delete_active_template_confirm
                    else R.string.text_delete_template_confirm,
                    templateName
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        confirmButton = {
            NeonButton(
                onClick = onConfirm,
                glowColor = MaterialTheme.colorScheme.error,
                style = NeonButtonStyle.PRIMARY
            ) {
                Text(stringResource(R.string.action_delete))
            }
        },
        dismissButton = {
            NeonButton(
                onClick = onDismiss,
                glowColor = MaterialTheme.colorScheme.secondary,
                style = NeonButtonStyle.TERTIARY
            ) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
