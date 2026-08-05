package com.kingpaging.qwelcome.data

object MessageTemplate {
    private val PLACEHOLDER = Regex("""\{\{\s*(\w+)\s*}}""")

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
        val valueMap = mutableMapOf(
            "customer_name" to data.customerName,
            "ssid" to data.ssid,
            "password" to data.password,
            "account_number" to data.accountNumber
        )

        valueMap["tech_signature"] = techProfile?.let(::buildTechSignature).orEmpty()

        return template.replace(PLACEHOLDER) { match ->
            valueMap[match.groupValues[1]] ?: ""
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
