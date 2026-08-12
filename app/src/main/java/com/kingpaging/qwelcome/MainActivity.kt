package com.kingpaging.qwelcome

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.kingpaging.qwelcome.di.AppContainer
import com.kingpaging.qwelcome.di.LocalCustomerIntakeViewModel
import com.kingpaging.qwelcome.di.LocalExportViewModel
import com.kingpaging.qwelcome.di.LocalImportViewModel
import com.kingpaging.qwelcome.di.LocalNavigator
import com.kingpaging.qwelcome.di.LocalSettingsViewModel
import com.kingpaging.qwelcome.di.LocalSoundPlayer
import com.kingpaging.qwelcome.di.LocalTemplateListViewModel
import com.kingpaging.qwelcome.navigation.AndroidNavigator
import com.kingpaging.qwelcome.navigation.AppNavGraph
import com.kingpaging.qwelcome.navigation.Navigator
import com.kingpaging.qwelcome.ui.theme.CyberpunkTheme
import com.kingpaging.qwelcome.util.SoundManager
import com.kingpaging.qwelcome.viewmodel.CustomerIntakeViewModel
import com.kingpaging.qwelcome.viewmodel.export.ExportViewModel
import com.kingpaging.qwelcome.viewmodel.factory.AppViewModelProvider
import com.kingpaging.qwelcome.viewmodel.import_pkg.ImportViewModel
import com.kingpaging.qwelcome.viewmodel.settings.SettingsViewModel
import com.kingpaging.qwelcome.viewmodel.templates.TemplateListViewModel

class MainActivity : ComponentActivity() {
    private lateinit var navigator: Navigator
    private val appContainer: AppContainer
        get() = (application as QWelcomeApplication).appContainer

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen before super.onCreate()
        installSplashScreen()

        super.onCreate(savedInstanceState)

        navigator = AndroidNavigator(applicationContext)

        enableEdgeToEdge()
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE,
        )
        setContent {
            val appViewModelFactory = remember(appContainer) { AppViewModelProvider(appContainer) }

            // Create ViewModels at Activity level for proper scoping
            val customerIntakeViewModel: CustomerIntakeViewModel =
                viewModel(
                    factory = appViewModelFactory,
                )
            val settingsViewModel: SettingsViewModel =
                viewModel(
                    factory = appViewModelFactory,
                )
            val exportViewModel: ExportViewModel =
                viewModel(
                    factory = appViewModelFactory,
                )
            val importViewModel: ImportViewModel =
                viewModel(
                    factory = appViewModelFactory,
                )
            val templateListViewModel: TemplateListViewModel =
                viewModel(
                    factory = appViewModelFactory,
                )
            val privacySettings by settingsViewModel.privacySettings.collectAsStateWithLifecycle()
            LaunchedEffect(privacySettings?.screenCaptureProtectionEnabled) {
                if (privacySettings?.screenCaptureProtectionEnabled == true) {
                    window.setFlags(
                        WindowManager.LayoutParams.FLAG_SECURE,
                        WindowManager.LayoutParams.FLAG_SECURE,
                    )
                } else if (privacySettings != null) {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }

            // Navigation controller for Jetpack Navigation Compose
            val navController = rememberNavController()

            // Theme follows system setting automatically
            CyberpunkTheme {
                // Provide ViewModels and Navigator via CompositionLocal for easier testing
                CompositionLocalProvider(
                    LocalCustomerIntakeViewModel provides customerIntakeViewModel,
                    LocalSettingsViewModel provides settingsViewModel,
                    LocalExportViewModel provides exportViewModel,
                    LocalImportViewModel provides importViewModel,
                    LocalTemplateListViewModel provides templateListViewModel,
                    LocalSoundPlayer provides SoundManager,
                    LocalNavigator provides navigator,
                ) {
                    // Connect ViewModel lifecycle methods for auto-clear security feature
                    val lifecycleOwner = LocalLifecycleOwner.current
                    DisposableEffect(lifecycleOwner) {
                        val observer =
                            LifecycleEventObserver { _, event ->
                                when (event) {
                                    Lifecycle.Event.ON_PAUSE -> customerIntakeViewModel.onPause()
                                    Lifecycle.Event.ON_RESUME -> customerIntakeViewModel.onResume()
                                    else -> {}
                                }
                            }
                        lifecycleOwner.lifecycle.addObserver(observer)
                        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                    }

                    // Use Jetpack Navigation Compose for screen management
                    AppNavGraph(
                        navController = navController,
                        appContainer = appContainer,
                    )
                }
            }
        }
    }
}
