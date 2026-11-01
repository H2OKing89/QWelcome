package com.kingpaging.qwelcome.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.kingpaging.qwelcome.R
import com.kingpaging.qwelcome.data.MessageTemplate
import com.kingpaging.qwelcome.ui.components.CenteredFormColumn
import com.kingpaging.qwelcome.ui.components.CyberpunkBackdrop
import com.kingpaging.qwelcome.ui.components.NeonTopAppBar
import com.kingpaging.qwelcome.ui.components.QWelcomeHeader
import com.kingpaging.qwelcome.ui.components.QrCodeBottomSheet
import com.kingpaging.qwelcome.util.WifiQrGenerator
import com.kingpaging.qwelcome.util.rememberHapticFeedback
import com.kingpaging.qwelcome.viewmodel.templates.TemplateSelectionUiState

private const val COMPACT_HEADER_FONT_SCALE = 1.5f

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
            uiState.accountNumberError != null -> accountNumber
            uiState.ssidError != null -> ssid
            uiState.passwordError != null -> password
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
    templateUiState: TemplateSelectionUiState,
    snackbarHostState: SnackbarHostState,
    formFocusTargets: CustomerFormFocusTargets,
    copySuccess: Boolean,
    actions: CustomerIntakeActions,
    onOpenSettings: () -> Unit,
    onOpenTemplates: () -> Unit = {},
) {
    val hapticFeedback = rememberHapticFeedback()
    val activeTemplate = templateUiState.templates.find { it.id == templateUiState.activeTemplateId }
    val templateContent = activeTemplate?.content.orEmpty()
    val compactHeader = LocalDensity.current.fontScale >= COMPACT_HEADER_FONT_SCALE
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
            modifier = Modifier.imePadding(),
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                NeonTopAppBar(
                    title = { QWelcomeHeader() },
                    actions = {
                        val newCustomerDescription = stringResource(R.string.content_desc_new_customer)
                        TextButton(
                            onClick = {
                                hapticFeedback()
                                actions.onClearForm()
                            },
                            modifier = Modifier.semantics { contentDescription = newCustomerDescription },
                        ) {
                            Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                stringResource(
                                    if (compactHeader) {
                                        R.string.intake_new_customer_compact
                                    } else {
                                        R.string.intake_new_customer
                                    },
                                ),
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
            bottomBar = {
                Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp) {
                    Box(
                        modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .widthIn(max = 720.dp)
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                        ) {
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
                        }
                    }
                }
            },
        ) { innerPadding ->
            CenteredFormColumn(padding = innerPadding) {
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
                    onPasswordChanged = actions.onPasswordChanged,
                    onPasswordVisibilityToggle = {
                        hapticFeedback()
                        passwordVisible = !passwordVisible
                    },
                    onAccountNumberChanged = actions.onAccountNumberChanged,
                    requiresAccount =
                        MessageTemplate.usesPlaceholder(templateContent, MessageTemplate.KEY_ACCOUNT_NUMBER),
                    requiresPassword = MessageTemplate.usesPlaceholder(templateContent, MessageTemplate.KEY_PASSWORD),
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
