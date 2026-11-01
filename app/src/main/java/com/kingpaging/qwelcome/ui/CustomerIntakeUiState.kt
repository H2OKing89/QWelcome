package com.kingpaging.qwelcome.ui

import com.kingpaging.qwelcome.data.CustomerData
import com.kingpaging.qwelcome.util.StringUtils
import com.kingpaging.qwelcome.util.WifiQrGenerator

/**
 * UI state for the Customer Intake form.
 *
 * This class represents the current state of the form including user input
 * and validation error messages. It is distinct from [CustomerData] which
 * represents validated, ready-to-use customer information.
 *
 * Use [toCustomerData] to convert validated UI state to a CustomerData instance.
 * Use [isValid] to check if the form is ready for submission.
 */
data class CustomerIntakeUiState(
    val customerName: String = "",
    val customerNameError: String? = null,
    val customerPhone: String = "",
    val customerPhoneError: String? = null,
    val ssid: String = "",
    val ssidError: String? = null,
    val password: String = "",
    val passwordError: String? = null,
    val accountNumber: String = "",
    val accountNumberError: String? = null,
    val securityType: WifiQrGenerator.SecurityType = WifiQrGenerator.DEFAULT_SECURITY_TYPE,
    val isHiddenNetwork: Boolean = false,
    /** Whether the QR code bottom sheet is currently shown */
    val showQrSheet: Boolean = false,
) {
    /** Security selection is the single source of truth for password requirements. */
    val isOpenNetwork: Boolean
        get() = securityType == WifiQrGenerator.SecurityType.OPEN

    /** True when the WiFi fields are valid enough to generate a QR code. */
    val qrEnabled: Boolean
        get() =
            WifiQrGenerator.validateSsid(ssid) is WifiQrGenerator.ValidationResult.Success &&
                (
                    isOpenNetwork ||
                        WifiQrGenerator.validatePassword(password) is WifiQrGenerator.ValidationResult.Success
                )

    /**
     * Returns true if all form fields pass validation (no errors present).
     *
     * Note: This only checks if error fields are null - it does NOT perform
     * validation itself. Call validation methods first to populate error fields,
     * then check this property to determine if submission should proceed.
     *
     * For open networks, password errors are ignored since passwords aren't required.
     */
    val isValid: Boolean
        get() =
            customerNameError == null &&
                customerPhoneError == null &&
                ssidError == null &&
                (isOpenNetwork || passwordError == null) &&
                accountNumberError == null

    val hasCustomerData: Boolean
        get() =
            customerName.isNotBlank() ||
                customerPhone.isNotBlank() ||
                ssid.isNotBlank() ||
                password.isNotBlank() ||
                accountNumber.isNotBlank()

    /**
     * Converts the UI state to a [CustomerData] instance.
     *
     * This mapper function bridges the UI layer (CustomerIntakeUiState) and
     * the data layer (CustomerData), converting all-caps customer names to title
     * case while preserving explicitly entered mixed-case spelling.
     *
     * Note: This should only be called after validation has passed, as it does
     * not perform any validation itself.
     *
     * @return A CustomerData instance with the current form values
     */
    fun toCustomerData(): CustomerData =
        CustomerData(
            customerName =
                if (customerName.any(Char::isUpperCase) && customerName.none(Char::isLowerCase)) {
                    StringUtils.toTitleCase(customerName)
                } else {
                    customerName
                },
            customerPhone = customerPhone,
            ssid = ssid,
            password = password,
            accountNumber = accountNumber,
            isOpenNetwork = isOpenNetwork,
        )
}
