package com.example.allowelcome.data

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
     * Format: "Name\nTitle, Department" (non-empty fields only)
     */
    private fun buildTechSignature(profile: TechProfile): String {
        val parts = mutableListOf<String>()

        // Name on its own line if present
        if (profile.name.isNotBlank()) {
            parts.add(profile.name)
        }

        // Title and dept on second line, comma-separated
        val titleDept = listOfNotNull(
            profile.title.takeIf { it.isNotBlank() },
            profile.dept.takeIf { it.isNotBlank() }
        ).joinToString(", ")

        if (titleDept.isNotBlank()) {
            parts.add(titleDept)
        }

        return parts.joinToString("\n")
    }
}

