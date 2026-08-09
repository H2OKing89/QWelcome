package com.kingpaging.qwelcome.ui.templates

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TemplateEditorStateTest {

    @Test
    fun `normalizeTemplateTag trims whitespace and caps length at 32 characters`() {
        assertEquals("residential", normalizeTemplateTag("  residential  "))
        assertEquals("a".repeat(32), normalizeTemplateTag("a".repeat(40)))
        assertEquals("", normalizeTemplateTag("   "))
    }

    @Test
    fun `containsTagIgnoreCase matches regardless of case`() {
        val tags = listOf("Residential", "Business")

        assertTrue(tags.containsTagIgnoreCase("residential"))
        assertTrue(tags.containsTagIgnoreCase("BUSINESS"))
        assertFalse(tags.containsTagIgnoreCase("install"))
    }

    @Test
    fun `save does not append a draft tag that differs only by case from an existing tag`() {
        val result = mergeDraftTag(existingTags = listOf("Residential"), draftTagInput = "residential")

        assertEquals(listOf("Residential"), result)
    }

    @Test
    fun `mergeDraftTag ignores a blank draft tag`() {
        val result = mergeDraftTag(existingTags = listOf("Residential"), draftTagInput = "   ")

        assertEquals(listOf("Residential"), result)
    }

    @Test
    fun `mergeDraftTag appends a normalized draft tag`() {
        val result = mergeDraftTag(existingTags = listOf("Residential"), draftTagInput = "  Business  ")

        assertEquals(listOf("Residential", "Business"), result)
    }

    @Test
    fun `mergeDraftTag skips an exact duplicate draft tag`() {
        val result = mergeDraftTag(existingTags = listOf("Residential"), draftTagInput = "Residential")

        assertEquals(listOf("Residential"), result)
    }

    @Test
    fun `mergeDraftTag skips a case-only duplicate draft tag`() {
        val result = mergeDraftTag(existingTags = listOf("Residential"), draftTagInput = "RESIDENTIAL")

        assertEquals(listOf("Residential"), result)
    }
}
