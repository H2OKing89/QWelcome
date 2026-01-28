@file:OptIn(ExperimentalMaterial3Api::class)

package com.kingpaging.qwelcome.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kingpaging.qwelcome.R
import com.kingpaging.qwelcome.di.LocalCustomerIntakeViewModel
import com.kingpaging.qwelcome.di.LocalNavigator
import com.kingpaging.qwelcome.di.LocalTemplateListViewModel
import com.kingpaging.qwelcome.ui.components.CyberpunkBackdrop
import com.kingpaging.qwelcome.ui.components.NeonButton
import com.kingpaging.qwelcome.ui.components.NeonButtonStyle
import com.kingpaging.qwelcome.ui.components.NeonCyanButton
import com.kingpaging.qwelcome.ui.components.NeonOutlinedField
import com.kingpaging.qwelcome.ui.components.NeonPanel
import com.kingpaging.qwelcome.ui.components.QrCodeBottomSheet
import com.kingpaging.qwelcome.ui.components.QWelcomeHeader
import com.kingpaging.qwelcome.ui.theme.LocalCyberColors
import com.kingpaging.qwelcome.ui.theme.LocalDarkTheme
import com.kingpaging.qwelcome.util.rememberHapticFeedback
import com.kingpaging.qwelcome.viewmodel.UiEvent
import kotlinx.coroutines.launch

