@file:OptIn(ExperimentalMaterial3Api::class)

package com.kingpaging.qwelcome.ui.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.core.net.toUri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kingpaging.qwelcome.R
import com.kingpaging.qwelcome.data.TechProfile
import com.kingpaging.qwelcome.di.LocalSettingsViewModel
import com.kingpaging.qwelcome.ui.components.CyberpunkBackdrop
import com.kingpaging.qwelcome.ui.components.NeonButton
import com.kingpaging.qwelcome.ui.components.NeonButtonStyle
import com.kingpaging.qwelcome.ui.components.NeonMagentaButton
import com.kingpaging.qwelcome.ui.components.NeonOutlinedField
import com.kingpaging.qwelcome.ui.components.NeonPanel
import com.kingpaging.qwelcome.util.rememberHapticFeedback
import com.kingpaging.qwelcome.viewmodel.settings.SettingsEvent
import com.kingpaging.qwelcome.viewmodel.settings.UpdateState

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

    val currentProfile by vm.techProfile.collectAsStateWithLifecycle()
    val activeTemplate by vm.activeTemplate.collectAsStateWithLifecycle()

    // Tech profile state - use rememberSaveable so rotation doesn't lose edits
    var name by rememberSaveable(currentProfile) { mutableStateOf(currentProfile.name) }
    var title by rememberSaveable(currentProfile) { mutableStateOf(currentProfile.title) }
    var dept by rememberSaveable(currentProfile) { mutableStateOf(currentProfile.dept) }

    // Update local state when currentProfile changes (e.g., after import on another screen)
    LaunchedEffect(currentProfile) {
        name = currentProfile.name
        title = currentProfile.title
        dept = currentProfile.dept
    }

    // Detect unsaved changes - only profile changes now (template editing moved to TemplateListScreen)
    val hasUnsavedChanges by remember(name, title, dept, currentProfile) {
        derivedStateOf {
            name != currentProfile.name ||
                title != currentProfile.title ||
                dept != currentProfile.dept
        }
    }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptic = rememberHapticFeedback()

    // Collect one-shot settings events (Toasts) with lifecycle awareness
    LaunchedEffect(lifecycleOwner, vm.settingsEvents) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            vm.settingsEvents.collect { event ->
                when (event) {
                    is SettingsEvent.ShowToast -> {
                        Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    // Discard changes confirmation dialog
    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Discard changes?") },
            text = { Text("You have unsaved changes that will be lost.") },
            confirmButton = {
                TextButton(onClick = {
                    haptic()
                    showDiscardDialog = false
                    onBack()
                }) {
                    Text("Discard", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { haptic(); showDiscardDialog = false }) {
                    Text("Keep editing")
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
                            haptic()
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
                    style = MaterialTheme.typography.titleLarge,
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

                // === MESSAGE TEMPLATE SECTION (simplified - just link to Templates screen) ===
                Text(
                    "Message Templates",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                NeonPanel {
                    Text(
                        "Create, edit, duplicate, and manage your message templates.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Currently using: ${activeTemplate.name}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(12.dp))
                    NeonButton(
                        onClick = onOpenTemplates,
                        modifier = Modifier.fillMaxWidth(),
                        style = NeonButtonStyle.SECONDARY
                    ) {
                        Text("Manage Templates")
                    }
                }

                Spacer(Modifier.height(8.dp))
                
                // === SAVE PROFILE BUTTON ===
                NeonMagentaButton(
                    onClick = {
                        vm.save(TechProfile(name, title, dept))
                        onBack()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = hasUnsavedChanges,
                    style = NeonButtonStyle.PRIMARY
                ) {
                    Text(if (hasUnsavedChanges) "Save Profile" else "No changes")
                }

                Spacer(Modifier.height(16.dp))

                // === DATA MANAGEMENT SECTION ===
                Text(
                    "Data Management",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                NeonPanel {
                    Text(
                        "Export templates to share with your team, or import from a backup.",
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

                Spacer(Modifier.height(32.dp))

                // === ABOUT SECTION === (at bottom - less frequently accessed)
                Text(
                    "About",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                
                val updateState by vm.updateState.collectAsStateWithLifecycle()
                // Capture string resource outside onClick for lint compliance
                val noBrowserMessage = stringResource(R.string.toast_no_browser)

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
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    TextButton(
                                        onClick = {
                                            haptic()
                                            val uri = state.downloadUrl.toUri()
                                            // Only allow https URLs for security
                                            if (uri.scheme != "https" && uri.scheme != "http") return@TextButton
                                            val intent = Intent(Intent.ACTION_VIEW, uri)
                                                .addCategory(Intent.CATEGORY_BROWSABLE)
                                            try {
                                                context.startActivity(intent)
                                            } catch (_: ActivityNotFoundException) {
                                                Toast.makeText(context, noBrowserMessage, Toast.LENGTH_SHORT).show()
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
                                    IconButton(
                                        onClick = { haptic(); vm.dismissUpdate() },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Filled.Close,
                                            contentDescription = "Dismiss update",
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
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
                        onClick = { haptic(); vm.checkForUpdate() },
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
                            val uri = "https://github.com/H2OKing89/QWelcome".toUri()
                            val intent = Intent(Intent.ACTION_VIEW, uri)
                                .addCategory(Intent.CATEGORY_BROWSABLE)
                            try {
                                context.startActivity(intent)
                            } catch (_: ActivityNotFoundException) {
                                Toast.makeText(context, noBrowserMessage, Toast.LENGTH_SHORT).show()
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


                Spacer(Modifier.height(32.dp))
            }
        }
    }
}
