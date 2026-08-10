package com.kingpaging.qwelcome.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kingpaging.qwelcome.viewmodel.factory.AppViewModelProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsStoreSelectionTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var store: SettingsStore
    private val firstTemplate = validTemplate("first")
    private val secondTemplate = validTemplate("second")

    @Before
    fun setup() = runBlocking {
        AppViewModelProvider.resetForTesting()
        context.protoDataStore.updateData { UserPreferences.getDefaultInstance() }
        store = SettingsStore(context, currentTimeMillis = { 100L })
        store.saveTemplates(listOf(firstTemplate, secondTemplate))
    }

    @After
    fun tearDown() = runBlocking {
        context.protoDataStore.updateData { UserPreferences.getDefaultInstance() }
        AppViewModelProvider.resetForTesting()
    }

    @Test
    fun defaultPreferences_haveEmptyUsageHistory() = runBlocking {
        assertTrue(store.templateLastUsedFlow.first().isEmpty())
    }

    @Test
    fun selection_atomicallyUpdatesActiveIdAndUsageTimestamp() = runBlocking {
        val result = store.setActiveTemplate(firstTemplate.id)

        assertTrue(result is TemplateSelectionResult.Selected)
        assertEquals(firstTemplate.id, store.activeTemplateIdFlow.first())
        assertEquals(100L, store.templateLastUsedFlow.first()[firstTemplate.id])
    }

    @Test
    fun reselectingActiveTemplate_preservesUsageTimestamp() = runBlocking {
        store.setActiveTemplate(firstTemplate.id)
        val usageTimestamp = store.templateLastUsedFlow.first()[firstTemplate.id]

        val result = store.setActiveTemplate(firstTemplate.id)

        assertTrue(result is TemplateSelectionResult.AlreadyActive)
        assertEquals(usageTimestamp, store.templateLastUsedFlow.first()[firstTemplate.id])
    }

    @Test
    fun unknownSelection_doesNotMutateDefaultStateOrUsageHistory() = runBlocking {
        val result = store.setActiveTemplate("missing-template")

        assertTrue(result is TemplateSelectionResult.NotFound)
        assertEquals(DEFAULT_TEMPLATE_ID, store.activeTemplateIdFlow.first())
        assertTrue(store.templateLastUsedFlow.first().isEmpty())
    }

    @Test
    fun invalidSelection_doesNotMutateActiveIdOrUsageHistory() = runBlocking {
        val invalidTemplate = Template(
            id = "invalid",
            name = "Invalid",
            content = "Missing required variables"
        )
        store.saveTemplate(invalidTemplate)

        val result = store.setActiveTemplate(invalidTemplate.id)

        assertTrue(result is TemplateSelectionResult.Blocked)
        assertEquals(DEFAULT_TEMPLATE_ID, store.activeTemplateIdFlow.first())
        assertTrue(store.templateLastUsedFlow.first().isEmpty())
    }

    @Test
    fun undo_restoresPreviousActiveIdAndTargetTimestamp() = runBlocking {
        store.setActiveTemplate(firstTemplate.id)
        store.setActiveTemplate(secondTemplate.id)
        val result = store.setActiveTemplate(firstTemplate.id)
        val change = (result as TemplateSelectionResult.Selected).change

        assertTrue(store.undoTemplateSelection(change))
        assertFalse(store.undoTemplateSelection(change))
        assertEquals(secondTemplate.id, store.activeTemplateIdFlow.first())
        assertEquals(100L, store.templateLastUsedFlow.first()[firstTemplate.id])
    }

    @Test
    fun undo_fallsBackToDefaultWhenPreviousTemplateWasDeleted() = runBlocking {
        store.setActiveTemplate(firstTemplate.id)
        val change = (
            store.setActiveTemplate(secondTemplate.id) as TemplateSelectionResult.Selected
        ).change
        store.deleteTemplate(firstTemplate.id)

        assertTrue(store.undoTemplateSelection(change))
        assertEquals(DEFAULT_TEMPLATE_ID, store.activeTemplateIdFlow.first())
    }

    @Test
    fun undo_rejectsStaleTokenAfterRapidReselection() = runBlocking {
        val staleChange = (store.setActiveTemplate(firstTemplate.id) as TemplateSelectionResult.Selected).change
        store.setActiveTemplate(secondTemplate.id)
        store.setActiveTemplate(firstTemplate.id)

        assertFalse(store.undoTemplateSelection(staleChange))
        assertEquals(firstTemplate.id, store.activeTemplateIdFlow.first())
        assertEquals(102L, store.templateLastUsedFlow.first()[firstTemplate.id])
    }

    @Test
    fun deleteAndReset_pruneUsageHistory() = runBlocking {
        store.setActiveTemplate(firstTemplate.id)
        store.setActiveTemplate(secondTemplate.id)

        store.deleteTemplate(firstTemplate.id)
        assertNull(store.templateLastUsedFlow.first()[firstTemplate.id])

        store.resetToDefaultTemplate()
        assertTrue(store.templateLastUsedFlow.first().isEmpty())
    }

    @Test
    fun restore_doesNotRecordUsageAndFallsBackFromInvalidTemplate() = runBlocking {
        val invalidTemplate = Template(
            id = "invalid",
            name = "Invalid",
            content = "Missing required variables"
        )
        store.saveTemplate(invalidTemplate)

        assertEquals(firstTemplate.id, store.restoreActiveTemplate(firstTemplate.id))
        assertTrue(store.templateLastUsedFlow.first().isEmpty())

        assertEquals(DEFAULT_TEMPLATE_ID, store.restoreActiveTemplate(invalidTemplate.id))
        assertEquals(DEFAULT_TEMPLATE_ID, store.activeTemplateIdFlow.first())
        assertTrue(store.templateLastUsedFlow.first().isEmpty())
    }

    private fun validTemplate(id: String) = Template(
        id = id,
        name = id.replaceFirstChar(Char::uppercase),
        content = "Hello {{ customer_name }}, use {{ ssid }}."
    )
}