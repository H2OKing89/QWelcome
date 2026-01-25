@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.allowelcome.ui.templates

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.allowelcome.data.DEFAULT_TEMPLATE_ID
import com.example.allowelcome.data.Template
import com.example.allowelcome.di.LocalTemplateListViewModel
import com.example.allowelcome.ui.components.CyberpunkBackdrop
import com.example.allowelcome.ui.components.NeonButton
import com.example.allowelcome.ui.components.NeonMagentaButton
import com.example.allowelcome.ui.components.NeonPanel
import com.example.allowelcome.ui.theme.CyberScheme
import com.example.allowelcome.viewmodel.templates.TemplateListEvent

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
                    title = { Text("Templates", color = CyberScheme.primary) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
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
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {
                        // Create a placeholder template for "new" using explicit marker
                        vm.startEditing(Template(id = NEW_TEMPLATE_ID, name = "", content = ""))
                    },
                    containerColor = CyberScheme.secondary,
                    contentColor = CyberScheme.onSecondary
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
                    CircularProgressIndicator(color = CyberScheme.primary)
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
                            color = CyberScheme.onSurface.copy(alpha = 0.7f),
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

    Card(
        onClick = onSelect,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) {
                CyberScheme.primary.copy(alpha = 0.15f)
            } else {
                CyberScheme.surface.copy(alpha = 0.6f)
            }
        ),
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
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isActive) CyberScheme.primary else CyberScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isActive) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Active",
                            tint = CyberScheme.secondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    if (isDefault) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = "Built-in",
                            tint = CyberScheme.onSurface.copy(alpha = 0.5f),
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
                        color = CyberScheme.tertiary,
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
                color = CyberScheme.onSurface.copy(alpha = 0.6f),
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
        NeonButton(
            onClick = onEdit,
            enabled = !isDefault,
            glowColor = CyberScheme.secondary,
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
            glowColor = CyberScheme.tertiary,
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
            glowColor = CyberScheme.error,
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
    var name by remember { mutableStateOf(if (isNew) "" else template.name) }
    var content by remember { mutableStateOf(if (isNew) defaultContent else template.content) }
    var nameError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CyberScheme.surface,
        title = {
            Text(
                if (isNew) "Create Template" else "Edit Template",
                color = CyberScheme.primary
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
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
                        focusedBorderColor = CyberScheme.secondary,
                        unfocusedBorderColor = CyberScheme.onSurface.copy(alpha = 0.3f),
                        cursorColor = CyberScheme.secondary,
                        focusedLabelColor = CyberScheme.secondary
                    )
                )

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Template Content") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 200.dp, max = 300.dp),
                    minLines = 8,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberScheme.secondary,
                        unfocusedBorderColor = CyberScheme.onSurface.copy(alpha = 0.3f),
                        cursorColor = CyberScheme.secondary,
                        focusedLabelColor = CyberScheme.secondary
                    )
                )

                Text(
                    "Placeholders: {{ customer_name }}, {{ ssid }}, {{ password }}, {{ account_number }}, {{ tech_signature }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = CyberScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        },
        confirmButton = {
            NeonMagentaButton(
                onClick = {
                    if (name.isBlank()) {
                        nameError = "Name is required"
                    } else {
                        onSave(name, content)
                    }
                }
            ) {
                Text(if (isNew) "Create" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = CyberScheme.onSurface.copy(alpha = 0.7f))
            }
        }
    )
}

@Composable
private fun DeleteConfirmationDialog(
    templateName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CyberScheme.surface,
        title = {
            Text("Delete Template?", color = CyberScheme.error)
        },
        text = {
            Text(
                "Are you sure you want to delete \"$templateName\"? This action cannot be undone.",
                color = CyberScheme.onSurface
            )
        },
        confirmButton = {
            NeonButton(
                onClick = onConfirm,
                glowColor = CyberScheme.error
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = CyberScheme.onSurface.copy(alpha = 0.7f))
            }
        }
    )
}
