package com.kingpaging.qwelcome.ui

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
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kingpaging.qwelcome.R
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
import com.kingpaging.qwelcome.util.WifiQrGenerator
import com.kingpaging.qwelcome.util.rememberHapticFeedback
import com.kingpaging.qwelcome.viewmodel.templates.TemplateListUiState

internal class FormFieldFocusTarget {
    val focusRequester = FocusRequester()
    val bringIntoViewRequester = BringIntoViewRequester()
}

internal class CustomerFormFocusTargets {
    val customerName = FormFieldFocusTarget()
    val customerPhone = FormFieldFocusTarget()
    val ssid = FormFieldFocusTarget()
    val password = FormFieldFocusTarget()
    val accountNumber = FormFieldFocusTarget()

    fun firstInvalid(uiState: CustomerIntakeUiState): FormFieldFocusTarget? = when {
        uiState.customerNameError != null -> customerName
        uiState.customerPhoneError != null -> customerPhone
        uiState.ssidError != null -> ssid
        uiState.passwordError != null -> password
        uiState.accountNumberError != null -> accountNumber
        else -> null
    }
}

internal data class CustomerIntakeActions(
    val onDismissQr: () -> Unit,
    val onClearForm: () -> Unit,
    val onTemplateSelected: (String) -> Unit,
    val onCustomerNameChanged: (String) -> Unit,
    val onCustomerPhoneChanged: (String) -> Unit,
    val onSsidChanged: (String) -> Unit,
    val onSecurityTypeChanged: (WifiQrGenerator.SecurityType) -> Unit,
    val onHiddenNetworkChanged: (Boolean) -> Unit,
    val onOpenNetworkChanged: (Boolean) -> Unit,
    val onPasswordChanged: (String) -> Unit,
    val onAccountNumberChanged: (String) -> Unit,
    val onSmsClick: () -> Unit,
    val onShareClick: () -> Unit,
    val onCopyClick: () -> Unit,
    val onShowQr: () -> Unit
)

