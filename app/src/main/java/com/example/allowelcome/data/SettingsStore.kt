package com.example.allowelcome.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

data class TechProfile(
    val name: String = "",
    val title: String = "",
    val dept: String = ""
)

class SettingsStore(private val context: Context) {

    private object Keys {
        val TECH_NAME = stringPreferencesKey("tech_name")
        val TECH_TITLE = stringPreferencesKey("tech_title")
        val TECH_DEPT = stringPreferencesKey("tech_dept")
    }

    val techProfileFlow: Flow<TechProfile> =
        context.dataStore.data.map { prefs ->
            TechProfile(
                name = prefs[Keys.TECH_NAME].orEmpty(),
                title = prefs[Keys.TECH_TITLE].orEmpty(),
                dept = prefs[Keys.TECH_DEPT].orEmpty()
            )
        }

    suspend fun saveTechProfile(profile: TechProfile) {
        context.dataStore.edit { prefs ->
            prefs[Keys.TECH_NAME] = profile.name.trim()
            prefs[Keys.TECH_TITLE] = profile.title.trim()
            prefs[Keys.TECH_DEPT] = profile.dept.trim()
        }
    }
}
