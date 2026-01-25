package com.example.allowelcome

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.allowelcome.data.SettingsStore
import com.example.allowelcome.data.ThemeMode
import com.example.allowelcome.ui.CustomerIntakeScreen
import com.example.allowelcome.ui.settings.SettingsScreen
import com.example.allowelcome.ui.theme.CyberpunkTheme

class MainActivity : ComponentActivity() {

    private lateinit var settingsStore: SettingsStore

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen before super.onCreate()
        installSplashScreen()
        
        super.onCreate(savedInstanceState)

        settingsStore = SettingsStore(applicationContext)

        enableEdgeToEdge()
        setContent {
            // Collect theme preference
            val themeMode by settingsStore.themeModeFlow.collectAsState(initial = ThemeMode.SYSTEM)
            val systemDarkTheme = isSystemInDarkTheme()

            // Determine if we should use dark theme
            val useDarkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> systemDarkTheme
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            CyberpunkTheme(darkTheme = useDarkTheme) {
                var showSettings by rememberSaveable { mutableStateOf(false) }

                if (showSettings) {
                    SettingsScreen(onBack = { showSettings = false })
                } else {
                    CustomerIntakeScreen(
                        onOpenSettings = { showSettings = true }
                    )
                }
            }
        }
    }
}
