package com.kingpaging.qwelcome.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for Template placeholder validation.
 * Tests regex patterns for required placeholders with various edge cases.
 */
class TemplateTest {
    
    @Test
    fun `findMissingPlaceholders returns empty for valid content`() {
        val content = "Hello {{ customer_name }}, your WiFi SSID is {{ ssid }}."
        val missing = Template.findMissingPlaceholders(content)
        assertTrue("Expected no missing placeholders", missing.isEmpty())
    }
    
    @Test
    fun `findMissingPlaceholders returns both when neither present`() {
        val content = "Hello there! Your WiFi is ready."
        val missing = Template.findMissingPlaceholders(content)
        assertEquals(2, missing.size)
        assertTrue("Should report customer_name missing", "{{ customer_name }}" in missing)
        assertTrue("Should report ssid missing", "{{ ssid }}" in missing)
    }
    
    @Test
    fun `findMissingPlaceholders returns customer_name when only ssid present`() {
        val content = "Your WiFi SSID is {{ ssid }}."
        val missing = Template.findMissingPlaceholders(content)
        assertEquals(1, missing.size)
        assertTrue("Should report customer_name missing", "{{ customer_name }}" in missing)
    }
    
    @Test
    fun `findMissingPlaceholders returns ssid when only customer_name present`() {
        val content = "Hello {{ customer_name }}!"
        val missing = Template.findMissingPlaceholders(content)
        assertEquals(1, missing.size)
        assertTrue("Should report ssid missing", "{{ ssid }}" in missing)
    }
    
    // ===== Whitespace tolerance tests =====
    
    @Test
    fun `accepts placeholder without spaces`() {
        val content = "Hi {{customer_name}}, SSID: {{ssid}}"
        val missing = Template.findMissingPlaceholders(content)
        assertTrue("Should accept placeholders without spaces", missing.isEmpty())
    }
    
    @Test
    fun `accepts placeholder with extra spaces`() {
        val content = "Hi {{  customer_name  }}, SSID: {{  ssid  }}"
        val missing = Template.findMissingPlaceholders(content)
        assertTrue("Should accept placeholders with extra spaces", missing.isEmpty())
    }
    
    @Test
    fun `accepts placeholder with mixed whitespace`() {
        val content = "Hi {{ customer_name}}, SSID: {{ssid }}"
        val missing = Template.findMissingPlaceholders(content)
        assertTrue("Should accept placeholders with mixed whitespace", missing.isEmpty())
    }
    
    @Test
    fun `accepts placeholder with tabs in whitespace`() {
        val content = "Hi {{\tcustomer_name\t}}, SSID: {{\tssid\t}}"
        val missing = Template.findMissingPlaceholders(content)
        assertTrue("Should accept placeholders with tabs", missing.isEmpty())
    }
    
    // ===== False positive prevention tests =====
    
    @Test
    fun `does not match customer_name_suffix as customer_name`() {
        val content = "Hi {{ customer_name_suffix }}!"
        val missing = Template.findMissingPlaceholders(content)
        assertTrue("Should NOT match customer_name_suffix", "{{ customer_name }}" in missing)
    }
    
    @Test
    fun `does not match customer_name_extra as customer_name`() {
        val content = "Hi {{ customer_name_extra }}, SSID: {{ ssid }}"
        val missing = Template.findMissingPlaceholders(content)
        assertEquals(1, missing.size)
        assertTrue("Should NOT match customer_name_extra", "{{ customer_name }}" in missing)
    }
    
    @Test
    fun `does not match ssid_password as ssid`() {
        val content = "Hi {{ customer_name }}, Password: {{ ssid_password }}"
        val missing = Template.findMissingPlaceholders(content)
        assertEquals(1, missing.size)
        assertTrue("Should NOT match ssid_password", "{{ ssid }}" in missing)
    }
    
    @Test
    fun `does not match prefix_customer_name as customer_name`() {
        val content = "Hi {{ my_customer_name }}, SSID: {{ ssid }}"
        val missing = Template.findMissingPlaceholders(content)
        assertEquals(1, missing.size)
        assertTrue("Should NOT match my_customer_name", "{{ customer_name }}" in missing)
    }
    
    @Test
    fun `does not match prefix_ssid as ssid`() {
        val content = "Hi {{ customer_name }}, SSID: {{ my_ssid }}"
        val missing = Template.findMissingPlaceholders(content)
        assertEquals(1, missing.size)
        assertTrue("Should NOT match my_ssid", "{{ ssid }}" in missing)
    }
    
    // ===== hasRequiredPlaceholders tests =====
    
    @Test
    fun `hasRequiredPlaceholders returns true for valid content`() {
        val content = "Hello {{ customer_name }}, WiFi: {{ ssid }}"
        assertTrue(Template.hasRequiredPlaceholders(content))
    }
    
    @Test
    fun `hasRequiredPlaceholders returns false for missing placeholders`() {
        val content = "Hello there!"
        assertFalse(Template.hasRequiredPlaceholders(content))
    }
    
    @Test
    fun `hasRequiredPlaceholders tolerates whitespace variations`() {
        val content = "Hi {{customer_name}}, {{  ssid  }}"
        assertTrue(Template.hasRequiredPlaceholders(content))
    }
    
    @Test
    fun `hasRequiredPlaceholders rejects false positives`() {
        val content = "Hi {{ customer_name_extra }}, {{ ssid_extra }}"
        assertFalse(Template.hasRequiredPlaceholders(content))
    }
    
    // ===== normalizeContent tests =====
    
    @Test
    fun `normalizeContent converts no-space placeholders to canonical form`() {
        val content = "Hi {{customer_name}}, {{ssid}}"
        val normalized = Template.normalizeContent(content)
        assertEquals("Hi {{ customer_name }}, {{ ssid }}", normalized)
    }
    
    @Test
    fun `normalizeContent converts extra-space placeholders to canonical form`() {
        val content = "Hi {{  customer_name  }}, {{   ssid   }}"
        val normalized = Template.normalizeContent(content)
        assertEquals("Hi {{ customer_name }}, {{ ssid }}", normalized)
    }
    
    @Test
    fun `normalizeContent preserves already-canonical placeholders`() {
        val content = "Hi {{ customer_name }}, {{ ssid }}"
        val normalized = Template.normalizeContent(content)
        assertEquals(content, normalized)
    }
    
    @Test
    fun `normalizeContent handles mixed whitespace styles`() {
        val content = "{{customer_name}} {{ ssid }} {{  password  }}"
        val normalized = Template.normalizeContent(content)
        assertEquals("{{ customer_name }} {{ ssid }} {{ password }}", normalized)
    }
    
    @Test
    fun `normalizeContent preserves non-placeholder text`() {
        val content = "Hello {{customer_name}}! Your network {{ ssid }} is ready."
        val normalized = Template.normalizeContent(content)
        assertEquals("Hello {{ customer_name }}! Your network {{ ssid }} is ready.", normalized)
    }
}
