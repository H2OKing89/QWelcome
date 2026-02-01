package com.kingpaging.qwelcome.data

object MessageTemplate {

    /** Placeholder keys - single source of truth */
    const val KEY_CUSTOMER_NAME = "{{ customer_name }}"
    const val KEY_SSID = "{{ ssid }}"
    const val KEY_PASSWORD = "{{ password }}"
    const val KEY_ACCOUNT_NUMBER = "{{ account_number }}"
    const val KEY_TECH_SIGNATURE = "{{ tech_signature }}"

    /** Available placeholders for templates (key to description) */
    val PLACEHOLDERS = listOf(
        KEY_CUSTOMER_NAME to "Customer's name",
        KEY_SSID to "WiFi network name",
        KEY_PASSWORD to "WiFi password",
        KEY_ACCOUNT_NUMBER to "Account number",
        KEY_TECH_SIGNATURE to "Tech signature (name, title, dept)"
    )

    /**
     * Generate message by applying placeholders to the template.
     *
     * @param template The template string with placeholders
     * @param data Customer data to fill in
     * @param techProfile Tech profile for signature placeholder (optional)
     */
    fun generate(
        template: String,
        data: CustomerData,
        techProfile: TechProfile? = null
    ): String {
        return applyPlaceholders(template, data, techProfile)
    }

    private fun applyPlaceholders(
        template: String,
        data: CustomerData,
        techProfile: TechProfile?
    ): String {
        // Map placeholder keys to their values from CustomerData
        val valueMap = mutableMapOf(
            KEY_CUSTOMER_NAME to data.customerName,
            KEY_SSID to data.ssid,
            KEY_PASSWORD to data.password,
            KEY_ACCOUNT_NUMBER to data.accountNumber
        )

        // Build tech signature if profile is provided
        if (techProfile != null) {
            valueMap[KEY_TECH_SIGNATURE] = buildTechSignature(techProfile)
        } else {
            // If no profile, remove the placeholder entirely
            valueMap[KEY_TECH_SIGNATURE] = ""
        }

        // Apply all replacements using the single source of truth
        return PLACEHOLDERS.fold(template) { result, (key, _) ->
            result.replace(key, valueMap[key] ?: "")
        }
    }

    /**
     * Builds a formatted tech signature from profile info.
     * Format: "Name\nTitle\nDepartment" - each non-empty field on its own line.
     */
    private fun buildTechSignature(profile: TechProfile): String {
        return listOf(profile.name, profile.title, profile.dept)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .joinToString("\n")
    }
}

