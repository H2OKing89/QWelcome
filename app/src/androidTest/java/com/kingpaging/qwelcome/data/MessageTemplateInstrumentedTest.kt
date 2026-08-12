package com.kingpaging.qwelcome.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MessageTemplateInstrumentedTest {
    @Test
    fun generate_replaces_whitespace_tolerant_placeholders_on_android() {
        val result =
            MessageTemplate.generate(
                template = "{{customer_name}} {{ ssid }} {{password}}",
                data =
                    CustomerData(
                        customerName = "Jane",
                        customerPhone = "555-123-4567",
                        ssid = "HomeNet",
                        password = "pass1234",
                        accountNumber = "A001",
                    ),
            )

        assertEquals("Jane HomeNet pass1234", result)
    }
}
