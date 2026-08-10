package com.kingpaging.qwelcome.ui.templates

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.kingpaging.qwelcome.ui.components.NeonOutlinedField
import com.kingpaging.qwelcome.ui.components.NeonWarningBanner
import com.kingpaging.qwelcome.ui.theme.LocalDarkTheme
import com.kingpaging.qwelcome.util.rememberHapticFeedback
import com.kingpaging.qwelcome.viewmodel.templates.TemplateListEvent
import kotlinx.coroutines.launch

@Suppress("LocalContextGetResourceValueCall")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TemplateListScreen(
    onBack: () -> Unit,
    onOpenEditor: () -> Unit
) {
    val vm = LocalTemplateListViewModel.current
    val soundPlayer = LocalSoundPlayer.current
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val haptic = rememberHapticFeedback()

    BackHandler { onBack() }

    LaunchedEffect(vm, lifecycleOwner, onOpenEditor, soundPlayer, context) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            launch {
                vm.navigateToEditor.collect {
                    onOpenEditor()
                }
            }
            launch {
                vm.events.collect { event ->
                    when (event) {
                        is TemplateListEvent.Error -> {
                            soundPlayer.playBeep()
                            Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
                        }

                        is TemplateListEvent.TemplateDeleted -> {
                            Toast.makeText(
                                context,
                                context.getString(R.string.toast_template_deleted, event.name),
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        is TemplateListEvent.TemplateDuplicated -> {
                            Toast.makeText(
                                context,
                                context.getString(R.string.toast_template_duplicated, event.template.name),
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        is TemplateListEvent.ActiveTemplateChanged -> {
                            Toast.makeText(
                                context,
                                context.getString(R.string.toast_template_active, event.template.name),
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        is TemplateListEvent.TemplateCreated,
                        is TemplateListEvent.TemplateUpdated -> Unit
                    }
                }
            }
        }
    }

    uiState.showDeleteConfirmation?.let { template ->
        DeleteConfirmationDialog(
            templateName = template.name,
            onConfirm = { vm.deleteTemplate(template.id) },
            onDismiss = { vm.dismissDeleteConfirmation() }
        )
    }

    CyberpunkBackdrop {
        Scaffold(
            containerColor = Color.Transparent,
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
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = {
                        haptic()
                        vm.startEditing(Template(id = NEW_TEMPLATE_ID, name = "", content = ""))
                    },
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(R.string.content_desc_create_template)
                        )
                    },
                    text = { Text(stringResource(R.string.action_new_template)) }
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
                val filteredTemplates = remember(uiState.templates, uiState.searchQuery, uiState.selectedTags) {
                    val query = uiState.searchQuery.trim().lowercase()
                    val normalizedSelectedTags = uiState.selectedTags
                        .map { it.trim().lowercase() }
                        .filter { it.isNotBlank() }
                        .toSet()
                    val matchesTag: (Template) -> Boolean = { template ->
                        normalizedSelectedTags.isEmpty() || template.tags.any { tag ->
                            tag.trim().lowercase() in normalizedSelectedTags
                        }
                    }
                    val matchesQuery: (Template) -> Boolean = { template ->
                        query.isEmpty() ||
                            template.name.contains(query, ignoreCase = true) ||
                            template.content.contains(query, ignoreCase = true) ||
                            template.tags.any { it.contains(query, ignoreCase = true) }
                    }
                    val defaultTemplate = uiState.templates
                        .find { it.id == DEFAULT_TEMPLATE_ID }
                        ?.takeIf(matchesTag)
                        ?.takeIf(matchesQuery)
                    val userTemplates = uiState.templates
                        .filter { it.id != DEFAULT_TEMPLATE_ID }
                        .filter(matchesTag)
                        .filter(matchesQuery)

                    listOfNotNull(defaultTemplate) + userTemplates
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item(key = "search") {
                        NeonOutlinedField(
                            value = uiState.searchQuery,
                            onValueChange = { vm.updateSearchQuery(it) },
                            label = { Text(stringResource(R.string.label_search_templates)) },
                            singleLine = true,
                            trailingIcon = {
                                if (uiState.searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { haptic(); vm.updateSearchQuery("") }) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = stringResource(R.string.content_desc_clear_search),
                                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        )
                                    }
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = stringResource(R.string.content_desc_search_templates),
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                }
                            },
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }

                    if (uiState.allTags.isNotEmpty()) {
                        item(key = "tag_filters") {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(bottom = 4.dp)
                            ) {
                                item(key = "all_tags") {
                                    FilterChip(
                                        selected = uiState.selectedTags.isEmpty(),
                                        onClick = { haptic(); vm.clearTagFilter() },
                                        label = { Text(stringResource(R.string.label_all_tags)) }
                                    )
                                }

                                items(items = uiState.allTags.sorted(), key = { it }) { tag ->
                                    val isSelected = tag in uiState.selectedTags
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { haptic(); vm.updateTagFilter(tag) },
                                        label = { Text(tag) },
                                        leadingIcon = if (isSelected) {
                                            {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                                                )
                                            }
                                        } else {
                                            null
                                        },
                                        modifier = Modifier.semantics {
                                            contentDescription = context.getString(
                                                R.string.content_desc_filter_by_tag,
                                                tag
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }

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
                            onEdit = {
                                haptic()
                                if (template.id == DEFAULT_TEMPLATE_ID) {
                                    vm.duplicateAndEdit(template)
                                } else {
                                    vm.startEditing(template)
                                }
                            },
                            onDuplicate = { haptic(); vm.duplicateTemplate(template) },
                            onDelete = { haptic(); vm.showDeleteConfirmation(template) },
                            onTagClick = { tag -> haptic(); vm.updateTagFilter(tag) }
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
                                    text = if (uiState.searchQuery.isNotEmpty()) {
                                        stringResource(R.string.text_no_templates_match, uiState.searchQuery)
                                    } else {
                                        stringResource(R.string.text_no_templates_for_filters)
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

                    item(key = "bottom_spacer") {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TemplateCard(
    template: Template,
    isActive: Boolean,
    isDefault: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onTagClick: (String) -> Unit
) {
    val isDark = LocalDarkTheme.current
    val haptic = rememberHapticFeedback()
    var actionsExpanded by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) {
                MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.15f else 0.12f)
            } else {
                if (isDark) MaterialTheme.colorScheme.surface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(
            if (isActive) 1.dp else 0.5.dp,
            if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isDark) 0.35f else 0.5f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = template.name,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                        color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isDefault) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = stringResource(R.string.content_desc_builtin_template),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                if (!isDefault) {
                    Box {
                        IconButton(
                            onClick = {
                                haptic()
                                actionsExpanded = true
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = stringResource(R.string.content_desc_template_actions),
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = actionsExpanded,
                            onDismissRequest = { actionsExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_duplicate_template)) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    actionsExpanded = false
                                    onDuplicate()
                                }
                            )

                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_delete_template)) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                },
                                onClick = {
                                    actionsExpanded = false
                                    onDelete()
                                }
                            )
                        }
                    }
                }
            }

            Text(
                text = template.content.take(100).replace("\n", " ") +
                    if (template.content.length > 100) "..." else "",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    lineHeight = 14.sp
                ),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 6.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = isActive,
                    onClick = { if (!isActive) onSelect() },
                    label = {
                        Text(
                            text = stringResource(
                                if (isActive) R.string.label_active else R.string.action_use_template
                            )
                        )
                    },
                    leadingIcon = if (isActive) {
                        {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(FilterChipDefaults.IconSize)
                            )
                        }
                    } else {
                        null
                    }
                )

                TextButton(onClick = onEdit) {
                    Icon(
                        imageVector = if (isDefault) Icons.Default.ContentCopy else Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(
                            if (isDefault) R.string.action_customize_copy
                            else R.string.action_edit_template
                        )
                    )
                }
            }

            if (template.tags.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    template.tags.forEach { tag ->
                        val tagFilterDescription = stringResource(
                            R.string.content_desc_filter_by_tag,
                            tag
                        )
                        AssistChip(
                            onClick = { onTagClick(tag) },
                            label = {
                                Text(
                                    text = tag,
                                    fontSize = 12.sp,
                                    maxLines = 1
                                )
                            },
                            modifier = Modifier.semantics {
                                contentDescription = tagFilterDescription
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DeleteConfirmationDialog(
    templateName: String,
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
                text = stringResource(R.string.text_delete_template_confirm, templateName),
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