@Suppress("FunctionNaming", "LongMethod", "LongParameterList")
@Composable
internal fun CustomerIntakeScreen(
    uiState: CustomerIntakeUiState,
    templateUiState: TemplateListUiState,
    snackbarHostState: SnackbarHostState,
    formFocusTargets: CustomerFormFocusTargets,
    copySuccess: Boolean,
    actions: CustomerIntakeActions,
    onOpenSettings: () -> Unit,
    onOpenTemplates: () -> Unit = {}
) {
    val hapticFeedback = rememberHapticFeedback()
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var advancedWifiOptionsExpanded by rememberSaveable { mutableStateOf(false) }
    var securityDropdownExpanded by remember { mutableStateOf(false) }
    var templateDropdownExpanded by remember { mutableStateOf(false) }

    if (uiState.showQrSheet) {
        QrCodeBottomSheet(
            ssid = uiState.ssid,
            password = uiState.password,
            isOpenNetwork = uiState.isOpenNetwork,
            securityType = uiState.securityType,
            isHiddenNetwork = uiState.isHiddenNetwork,
            onDismiss = actions.onDismissQr
        )
    }

    CyberpunkBackdrop {
        Scaffold(
            // Intentional: keep scaffold transparent so the cyberpunk backdrop remains visible.
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                NeonTopAppBar(
                    title = { QWelcomeHeader() },
                    actions = {
                        IconButton(onClick = {
                            hapticFeedback()
                            actions.onClearForm()
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
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp) // Top-aligned with spacing feels more like a tool
            ) {
                TemplateSelector(
                    templateUiState = templateUiState,
                    expanded = templateDropdownExpanded,
                    onExpandedChange = { templateDropdownExpanded = it },
                    onTemplateSelected = {
                        hapticFeedback()
                        actions.onTemplateSelected(it)
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
                    focusTargets = formFocusTargets,
                    passwordVisible = passwordVisible,
                    onCustomerNameChanged = actions.onCustomerNameChanged,
                    onCustomerPhoneChanged = actions.onCustomerPhoneChanged,
                    onSsidChanged = actions.onSsidChanged,
                    advancedWifiOptionsExpanded = advancedWifiOptionsExpanded,
                    onAdvancedWifiOptionsExpandedChange = { expanded ->
                        hapticFeedback()
                        advancedWifiOptionsExpanded = expanded
                        if (!expanded) securityDropdownExpanded = false
                    },
                    securityType = uiState.securityType,
                    securityDropdownExpanded = securityDropdownExpanded,
                    onSecurityDropdownExpandedChange = { securityDropdownExpanded = it },
                    onSecurityTypeChanged = {
                        hapticFeedback()
                        actions.onSecurityTypeChanged(it)
                    },
                    isHiddenNetwork = uiState.isHiddenNetwork,
                    onHiddenNetworkChanged = {
                        hapticFeedback()
                        actions.onHiddenNetworkChanged(it)
                    },
                    onOpenNetworkChanged = {
                        hapticFeedback()
                        actions.onOpenNetworkChanged(it)
                    },
                    onPasswordChanged = actions.onPasswordChanged,
                    onPasswordVisibilityToggle = {
                        hapticFeedback()
                        passwordVisible = !passwordVisible
                    },
                    onAccountNumberChanged = actions.onAccountNumberChanged
                )

                ActionButtonRow(
                    copySuccess = copySuccess,
                    onSmsClick = {
                        hapticFeedback()
                        actions.onSmsClick()
                    },
                    onShareClick = {
                        hapticFeedback()
                        actions.onShareClick()
                    },
                    onCopyClick = {
                        hapticFeedback()
                        actions.onCopyClick()
                    }
                )

                QrCodeSection(
                    uiState = uiState,
                    enabled = uiState.qrEnabled,
                    onShowQrClick = {
                        hapticFeedback()
                        actions.onShowQr()
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
        onExpandedChange = { onExpandedChange(it) },
        selectedText = activeTemplate?.name ?: "",
        label = { Text(stringResource(R.string.label_template)) },
        modifier = Modifier.fillMaxWidth()
    ) {
        templateUiState.templates.forEach { template ->
            key(template.id) {
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
    focusTargets: CustomerFormFocusTargets,
    passwordVisible: Boolean,
    onCustomerNameChanged: (String) -> Unit,
    onCustomerPhoneChanged: (String) -> Unit,
    onSsidChanged: (String) -> Unit,
    advancedWifiOptionsExpanded: Boolean,
    onAdvancedWifiOptionsExpandedChange: (Boolean) -> Unit,
    securityType: WifiQrGenerator.SecurityType,
    securityDropdownExpanded: Boolean,
    onSecurityDropdownExpandedChange: (Boolean) -> Unit,
    onSecurityTypeChanged: (WifiQrGenerator.SecurityType) -> Unit,
    isHiddenNetwork: Boolean,
    onHiddenNetworkChanged: (Boolean) -> Unit,
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
            supportingText = { uiState.customerNameError?.let { Text(it) } },
            modifier = Modifier
                .focusRequester(focusTargets.customerName.focusRequester)
                .bringIntoViewRequester(focusTargets.customerName.bringIntoViewRequester)
        )

        NeonOutlinedField(
            value = uiState.customerPhone,
            onValueChange = onCustomerPhoneChanged,
            label = { Text(stringResource(R.string.label_customer_phone)) },
            isError = uiState.customerPhoneError != null,
            supportingText = { uiState.customerPhoneError?.let { Text(it) } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier
                .focusRequester(focusTargets.customerPhone.focusRequester)
                .bringIntoViewRequester(focusTargets.customerPhone.bringIntoViewRequester)
        )
        NeonOutlinedField(
            value = uiState.ssid,
            onValueChange = onSsidChanged,
            label = { Text(stringResource(R.string.label_wifi_ssid)) },
            isError = uiState.ssidError != null,
            supportingText = { uiState.ssidError?.let { Text(it) } },
            modifier = Modifier
                .focusRequester(focusTargets.ssid.focusRequester)
                .bringIntoViewRequester(focusTargets.ssid.bringIntoViewRequester)
        )

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
            },
            modifier = Modifier
                .focusRequester(focusTargets.password.focusRequester)
                .bringIntoViewRequester(focusTargets.password.bringIntoViewRequester)
        )

        val expandedLabel = stringResource(R.string.state_expanded)
        val collapsedLabel = stringResource(R.string.state_collapsed)
        NeonButton(
            onClick = {
                onAdvancedWifiOptionsExpandedChange(!advancedWifiOptionsExpanded)
            },
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    stateDescription = if (advancedWifiOptionsExpanded) expandedLabel else collapsedLabel
                },
            style = NeonButtonStyle.TERTIARY
        ) {
            Text(
                text = stringResource(R.string.label_advanced_wifi_options),
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (advancedWifiOptionsExpanded) {
                    Icons.Default.ExpandLess
                } else {
                    Icons.Default.ExpandMore
                },
                contentDescription = null
            )
        }

        if (advancedWifiOptionsExpanded) {
            AdvancedWifiOptionsSection(
                isOpenNetwork = uiState.isOpenNetwork,
                onOpenNetworkChanged = onOpenNetworkChanged,
                isHiddenNetwork = isHiddenNetwork,
                onHiddenNetworkChanged = onHiddenNetworkChanged,
                securityType = securityType,
                securityDropdownExpanded = securityDropdownExpanded,
                onSecurityDropdownExpandedChange = onSecurityDropdownExpandedChange,
                onSecurityTypeChanged = onSecurityTypeChanged
            )
        }

        NeonOutlinedField(
            value = uiState.accountNumber,
            onValueChange = onAccountNumberChanged,
            label = { Text(stringResource(R.string.label_account_number)) },
            isError = uiState.accountNumberError != null,
            supportingText = { uiState.accountNumberError?.let { Text(it) } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier
                .focusRequester(focusTargets.accountNumber.focusRequester)
                .bringIntoViewRequester(focusTargets.accountNumber.bringIntoViewRequester)
        )
    }
}

@Composable
private fun AdvancedWifiOptionsSection(
    isOpenNetwork: Boolean,
    onOpenNetworkChanged: (Boolean) -> Unit,
    isHiddenNetwork: Boolean,
    onHiddenNetworkChanged: (Boolean) -> Unit,
    securityType: WifiQrGenerator.SecurityType,
    securityDropdownExpanded: Boolean,
    onSecurityDropdownExpandedChange: (Boolean) -> Unit,
    onSecurityTypeChanged: (WifiQrGenerator.SecurityType) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = isOpenNetwork,
                onValueChange = onOpenNetworkChanged,
                role = Role.Checkbox
            )
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isOpenNetwork,
            onCheckedChange = null,
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

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = isHiddenNetwork,
                onValueChange = onHiddenNetworkChanged,
                role = Role.Checkbox
            )
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isHiddenNetwork,
            onCheckedChange = null,
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.secondary,
                uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
        Text(
            text = stringResource(R.string.label_hidden_network),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 8.dp)
        )
    }

    if (!isOpenNetwork) {
        NeonDropdownMenuBox(
            expanded = securityDropdownExpanded,
            onExpandedChange = onSecurityDropdownExpandedChange,
            selectedText = stringResource(
                if (securityType == WifiQrGenerator.SecurityType.WPA2_PSK) {
                    R.string.security_wpa2
                } else {
                    R.string.security_wpa3_sae
                }
            ),
            label = { Text(stringResource(R.string.label_wifi_security)) },
            modifier = Modifier.fillMaxWidth()
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.security_wpa2)) },
                onClick = {
                    onSecurityTypeChanged(WifiQrGenerator.SecurityType.WPA2_PSK)
                    onSecurityDropdownExpandedChange(false)
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.security_wpa3_sae)) },
                onClick = {
                    onSecurityTypeChanged(WifiQrGenerator.SecurityType.WPA3_SAE)
                    onSecurityDropdownExpandedChange(false)
                }
            )
        }
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
        uiState.ssidError != null -> uiState.ssidError
        !uiState.isOpenNetwork && uiState.passwordError != null -> uiState.passwordError
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
