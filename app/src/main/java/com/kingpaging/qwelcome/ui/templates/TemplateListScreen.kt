@file:OptIn(ExperimentalMaterial3Api::class)

package com.kingpaging.qwelcome.ui.templates

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.delete
import androidx.compose.foundation.text.input.insert
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kingpaging.qwelcome.R
import com.kingpaging.qwelcome.data.DEFAULT_TEMPLATE_ID
import com.kingpaging.qwelcome.data.MessageTemplate
import com.kingpaging.qwelcome.data.Template
import com.kingpaging.qwelcome.di.LocalTemplateListViewModel
import com.kingpaging.qwelcome.ui.components.CyberpunkBackdrop
import com.kingpaging.qwelcome.ui.components.InteractivePlaceholderChip
import com.kingpaging.qwelcome.ui.components.NeonButton
import com.kingpaging.qwelcome.ui.components.NeonButtonStyle
import com.kingpaging.qwelcome.ui.components.NeonOutlinedField
import com.kingpaging.qwelcome.ui.theme.LocalDarkTheme
import com.kingpaging.qwelcome.viewmodel.templates.TemplateListEvent
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

/**
 * Marker ID for new templates being created (not yet persisted).
 * Using a dedicated sentinel value that cannot collide with real UUIDs.
 */
private const val NEW_TEMPLATE_ID = "__new__"

