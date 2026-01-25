@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.allowelcome.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.allowelcome.ui.components.CyberpunkBackdrop
import com.example.allowelcome.ui.components.NeonButton
import com.example.allowelcome.ui.components.NeonCyanButton
import com.example.allowelcome.ui.components.NeonMagentaButton
import com.example.allowelcome.ui.components.NeonOutlinedField
import com.example.allowelcome.ui.components.NeonPanel
import com.example.allowelcome.ui.components.QrCodeBottomSheet
import com.example.allowelcome.ui.components.QWelcomeHeader
import com.example.allowelcome.ui.theme.CyberScheme
import com.example.allowelcome.viewmodel.CustomerIntakeViewModel
import com.example.allowelcome.viewmodel.factory.AppViewModelProvider

@Composable
fun CustomerIntakeScreen(
    onOpenSettings: () -> Unit,
    customerIntakeViewModel: CustomerIntakeViewModel = viewModel(factory = AppViewModelProvider(LocalContext.current))
) {
    val uiState by customerIntakeViewModel.uiState.collectAsState()
    var passwordVisible by remember { mutableStateOf(false) }
    var showQrSheet by remember { mutableStateOf(false) }
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
                        onClick = { customerIntakeViewModel.onSmsClicked(context) },
                        modifier = Modifier.weight(1f)
                    ) { Text("SMS") }
                    NeonMagentaButton(
                        onClick = { customerIntakeViewModel.onShareClicked(context) },
                        modifier = Modifier.weight(1f)
                    ) { Text("Share") }
                    NeonCyanButton(
                        onClick = { customerIntakeViewModel.onCopyClicked(context) },
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
