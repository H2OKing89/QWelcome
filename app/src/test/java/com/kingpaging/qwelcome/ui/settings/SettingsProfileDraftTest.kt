package com.kingpaging.qwelcome.ui.settings

import com.kingpaging.qwelcome.data.TechProfile
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsProfileDraftTest {
    @Test
    fun `initial loading values cannot erase restored edits`() {
        val persisted = TechProfile("Technician", "Title", "Department")
        val draft = SettingsProfileDraft(persisted, persisted.copy(name = "Unsaved name"))

        draft.updatePersisted(TechProfile())
        draft.updatePersisted(persisted)

        assertEquals(persisted.copy(name = "Unsaved name"), draft.toProfile())
    }

    @Test
    fun `external changes update untouched fields and preserve edited fields`() {
        val persisted = TechProfile("Technician", "Title", "Department")
        val draft = SettingsProfileDraft(persisted)
        draft.title = "Unsaved title"

        draft.updatePersisted(TechProfile("Imported name", "Imported title", "Imported department"))

        assertEquals(TechProfile("Imported name", "Unsaved title", "Imported department"), draft.toProfile())
    }
}
