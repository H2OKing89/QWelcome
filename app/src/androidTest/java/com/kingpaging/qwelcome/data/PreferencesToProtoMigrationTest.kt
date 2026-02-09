package com.kingpaging.qwelcome.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kingpaging.qwelcome.viewmodel.factory.AppViewModelProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PreferencesToProtoMigrationTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val json = Json { encodeDefaults = true }

    @Before
    fun setup() = runBlocking {
        AppViewModelProvider.resetForTesting()
        clearLegacyPrefs()
    }

    @After
    fun tearDown() = runBlocking {
        clearLegacyPrefs()
        AppViewModelProvider.resetForTesting()
    }

    @Test
    fun migrate_maps_legacy_values_and_preserves_template_metadata() = runBlocking {
        val template = Template(
            id = "legacy-template-1",
            name = "Legacy Welcome",
            content = "Hello {{ customer_name }} {{ ssid }}",
            createdAt = "2026-01-01T00:00:00Z",
            modifiedAt = "2026-01-02T00:00:00Z",
            slug = "legacy_welcome",
            sortOrder = 27,
            tags = listOf("fiber", "vip")
        )
        val templatesJson = json.encodeToString(listOf(template))

        context.tempPreferencesDataStore.edit { prefs ->
            prefs[stringPreferencesKey("tech_name")] = "Tech Name"
            prefs[stringPreferencesKey("tech_title")] = "Field Tech"
            prefs[stringPreferencesKey("tech_dept")] = "Install"
            prefs[stringPreferencesKey("templates_json")] = templatesJson
            prefs[stringPreferencesKey("active_template_id")] = "legacy-template-1"
        }

        val migration = PreferencesToProtoMigration(context)
        assertTrue(migration.shouldMigrate(UserPreferences.getDefaultInstance()))

        val migrated = migration.migrate(UserPreferences.getDefaultInstance())

        assertEquals("legacy-template-1", migrated.activeTemplateId)
        assertEquals("Tech Name", migrated.techProfile.name)
        assertEquals("Field Tech", migrated.techProfile.title)
        assertEquals("Install", migrated.techProfile.dept)
        assertEquals(1, migrated.templatesCount)

        val migratedTemplate = migrated.templatesList.first()
        assertEquals(27, migratedTemplate.sortOrder)
        assertEquals(listOf("fiber", "vip"), migratedTemplate.tagsList)

        migration.cleanUp()
        val cleanedPrefs = context.tempPreferencesDataStore.data.first()
        assertTrue(cleanedPrefs[booleanPreferencesKey("proto_migration_completed")] == true)
        assertNull(cleanedPrefs[stringPreferencesKey("tech_name")])
        assertNull(cleanedPrefs[stringPreferencesKey("tech_title")])
        assertNull(cleanedPrefs[stringPreferencesKey("tech_dept")])
        assertNull(cleanedPrefs[stringPreferencesKey("templates_json")])
        assertNull(cleanedPrefs[stringPreferencesKey("active_template_id")])
    }

    @Test
    fun migrate_throws_when_templates_json_is_corrupted() = runBlocking {
        context.tempPreferencesDataStore.edit { prefs ->
            prefs[stringPreferencesKey("templates_json")] = "{invalid-json"
        }

        val migration = PreferencesToProtoMigration(context)
        assertTrue(migration.shouldMigrate(UserPreferences.getDefaultInstance()))

        var threw = false
        try {
            migration.migrate(UserPreferences.getDefaultInstance())
        } catch (e: IllegalStateException) {
            threw = true
            assertTrue(e.message?.contains("Failed to parse legacy templates JSON") == true)
        }

        assertTrue("Expected migration to throw for corrupted templates JSON", threw)
    }

    private suspend fun clearLegacyPrefs() {
        context.tempPreferencesDataStore.edit { it.clear() }
    }
}
