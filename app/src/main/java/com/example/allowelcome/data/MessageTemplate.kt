package com.example.allowelcome.data

import android.content.Context
import com.example.allowelcome.R

object MessageTemplate {
    
    /** Available placeholders for templates */
    val PLACEHOLDERS = listOf(
        "{{ customer_name }}" to "Customer's name",
        "{{ ssid }}" to "WiFi network name",
        "{{ password }}" to "WiFi password",
        "{{ account_number }}" to "Account number"
    )

    /** Generate message using the default template from strings.xml */
    fun generate(context: Context, data: CustomerData): String {
        val template = context.getString(R.string.welcome_template)
        return applyPlaceholders(template, data)
    }

    /** Generate message using a custom template string */
    fun generate(template: String, data: CustomerData): String {
        return applyPlaceholders(template, data)
    }

    private fun applyPlaceholders(template: String, data: CustomerData): String {
        return template
            .replace("{{ customer_name }}", data.customerName)
            .replace("{{ ssid }}", data.ssid)
            .replace("{{ password }}", data.password)
            .replace("{{ account_number }}", data.accountNumber)
    }
}
