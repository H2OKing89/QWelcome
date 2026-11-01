@file:Suppress("FunctionNaming")

package com.kingpaging.qwelcome.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardActions
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.ImeAction
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
import com.kingpaging.qwelcome.ui.components.displayLabelResource
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
    onPasswordChanged: (String) -> Unit,
    onPasswordVisibilityToggle: () -> Unit,
    onAccountNumberChanged: (String) -> Unit,
    requiresAccount: Boolean = false,
    requiresPassword: Boolean = false,
) {
    NeonPanel {
        Text(
            stringResource(R.string.intake_customer_section),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() },
        )
        CustomerContactFields(
            uiState = uiState,
            focusTargets = focusTargets,
            onCustomerNameChanged = onCustomerNameChanged,
            onCustomerPhoneChanged = onCustomerPhoneChanged,
        )
        AccountNumberField(
            uiState = uiState,
            focusTargets = focusTargets,
            onAccountNumberChanged = onAccountNumberChanged,
            required = requiresAccount,
        )
    }
    NeonPanel {
        Text(
            stringResource(R.string.intake_wifi_section),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() },
        )
        WifiCredentialFields(
            uiState = uiState,
            focusTargets = focusTargets,
            passwordVisible = passwordVisible,
            onSsidChanged = onSsidChanged,
            onPasswordChanged = onPasswordChanged,
            onPasswordVisibilityToggle = onPasswordVisibilityToggle,
            requiresPassword = requiresPassword,
        )
        AdvancedWifiOptions(
            isHiddenNetwork = isHiddenNetwork,
            onHiddenNetworkChanged = onHiddenNetworkChanged,
            securityType = securityType,
            expanded = advancedWifiOptionsExpanded,
            onExpandedChange = onAdvancedWifiOptionsExpandedChange,
            securityDropdownExpanded = securityDropdownExpanded,
            onSecurityDropdownExpandedChange = onSecurityDropdownExpandedChange,
            onSecurityTypeChanged = onSecurityTypeChanged,
        )
    }
}

@Composable
private fun CustomerContactFields(
    uiState: CustomerIntakeUiState,
    focusTargets: CustomerFormFocusTargets,
    onCustomerNameChanged: (String) -> Unit,
    onCustomerPhoneChanged: (String) -> Unit,
) {
    NeonOutlinedField(
        value = uiState.customerName,
        onValueChange = onCustomerNameChanged,
        label = { IntakeFieldLabel(R.string.label_customer_name, required = true) },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        keyboardActions = KeyboardActions(onNext = { focusTargets.customerPhone.focusRequester.requestFocus() }),
        isError = uiState.customerNameError != null,
        supportingText = if (uiState.customerNameError != null) ({ Text(uiState.customerNameError) }) else null,
        modifier =
            Modifier
                .focusRequester(focusTargets.customerName.focusRequester)
                .bringIntoViewRequester(focusTargets.customerName.bringIntoViewRequester),
    )

    NeonOutlinedField(
        value = uiState.customerPhone,
        onValueChange = onCustomerPhoneChanged,
        label = {
            IntakeFieldLabel(
                R.string.label_customer_phone,
                required = false,
                hint = stringResource(R.string.intake_phone_for_sms),
            )
        },
        isError = uiState.customerPhoneError != null,
        supportingText = if (uiState.customerPhoneError != null) ({ Text(uiState.customerPhoneError) }) else null,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
        keyboardActions = KeyboardActions(onNext = { focusTargets.accountNumber.focusRequester.requestFocus() }),
        modifier =
            Modifier
                .focusRequester(focusTargets.customerPhone.focusRequester)
                .bringIntoViewRequester(focusTargets.customerPhone.bringIntoViewRequester),
    )
}

