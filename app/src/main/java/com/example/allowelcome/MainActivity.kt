package com.example.allowelcome

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.allowelcome.di.LocalCustomerIntakeViewModel
import com.example.allowelcome.di.LocalNavigator
import com.example.allowelcome.di.LocalSettingsViewModel
import com.example.allowelcome.navigation.AndroidNavigator
import com.example.allowelcome.navigation.Navigator
import com.example.allowelcome.ui.CustomerIntakeScreen
import com.example.allowelcome.ui.settings.SettingsScreen
import com.example.allowelcome.ui.theme.CyberpunkTheme
import com.example.allowelcome.viewmodel.CustomerIntakeViewModel
import com.example.allowelcome.viewmodel.factory.AppViewModelProvider
import com.example.allowelcome.viewmodel.settings.SettingsViewModel

class MainActivity : ComponentActivity() {

    private lateinit var navigator: Navigator

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen before super.onCreate()
        installSplashScreen()
        
        super.onCreate(savedInstanceState)

        navigator = AndroidNavigator(applicationContext)

        enableEdgeToEdge()
        setContent {
            // Create ViewModels at Activity level for proper scoping
            val customerIntakeViewModel: CustomerIntakeViewModel = viewModel(
                factory = AppViewModelProvider(applicationContext)
            )
            val settingsViewModel: SettingsViewModel = viewModel(
                factory = AppViewModelProvider(applicationContext)
            )

            // Theme follows system setting automatically
            CyberpunkTheme {
                // Provide ViewModels and Navigator via CompositionLocal for easier testing
                CompositionLocalProvider(
                    LocalCustomerIntakeViewModel provides customerIntakeViewModel,
                    LocalSettingsViewModel provides settingsViewModel,
                    LocalNavigator provides navigator
                ) {
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
}
