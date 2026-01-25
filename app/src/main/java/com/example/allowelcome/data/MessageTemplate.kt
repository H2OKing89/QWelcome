package com.example.allowelcome.data

object MessageTemplate {

    /** Placeholder keys - single source of truth */
    const val KEY_CUSTOMER_NAME = "{{ customer_name }}"
    const val KEY_SSID = "{{ ssid }}"
    const val KEY_PASSWORD = "{{ password }}"
    const val KEY_ACCOUNT_NUMBER = "{{ account_number }}"

    /** Available placeholders for templates (key to description) */
    val PLACEHOLDERS = listOf(
        KEY_CUSTOMER_NAME to "Customer's name",
        KEY_SSID to "WiFi network name",
        KEY_PASSWORD to "WiFi password",
        KEY_ACCOUNT_NUMBER to "Account number"
    )

    /** Generate message by applying placeholders to the template */
    fun generate(template: String, data: CustomerData): String {
        return applyPlaceholders(template, data)
    }

    private fun applyPlaceholders(template: String, data: CustomerData): String {
        // Map placeholder keys to their values from CustomerData
        val valueMap = mapOf(
            KEY_CUSTOMER_NAME to data.customerName,
            KEY_SSID to data.ssid,
            KEY_PASSWORD to data.password,
            KEY_ACCOUNT_NUMBER to data.accountNumber
        )

        // Apply all replacements using the single source of truth
        return PLACEHOLDERS.fold(template) { result, (key, _) ->
            result.replace(key, valueMap[key] ?: "")
        }
    }
}
