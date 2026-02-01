package com.kingpaging.qwelcome.data

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for [MessageTemplate] - Template placeholder replacement.
 *
 * Tests cover:
 * - Basic placeholder replacement for all supported placeholders
 * - Tech signature generation with various field combinations
 * - Edge cases (empty values, missing profile)
 */
class MessageTemplateTest {

    // ========== Basic Placeholder Tests ==========

    @Test
    fun `generate replaces customer_name placeholder`() {
        val template = "Hello {{ customer_name }}!"
        val data = createCustomerData(customerName = "John Doe")
        
        val result = MessageTemplate.generate(template, data)
        
        assertEquals("Hello John Doe!", result)
    }

    @Test
    fun `generate replaces ssid placeholder`() {
        val template = "Network: {{ ssid }}"
        val data = createCustomerData(ssid = "MyWiFi")
        
        val result = MessageTemplate.generate(template, data)
        
        assertEquals("Network: MyWiFi", result)
    }

    @Test
    fun `generate replaces password placeholder`() {
        val template = "Password: {{ password }}"
        val data = createCustomerData(password = "secret123")
        
        val result = MessageTemplate.generate(template, data)
        
        assertEquals("Password: secret123", result)
    }

    @Test
    fun `generate replaces account_number placeholder`() {
        val template = "Account: {{ account_number }}"
        val data = createCustomerData(accountNumber = "ACC-12345")
        
        val result = MessageTemplate.generate(template, data)
        
        assertEquals("Account: ACC-12345", result)
    }

    @Test
    fun `generate replaces multiple placeholders`() {
        val template = "Hi {{ customer_name }}, your WiFi is {{ ssid }} with password {{ password }}. Account: {{ account_number }}"
        val data = createCustomerData(
            customerName = "Jane",
            ssid = "HomeNet",
            password = "pass1234",
            accountNumber = "A001"
        )
        
        val result = MessageTemplate.generate(template, data)
        
        assertEquals("Hi Jane, your WiFi is HomeNet with password pass1234. Account: A001", result)
    }

    @Test
    fun `generate handles duplicate placeholders`() {
        val template = "{{ customer_name }}, welcome {{ customer_name }}!"
        val data = createCustomerData(customerName = "Bob")
        
        val result = MessageTemplate.generate(template, data)
        
        assertEquals("Bob, welcome Bob!", result)
    }

    // ========== Tech Signature Tests ==========

    @Test
    fun `generate replaces tech_signature with full profile`() {
        val template = "Message\n\n{{ tech_signature }}"
        val data = createCustomerData()
        val profile = TechProfile(name = "John Tech", title = "Installer", dept = "Field Services")
        
        val result = MessageTemplate.generate(template, data, profile)
        
        assertEquals("Message\n\nJohn Tech\nInstaller\nField Services", result)
    }

    @Test
    fun `generate handles tech profile with name only`() {
        val template = "Signed: {{ tech_signature }}"
        val data = createCustomerData()
        val profile = TechProfile(name = "John", title = "", dept = "")
        
        val result = MessageTemplate.generate(template, data, profile)
        
        assertEquals("Signed: John", result)
    }

    @Test
    fun `generate handles tech profile with title only`() {
        val template = "From: {{ tech_signature }}"
        val data = createCustomerData()
        val profile = TechProfile(name = "", title = "Senior Installer", dept = "")
        
        val result = MessageTemplate.generate(template, data, profile)
        
        assertEquals("From: Senior Installer", result)
    }

    @Test
    fun `generate handles tech profile with dept only`() {
        val template = "From: {{ tech_signature }}"
        val data = createCustomerData()
        val profile = TechProfile(name = "", title = "", dept = "IT Department")
        
        val result = MessageTemplate.generate(template, data, profile)
        
        assertEquals("From: IT Department", result)
    }

    @Test
    fun `generate handles tech profile with name and title`() {
        val template = "{{ tech_signature }}"
        val data = createCustomerData()
        val profile = TechProfile(name = "Jane Doe", title = "Lead Tech", dept = "")
        
        val result = MessageTemplate.generate(template, data, profile)
        
        assertEquals("Jane Doe\nLead Tech", result)
    }

    @Test
    fun `generate handles tech profile with title and dept`() {
        val template = "{{ tech_signature }}"
        val data = createCustomerData()
        val profile = TechProfile(name = "", title = "Manager", dept = "Operations")

        val result = MessageTemplate.generate(template, data, profile)

        assertEquals("Manager\nOperations", result)
    }

    @Test
    fun `generate handles tech profile with name and dept`() {
        val template = "{{ tech_signature }}"
        val data = createCustomerData()
        val profile = TechProfile(name = "Jane Doe", title = "", dept = "IT Department")

        val result = MessageTemplate.generate(template, data, profile)

        assertEquals("Jane Doe\nIT Department", result)
    }

