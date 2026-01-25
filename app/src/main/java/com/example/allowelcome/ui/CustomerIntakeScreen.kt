@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.allowelcome.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import android.widget.Toast
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.allowelcome.di.LocalCustomerIntakeViewModel
import com.example.allowelcome.di.LocalNavigator
import com.example.allowelcome.di.LocalTemplateListViewModel
import com.example.allowelcome.ui.components.CyberpunkBackdrop
import com.example.allowelcome.ui.components.NeonButton
import com.example.allowelcome.ui.components.NeonCyanButton
import com.example.allowelcome.ui.components.NeonMagentaButton
import com.example.allowelcome.ui.components.NeonOutlinedField
import com.example.allowelcome.ui.components.NeonPanel
import com.example.allowelcome.ui.components.QrCodeBottomSheet
import com.example.allowelcome.ui.components.QWelcomeHeader
import com.example.allowelcome.ui.theme.CyberScheme
import com.example.allowelcome.viewmodel.UiEvent

@Composable
fun CustomerIntakeScreen(
    onOpenSettings: () -> Unit,
    onOpenTemplates: () -> Unit = {}
) {
    // Get ViewModel and Navigator from CompositionLocals (provided at Activity level)
    val customerIntakeViewModel = LocalCustomerIntakeViewModel.current
    val templateListViewModel = LocalTemplateListViewModel.current
    val navigator = LocalNavigator.current
    
    val uiState by customerIntakeViewModel.uiState.collectAsState()
    val templateUiState by templateListViewModel.uiState.collectAsState()
    var passwordVisible by remember { mutableStateOf(false) }
    var showQrSheet by remember { mutableStateOf(false) }
    var templateDropdownExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Lifecycle observer for auto-clear after 10 min in background
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> customerIntakeViewModel.onPause()
                Lifecycle.Event.ON_RESUME -> customerIntakeViewModel.onResume()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Collect UI events (Toasts, rate limit warnings)
    LaunchedEffect(Unit) {
        customerIntakeViewModel.uiEvent.collect { event ->
            when (event) {
                is UiEvent.ShowToast -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
                is UiEvent.RateLimitExceeded -> {
                    Toast.makeText(context, "Rate limit exceeded. Please wait.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // QR Code Bottom Sheet
    if (showQrSheet && uiState.ssid.isNotBlank() && uiState.password.isNotBlank()) {
        QrCodeBottomSheet(
            ssid = uiState.ssid,
            password = uiState.password,
            onDismiss = { showQrSheet = false }
        )
    }

    CyberpunkBackdrop {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { QWelcomeHeader() },
                    actions = {
                        IconButton(onClick = { customerIntakeViewModel.clearForm() }) {
                            Icon(Icons.Filled.PersonAdd, contentDescription = "New Customer")
                        }
                        IconButton(onClick = onOpenSettings) {
                            Icon(Icons.Filled.Settings, contentDescription = "Settings")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        actionIconContentColor = CyberScheme.primary
                    )
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .imePadding() // Add padding for the keyboard
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center
            ) {
                // Template selector dropdown
                if (templateUiState.templates.isNotEmpty()) {
                    val activeTemplate = remember(
                        templateUiState.templates,
                        templateUiState.activeTemplateId
                    ) {
                        templateUiState.templates.find { it.id == templateUiState.activeTemplateId }
                    }
                    
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedCard(
                            onClick = { templateDropdownExpanded = true },
                            colors = CardDefaults.outlinedCardColors(
                                containerColor = CyberScheme.surface.copy(alpha = 0.6f)
                            ),
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = androidx.compose.ui.graphics.SolidColor(
                                    CyberScheme.secondary.copy(alpha = 0.5f)
                                )
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Template",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CyberScheme.secondary
                                    )
                                    Text(
                                        activeTemplate?.name ?: "Select Template",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = CyberScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Icon(
                                    Icons.Filled.ArrowDropDown,
                                    contentDescription = "Select template",
                                    tint = CyberScheme.secondary
                                )
                            }
                        }
                        
                        DropdownMenu(
                            expanded = templateDropdownExpanded,
                            onDismissRequest = { templateDropdownExpanded = false },
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                        ) {
                            templateUiState.templates.forEach { template ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(template.name)
                                            if (template.id == templateUiState.activeTemplateId) {
                                                Icon(
                                                    Icons.Filled.Check,
                                                    contentDescription = "Active",
                                                    tint = CyberScheme.secondary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        templateListViewModel.setActiveTemplate(template.id)
                                        templateDropdownExpanded = false
                                    }
                                )
                            }
                            
                            HorizontalDivider()
                            
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Manage Templates...",
                                        color = CyberScheme.tertiary
                                    )
                                },
                                onClick = {
                                    templateDropdownExpanded = false
                                    onOpenTemplates()
                                }
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                NeonPanel {
                    NeonOutlinedField(
                        value = uiState.customerName,
                        onValueChange = { customerIntakeViewModel.onCustomerNameChanged(it) },
                        label = { Text("Customer Name") },
                        isError = uiState.customerNameError != null,
                        supportingText = { uiState.customerNameError?.let { Text(it) } }
                    )
                    NeonOutlinedField(
                        value = uiState.customerPhone,
                        onValueChange = { customerIntakeViewModel.onCustomerPhoneChanged(it) },
                        label = { Text("Customer Phone") },
                        isError = uiState.customerPhoneError != null,
                        supportingText = { uiState.customerPhoneError?.let { Text(it) } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                    )
                    NeonOutlinedField(
                        value = uiState.ssid,
                        onValueChange = { customerIntakeViewModel.onSsidChanged(it) },
                        label = { Text("WiFi SSID") },
                        isError = uiState.ssidError != null,
                        supportingText = { uiState.ssidError?.let { Text(it) } }
                    )
                    NeonOutlinedField(
                        value = uiState.password,
                        onValueChange = { customerIntakeViewModel.onPasswordChanged(it) },
                        label = { Text("WiFi Password") },
                        isError = uiState.passwordError != null,
                        supportingText = { uiState.passwordError?.let { Text(it) } },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                            val description = if (passwordVisible) "Hide password" else "Show password"
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(imageVector = image, description)
                            }
                        }
                    )
                    NeonOutlinedField(
                        value = uiState.accountNumber,
                        onValueChange = { customerIntakeViewModel.onAccountNumberChanged(it) },
                        label = { Text("Account Number") },
                        isError = uiState.accountNumberError != null,
                        supportingText = { uiState.accountNumberError?.let { Text(it) } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Main action buttons row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    NeonCyanButton(
                        onClick = { customerIntakeViewModel.onSmsClicked(navigator) },
                        modifier = Modifier.weight(1f)
                    ) { Text("SMS") }
                    NeonMagentaButton(
                        onClick = { customerIntakeViewModel.onShareClicked(navigator) },
                        modifier = Modifier.weight(1f)
                    ) { Text("Share") }
                    NeonCyanButton(
                        onClick = { customerIntakeViewModel.onCopyClicked(navigator) },
                        modifier = Modifier.weight(1f)
                    ) { Text("Copy") }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // QR Code button - full width, purple themed
                NeonButton(
                    onClick = { showQrSheet = true },
                    modifier = Modifier.fillMaxWidth(),
                    glowColor = CyberScheme.tertiary, // Purple
                    enabled = uiState.ssid.isNotBlank() && uiState.password.isNotBlank()
                ) {
                    Icon(
                        Icons.Filled.QrCode2,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("WiFi QR Code")
                }
            }
        }
    }
}
