@file:OptIn(ExperimentalMaterial3Api::class)

package com.kingpaging.qwelcome.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kingpaging.qwelcome.data.DEFAULT_TEMPLATE_ID
import com.kingpaging.qwelcome.data.MessageTemplate
import com.kingpaging.qwelcome.data.TechProfile
import com.kingpaging.qwelcome.data.Template
import com.kingpaging.qwelcome.di.LocalSettingsViewModel
import com.kingpaging.qwelcome.ui.components.CyberpunkBackdrop
import com.kingpaging.qwelcome.ui.components.NeonButton
import com.kingpaging.qwelcome.ui.components.NeonButtonStyle
import com.kingpaging.qwelcome.ui.components.NeonMagentaButton
import com.kingpaging.qwelcome.ui.components.NeonOutlinedField
import com.kingpaging.qwelcome.ui.components.NeonPanel
import com.kingpaging.qwelcome.ui.components.PlaceholderChipsRow
import com.kingpaging.qwelcome.viewmodel.settings.UpdateState

/**
 * Safely truncates text without splitting placeholders like {{ }} or words.
 * Falls back to word boundary if placeholder boundary not found.
 */
private fun safeTruncate(text: String, maxLength: Int): String {
    if (text.length <= maxLength) return text
    if (maxLength <= 0) return "..."

    // Find a safe cutoff point that doesn't split {{ or }}
    var cutoff = maxLength.coerceAtMost(text.length)

    // Check if we're in the middle of a placeholder
    val beforeCutoff = text.substring(0, cutoff)
    val lastOpenBrace = beforeCutoff.lastIndexOf("{{") 
    val lastCloseBrace = beforeCutoff.lastIndexOf("}}")

    // If we found {{ after the last }}, we're inside a placeholder - back up
    // Note: lastOpenBrace > lastCloseBrace implies lastOpenBrace >= 0
    if (lastOpenBrace > lastCloseBrace) {
        cutoff = lastOpenBrace
    }

    // Try to break at a space for cleaner output
    if (cutoff > 0) {
        val lastSpace = text.substring(0, cutoff).lastIndexOf(' ')
        if (lastSpace > 0 && lastSpace > cutoff - 30) {
            cutoff = lastSpace
        }
    }

    // Ensure cutoff is valid
    cutoff = cutoff.coerceIn(1, text.length)

    return text.substring(0, cutoff).trimEnd() + "..."
}

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenExport: () -> Unit = {},
    onOpenImport: () -> Unit = {},
    onOpenTemplates: () -> Unit = {}
) {
    // Get ViewModel from CompositionLocal (provided at Activity level)
    val vm = LocalSettingsViewModel.current
    
    // Discard confirmation dialog state
    var showDiscardDialog by rememberSaveable { mutableStateOf(false) }
    // Restore default confirmation dialog state
    var showRestoreDialog by rememberSaveable { mutableStateOf(false) }
    // Template body expanded state (collapsed by default to reduce visual weight)
    var showTemplateBody by rememberSaveable { mutableStateOf(false) }

    val currentProfile by vm.techProfile.collectAsState()
    val activeTemplate by vm.activeTemplate.collectAsState()
    val defaultTemplateContent = remember { vm.getDefaultTemplateContent() }

    // Derive useCustom from whether active template is the default
    val isUsingDefault = activeTemplate.id == DEFAULT_TEMPLATE_ID

    // Tech profile state - use rememberSaveable so rotation doesn't lose edits
    var name by rememberSaveable(currentProfile) { mutableStateOf(currentProfile.name) }
    var title by rememberSaveable(currentProfile) { mutableStateOf(currentProfile.title) }
    var dept by rememberSaveable(currentProfile) { mutableStateOf(currentProfile.dept) }

    // Template state - use activeTemplate as key to recompute when it changes
    var useCustom by rememberSaveable(activeTemplate) { mutableStateOf(!isUsingDefault) }
    var customTemplate by rememberSaveable(activeTemplate) {
        mutableStateOf(
            if (isUsingDefault) defaultTemplateContent else activeTemplate.content
        )
    }

    // Detect unsaved changes by comparing current values to saved values
    val hasUnsavedChanges by remember(name, title, dept, useCustom, customTemplate, currentProfile, activeTemplate, isUsingDefault) {
        derivedStateOf {
            // Profile changes
            val profileChanged = name != currentProfile.name ||
                    title != currentProfile.title ||
                    dept != currentProfile.dept
            
            // Template changes
            val templateChanged = if (useCustom) {
                // Custom mode: either switching from default, or content changed
                isUsingDefault || customTemplate != activeTemplate.content
            } else {
                // Default mode: changed if was previously using custom
                !isUsingDefault
            }
            
            profileChanged || templateChanged
        }
    }

    val context = LocalContext.current

    // Discard changes confirmation dialog
    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Discard changes?") },
            text = { Text("You have unsaved changes that will be lost.") },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardDialog = false
                    onBack()
                }) {
                    Text("Discard", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text("Keep editing")
                }
            }
        )
    }

    // Restore default template confirmation dialog (destructive action)
    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            icon = {
                Icon(
                    Icons.Filled.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Restore Default Template?") },
            text = { Text("This will replace your current template content with the built-in default. Your edits will be lost.") },
            confirmButton = {
                TextButton(onClick = {
                    customTemplate = defaultTemplateContent
                    useCustom = false  // Fully revert to default mode
                    showRestoreDialog = false
                }) {
                    Text("Restore", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Handle system back button - respect unsaved changes
    BackHandler {
        if (hasUnsavedChanges) showDiscardDialog = true else onBack()
    }

    CyberpunkBackdrop {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("Settings") },
                    navigationIcon = {
                        IconButton(onClick = {
                            if (hasUnsavedChanges) showDiscardDialog = true else onBack()
                        }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                        navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // === TECH PROFILE SECTION ===
                Text(
                    "Tech Profile",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                NeonPanel {
                    NeonOutlinedField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Tech name") }
                    )
                    NeonOutlinedField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title") }
                    )
                    NeonOutlinedField(
                        value = dept,
                        onValueChange = { dept = it },
                        label = { Text("Department / line") }
                    )
                }

                Spacer(Modifier.height(8.dp))

                // === MESSAGE TEMPLATE SECTION ===
                Text(
                    "Message Template",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                NeonPanel {
                    // Manage Templates - SECONDARY since Save All is the PRIMARY action
                    NeonButton(
                        onClick = onOpenTemplates,
                        modifier = Modifier.fillMaxWidth(),
                        style = NeonButtonStyle.SECONDARY
                    ) {
                        Text("Manage Templates")
                    }
                    
                    Spacer(Modifier.height(12.dp))
                    
                    // Clear toggle with explicit label
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Use Selected Template",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                if (useCustom) "Using: ${activeTemplate.name}" else "Using built-in default",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Switch(
                            checked = useCustom,
                            onCheckedChange = { useCustom = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.secondary,
                                checkedTrackColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
                                uncheckedThumbColor = MaterialTheme.colorScheme.primary,
                                uncheckedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                            )
                        )
                    }

                    if (useCustom) {
                        Spacer(Modifier.height(8.dp))

                        // Placeholder chips - scannable format per ChatGPT feedback
                        Text(
                            "Available placeholders:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Spacer(Modifier.height(4.dp))
                        PlaceholderChipsRow(
                            placeholders = MessageTemplate.PLACEHOLDERS,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(12.dp))

                        // Collapsible template editor - reduces visual weight
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Template Body",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            TextButton(onClick = { showTemplateBody = !showTemplateBody }) {
                                Text(if (showTemplateBody) "Hide" else "Show")
                                Icon(
                                    if (showTemplateBody) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        if (showTemplateBody) {
                            OutlinedTextField(
                                value = customTemplate,
                                onValueChange = { customTemplate = it },
                                label = { Text("Custom Template") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 200.dp),
                                minLines = 8,
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
                        } else {
                            // Show preview when collapsed
                            Text(
                                safeTruncate(customTemplate, 100),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    } else {
                        // Show read-only default template preview
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Default template (read-only):",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Text(
                            safeTruncate(defaultTemplateContent, 150),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // === EXPORT / IMPORT SECTION ===
                Text(
                    "Export & Share",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                NeonPanel {
                    Text(
                        "Share templates with your team via Slack, Teams, or email.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    if (hasUnsavedChanges) {
                        Text(
                            "Export is disabled until changes are saved.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    // Export/Import are SECONDARY actions - not the main thing on this screen
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        NeonMagentaButton(
                            onClick = onOpenExport,
                            modifier = Modifier.weight(1f),
                            enabled = !hasUnsavedChanges,
                            style = NeonButtonStyle.SECONDARY
                        ) {
                            Text(if (hasUnsavedChanges) "Save First" else "Export")
                        }
                        NeonButton(
                            onClick = onOpenImport,
                            modifier = Modifier.weight(1f),
                            style = NeonButtonStyle.SECONDARY
                        ) {
                            Text("Import")
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // === ABOUT & UPDATES SECTION ===
                Text(
                    "About",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                
                val updateState by vm.updateState.collectAsState()
                
                NeonPanel {
                    // Version info
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.Info,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Version ${vm.currentVersion}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        
                        // Update status indicator
                        when (val state = updateState) {
                            is UpdateState.Checking -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                            is UpdateState.UpToDate -> {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Filled.CheckCircle,
                                        contentDescription = "Up to date",
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        "Up to date",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            is UpdateState.Available -> {
                                TextButton(
                                    onClick = {
                                        val uri = Uri.parse(state.downloadUrl)
                                        // Only allow https URLs for security
                                        if (uri.scheme != "https" && uri.scheme != "http") return@TextButton
                                        val intent = Intent(Intent.ACTION_VIEW, uri)
                                            .addCategory(Intent.CATEGORY_BROWSABLE)
                                        try {
                                            context.startActivity(intent)
                                        } catch (e: android.content.ActivityNotFoundException) {
                                            // No browser installed - silently fail
                                            // Could show toast but unlikely edge case
                                        }
                                    }
                                ) {
                                    Icon(
                                        Icons.Filled.Download,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text("v${state.version} available")
                                }
                            }
                            is UpdateState.Error -> {
                                Text(
                                    "Check failed",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            else -> { /* Idle or Dismissed - show nothing */ }
                        }
                    }
                    
                    Spacer(Modifier.height(12.dp))
                    
                    // Check for updates button
                    OutlinedButton(
                        onClick = { vm.checkForUpdate() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = updateState !is UpdateState.Checking
                    ) {
                        Icon(
                            Icons.Filled.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            when (updateState) {
                                is UpdateState.Checking -> "Checking..."
                                else -> "Check for Updates"
                            }
                        )
                    }
                    
                    Spacer(Modifier.height(8.dp))
                    
                    // View on GitHub link
                    TextButton(
                        onClick = {
                            val uri = Uri.parse("https://github.com/H2OKing89/QWelcome")
                            val intent = Intent(Intent.ACTION_VIEW, uri)
                                .addCategory(Intent.CATEGORY_BROWSABLE)
                            try {
                                context.startActivity(intent)
                            } catch (e: android.content.ActivityNotFoundException) {
                                // No browser installed - silently fail
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Filled.Code,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("View on GitHub")
                    }
                }

                Spacer(Modifier.height(16.dp))

                // === SAVE BUTTON - PRIMARY action, disabled until changes exist ===
                NeonMagentaButton(
                    onClick = {
                        vm.save(TechProfile(name, title, dept))
                        
                        if (useCustom && customTemplate.isNotBlank()) {
                            // Save or update the custom template
                            val templateToSave = if (!isUsingDefault) {
                                // Update existing non-default template - preserve name using copy()
                                activeTemplate.copy(content = customTemplate)
                            } else {
                                // Create new custom template from default
                                Template.create(name = "Custom", content = customTemplate)
                            }
                            vm.saveTemplate(templateToSave)
                            vm.setActiveTemplate(templateToSave.id)
                        } else {
                            // Switch to default template
                            vm.setActiveTemplate(DEFAULT_TEMPLATE_ID)
                        }
                        
                        onBack()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = hasUnsavedChanges,
                    style = NeonButtonStyle.PRIMARY
                ) {
                    Text(if (hasUnsavedChanges) "Save All" else "No changes")
                }

                // === DANGER ZONE === (only show when using custom template)
                if (useCustom && customTemplate != defaultTemplateContent) {
                    Spacer(Modifier.height(24.dp))
                    
                    Text(
                        "Danger Zone",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                    )
                    Spacer(Modifier.height(8.dp))
                    
                    OutlinedButton(
                        onClick = { showRestoreDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                            brush = androidx.compose.ui.graphics.SolidColor(
                                MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                            )
                        )
                    ) {
                        Icon(
                            Icons.Filled.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Restore Default Template")
                    }
                }

                Spacer(Modifier.height(32.dp))
            }
        }
    }
}
