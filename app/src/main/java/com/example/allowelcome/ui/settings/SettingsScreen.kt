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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.allowelcome.data.MessageTemplate
import com.example.allowelcome.data.TechProfile
import com.example.allowelcome.data.TemplateSettings
import com.example.allowelcome.ui.components.CyberpunkBackdrop
import com.example.allowelcome.ui.components.NeonMagentaButton
import com.example.allowelcome.ui.components.NeonOutlinedField
import com.example.allowelcome.ui.components.NeonPanel
import com.example.allowelcome.ui.theme.CyberScheme
import com.example.allowelcome.viewmodel.factory.AppViewModelProvider
import com.example.allowelcome.viewmodel.settings.SettingsViewModel

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    vm: SettingsViewModel = viewModel(factory = AppViewModelProvider(LocalContext.current))
) {
    // Handle system back button
    BackHandler { onBack() }

    val currentProfile by vm.techProfile.collectAsState()
    val currentTemplate by vm.templateSettings.collectAsState()
    val defaultTemplate = remember { vm.getDefaultTemplate() }

    // Tech profile state
    var name by remember(currentProfile) { mutableStateOf(currentProfile.name) }
    var title by remember(currentProfile) { mutableStateOf(currentProfile.title) }
    var dept by remember(currentProfile) { mutableStateOf(currentProfile.dept) }

    // Template state
    var useCustom by remember(currentTemplate) { mutableStateOf(currentTemplate.useCustomTemplate) }
    var customTemplate by remember(currentTemplate) {
        mutableStateOf(currentTemplate.customTemplate.ifBlank { defaultTemplate })
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
                            color = Color.White.copy(alpha = 0.7f)
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
                                unfocusedBorderColor = Color.White.copy(alpha = 0.18f),
                                cursorColor = CyberScheme.secondary,
                                focusedLabelColor = CyberScheme.secondary,
                            )
                        )

                        Spacer(Modifier.height(8.dp))

                        // Reset to default button
                        TextButton(
                            onClick = { customTemplate = defaultTemplate }
                        ) {
                            Text("Restore Default Template", color = CyberScheme.tertiary)
                        }
                    } else {
                        // Show read-only default template preview
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Default template (read-only):",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        Text(
                            defaultTemplate.take(150) + "...",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // === SAVE BUTTON ===
                NeonMagentaButton(
                    onClick = {
                        vm.save(TechProfile(name, title, dept))
                        vm.saveTemplate(
                            TemplateSettings(
                                useCustomTemplate = useCustom,
                                customTemplate = if (useCustom) customTemplate else ""
                            )
                        )
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
