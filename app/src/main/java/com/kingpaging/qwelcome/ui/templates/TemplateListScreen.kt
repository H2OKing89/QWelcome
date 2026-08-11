package com.kingpaging.qwelcome.ui.templates

import android.content.Context
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
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.kingpaging.qwelcome.R
import com.kingpaging.qwelcome.data.DEFAULT_TEMPLATE_ID
import com.kingpaging.qwelcome.data.NEW_TEMPLATE_ID
import com.kingpaging.qwelcome.data.Template
import com.kingpaging.qwelcome.di.LocalSoundPlayer
import com.kingpaging.qwelcome.di.LocalTemplateListViewModel
import com.kingpaging.qwelcome.ui.components.CyberpunkBackdrop
import com.kingpaging.qwelcome.ui.components.NeonButton
import com.kingpaging.qwelcome.ui.components.NeonButtonStyle
import com.kingpaging.qwelcome.ui.components.NeonWarningBanner
import com.kingpaging.qwelcome.util.SoundPlayer
import com.kingpaging.qwelcome.util.rememberHapticFeedback
import com.kingpaging.qwelcome.viewmodel.templates.TemplateListEvent
import com.kingpaging.qwelcome.viewmodel.templates.TemplateListEventOwner
import com.kingpaging.qwelcome.viewmodel.templates.TemplateListViewModel
import com.kingpaging.qwelcome.viewmodel.templates.filterAndOrderTemplates
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Suppress("FunctionNaming", "LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateListScreen(
    onBack: () -> Unit,
    onOpenEditor: (String) -> Unit
) {
    val vm = LocalTemplateListViewModel.current
    val soundPlayer = LocalSoundPlayer.current
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val haptic = rememberHapticFeedback()
    val snackbarHostState = remember { SnackbarHostState() }
    var showTagFilters by remember { mutableStateOf(false) }
    var previewTemplate by remember { mutableStateOf<Template?>(null) }
    var renameTemplate by remember { mutableStateOf<Template?>(null) }

    BackHandler { onBack() }

    CollectTemplateListEffects(
        viewModel = vm,
        soundPlayer = soundPlayer,
        snackbarHostState = snackbarHostState,
        onOpenEditor = onOpenEditor
    )

    uiState.showDeleteConfirmation?.let { template ->
        DeleteConfirmationDialog(
            templateName = template.name,
            isActive = template.id == uiState.activeTemplateId,
            onConfirm = { vm.deleteTemplate(template.id) },
            onDismiss = { vm.dismissDeleteConfirmation() }
        )
    }

    if (showTagFilters) {
        TemplateTagFilterSheet(
            allTags = uiState.allTags,
            selectedTags = uiState.selectedTags,
            onToggle = {
                haptic()
                vm.updateTagFilter(it)
            },
            onClear = {
                haptic()
                vm.clearTagFilter()
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
                vm.renameTemplate(template.id, name)
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
                        onQueryChange = vm::updateSearchQuery,
                        onClearSearch = {
                            haptic()
                            vm.updateSearchQuery("")
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
                                    onDismiss = { haptic(); vm.dismissTemplateLimitWarning() }
                                )
                            }
                        }

                        items(items = filteredTemplates, key = { it.id }) { template ->
                            TemplateCard(
                                template = template,
                                isActive = template.id == uiState.activeTemplateId,
                                isDefault = template.id == DEFAULT_TEMPLATE_ID,
                                onSelect = { haptic(); vm.setActiveTemplate(template.id) },
                                onPreview = {
                                    haptic()
                                    previewTemplate = template
                                },
                                onEdit = {
                                    haptic()
                                    if (template.id == DEFAULT_TEMPLATE_ID) {
                                        vm.duplicateAndEdit(template)
                                    } else {
                                        onOpenEditor(template.id)
                                    }
                                },
                                onRename = {
                                    haptic()
                                    renameTemplate = template
                                },
                                onDuplicate = { haptic(); vm.duplicateTemplate(template) },
                                onDelete = { haptic(); vm.showDeleteConfirmation(template) },
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
                                            vm.updateSearchQuery("")
                                            vm.clearTagFilter()
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

@Suppress("FunctionNaming", "LocalContextGetResourceValueCall")
@Composable
private fun CollectTemplateListEffects(
    viewModel: TemplateListViewModel,
    soundPlayer: SoundPlayer,
    snackbarHostState: SnackbarHostState,
    onOpenEditor: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnOpenEditor by rememberUpdatedState(onOpenEditor)

    LaunchedEffect(viewModel, lifecycleOwner, soundPlayer, snackbarHostState) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            launch {
                viewModel.eventsFor(TemplateListEventOwner.LIBRARY).collectLatest { event ->
                    if (event is TemplateListEvent.TemplateDuplicated && event.openEditor) {
                        currentOnOpenEditor(event.template.id)
                    }
                    val snackbar = event.toSnackbar(context) ?: return@collectLatest
                    if (event.shouldBeep()) {
                        soundPlayer.playBeep()
                    }
                    val result = snackbarHostState.showSnackbar(
                        message = snackbar.message,
                        actionLabel = snackbar.actionLabel,
                        duration = snackbar.duration
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        event.performSnackbarAction(viewModel, currentOnOpenEditor)
                    }
                }
            }
        }
    }
}

private data class TemplateSnackbar(
    val message: String,
    val actionLabel: String? = null,
    val duration: SnackbarDuration = SnackbarDuration.Short
)

private fun TemplateListEvent.toSnackbar(
    context: Context
): TemplateSnackbar? = when (this) {
    is TemplateListEvent.Error -> TemplateSnackbar(
        message = checkNotNull(toTemplateErrorMessage(context)),
        duration = SnackbarDuration.Long
    )
    is TemplateListEvent.TemplateDeleted -> TemplateSnackbar(
        context.getString(R.string.toast_template_deleted, name)
    )
    is TemplateListEvent.TemplateDuplicated -> TemplateSnackbar(
        context.getString(R.string.toast_template_duplicated, template.name)
    )
    is TemplateListEvent.ActiveTemplateChanged -> TemplateSnackbar(
        message = context.getString(R.string.toast_template_active, template.name),
        actionLabel = context.getString(R.string.action_undo),
        duration = SnackbarDuration.Long
    )
    is TemplateListEvent.TemplateSelectionBlocked -> TemplateSnackbar(
        message = checkNotNull(toTemplateErrorMessage(context)),
        actionLabel = context.getString(R.string.action_fix),
        duration = SnackbarDuration.Long
    )
    is TemplateListEvent.TemplateRenamed -> TemplateSnackbar(
        context.getString(R.string.toast_template_renamed, template.name)
    )
}

private fun TemplateListEvent.shouldBeep(): Boolean =
    this is TemplateListEvent.Error || this is TemplateListEvent.TemplateSelectionBlocked

private fun TemplateListEvent.performSnackbarAction(
    viewModel: TemplateListViewModel,
    onOpenEditor: (String) -> Unit
) {
    when (this) {
        is TemplateListEvent.ActiveTemplateChanged -> viewModel.undoTemplateSelection(change)
        is TemplateListEvent.TemplateSelectionBlocked -> onOpenEditor(template.id)
        else -> Unit
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