    @Test
    fun `generate removes tech_signature placeholder when no profile provided`() {
        val template = "Message\n\n{{ tech_signature }}"
        val data = createCustomerData()
        
        val result = MessageTemplate.generate(template, data, techProfile = null)
        
        assertEquals("Message\n\n", result)
    }

    @Test
    fun `generate handles empty tech profile`() {
        val template = "Text{{ tech_signature }}End"
        val data = createCustomerData()
        val profile = TechProfile(name = "", title = "", dept = "")
        
        val result = MessageTemplate.generate(template, data, profile)
        
        assertEquals("TextEnd", result)
    }

    @Test
    fun `generate handles whitespace-only tech profile fields`() {
        val template = "{{ tech_signature }}"
        val data = createCustomerData()
        val profile = TechProfile(name = "   ", title = "   ", dept = "   ")
        
        val result = MessageTemplate.generate(template, data, profile)
        
        assertEquals("", result)
    }

    // ========== Edge Cases ==========

    @Test
    fun `generate handles empty customer values`() {
        val template = "Name: {{ customer_name }}, SSID: {{ ssid }}"
        val data = createCustomerData(customerName = "", ssid = "")
        
        val result = MessageTemplate.generate(template, data)
        
        assertEquals("Name: , SSID: ", result)
    }

    @Test
    fun `generate handles template without placeholders`() {
        val template = "This is plain text with no placeholders"
        val data = createCustomerData()
        
        val result = MessageTemplate.generate(template, data)
        
        assertEquals("This is plain text with no placeholders", result)
    }

    @Test
    fun `generate handles empty template`() {
        val template = ""
        val data = createCustomerData()

        val result = MessageTemplate.generate(template, data)

        assertEquals("", result)
    }

    @Test
    fun `generate leaves unrecognized placeholders unchanged`() {
        val template = "Hello {{ unknown_field }}!"
        val data = createCustomerData()

        val result = MessageTemplate.generate(template, data)

        assertEquals("Hello {{ unknown_field }}!", result)
    }

    @Test
    fun `generate handles special characters in values`() {
        val template = "{{ customer_name }} - {{ ssid }}"
        val data = createCustomerData(
            customerName = "O'Brien & Co.",
            ssid = "WiFi<>Network"
        )
        
        val result = MessageTemplate.generate(template, data)
        
        assertEquals("O'Brien & Co. - WiFi<>Network", result)
    }

    @Test
    fun `generate handles unicode in values`() {
        val template = "{{ customer_name }}: {{ ssid }}"
        val data = createCustomerData(
            customerName = "José García",
            ssid = "网络🏠"
        )
        
        val result = MessageTemplate.generate(template, data)
        
        assertEquals("José García: 网络🏠", result)
    }

    @Test
    fun `generate handles newlines in template`() {
        val template = "Line 1\n{{ customer_name }}\nLine 3"
        val data = createCustomerData(customerName = "User")
        
        val result = MessageTemplate.generate(template, data)
        
        assertEquals("Line 1\nUser\nLine 3", result)
    }

    // ========== Constants Tests ==========

    @Test
    fun `PLACEHOLDERS contains all expected keys`() {
        val keys = MessageTemplate.PLACEHOLDERS.map { it.first }

        val expectedKeys = setOf(
            MessageTemplate.KEY_CUSTOMER_NAME,
            MessageTemplate.KEY_SSID,
            MessageTemplate.KEY_PASSWORD,
            MessageTemplate.KEY_ACCOUNT_NUMBER,
            MessageTemplate.KEY_TECH_SIGNATURE
        )

        assertEquals(expectedKeys, keys.toSet())
    }

    @Test
    fun `placeholder keys have expected format`() {
        assertEquals("{{ customer_name }}", MessageTemplate.KEY_CUSTOMER_NAME)
        assertEquals("{{ ssid }}", MessageTemplate.KEY_SSID)
        assertEquals("{{ password }}", MessageTemplate.KEY_PASSWORD)
        assertEquals("{{ account_number }}", MessageTemplate.KEY_ACCOUNT_NUMBER)
        assertEquals("{{ tech_signature }}", MessageTemplate.KEY_TECH_SIGNATURE)
    }

    // ========== Helper Functions ==========

    private fun createCustomerData(
        customerName: String = "Test Customer",
        customerPhone: String = "555-123-4567",
        ssid: String = "TestNetwork",
        password: String = "TestPass123",
        accountNumber: String = "TEST001"
    ) = CustomerData(
        customerName = customerName,
        customerPhone = customerPhone,
        ssid = ssid,
        password = password,
        accountNumber = accountNumber
    )
}
