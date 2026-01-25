@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.allowelcome.ui.settings

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.allowelcome.data.DEFAULT_TEMPLATE_ID
import com.example.allowelcome.data.MessageTemplate
import com.example.allowelcome.data.TechProfile
import com.example.allowelcome.data.Template
import com.example.allowelcome.di.LocalSettingsViewModel
import com.example.allowelcome.ui.components.CyberpunkBackdrop
import com.example.allowelcome.ui.components.NeonMagentaButton
import com.example.allowelcome.ui.components.NeonOutlinedField
import com.example.allowelcome.ui.components.NeonPanel
import com.example.allowelcome.ui.theme.CyberScheme

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
    if (lastOpenBrace > lastCloseBrace && lastOpenBrace > 0) {
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
    onBack: () -> Unit
) {
    // Get ViewModel from CompositionLocal (provided at Activity level)
    val vm = LocalSettingsViewModel.current
    
    // Handle system back button
    BackHandler { onBack() }

    val currentProfile by vm.techProfile.collectAsState()
    val activeTemplate by vm.activeTemplate.collectAsState()
    val defaultTemplateContent = remember { vm.getDefaultTemplateContent() }

    // Derive useCustom from whether active template is the default
    val isUsingDefault = activeTemplate.id == DEFAULT_TEMPLATE_ID

    // Tech profile state
    var name by remember(currentProfile) { mutableStateOf(currentProfile.name) }
    var title by remember(currentProfile) { mutableStateOf(currentProfile.title) }
    var dept by remember(currentProfile) { mutableStateOf(currentProfile.dept) }

    // Template state - use activeTemplate as key to recompute when it changes
    var useCustom by remember(activeTemplate) { mutableStateOf(!isUsingDefault) }
    var customTemplate by remember(activeTemplate) {
        mutableStateOf(
            if (isUsingDefault) defaultTemplateContent else activeTemplate.content
        )
    }
    // Track the custom template ID for updates
    var customTemplateId by remember(activeTemplate) {
        mutableStateOf(if (isUsingDefault) null else activeTemplate.id)
    }

    CyberpunkBackdrop {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("Settings", color = CyberScheme.primary) },
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
                    color = CyberScheme.primary
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
                    color = CyberScheme.primary
                )
                NeonPanel {
                    // Toggle between Default and Custom
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            if (useCustom) "Using Custom Template" else "Using Default Template",
                            color = if (useCustom) CyberScheme.secondary else CyberScheme.primary
                        )
                        Switch(
                            checked = useCustom,
                            onCheckedChange = { useCustom = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = CyberScheme.secondary,
                                checkedTrackColor = CyberScheme.secondary.copy(alpha = 0.5f),
                                uncheckedThumbColor = CyberScheme.primary,
                                uncheckedTrackColor = CyberScheme.primary.copy(alpha = 0.3f)
                            )
                        )
                    }

                    if (useCustom) {
                        Spacer(Modifier.height(8.dp))

                        // Placeholder hints
                        Text(
                            "Available placeholders:",
                            style = MaterialTheme.typography.labelMedium,
                            color = CyberScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Text(
                            MessageTemplate.PLACEHOLDERS.joinToString(" • ") { it.first },
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            ),
                            color = CyberScheme.tertiary
                        )

                        Spacer(Modifier.height(8.dp))

                        // Custom template editor
                        OutlinedTextField(
                            value = customTemplate,
                            onValueChange = { customTemplate = it },
                            label = { Text("Custom Template") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 200.dp),
                            minLines = 8,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyberScheme.secondary.copy(alpha = 0.85f),
                                unfocusedBorderColor = CyberScheme.onSurface.copy(alpha = 0.25f),
                                cursorColor = CyberScheme.secondary,
                                focusedLabelColor = CyberScheme.secondary,
                            )
                        )

                        Spacer(Modifier.height(8.dp))

                        // Reset to default button
                        TextButton(
                            onClick = { customTemplate = defaultTemplateContent }
                        ) {
                            Text("Restore Default Template", color = CyberScheme.tertiary)
                        }
                    } else {
                        // Show read-only default template preview
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Default template (read-only):",
                            style = MaterialTheme.typography.labelMedium,
                            color = CyberScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Text(
                            safeTruncate(defaultTemplateContent, 150),
                            style = MaterialTheme.typography.bodySmall,
                            color = CyberScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // === SAVE BUTTON ===
                NeonMagentaButton(
                    onClick = {
                        vm.save(TechProfile(name, title, dept))
                        
                        if (useCustom && customTemplate.isNotBlank()) {
                            // Save or update the custom template
                            val templateToSave = if (customTemplateId != null) {
                                // Update existing custom template
                                Template(
                                    id = customTemplateId!!,
                                    name = "Custom",
                                    content = customTemplate
                                )
                            } else {
                                // Create new custom template
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
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save All")
                }

                Spacer(Modifier.height(32.dp))
            }
        }
    }
}
