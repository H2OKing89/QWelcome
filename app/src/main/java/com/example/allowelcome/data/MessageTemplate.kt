package com.example.allowelcome.data

import android.content.Context
import com.example.allowelcome.R

object MessageTemplate {
    fun generate(context: Context, data: CustomerData): String {
        val template = context.getString(R.string.welcome_template)
        return template
            .replace("{{ customer_name }}", data.customerName)
            .replace("{{ ssid }}", data.ssid)
            .replace("{{ password }}", data.password)
            .replace("{{ account_number }}", data.accountNumber)
    }
}
