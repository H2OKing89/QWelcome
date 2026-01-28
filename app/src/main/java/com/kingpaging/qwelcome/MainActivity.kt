package com.kingpaging.qwelcome

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kingpaging.qwelcome.di.LocalCustomerIntakeViewModel
import com.kingpaging.qwelcome.di.LocalExportViewModel
import com.kingpaging.qwelcome.di.LocalImportViewModel
import com.kingpaging.qwelcome.di.LocalNavigator
import com.kingpaging.qwelcome.di.LocalSettingsViewModel
import com.kingpaging.qwelcome.di.LocalTemplateListViewModel
import com.kingpaging.qwelcome.navigation.AndroidNavigator
import com.kingpaging.qwelcome.navigation.Navigator
import com.kingpaging.qwelcome.ui.CustomerIntakeScreen
import com.kingpaging.qwelcome.ui.export.ExportScreen
import com.kingpaging.qwelcome.ui.import_pkg.ImportScreen
import com.kingpaging.qwelcome.ui.settings.SettingsScreen
import com.kingpaging.qwelcome.ui.templates.TemplateListScreen
import com.kingpaging.qwelcome.ui.theme.CyberpunkTheme
import com.kingpaging.qwelcome.viewmodel.CustomerIntakeViewModel
import com.kingpaging.qwelcome.viewmodel.export.ExportViewModel
import com.kingpaging.qwelcome.viewmodel.import_pkg.ImportViewModel
import com.kingpaging.qwelcome.viewmodel.factory.AppViewModelProvider
import com.kingpaging.qwelcome.viewmodel.settings.SettingsViewModel
import com.kingpaging.qwelcome.viewmodel.templates.TemplateListViewModel

/**
 * Simple screen navigation state for the app. Using a stable string `key`
 * makes this robust against enum renaming during app updates.
 */
private enum class Screen(val key: String) {
    Main("main"),
    Settings("settings"),
    Export("export"),
    Import("import"),
    TemplateList("template_list")
}

/**
 * Custom Saver for the Screen enum to make it robust to enum name changes.
 * It saves and restores the enum based on its stable `key` property.
 */
private val ScreenSaver = Saver<Screen, String>(
    save = { it.key },
    restore = { key -> Screen.entries.first { it.key == key } }
)

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
                    // Connect ViewModel lifecycle methods for auto-clear security feature
                    val lifecycleOwner = LocalLifecycleOwner.current
                    DisposableEffect(lifecycleOwner) {
                        val observer = LifecycleEventObserver { _, event ->
                            when (event) {
                                Lifecycle.Event.ON_PAUSE -> customerIntakeViewModel.onPause()
                                Lifecycle.Event.ON_RESUME -> customerIntakeViewModel.onResume()
                                else -> {}
                            }
                        }
                        lifecycleOwner.lifecycle.addObserver(observer)
                        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                    }

                    var currentScreen by rememberSaveable(stateSaver = ScreenSaver) { mutableStateOf(Screen.Main) }
                    // Track origin screen for TemplateList navigation
                    var templateListOrigin by rememberSaveable(stateSaver = ScreenSaver) { mutableStateOf(Screen.Settings) }

                    // Handle system back button/gesture
                    BackHandler(enabled = currentScreen != Screen.Main) {
                        currentScreen = when (currentScreen) {
                            Screen.TemplateList -> templateListOrigin
                            Screen.Export, Screen.Import -> Screen.Settings
                            else -> Screen.Main
                        }
                    }

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
