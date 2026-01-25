@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.allowelcome.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.allowelcome.viewmodel.CustomerIntakeViewModel
import com.example.allowelcome.viewmodel.factory.AppViewModelProvider

@Composable
fun CustomerIntakeScreen(
    onOpenSettings: () -> Unit,
    customerIntakeViewModel: CustomerIntakeViewModel = viewModel(factory = AppViewModelProvider(LocalContext.current))
) {
    val uiState by customerIntakeViewModel.uiState.collectAsState()
    var passwordVisible by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ALLO Customer Intake") },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = uiState.customerName,
                onValueChange = { customerIntakeViewModel.onCustomerNameChanged(it) },
                label = { Text("Customer Name") },
                isError = uiState.customerNameError != null,
                supportingText = { uiState.customerNameError?.let { Text(it) } },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = uiState.customerPhone,
                onValueChange = { customerIntakeViewModel.onCustomerPhoneChanged(it) },
                label = { Text("Customer Phone") },
                isError = uiState.customerPhoneError != null,
                supportingText = { uiState.customerPhoneError?.let { Text(it) } },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = uiState.ssid,
                onValueChange = { customerIntakeViewModel.onSsidChanged(it) },
                label = { Text("WiFi SSID") },
                isError = uiState.ssidError != null,
                supportingText = { uiState.ssidError?.let { Text(it) } },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
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
                },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = uiState.accountNumber,
                onValueChange = { customerIntakeViewModel.onAccountNumberChanged(it) },
                label = { Text("Account Number") },
                isError = uiState.accountNumberError != null,
                supportingText = { uiState.accountNumberError?.let { Text(it) } },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(onClick = { customerIntakeViewModel.onSmsClicked(context) }) {
                    Text("SMS")
                }
                Button(onClick = { customerIntakeViewModel.onShareClicked(context) }) {
                    Text("Share")
                }
                Button(onClick = { customerIntakeViewModel.onCopyClicked(context) }) {
                    Text("Copy")
                }
            }
        }
    }
}