@Suppress("LocalContextGetResourceValueCall")
@Composable
fun CustomerIntakeScreen(
    onOpenSettings: () -> Unit,
    onOpenTemplates: () -> Unit = {}
) {
    // Get ViewModel and Navigator from CompositionLocals (provided at Activity level)
    val customerIntakeViewModel = LocalCustomerIntakeViewModel.current
    val templateListViewModel = LocalTemplateListViewModel.current
    val navigator = LocalNavigator.current
    val hapticFeedback = rememberHapticFeedback()

    val uiState by customerIntakeViewModel.uiState.collectAsStateWithLifecycle()
    val templateUiState by templateListViewModel.uiState.collectAsStateWithLifecycle()
    // Use rememberSaveable so rotation/process death doesn't reset these
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var showQrSheet by rememberSaveable { mutableStateOf(false) }
    // Dropdown state is transient - use remember so it collapses on rotation
    var templateDropdownExpanded by remember { mutableStateOf(false) }
    // Copy success feedback - brief visual confirmation (ChatGPT feedback: animate meaning)
    var copySuccess by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Note: Lifecycle observer for auto-clear is registered in MainActivity
    // to ensure single registration and proper cleanup.

    // Collect UI events (Toasts, rate limit warnings, copy success feedback)
    LaunchedEffect(Unit) {
        customerIntakeViewModel.uiEvent.collect { event ->
            when (event) {
                is UiEvent.CopySuccess -> {
                    // Typed event for copy feedback - non-blocking reset
                    copySuccess = true
                    launch {
                        kotlinx.coroutines.delay(1500L)
                        copySuccess = false
                    }
                }
                is UiEvent.ShowToast -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
                is UiEvent.RateLimitExceeded -> {
                    Toast.makeText(context, context.getString(R.string.toast_rate_limit), Toast.LENGTH_LONG).show()
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
                        IconButton(onClick = {
                            hapticFeedback()
                            customerIntakeViewModel.clearForm()
                        }) {
                            Icon(Icons.Filled.PersonAdd, contentDescription = "New Customer")
                        }
                        IconButton(onClick = {
                            hapticFeedback()
                            onOpenSettings()
                        }) {
                            Icon(Icons.Filled.Settings, contentDescription = "Settings")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        actionIconContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp) // Top-aligned with spacing feels more like a tool
            ) {
                // Template selector using ExposedDropdownMenuBox for proper light UI
                val isDark = LocalDarkTheme.current
                if (templateUiState.templates.isNotEmpty()) {
                    val activeTemplate = remember(
                        templateUiState.templates,
                        templateUiState.activeTemplateId
                    ) {
                        templateUiState.templates.find { it.id == templateUiState.activeTemplateId }
                    }

                    // ExposedDropdownMenuBox provides proper anchoring and sizing for light UI
                    ExposedDropdownMenuBox(
                        expanded = templateDropdownExpanded,
                        onExpandedChange = { templateDropdownExpanded = !templateDropdownExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = activeTemplate?.name ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Template") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = templateDropdownExpanded) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.secondary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.secondary.copy(alpha = if (isDark) 0.45f else 0.28f),
                                focusedLabelColor = MaterialTheme.colorScheme.secondary,
                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                        )

                        ExposedDropdownMenu(
                            expanded = templateDropdownExpanded,
                            onDismissRequest = { templateDropdownExpanded = false }
                        ) {
                            templateUiState.templates.forEach { template ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(template.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            if (template.id == templateUiState.activeTemplateId) {
                                                Icon(
                                                    Icons.Filled.Check,
                                                    contentDescription = "Active",
                                                    tint = MaterialTheme.colorScheme.secondary,
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
                                text = { Text("Manage Templates…", color = MaterialTheme.colorScheme.tertiary) },
                                onClick = {
                                    templateDropdownExpanded = false
                                    onOpenTemplates()
                                }
                            )
                        }
                    }
                }

                NeonPanel(
                    modifier = Modifier.semantics(mergeDescendants = true) {}
                ) {
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
                            IconButton(onClick = {
                                hapticFeedback()
                                passwordVisible = !passwordVisible
                            }) {
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

                // === SEND SECTION ===
                Text(
                    "Send",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.semantics { heading() }
                )

                // Button hierarchy: SMS = primary (hero), Share = secondary, Copy = tertiary
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // SMS = Primary action (filled, most prominent - the hero button)
                    NeonCyanButton(
                        onClick = {
                            hapticFeedback()
                            customerIntakeViewModel.onSmsClicked(navigator)
                        },
                        modifier = Modifier.weight(1f),
                        style = NeonButtonStyle.PRIMARY
                    ) {
                        Icon(
                            Icons.Filled.Send,
                            contentDescription = "Send SMS",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("SMS")
                    }
                    // Share = Secondary (outlined, important but not main)
                    NeonCyanButton(
                        onClick = {
                            hapticFeedback()
                            customerIntakeViewModel.onShareClicked(navigator)
                        },
                        modifier = Modifier.weight(1f),
                        style = NeonButtonStyle.SECONDARY
                    ) {
                        Icon(
                            Icons.Filled.Share,
                            contentDescription = "Share",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Share")
                    }
                    // Copy = Tertiary (lowest emphasis - utility action)
                    // Success state provides visual feedback on action completion
                    val cyberColors = LocalCyberColors.current
                    NeonButton(
                        onClick = {
                            hapticFeedback()
                            customerIntakeViewModel.onCopyClicked(navigator)
                        },
                        modifier = Modifier.weight(1f),
                        style = NeonButtonStyle.TERTIARY,
                        // Success state: Switch to success green color
                        glowColor = if (copySuccess) cyberColors.success else MaterialTheme.colorScheme.primary
                    ) {
                        Icon(
                            // Success state: Show check icon instead of copy icon
                            if (copySuccess) Icons.Filled.Check else Icons.Filled.ContentCopy,
                            contentDescription = "Copy to clipboard",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(if (copySuccess) "Copied!" else "Copy")
                    }
                }

                // === WIFI QR CODE SECTION ===
                // WPA/WPA2 passwords must be 8-63 characters
                val passwordValid = uiState.password.length >= 8
                val qrEnabled = uiState.ssid.isNotBlank() && passwordValid

                val qrHint = when {
                    uiState.ssid.isBlank() && uiState.password.isBlank() -> "Enter SSID + password to generate"
                    uiState.ssid.isBlank() -> "Enter SSID"
                    !passwordValid -> "Password needs 8+ characters (${uiState.password.length}/8)"
                    else -> uiState.ssid
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.semantics(mergeDescendants = true) {}) {
                        Text(
                            "WiFi QR Code",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.semantics { heading() }
                        )
                        Text(
                            qrHint,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (qrEnabled) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                    NeonButton(
                        onClick = {
                            hapticFeedback()
                            showQrSheet = true
                        },
                        glowColor = MaterialTheme.colorScheme.tertiary,
                        style = NeonButtonStyle.TERTIARY,
                        enabled = qrEnabled
                    ) {
                        Icon(
                            Icons.Filled.QrCode2,
                            contentDescription = "Show QR Code",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Show QR")
                    }
                }
            }
        }
    }
}
