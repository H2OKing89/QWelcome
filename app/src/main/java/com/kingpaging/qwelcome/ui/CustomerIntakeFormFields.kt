@file:Suppress("FunctionNaming")

package com.kingpaging.qwelcome.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.kingpaging.qwelcome.R
import com.kingpaging.qwelcome.ui.components.NeonButton
import com.kingpaging.qwelcome.ui.components.NeonButtonStyle
import com.kingpaging.qwelcome.ui.components.NeonDropdownMenuBox
import com.kingpaging.qwelcome.ui.components.NeonOutlinedField
import com.kingpaging.qwelcome.ui.components.NeonPanel
import com.kingpaging.qwelcome.util.WifiQrGenerator

@Composable
internal fun CustomerFormFields(
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
        CustomerContactFields(
            uiState = uiState,
            focusTargets = focusTargets,
            onCustomerNameChanged = onCustomerNameChanged,
            onCustomerPhoneChanged = onCustomerPhoneChanged
        )
        WifiCredentialFields(
            uiState = uiState,
            focusTargets = focusTargets,
            passwordVisible = passwordVisible,
            onSsidChanged = onSsidChanged,
            onPasswordChanged = onPasswordChanged,
            onPasswordVisibilityToggle = onPasswordVisibilityToggle
        )
        AdvancedWifiOptions(
            isOpenNetwork = uiState.isOpenNetwork,
            onOpenNetworkChanged = onOpenNetworkChanged,
            isHiddenNetwork = isHiddenNetwork,
            onHiddenNetworkChanged = onHiddenNetworkChanged,
            securityType = securityType,
            expanded = advancedWifiOptionsExpanded,
            onExpandedChange = onAdvancedWifiOptionsExpandedChange,
            securityDropdownExpanded = securityDropdownExpanded,
            onSecurityDropdownExpandedChange = onSecurityDropdownExpandedChange,
            onSecurityTypeChanged = onSecurityTypeChanged
        )
        AccountNumberField(
            uiState = uiState,
            focusTargets = focusTargets,
            onAccountNumberChanged = onAccountNumberChanged
        )
    }
}


@Composable
private fun CustomerContactFields(
    uiState: CustomerIntakeUiState,
    focusTargets: CustomerFormFocusTargets,
    onCustomerNameChanged: (String) -> Unit,
    onCustomerPhoneChanged: (String) -> Unit
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
}

@Composable
private fun WifiCredentialFields(
    uiState: CustomerIntakeUiState,
    focusTargets: CustomerFormFocusTargets,
    passwordVisible: Boolean,
    onSsidChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onPasswordVisibilityToggle: () -> Unit
) {
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
    WifiPasswordField(
        uiState = uiState,
        focusTargets = focusTargets,
        passwordVisible = passwordVisible,
        onPasswordChanged = onPasswordChanged,
        onPasswordVisibilityToggle = onPasswordVisibilityToggle
    )
}

@Composable
private fun WifiPasswordField(
    uiState: CustomerIntakeUiState,
    focusTargets: CustomerFormFocusTargets,
    passwordVisible: Boolean,
    onPasswordChanged: (String) -> Unit,
    onPasswordVisibilityToggle: () -> Unit
) {
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
}

@Composable
private fun AdvancedWifiOptions(
    isOpenNetwork: Boolean,
    onOpenNetworkChanged: (Boolean) -> Unit,
    isHiddenNetwork: Boolean,
    onHiddenNetworkChanged: (Boolean) -> Unit,
    securityType: WifiQrGenerator.SecurityType,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    securityDropdownExpanded: Boolean,
    onSecurityDropdownExpandedChange: (Boolean) -> Unit,
    onSecurityTypeChanged: (WifiQrGenerator.SecurityType) -> Unit
) {
    val expandedLabel = stringResource(R.string.state_expanded)
    val collapsedLabel = stringResource(R.string.state_collapsed)
    NeonButton(
        onClick = { onExpandedChange(!expanded) },
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                stateDescription = if (expanded) expandedLabel else collapsedLabel
            },
        style = NeonButtonStyle.TERTIARY
    ) {
        Text(
            text = stringResource(R.string.label_advanced_wifi_options),
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = null
        )
    }

    if (expanded) {
        AdvancedWifiOptionsSection(
            isOpenNetwork = isOpenNetwork,
            onOpenNetworkChanged = onOpenNetworkChanged,
            isHiddenNetwork = isHiddenNetwork,
            onHiddenNetworkChanged = onHiddenNetworkChanged,
            securityType = securityType,
            securityDropdownExpanded = securityDropdownExpanded,
            onSecurityDropdownExpandedChange = onSecurityDropdownExpandedChange,
            onSecurityTypeChanged = onSecurityTypeChanged
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
    NetworkOptionToggle(
        value = isOpenNetwork,
        onValueChange = onOpenNetworkChanged,
        labelResource = R.string.label_open_network
    )
    NetworkOptionToggle(
        value = isHiddenNetwork,
        onValueChange = onHiddenNetworkChanged,
        labelResource = R.string.label_hidden_network
    )

    if (!isOpenNetwork) {
        SecurityTypeSelector(
            securityType = securityType,
            expanded = securityDropdownExpanded,
            onExpandedChange = onSecurityDropdownExpandedChange,
            onSecurityTypeChanged = onSecurityTypeChanged
        )
    }
}

@Composable
private fun NetworkOptionToggle(
    value: Boolean,
    onValueChange: (Boolean) -> Unit,
    @StringRes labelResource: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = value,
                onValueChange = onValueChange,
                role = Role.Checkbox
            )
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = value,
            onCheckedChange = null,
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.secondary,
                uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
        Text(
            text = stringResource(labelResource),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
private fun SecurityTypeSelector(
    securityType: WifiQrGenerator.SecurityType,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSecurityTypeChanged: (WifiQrGenerator.SecurityType) -> Unit
) {
    NeonDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = onExpandedChange,
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
                onExpandedChange(false)
            }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.security_wpa3_sae)) },
            onClick = {
                onSecurityTypeChanged(WifiQrGenerator.SecurityType.WPA3_SAE)
                onExpandedChange(false)
            }
        )
    }
}

@Composable
private fun AccountNumberField(
    uiState: CustomerIntakeUiState,
    focusTargets: CustomerFormFocusTargets,
    onAccountNumberChanged: (String) -> Unit
) {
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