@Suppress("LocalContextGetResourceValueCall")
@Composable
fun TemplateListScreen(
    onBack: () -> Unit
) {
    val vm = LocalTemplateListViewModel.current
    val context = LocalContext.current
    val uiState by vm.uiState.collectAsState()

    // Handle system back button
    BackHandler {
        if (uiState.editingTemplate != null) {
            vm.cancelEditing()
        } else {
            onBack()
        }
    }

    // Handle one-shot events
    LaunchedEffect(Unit) {
        vm.events.collect { event ->
            when (event) {
                is TemplateListEvent.Error -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
                }
                is TemplateListEvent.TemplateCreated -> {
                    Toast.makeText(context, context.getString(R.string.toast_template_created, event.template.name), Toast.LENGTH_SHORT).show()
                }
                is TemplateListEvent.TemplateUpdated -> {
                    Toast.makeText(context, context.getString(R.string.toast_template_updated, event.template.name), Toast.LENGTH_SHORT).show()
                }
                is TemplateListEvent.TemplateDeleted -> {
                    Toast.makeText(context, context.getString(R.string.toast_template_deleted, event.name), Toast.LENGTH_SHORT).show()
                }
                is TemplateListEvent.TemplateDuplicated -> {
                    Toast.makeText(context, context.getString(R.string.toast_template_duplicated, event.template.name), Toast.LENGTH_SHORT).show()
                }
                is TemplateListEvent.ActiveTemplateChanged -> {
                    Toast.makeText(context, context.getString(R.string.toast_template_active, event.template.name), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Delete confirmation dialog
    uiState.showDeleteConfirmation?.let { template ->
        DeleteConfirmationDialog(
            templateName = template.name,
            onConfirm = { vm.deleteTemplate(template.id) },
            onDismiss = { vm.dismissDeleteConfirmation() }
        )
    }

    // Edit/Create dialog
    uiState.editingTemplate?.let { template ->
        val isNewTemplate = template.id == NEW_TEMPLATE_ID
        TemplateEditDialog(
            template = template,
            isNew = isNewTemplate,
            defaultContent = vm.getDefaultTemplateContent(),
            onSave = { name, content ->
                if (isNewTemplate) {
                    vm.createTemplate(name, content)
                } else {
                    vm.updateTemplate(template.id, name, content)
                }
            },
            onDismiss = { vm.cancelEditing() }
        )
    }

    CyberpunkBackdrop {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("Templates", color = MaterialTheme.colorScheme.primary) },
                    navigationIcon = {
                        IconButton(onClick = {
                            if (uiState.editingTemplate != null) {
                                vm.cancelEditing()
                            } else {
                                onBack()
                            }
                        }) {
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
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {
                        // Create a placeholder template for "new" using explicit marker
                        vm.startEditing(Template(id = NEW_TEMPLATE_ID, name = "", content = ""))
                    },
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Create Template")
                }
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
                // Filter templates by search query (default always shown at top)
                val filteredTemplates = remember(uiState.templates, uiState.searchQuery) {
                    val query = uiState.searchQuery.trim().lowercase()
                    if (query.isEmpty()) {
                        uiState.templates
                    } else {
                        // Default template always shown at top, then filtered user templates
                        val defaultTemplate = uiState.templates.find { it.id == DEFAULT_TEMPLATE_ID }
                        val userTemplates = uiState.templates
                            .filter { it.id != DEFAULT_TEMPLATE_ID }
                            .filter { it.name.lowercase().contains(query) }
                        listOfNotNull(defaultTemplate) + userTemplates
                    }
                }
                
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Search bar
                    item(key = "search") {
                        NeonOutlinedField(
                            value = uiState.searchQuery,
                            onValueChange = { vm.updateSearchQuery(it) },
                            label = { Text("Search templates") },
                            singleLine = true,
                            trailingIcon = {
                                if (uiState.searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { vm.updateSearchQuery("") }) {
                                        Icon(
                                            Icons.Default.Search,
                                            contentDescription = "Clear search",
                                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        )
                                    }
                                } else {
                                    Icon(
                                        Icons.Default.Search,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                }
                            },
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    
                    // Help text
                    item(key = "header") {
                        Text(
                            "Tap to select. Actions: edit, duplicate, delete.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    items(
                        items = filteredTemplates,
                        key = { it.id }
                    ) { template ->
                        TemplateCard(
                            template = template,
                            isActive = template.id == uiState.activeTemplateId,
                            isDefault = template.id == DEFAULT_TEMPLATE_ID,
                            onSelect = { vm.setActiveTemplate(template.id) },
                            onEdit = { vm.startEditing(template) },
                            onDuplicate = { vm.duplicateTemplate(template) },
                            onDelete = { vm.showDeleteConfirmation(template) }
                        )
                    }
                    
                    // No results message
                    if (filteredTemplates.isEmpty() || (filteredTemplates.size == 1 && filteredTemplates.first().id == DEFAULT_TEMPLATE_ID && uiState.searchQuery.isNotEmpty())) {
                        item(key = "no_results") {
                            Text(
                                "No templates match \"${uiState.searchQuery}\"",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                modifier = Modifier.padding(vertical = 16.dp)
                            )
                        }
                    }

                    item(key = "bottom_spacer") {
                        Spacer(Modifier.height(80.dp)) // Space for FAB
                    }
                }
            }
        }
    }
}

@Composable
private fun TemplateCard(
    template: Template,
    isActive: Boolean,
    isDefault: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    val isDark = LocalDarkTheme.current

    Card(
        onClick = onSelect,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) {
                MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.15f else 0.12f)
            } else {
                if (isDark) MaterialTheme.colorScheme.surface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = if (!isDark) {
            androidx.compose.foundation.BorderStroke(
                0.5.dp,
                if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
        } else null,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Header row with title and action icons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Title and status icons
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
                    if (isActive) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Active",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    if (isDefault) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = "Built-in (duplicate to customize)",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Always-visible action icon buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(0.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Edit button - for default template, triggers duplicate instead
                    IconButton(
                        onClick = if (isDefault) onDuplicate else onEdit,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            if (isDefault) Icons.Default.ContentCopy else Icons.Default.Edit,
                            contentDescription = if (isDefault) "Duplicate to edit" else "Edit",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Duplicate button
                    IconButton(
                        onClick = onDuplicate,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = "Duplicate",
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Delete button (hidden for default template)
                    if (!isDefault) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Content preview
            Text(
                text = template.content.take(100).replace("\n", " ") + if (template.content.length > 100) "..." else "",
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
        }
    }
}

/**
 * Inserts text at the current cursor position in a TextFieldState.
 * If there's a selection, replaces the selected text.
 */
private fun insertAtCursor(state: TextFieldState, textToInsert: String) {
    state.edit {
        val selection = this.selection
        // Delete any selected text first
        if (selection.start != selection.end) {
            delete(selection.start, selection.end)
        }
        // Insert at cursor position
        insert(selection.start, textToInsert)
        // Move cursor to end of inserted text
        placeCursorAfterCharAt(selection.start + textToInsert.length - 1)
    }
}

@OptIn(FlowPreview::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
private fun TemplateEditDialog(
    template: Template,
    isNew: Boolean,
    defaultContent: String,
    onSave: (name: String, content: String) -> Unit,
    onDismiss: () -> Unit
) {
    val secondaryColor = MaterialTheme.colorScheme.secondary
    
    // Key the composable so editor state resets cleanly per-template
    key(template.id) {
        // Track original values for dirty detection
        val originalName = remember { if (isNew) "" else template.name }
        val originalContent = remember { if (isNew) defaultContent else template.content }
        
        var name by remember { mutableStateOf(originalName) }
        var nameError by remember { mutableStateOf<String?>(null) }
        var contentError by remember { mutableStateOf<String?>(null) }
        var showDiscardDialog by remember { mutableStateOf(false) }
        
        val contentState = rememberTextFieldState(initialText = originalContent)
        val contentFocusRequester = remember { FocusRequester() }
        val scope = rememberCoroutineScope()
        
        // Compute dirty state
        val currentContent = contentState.text.toString()
        val isDirty = name != originalName || currentContent != originalContent
        
        // Handle dismiss with unsaved changes check
        val handleDismiss: () -> Unit = {
            if (isDirty) {
                showDiscardDialog = true
            } else {
                onDismiss()
            }
        }
        
        // Validate on content changes (debounced, skip initial)
        LaunchedEffect(Unit) {
            snapshotFlow { contentState.text.toString() }
                .drop(1) // Skip initial value to avoid flash
                .debounce(150)
                .collect { text ->
                    val missing = Template.findMissingPlaceholders(text)
                    contentError = if (missing.isNotEmpty()) {
                        "Missing: ${missing.joinToString(", ") { it.removePrefix("{{ ").removeSuffix(" }}") }}"
                    } else {
                        null
                    }
                }
        }
        
        // Discard changes confirmation dialog
        if (showDiscardDialog) {
            AlertDialog(
                onDismissRequest = { showDiscardDialog = false },
                containerColor = MaterialTheme.colorScheme.surface,
                title = { Text("Discard changes?", color = MaterialTheme.colorScheme.secondary) },
                text = {
                    Text(
                        "You have unsaved changes. Are you sure you want to discard them?",
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                confirmButton = {
                    NeonButton(
                        onClick = { 
                            showDiscardDialog = false
                            onDismiss() 
                        },
                        glowColor = MaterialTheme.colorScheme.error,
                        style = NeonButtonStyle.PRIMARY
                    ) {
                        Text("Discard")
                    }
                },
                dismissButton = {
                    NeonButton(
                        onClick = { showDiscardDialog = false },
                        glowColor = secondaryColor,
                        style = NeonButtonStyle.TERTIARY
                    ) {
                        Text("Keep Editing")
                    }
                }
            )
        }
        
        Dialog(
            onDismissRequest = handleDismiss,
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .fillMaxHeight(0.85f)
                    .imePadding(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Title - compact
                    Text(
                        text = if (isNew) "Create Template" else "Edit Template",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Template Name field - compact with keyboard navigation
                    OutlinedTextField(
                        value = name,
                        onValueChange = { newValue ->
                            // Max 50 chars for template name
                            if (newValue.length <= 50) {
                                name = newValue
                                nameError = null
                            }
                        },
                        label = { Text("Name") },
                        isError = nameError != null,
                        supportingText = nameError?.let { { Text(it) } },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(
                            onNext = { contentFocusRequester.requestFocus() }
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = secondaryColor,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            cursorColor = secondaryColor,
                            focusedLabelColor = secondaryColor,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    
                    // Inline placeholder chips with legend
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            "Insert",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Text(
                            "·",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                        Text(
                            "Customer, SSID required",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        MessageTemplate.PLACEHOLDERS.forEach { (placeholder, _) ->
                            InteractivePlaceholderChip(
                                placeholder = placeholder,
                                onClick = {
                                    insertAtCursor(contentState, placeholder)
                                    contentFocusRequester.requestFocus()
                                },
                                isRequired = placeholder in Template.REQUIRED_PLACEHOLDERS
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Template Content field - expands to fill space, handles own scrolling
                    val contentInteractionSource = remember { MutableInteractionSource() }
                    val hasContentError = contentError != null
                    BasicTextField(
                        state = contentState,
                        lineLimits = TextFieldLineLimits.MultiLine(
                            minHeightInLines = 6,
                            maxHeightInLines = Int.MAX_VALUE
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .focusRequester(contentFocusRequester),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(secondaryColor),
                        interactionSource = contentInteractionSource,
                        decorator = { innerTextField ->
                            OutlinedTextFieldDefaults.DecorationBox(
                                value = contentState.text.toString(),
                                innerTextField = innerTextField,
                                enabled = true,
                                singleLine = false,
                                visualTransformation = androidx.compose.ui.text.input.VisualTransformation.None,
                                interactionSource = contentInteractionSource,
                                label = { Text("Message") },
                                isError = hasContentError,
                                supportingText = if (hasContentError) {
                                    { Text(contentError ?: "", color = MaterialTheme.colorScheme.error) }
                                } else null,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = if (hasContentError) MaterialTheme.colorScheme.error else secondaryColor,
                                    unfocusedBorderColor = if (hasContentError) MaterialTheme.colorScheme.error.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outline,
                                    cursorColor = secondaryColor,
                                    focusedLabelColor = if (hasContentError) MaterialTheme.colorScheme.error else secondaryColor,
                                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                container = {
                                    OutlinedTextFieldDefaults.Container(
                                        enabled = true,
                                        isError = hasContentError,
                                        interactionSource = contentInteractionSource,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = if (hasContentError) MaterialTheme.colorScheme.error else secondaryColor,
                                            unfocusedBorderColor = if (hasContentError) MaterialTheme.colorScheme.error.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outline
                                        )
                                    )
                                }
                            )
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Button row - both magenta for consistency
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Cancel - tertiary magenta (checks for unsaved changes)
                        NeonButton(
                            onClick = handleDismiss,
                            glowColor = secondaryColor,
                            style = NeonButtonStyle.TERTIARY
                        ) {
                            Text("Cancel")
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        // Save - primary magenta (filled)
                        val canSave = name.isNotBlank() && contentError == null
                        NeonButton(
                            onClick = {
                                if (name.isBlank()) {
                                    nameError = "Name is required"
                                } else if (contentError == null) {
                                    onSave(name, contentState.text.toString())
                                }
                            },
                            enabled = canSave,
                            glowColor = secondaryColor,
                            style = NeonButtonStyle.PRIMARY
                        ) {
                            Text(if (isNew) "Create" else "Save")
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
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text("Delete Template?", color = MaterialTheme.colorScheme.error)
        },
        text = {
            Text(
                "Are you sure you want to delete \"$templateName\"? This action cannot be undone.",
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        confirmButton = {
            // Destructive action - PRIMARY style with error color
            NeonButton(
                onClick = onConfirm,
                glowColor = MaterialTheme.colorScheme.error,
                style = NeonButtonStyle.PRIMARY
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            // Cancel - TERTIARY magenta for consistency
            NeonButton(
                onClick = onDismiss,
                glowColor = MaterialTheme.colorScheme.secondary,
                style = NeonButtonStyle.TERTIARY
            ) {
                Text("Cancel")
            }
        }
    )
}
