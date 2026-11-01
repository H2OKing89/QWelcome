package com.kingpaging.qwelcome.viewmodel

import com.kingpaging.qwelcome.R
import com.kingpaging.qwelcome.data.MessageTemplate
import com.kingpaging.qwelcome.data.Template
import com.kingpaging.qwelcome.ui.CustomerIntakeUiState
import com.kingpaging.qwelcome.util.ResourceProvider
import com.kingpaging.qwelcome.util.WifiQrGenerator

internal data class CustomerMessageValidation(
    val form: CustomerIntakeUiState,
    val canSubmit: Boolean,
)

/** Message requirements and live credential errors are evaluated separately. */
internal class CustomerMessageValidator(
    private val resources: ResourceProvider,
) {
    fun validate(
        form: CustomerIntakeUiState,
        requirePhone: Boolean,
        template: Template,
    ): CustomerMessageValidation {
        val requiresPassword = MessageTemplate.usesPlaceholder(template.content, MessageTemplate.KEY_PASSWORD)
        val requiresAccount = MessageTemplate.usesPlaceholder(template.content, MessageTemplate.KEY_ACCOUNT_NUMBER)
        val validated =
            form.copy(
                customerNameError = requiredError(form.customerName, R.string.error_name_empty),
                customerPhoneError = phoneError(form.customerPhone, requirePhone),
                ssidError = wifiError(WifiQrGenerator.validateSsid(form.ssid)),
                passwordError = passwordError(form, requiresPassword),
                accountNumberError =
                    if (requiresAccount) requiredError(form.accountNumber, R.string.error_account_empty) else null,
            )
        val requiredErrors =
            listOf(
                validated.customerNameError,
                validated.customerPhoneError,
                validated.ssidError,
                validated.accountNumberError,
                validated.passwordError.takeIf { requiresPassword },
            )
        return CustomerMessageValidation(validated, requiredErrors.all { it == null })
    }

    private fun requiredError(
        value: String,
        message: Int,
    ): String? = if (value.isBlank()) resources.getString(message) else null

    private fun phoneError(
        phone: String,
        required: Boolean,
    ): String? =
        when {
            !required -> null
            phone.isBlank() -> resources.getString(R.string.error_phone_empty)
            else -> CustomerIntakeViewModel.validatePhoneNumber(phone, progressiveMode = false, resources)
        }

    private fun passwordError(
        form: CustomerIntakeUiState,
        required: Boolean,
    ): String? =
        if (form.isOpenNetwork || (!required && form.password.isEmpty())) {
            null
        } else {
            wifiError(WifiQrGenerator.validatePassword(form.password))
        }

    private fun wifiError(result: WifiQrGenerator.ValidationResult): String? =
        when (result) {
            WifiQrGenerator.ValidationResult.Success -> null
            is WifiQrGenerator.ValidationResult.Error -> resources.getString(result.messageResId)
        }
}
