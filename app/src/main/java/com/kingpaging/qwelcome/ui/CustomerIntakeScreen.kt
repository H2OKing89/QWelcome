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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
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
import com.kingpaging.qwelcome.ui.components.NeonDropdownMenuBox
import com.kingpaging.qwelcome.ui.components.NeonOutlinedField
import com.kingpaging.qwelcome.ui.components.NeonPanel
import com.kingpaging.qwelcome.ui.components.NeonTopAppBar
import com.kingpaging.qwelcome.ui.components.QrCodeBottomSheet
import com.kingpaging.qwelcome.ui.components.QWelcomeHeader
import com.kingpaging.qwelcome.ui.theme.LocalCyberColors
import com.kingpaging.qwelcome.util.rememberHapticFeedback
import com.kingpaging.qwelcome.di.LocalSoundPlayer
import com.kingpaging.qwelcome.util.WifiQrGenerator
import com.kingpaging.qwelcome.viewmodel.UiEvent
import com.kingpaging.qwelcome.viewmodel.templates.TemplateListUiState
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
    val soundPlayer = LocalSoundPlayer.current
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
                is UiEvent.ValidationFailed,
                is UiEvent.ActionFailed -> {
                    soundPlayer.playBeep()
                }
                is UiEvent.RateLimitExceeded -> {
                    soundPlayer.playBeep()
                    Toast.makeText(context, context.getString(R.string.toast_rate_limit), Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // QR Code Bottom Sheet
    // For open networks, only SSID is required
    // For secured networks, both SSID and valid password are required
    val qrEnabled = if (uiState.isOpenNetwork) {
        uiState.ssid.isNotBlank()
    } else {
        uiState.ssid.isNotBlank() && uiState.password.length >= WifiQrGenerator.MIN_PASSWORD_LENGTH
    }

    if (showQrSheet && qrEnabled) {
        QrCodeBottomSheet(
            ssid = uiState.ssid,
            password = uiState.password,
            isOpenNetwork = uiState.isOpenNetwork,
            onDismiss = { showQrSheet = false }
        )
    }

    CyberpunkBackdrop {
        Scaffold(
            // Intentional: keep scaffold transparent so the cyberpunk backdrop remains visible.
            containerColor = Color.Transparent,
            topBar = {
                NeonTopAppBar(
                    title = { QWelcomeHeader() },
                    actions = {
                        IconButton(onClick = {
                            hapticFeedback()
                            customerIntakeViewModel.clearForm()
                        }) {
                            Icon(Icons.Filled.PersonAdd, contentDescription = stringResource(R.string.content_desc_new_customer))
                        }
                        IconButton(onClick = {
                            hapticFeedback()
                            onOpenSettings()
                        }) {
                            Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.content_desc_settings))
                        }
                    },
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground
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
                TemplateSelector(
                    templateUiState = templateUiState,
                    expanded = templateDropdownExpanded,
                    onExpandedChange = { templateDropdownExpanded = it },
                    onTemplateSelected = {
                        hapticFeedback()
                        templateListViewModel.setActiveTemplate(it)
                        templateDropdownExpanded = false
                    },
                    onManageTemplates = {
                        hapticFeedback()
                        templateDropdownExpanded = false
                        onOpenTemplates()
                    }
                )

                CustomerFormFields(
                    uiState = uiState,
                    passwordVisible = passwordVisible,
                    onCustomerNameChanged = customerIntakeViewModel::onCustomerNameChanged,
                    onCustomerPhoneChanged = customerIntakeViewModel::onCustomerPhoneChanged,
                    onSsidChanged = customerIntakeViewModel::onSsidChanged,
                    onOpenNetworkChanged = {
                        hapticFeedback()
                        customerIntakeViewModel.onOpenNetworkChanged(it)
                    },
                    onPasswordChanged = customerIntakeViewModel::onPasswordChanged,
                    onPasswordVisibilityToggle = {
                        hapticFeedback()
                        passwordVisible = !passwordVisible
                    },
                    onAccountNumberChanged = customerIntakeViewModel::onAccountNumberChanged
                )

                ActionButtonRow(
                    copySuccess = copySuccess,
                    onSmsClick = {
                        hapticFeedback()
                        customerIntakeViewModel.onSmsClicked(navigator)
                    },
                    onShareClick = {
                        hapticFeedback()
                        customerIntakeViewModel.onShareClicked(navigator)
                    },
                    onCopyClick = {
                        hapticFeedback()
                        customerIntakeViewModel.onCopyClicked(navigator)
                    }
                )

                QrCodeSection(
                    uiState = uiState,
                    enabled = qrEnabled,
                    onShowQrClick = {
                        hapticFeedback()
                        showQrSheet = true
                    }
                )
            }
        }
    }
}

