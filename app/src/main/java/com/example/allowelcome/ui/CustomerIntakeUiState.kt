package com.example.allowelcome.ui

import com.example.allowelcome.data.CustomerData
import com.example.allowelcome.util.StringUtils

/**
 * UI state for the Customer Intake form.
 * 
 * This class represents the current state of the form including user input
 * and validation error messages. It is distinct from [CustomerData] which
 * represents validated, ready-to-use customer information.
 * 
 * Use [toCustomerData] to convert validated UI state to a CustomerData instance.
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
) {
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