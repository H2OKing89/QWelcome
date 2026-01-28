@file:OptIn(ExperimentalMaterial3Api::class)

package com.kingpaging.qwelcome.ui.templates

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kingpaging.qwelcome.data.DEFAULT_TEMPLATE_ID
import com.kingpaging.qwelcome.data.Template
import com.kingpaging.qwelcome.di.LocalTemplateListViewModel
import com.kingpaging.qwelcome.ui.components.CyberpunkBackdrop
import com.kingpaging.qwelcome.ui.components.NeonButton
import com.kingpaging.qwelcome.ui.components.NeonButtonStyle
import com.kingpaging.qwelcome.ui.components.NeonMagentaButton
import com.kingpaging.qwelcome.ui.theme.LocalDarkTheme
import com.kingpaging.qwelcome.viewmodel.templates.TemplateListEvent

/**
 * Marker ID for new templates being created (not yet persisted).
 * Using a dedicated sentinel value that cannot collide with real UUIDs.
 */
private const val NEW_TEMPLATE_ID = "__new__"

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
                    Toast.makeText(context, "Created: ${event.template.name}", Toast.LENGTH_SHORT).show()
                }
                is TemplateListEvent.TemplateUpdated -> {
                    Toast.makeText(context, "Updated: ${event.template.name}", Toast.LENGTH_SHORT).show()
                }
                is TemplateListEvent.TemplateDeleted -> {
                    Toast.makeText(context, "Deleted: ${event.name}", Toast.LENGTH_SHORT).show()
                }
                is TemplateListEvent.TemplateDuplicated -> {
                    Toast.makeText(context, "Duplicated: ${event.template.name}", Toast.LENGTH_SHORT).show()
                }
                is TemplateListEvent.ActiveTemplateChanged -> {
                    Toast.makeText(context, "Active: ${event.template.name}", Toast.LENGTH_SHORT).show()
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
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item(key = "header") {
                        Text(
                            "Tap a template to select it. Long press for more options.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    items(
                        items = uiState.templates,
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
    var showActions by remember { mutableStateOf(false) }
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
        // No elevation - Material3 elevation creates tonal overlays (gray tint in light mode)
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = if (!isDark) {
            // Light mode: thin border for definition instead of elevation
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
                .padding(16.dp)
        ) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                            contentDescription = "Built-in",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Toggle actions button
                TextButton(
                    onClick = { showActions = !showActions }
                ) {
                    Text(
                        if (showActions) "Less" else "More",
                        color = MaterialTheme.colorScheme.tertiary,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            // Content preview
            Text(
                text = template.content.take(120).replace("\n", " ") + if (template.content.length > 120) "..." else "",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    lineHeight = 14.sp
                ),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 8.dp)
            )

            // Expanded actions
            AnimatedVisibility(
                visible = showActions,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                TemplateActionButtons(
                    isDefault = isDefault,
                    onEdit = onEdit,
                    onDuplicate = onDuplicate,
                    onDelete = onDelete,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                )
            }
        }
    }
}

/**
 * Action buttons for template cards (Edit, Copy, Delete).
 * Extracted to reduce cognitive complexity of TemplateCard.
 */
@Composable
private fun TemplateActionButtons(
    isDefault: Boolean,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Edit button (disabled for default template)
        // Using SECONDARY style - these are card-level actions, not screen-level primary actions
        NeonButton(
            onClick = onEdit,
            enabled = !isDefault,
            glowColor = MaterialTheme.colorScheme.secondary,
            style = NeonButtonStyle.SECONDARY,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                Icons.Default.Edit,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text("Edit", style = MaterialTheme.typography.labelMedium)
        }

        // Duplicate button
        NeonButton(
            onClick = onDuplicate,
            glowColor = MaterialTheme.colorScheme.tertiary,
            style = NeonButtonStyle.SECONDARY,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                Icons.Default.ContentCopy,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text("Copy", style = MaterialTheme.typography.labelMedium)
        }

        // Delete button (disabled for default template)
        NeonButton(
            onClick = onDelete,
            enabled = !isDefault,
            glowColor = MaterialTheme.colorScheme.error,
            style = NeonButtonStyle.SECONDARY,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                Icons.Default.Delete,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text("Delete", style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun TemplateEditDialog(
    template: Template,
    isNew: Boolean,
    defaultContent: String,
    onSave: (name: String, content: String) -> Unit,
    onDismiss: () -> Unit
) {
    // Key the composable so editor state resets cleanly per-template
    key(template.id) {
        var name by remember { mutableStateOf(if (isNew) "" else template.name) }
        var nameError by remember { mutableStateOf<String?>(null) }
        
        // Use the new state-based text input API for better selection/scrolling behavior
        val contentState = rememberTextFieldState(
            initialText = if (isNew) defaultContent else template.content
        )
        
        Dialog(
            onDismissRequest = onDismiss,
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
                        .padding(24.dp)
                ) {
                    // Title
                    Text(
                        text = if (isNew) "Create Template" else "Edit Template",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Template Name field
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            nameError = null
                        },
                        label = { Text("Template Name") },
                        placeholder = { Text("e.g., Business Welcome") },
                        isError = nameError != null,
                        supportingText = nameError?.let { { Text(it) } },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.secondary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            cursorColor = MaterialTheme.colorScheme.secondary,
                            focusedLabelColor = MaterialTheme.colorScheme.secondary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Template Content field - uses state-based BasicTextField for proper selection/scroll
                    // Wrapped in OutlinedTextFieldDefaults.DecorationBox for Material3 styling
                    // weight(1f) takes remaining space; small min height ensures buttons visible on tiny screens
                    val contentInteractionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                    BasicTextField(
                        state = contentState,
                        lineLimits = TextFieldLineLimits.MultiLine(
                            minHeightInLines = 6,
                            maxHeightInLines = Int.MAX_VALUE
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .heightIn(min = 100.dp), // Low min ensures buttons stay visible on small screens/large fonts
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.secondary),
                        interactionSource = contentInteractionSource,
                        decorator = { innerTextField ->
                            OutlinedTextFieldDefaults.DecorationBox(
                                value = contentState.text.toString(),
                                innerTextField = innerTextField,
                                enabled = true,
                                singleLine = false,
                                visualTransformation = androidx.compose.ui.text.input.VisualTransformation.None,
                                interactionSource = contentInteractionSource,
                                label = { Text("Template Content") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.secondary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                    cursorColor = MaterialTheme.colorScheme.secondary,
                                    focusedLabelColor = MaterialTheme.colorScheme.secondary,
                                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                ),
                                container = {
                                    OutlinedTextFieldDefaults.Container(
                                        enabled = true,
                                        isError = false,
                                        interactionSource = contentInteractionSource,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = MaterialTheme.colorScheme.secondary,
                                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                        )
                                    )
                                }
                            )
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Placeholder hints
                    Text(
                        "Placeholders: {{ customer_name }}, {{ ssid }}, {{ password }}, {{ account_number }}, {{ tech_signature }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Button row - always visible at bottom
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Cancel", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        NeonMagentaButton(
                            onClick = {
                                if (name.isBlank()) {
                                    nameError = "Name is required"
                                } else {
                                    onSave(name, contentState.text.toString())
                                }
                            }
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
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            }
        }
    )
}
