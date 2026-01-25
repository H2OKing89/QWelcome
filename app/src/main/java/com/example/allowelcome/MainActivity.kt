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
import com.example.allowelcome.di.LocalExportViewModel
import com.example.allowelcome.di.LocalImportViewModel
import com.example.allowelcome.di.LocalNavigator
import com.example.allowelcome.di.LocalSettingsViewModel
import com.example.allowelcome.di.LocalTemplateListViewModel
import com.example.allowelcome.navigation.AndroidNavigator
import com.example.allowelcome.navigation.Navigator
import com.example.allowelcome.ui.CustomerIntakeScreen
import com.example.allowelcome.ui.export.ExportScreen
import com.example.allowelcome.ui.import_pkg.ImportScreen
import com.example.allowelcome.ui.settings.SettingsScreen
import com.example.allowelcome.ui.templates.TemplateListScreen
import com.example.allowelcome.ui.theme.CyberpunkTheme
import com.example.allowelcome.viewmodel.CustomerIntakeViewModel
import com.example.allowelcome.viewmodel.export.ExportViewModel
import com.example.allowelcome.viewmodel.import_pkg.ImportViewModel
import com.example.allowelcome.viewmodel.factory.AppViewModelProvider
import com.example.allowelcome.viewmodel.settings.SettingsViewModel
import com.example.allowelcome.viewmodel.templates.TemplateListViewModel

/**
 * Simple screen navigation state for the app.
 */
private enum class Screen {
    Main,
    Settings,
    Export,
    Import,
    TemplateList
}

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
            val exportViewModel: ExportViewModel = viewModel(
                factory = AppViewModelProvider(applicationContext)
            )
            val importViewModel: ImportViewModel = viewModel(
                factory = AppViewModelProvider(applicationContext)
            )
            val templateListViewModel: TemplateListViewModel = viewModel(
                factory = AppViewModelProvider(applicationContext)
            )

            // Theme follows system setting automatically
            CyberpunkTheme {
                // Provide ViewModels and Navigator via CompositionLocal for easier testing
                CompositionLocalProvider(
                    LocalCustomerIntakeViewModel provides customerIntakeViewModel,
                    LocalSettingsViewModel provides settingsViewModel,
                    LocalExportViewModel provides exportViewModel,
                    LocalImportViewModel provides importViewModel,
                    LocalTemplateListViewModel provides templateListViewModel,
                    LocalNavigator provides navigator
                ) {
                    var currentScreen by rememberSaveable { mutableStateOf(Screen.Main) }
                    // Track origin screen for TemplateList navigation
                    var templateListOrigin by rememberSaveable { mutableStateOf(Screen.Settings) }

                    when (currentScreen) {
                        Screen.Main -> {
                            CustomerIntakeScreen(
                                onOpenSettings = { currentScreen = Screen.Settings },
                                onOpenTemplates = {
                                    templateListOrigin = Screen.Main
                                    currentScreen = Screen.TemplateList
                                }
                            )
                        }
                        Screen.Settings -> {
                            SettingsScreen(
                                onBack = { currentScreen = Screen.Main },
                                onOpenExport = { currentScreen = Screen.Export },
                                onOpenImport = { currentScreen = Screen.Import },
                                onOpenTemplates = {
                                    templateListOrigin = Screen.Settings
                                    currentScreen = Screen.TemplateList
                                }
                            )
                        }
                        Screen.Export -> {
                            ExportScreen(
                                onBack = { currentScreen = Screen.Settings }
                            )
                        }
                        Screen.Import -> {
                            ImportScreen(
                                onBack = { currentScreen = Screen.Settings },
                                onImportComplete = { currentScreen = Screen.Settings }
                            )
                        }
                        Screen.TemplateList -> {
                            TemplateListScreen(
                                onBack = { currentScreen = templateListOrigin }
                            )
                        }
                    }
                }
            }
        }
    }
}
