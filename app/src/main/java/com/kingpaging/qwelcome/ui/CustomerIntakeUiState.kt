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
    /** When true, password validation is skipped (for open/guest networks) */
    val isOpenNetwork: Boolean = false,
    /** Whether the QR code bottom sheet is currently shown */
    val showQrSheet: Boolean = false,
) {

    /** True when the WiFi fields are valid enough to generate a QR code. */
    val qrEnabled: Boolean
        get() = if (isOpenNetwork) {
            ssid.isNotBlank() && ssidError == null
        } else {
            ssid.isNotBlank() &&
                ssidError == null &&
                password.isNotBlank() &&
                password.length >= WifiQrGenerator.MIN_PASSWORD_LENGTH &&
                passwordError == null
        }

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
        get() = customerNameError == null &&
                customerPhoneError == null &&
                ssidError == null &&
                (isOpenNetwork || passwordError == null) &&
                accountNumberError == null
    /**
     * Converts the UI state to a [CustomerData] instance.
     * 
     * This mapper function bridges the UI layer (CustomerIntakeUiState) and
     * the data layer (CustomerData), applying transformations like title case
     * formatting to the customer name.
     * 
     * Note: This should only be called after validation has passed, as it does
     * not perform any validation itself.
     * 
     * @return A CustomerData instance with the current form values
     */
    fun toCustomerData(): CustomerData = CustomerData(
        customerName = StringUtils.toTitleCase(customerName),
        customerPhone = customerPhone,
        ssid = ssid,
        password = password,
        accountNumber = accountNumber
    )
}