@Composable
private fun WifiCredentialFields(
    uiState: CustomerIntakeUiState,
    focusTargets: CustomerFormFocusTargets,
    passwordVisible: Boolean,
    onSsidChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onPasswordVisibilityToggle: () -> Unit,
    requiresPassword: Boolean,
) {
    val focusManager = LocalFocusManager.current
    NeonOutlinedField(
        value = uiState.ssid,
        onValueChange = onSsidChanged,
        keyboardOptions =
            KeyboardOptions(
                autoCorrectEnabled = false,
                imeAction = if (uiState.isOpenNetwork) ImeAction.Done else ImeAction.Next,
            ),
        keyboardActions =
            KeyboardActions(
                onNext = { focusTargets.password.focusRequester.requestFocus() },
                onDone = { focusManager.clearFocus() },
            ),
        label = { IntakeFieldLabel(R.string.label_wifi_ssid, required = true) },
        isError = uiState.ssidError != null,
        supportingText = if (uiState.ssidError != null) ({ Text(uiState.ssidError) }) else null,
        modifier =
            Modifier
                .focusRequester(focusTargets.ssid.focusRequester)
                .bringIntoViewRequester(focusTargets.ssid.bringIntoViewRequester),
    )
    WifiPasswordField(
        uiState = uiState,
        focusTargets = focusTargets,
        passwordVisible = passwordVisible,
        onPasswordChanged = onPasswordChanged,
        onPasswordVisibilityToggle = onPasswordVisibilityToggle,
        required = requiresPassword && !uiState.isOpenNetwork,
    )
}

