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
        val savedTags = mutableListOf<List<String>>()
        val state = TemplateEditorStateHarness()

        state.save(
            existingTags = listOf("Residential"),
            newTagInput = "residential",
            onTagsCaptured = { savedTags.add(it) }
        )

        assertEquals(listOf(listOf("Residential")), savedTags)
    }
}

/**
 * Minimal harness that exercises the same draft-tag normalization/dedup logic used by
 * `TemplateEditorState.save()`, since that class is file-private and not directly testable.
 */
private class TemplateEditorStateHarness {
    fun save(
        existingTags: List<String>,
        newTagInput: String,
        onTagsCaptured: (List<String>) -> Unit
    ) {
        val draftTag = normalizeTemplateTag(newTagInput)
        val tags = if (draftTag.isBlank() || existingTags.containsTagIgnoreCase(draftTag)) {
            existingTags
        } else {
            existingTags + draftTag
        }
        onTagsCaptured(tags)
    }
}