@Composable
private fun TemplateSelector(
    templateUiState: TemplateListUiState,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onTemplateSelected: (String) -> Unit,
    onManageTemplates: () -> Unit
) {
    if (templateUiState.templates.isEmpty()) return

    val activeTemplate = remember(
        templateUiState.templates,
        templateUiState.activeTemplateId
    ) {
        templateUiState.templates.find { it.id == templateUiState.activeTemplateId }
    }

    NeonDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { onExpandedChange(!expanded) },
        selectedText = activeTemplate?.name ?: "",
        label = { Text(stringResource(R.string.label_template)) },
        modifier = Modifier.fillMaxWidth()
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
                                contentDescription = stringResource(R.string.label_active),
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                },
                onClick = { onTemplateSelected(template.id) }
            )
        }

        HorizontalDivider()

        DropdownMenuItem(
            text = {
                Text(
                    stringResource(R.string.action_manage_templates),
                    color = MaterialTheme.colorScheme.tertiary
                )
            },
            onClick = onManageTemplates
        )
    }
}

@Composable
private fun CustomerFormFields(
    uiState: CustomerIntakeUiState,
    passwordVisible: Boolean,
    onCustomerNameChanged: (String) -> Unit,
    onCustomerPhoneChanged: (String) -> Unit,
    onSsidChanged: (String) -> Unit,
    onOpenNetworkChanged: (Boolean) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onPasswordVisibilityToggle: () -> Unit,
    onAccountNumberChanged: (String) -> Unit
) {
    NeonPanel(
        modifier = Modifier.semantics(mergeDescendants = true) {}
    ) {
        NeonOutlinedField(
            value = uiState.customerName,
            onValueChange = onCustomerNameChanged,
            label = { Text(stringResource(R.string.label_customer_name)) },
            isError = uiState.customerNameError != null,
            supportingText = { uiState.customerNameError?.let { Text(it) } }
        )
        NeonOutlinedField(
            value = uiState.customerPhone,
            onValueChange = onCustomerPhoneChanged,
            label = { Text(stringResource(R.string.label_customer_phone)) },
            isError = uiState.customerPhoneError != null,
            supportingText = { uiState.customerPhoneError?.let { Text(it) } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
        )
        NeonOutlinedField(
            value = uiState.ssid,
            onValueChange = onSsidChanged,
            label = { Text(stringResource(R.string.label_wifi_ssid)) },
            isError = uiState.ssidError != null,
            supportingText = { uiState.ssidError?.let { Text(it) } }
        )

        // Open Network toggle - allows skipping password for guest networks
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(
                    value = uiState.isOpenNetwork,
                    onValueChange = onOpenNetworkChanged,
                    role = Role.Checkbox
                )
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = uiState.isOpenNetwork,
                onCheckedChange = null, // Handled by Row's toggleable
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.secondary,
                    uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            Text(
                text = stringResource(R.string.label_open_network),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        NeonOutlinedField(
            value = if (uiState.isOpenNetwork) "" else uiState.password,
            onValueChange = onPasswordChanged,
            label = { Text(stringResource(R.string.label_wifi_password)) },
            enabled = !uiState.isOpenNetwork,
            isError = uiState.passwordError != null,
            supportingText = {
                if (uiState.isOpenNetwork) {
                    Text(stringResource(R.string.hint_password_disabled))
                } else {
                    uiState.passwordError?.let { Text(it) }
                }
            },
            visualTransformation = if (passwordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            trailingIcon = {
                if (!uiState.isOpenNetwork) {
                    val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                    val description = if (passwordVisible) {
                        stringResource(R.string.content_desc_hide_password)
                    } else {
                        stringResource(R.string.content_desc_show_password)
                    }
                    IconButton(onClick = onPasswordVisibilityToggle) {
                        Icon(imageVector = image, contentDescription = description)
                    }
                }
            }
        )
        NeonOutlinedField(
            value = uiState.accountNumber,
            onValueChange = onAccountNumberChanged,
            label = { Text(stringResource(R.string.label_account_number)) },
            isError = uiState.accountNumberError != null,
            supportingText = { uiState.accountNumberError?.let { Text(it) } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
    }
}

@Composable
private fun ActionButtonRow(
    copySuccess: Boolean,
    onSmsClick: () -> Unit,
    onShareClick: () -> Unit,
    onCopyClick: () -> Unit
) {
    // === SEND SECTION ===
    Text(
        stringResource(R.string.header_send),
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
            onClick = onSmsClick,
            modifier = Modifier.weight(1f),
            style = NeonButtonStyle.PRIMARY
        ) {
            Icon(
                Icons.AutoMirrored.Filled.Send,
                contentDescription = stringResource(R.string.content_desc_send_sms),
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.action_sms))
        }
        // Share = Secondary (outlined, important but not main)
        NeonCyanButton(
            onClick = onShareClick,
            modifier = Modifier.weight(1f),
            style = NeonButtonStyle.SECONDARY
        ) {
            Icon(
                Icons.Filled.Share,
                contentDescription = stringResource(R.string.action_share),
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(stringResource(R.string.action_share))
        }
        // Copy = Tertiary (lowest emphasis - utility action)
        // Success state provides visual feedback on action completion
        val cyberColors = LocalCyberColors.current
        NeonButton(
            onClick = onCopyClick,
            modifier = Modifier.weight(1f),
            style = NeonButtonStyle.TERTIARY,
            // Success state: Switch to success green color
            glowColor = if (copySuccess) cyberColors.success else MaterialTheme.colorScheme.primary
        ) {
            Icon(
                // Success state: Show check icon instead of copy icon
                if (copySuccess) Icons.Filled.Check else Icons.Filled.ContentCopy,
                contentDescription = stringResource(R.string.content_desc_copy_clipboard),
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                if (copySuccess) {
                    stringResource(R.string.action_copied)
                } else {
                    stringResource(R.string.action_copy)
                }
            )
        }
    }
}

@Composable
private fun QrCodeSection(
    uiState: CustomerIntakeUiState,
    enabled: Boolean,
    onShowQrClick: () -> Unit
) {
    val qrHint = when {
        uiState.isOpenNetwork && uiState.ssid.isBlank() -> stringResource(R.string.hint_qr_enter_ssid_open)
        uiState.isOpenNetwork && uiState.ssid.isNotBlank() -> stringResource(R.string.hint_qr_open_network, uiState.ssid)
        uiState.ssid.isBlank() && uiState.password.isBlank() -> stringResource(R.string.hint_qr_enter_both)
        uiState.ssid.isBlank() -> stringResource(R.string.hint_qr_enter_ssid)
        uiState.password.length < WifiQrGenerator.MIN_PASSWORD_LENGTH ->
            stringResource(R.string.hint_qr_password_length, uiState.password.length)
        else -> uiState.ssid
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.semantics(mergeDescendants = true) {}) {
            Text(
                stringResource(R.string.header_wifi_qr),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.semantics { heading() }
            )
            Text(
                qrHint,
                style = MaterialTheme.typography.bodySmall,
                color = if (enabled) {
                    MaterialTheme.colorScheme.tertiary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                }
            )
        }
        NeonButton(
            onClick = onShowQrClick,
            glowColor = MaterialTheme.colorScheme.tertiary,
            style = NeonButtonStyle.TERTIARY,
            enabled = enabled
        ) {
            Icon(
                Icons.Filled.QrCode2,
                contentDescription = stringResource(R.string.content_desc_show_qr),
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.action_show_qr))
        }
    }
}