@Composable
private fun WifiPasswordField(
    uiState: CustomerIntakeUiState,
    focusTargets: CustomerFormFocusTargets,
    passwordVisible: Boolean,
    onPasswordChanged: (String) -> Unit,
    onPasswordVisibilityToggle: () -> Unit,
    required: Boolean,
) {
    val focusManager = LocalFocusManager.current
    NeonOutlinedField(
        value = if (uiState.isOpenNetwork) "" else uiState.password,
        onValueChange = onPasswordChanged,
        keyboardOptions =
            KeyboardOptions(
                keyboardType = KeyboardType.Password,
                autoCorrectEnabled = false,
                imeAction = ImeAction.Done,
            ),
        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
        label = { IntakeFieldLabel(R.string.label_wifi_password, required = required) },
        enabled = !uiState.isOpenNetwork,
        isError = uiState.passwordError != null,
        supportingText =
            when {
                uiState.isOpenNetwork -> ({ Text(stringResource(R.string.hint_password_disabled)) })
                uiState.passwordError != null -> ({ Text(uiState.passwordError) })
                else -> null
            },
        visualTransformation =
            if (passwordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
        trailingIcon = {
            if (!uiState.isOpenNetwork) {
                val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                val description =
                    if (passwordVisible) {
                        stringResource(R.string.content_desc_hide_password)
                    } else {
                        stringResource(R.string.content_desc_show_password)
                    }
                IconButton(onClick = onPasswordVisibilityToggle) {
                    Icon(imageVector = image, contentDescription = description)
                }
            }
        },
        modifier =
            Modifier
                .focusRequester(focusTargets.password.focusRequester)
                .bringIntoViewRequester(focusTargets.password.bringIntoViewRequester),
    )
}

@Composable
private fun AdvancedWifiOptions(
    isHiddenNetwork: Boolean,
    onHiddenNetworkChanged: (Boolean) -> Unit,
    securityType: WifiQrGenerator.SecurityType,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    securityDropdownExpanded: Boolean,
    onSecurityDropdownExpandedChange: (Boolean) -> Unit,
    onSecurityTypeChanged: (WifiQrGenerator.SecurityType) -> Unit,
) {
    val expandedLabel = stringResource(R.string.state_expanded)
    val collapsedLabel = stringResource(R.string.state_collapsed)
    NeonButton(
        onClick = { onExpandedChange(!expanded) },
        modifier =
            Modifier
                .fillMaxWidth()
                .semantics {
                    stateDescription = if (expanded) expandedLabel else collapsedLabel
                },
        style = NeonButtonStyle.TERTIARY,
    ) {
        Text(
            text = stringResource(R.string.label_advanced_wifi_options),
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = null,
        )
    }

    if (expanded) {
        AdvancedWifiOptionsSection(
            isHiddenNetwork = isHiddenNetwork,
            onHiddenNetworkChanged = onHiddenNetworkChanged,
            securityType = securityType,
            securityDropdownExpanded = securityDropdownExpanded,
            onSecurityDropdownExpandedChange = onSecurityDropdownExpandedChange,
            onSecurityTypeChanged = onSecurityTypeChanged,
        )
    }
}

@Composable
private fun AdvancedWifiOptionsSection(
    isHiddenNetwork: Boolean,
    onHiddenNetworkChanged: (Boolean) -> Unit,
    securityType: WifiQrGenerator.SecurityType,
    securityDropdownExpanded: Boolean,
    onSecurityDropdownExpandedChange: (Boolean) -> Unit,
    onSecurityTypeChanged: (WifiQrGenerator.SecurityType) -> Unit,
) {
    SecurityTypeSelector(
        securityType = securityType,
        expanded = securityDropdownExpanded,
        onExpandedChange = onSecurityDropdownExpandedChange,
        onSecurityTypeChanged = onSecurityTypeChanged,
    )
    NetworkOptionToggle(
        value = isHiddenNetwork,
        onValueChange = onHiddenNetworkChanged,
        labelResource = R.string.label_hidden_network,
    )
}

@Composable
private fun NetworkOptionToggle(
    value: Boolean,
    onValueChange: (Boolean) -> Unit,
    @StringRes labelResource: Int,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .toggleable(
                    value = value,
                    onValueChange = onValueChange,
                    role = Role.Checkbox,
                ).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = value,
            onCheckedChange = null,
            colors =
                CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.secondary,
                    uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
        )
        Text(
            text = stringResource(labelResource),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Composable
private fun SecurityTypeSelector(
    securityType: WifiQrGenerator.SecurityType,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSecurityTypeChanged: (WifiQrGenerator.SecurityType) -> Unit,
) {
    NeonDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        selectedText = stringResource(securityType.displayLabelResource()),
        label = { Text(stringResource(R.string.label_wifi_security)) },
        modifier = Modifier.fillMaxWidth(),
    ) {
        listOf(
            WifiQrGenerator.SecurityType.OPEN,
            WifiQrGenerator.SecurityType.WPA2_PSK,
            WifiQrGenerator.SecurityType.WPA_WPA2_PERSONAL,
            WifiQrGenerator.SecurityType.WPA3_SAE,
            WifiQrGenerator.SecurityType.WPA2_WPA3_PERSONAL,
        ).forEach { option ->
            DropdownMenuItem(
                text = { Text(stringResource(option.displayLabelResource())) },
                onClick = {
                    onSecurityTypeChanged(option)
                    onExpandedChange(false)
                },
            )
        }
    }
}

@Composable
private fun IntakeFieldLabel(
    @StringRes labelResource: Int,
    required: Boolean,
    hint: String? = null,
) {
    val supportingLabel = if (required) stringResource(R.string.intake_required) else hint
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(stringResource(labelResource))
        if (supportingLabel != null) {
            Text(
                supportingLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun AccountNumberField(
    uiState: CustomerIntakeUiState,
    focusTargets: CustomerFormFocusTargets,
    onAccountNumberChanged: (String) -> Unit,
    required: Boolean,
) {
    NeonOutlinedField(
        value = uiState.accountNumber,
        onValueChange = onAccountNumberChanged,
        label = { IntakeFieldLabel(R.string.label_account_number, required = required) },
        isError = uiState.accountNumberError != null,
        supportingText = if (uiState.accountNumberError != null) ({ Text(uiState.accountNumberError) }) else null,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
        keyboardActions = KeyboardActions(onNext = { focusTargets.ssid.focusRequester.requestFocus() }),
        modifier =
            Modifier
                .focusRequester(focusTargets.accountNumber.focusRequester)
                .bringIntoViewRequester(focusTargets.accountNumber.bringIntoViewRequester),
    )
}
