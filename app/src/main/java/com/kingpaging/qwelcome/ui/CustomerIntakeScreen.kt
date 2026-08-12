package com.kingpaging.qwelcome.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kingpaging.qwelcome.R
import com.kingpaging.qwelcome.ui.components.CyberpunkBackdrop
import com.kingpaging.qwelcome.ui.components.NeonTopAppBar
import com.kingpaging.qwelcome.ui.components.QWelcomeHeader
import com.kingpaging.qwelcome.ui.components.QrCodeBottomSheet
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

    fun firstInvalid(uiState: CustomerIntakeUiState): FormFieldFocusTarget? =
        when {
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
    val onShowQr: () -> Unit,
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
    onOpenTemplates: () -> Unit = {},
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
            onDismiss = actions.onDismissQr,
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
                            Icon(
                                Icons.Filled.PersonAdd,
                                contentDescription = stringResource(R.string.content_desc_new_customer),
                            )
                        }
                        IconButton(onClick = {
                            hapticFeedback()
                            onOpenSettings()
                        }) {
                            Icon(
                                Icons.Filled.Settings,
                                contentDescription = stringResource(R.string.content_desc_settings),
                            )
                        }
                    },
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground,
                )
            },
        ) { innerPadding ->
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .imePadding()
                        .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp), // Top-aligned with spacing feels more like a tool
            ) {
                CustomerIntakeTemplateSelector(
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
                    },
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
                    onAccountNumberChanged = actions.onAccountNumberChanged,
                )

                CustomerIntakeActionButtonRow(
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
                    },
                )

                CustomerIntakeQrCodeSection(
                    uiState = uiState,
                    enabled = uiState.qrEnabled,
                    onShowQrClick = {
                        hapticFeedback()
                        actions.onShowQr()
                    },
                )
            }
        }
    }
}
