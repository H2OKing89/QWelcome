package com.kingpaging.qwelcome.ui.settings

import androidx.compose.runtime.saveable.SaverScope
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

    @Test
    fun `clearing a field before stored profile loads preserves the edit`() {
        val draft = SettingsProfileDraft(TechProfile())
        draft.name = "Temporary name"
        draft.name = ""

        draft.updatePersisted(TechProfile("Stored name", "Stored title", "Stored department"))

        assertEquals(TechProfile("", "Stored title", "Stored department"), draft.toProfile())
    }

    @Test
    fun `saved draft preserves cleared fields after baseline changes`() {
        val persisted = TechProfile("Technician", "Title", "Department")
        val draft = SettingsProfileDraft(persisted)
        draft.title = ""
        draft.department = ""
        draft.updatePersisted(TechProfile())
        val saverScope =
            object : SaverScope {
                override fun canBeSaved(value: Any): Boolean = true
            }
        val saved = with(SettingsProfileDraft.Saver) { saverScope.save(draft) }
        val restored = requireNotNull(SettingsProfileDraft.Saver.restore(requireNotNull(saved)))

        restored.updatePersisted(persisted)

        assertEquals(TechProfile("Technician", "", ""), restored.toProfile())
    }
}
