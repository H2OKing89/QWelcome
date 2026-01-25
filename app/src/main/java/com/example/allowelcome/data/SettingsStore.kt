package com.example.allowelcome.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.allowelcome.R
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

data class TechProfile(
    val name: String = "",
    val title: String = "",
    val dept: String = ""
)

data class TemplateSettings(
    val useCustomTemplate: Boolean = false,
    val customTemplate: String = ""
)

class SettingsStore(private val context: Context) {

    private object Keys {
        val TECH_NAME = stringPreferencesKey("tech_name")
        val TECH_TITLE = stringPreferencesKey("tech_title")
        val TECH_DEPT = stringPreferencesKey("tech_dept")
        val USE_CUSTOM_TEMPLATE = booleanPreferencesKey("use_custom_template")
        val CUSTOM_TEMPLATE = stringPreferencesKey("custom_template")
    }

    /** The default template that ships with the app (read-only) */
    fun getDefaultTemplate(): String = context.getString(R.string.welcome_template)

    val techProfileFlow: Flow<TechProfile> =
        context.dataStore.data.map { prefs ->
            TechProfile(
                name = prefs[Keys.TECH_NAME].orEmpty(),
                title = prefs[Keys.TECH_TITLE].orEmpty(),
                dept = prefs[Keys.TECH_DEPT].orEmpty()
            )
        }

    val templateSettingsFlow: Flow<TemplateSettings> =
        context.dataStore.data.map { prefs ->
            TemplateSettings(
                useCustomTemplate = prefs[Keys.USE_CUSTOM_TEMPLATE] ?: false,
                customTemplate = prefs[Keys.CUSTOM_TEMPLATE].orEmpty()
            )
        }

    /** Returns the active template (custom if enabled, otherwise default) */
    val activeTemplateFlow: Flow<String> =
        context.dataStore.data.map { prefs ->
            val useCustom = prefs[Keys.USE_CUSTOM_TEMPLATE] ?: false
            val customTemplate = prefs[Keys.CUSTOM_TEMPLATE].orEmpty()
            if (useCustom && customTemplate.isNotBlank()) {
                customTemplate
            } else {
                getDefaultTemplate()
            }
        }

    suspend fun saveTechProfile(profile: TechProfile) {
        context.dataStore.edit { prefs ->
            prefs[Keys.TECH_NAME] = profile.name.trim()
            prefs[Keys.TECH_TITLE] = profile.title.trim()
            prefs[Keys.TECH_DEPT] = profile.dept.trim()
        }
    }

    suspend fun saveTemplateSettings(settings: TemplateSettings) {
        context.dataStore.edit { prefs ->
            prefs[Keys.USE_CUSTOM_TEMPLATE] = settings.useCustomTemplate
            prefs[Keys.CUSTOM_TEMPLATE] = settings.customTemplate
        }
    }

    suspend fun resetToDefaultTemplate() {
        context.dataStore.edit { prefs ->
            prefs[Keys.USE_CUSTOM_TEMPLATE] = false
            prefs[Keys.CUSTOM_TEMPLATE] = ""
        }
    }
}
