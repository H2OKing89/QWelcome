package com.kingpaging.qwelcome.navigation

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kingpaging.qwelcome.QWelcomeApplication
import com.kingpaging.qwelcome.di.AppContainer
import com.kingpaging.qwelcome.di.LocalCustomerIntakeViewModel
import com.kingpaging.qwelcome.di.LocalNavigator
import com.kingpaging.qwelcome.di.LocalSettingsViewModel
import com.kingpaging.qwelcome.di.LocalSoundPlayer
import com.kingpaging.qwelcome.di.LocalTemplateListViewModel
import com.kingpaging.qwelcome.di.LocalTemplateSelectionViewModel
import com.kingpaging.qwelcome.testutil.FakeNavigator
import com.kingpaging.qwelcome.testutil.FakeSoundPlayer
import com.kingpaging.qwelcome.ui.theme.CyberpunkTheme
import com.kingpaging.qwelcome.viewmodel.CustomerIntakeViewModel
import com.kingpaging.qwelcome.viewmodel.export.ExportViewModel
import com.kingpaging.qwelcome.viewmodel.factory.AppViewModelProvider
import com.kingpaging.qwelcome.viewmodel.import_pkg.ImportViewModel
import com.kingpaging.qwelcome.viewmodel.settings.SettingsViewModel
import com.kingpaging.qwelcome.viewmodel.templates.TemplateListViewModel
import com.kingpaging.qwelcome.viewmodel.templates.TemplateSelectionViewModel
import org.junit.Assert.assertNotSame
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DestinationViewModelScopeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var appContainer: AppContainer
    private lateinit var navController: NavHostController

    @Before
    fun setUp() {
        val application = ApplicationProvider.getApplicationContext<QWelcomeApplication>()
        appContainer = application.appContainer

        composeRule.setContent {
            val factory = remember(appContainer) { AppViewModelProvider(appContainer) }
            val customerIntakeViewModel: CustomerIntakeViewModel = viewModel(factory = factory)
            val settingsViewModel: SettingsViewModel = viewModel(factory = factory)
            val templateListViewModel: TemplateListViewModel = viewModel(factory = factory)
            val templateSelectionViewModel: TemplateSelectionViewModel = viewModel(factory = factory)
            val controller = rememberNavController()

            SideEffect {
                navController = controller
            }

            CyberpunkTheme {
                CompositionLocalProvider(
                    LocalCustomerIntakeViewModel provides customerIntakeViewModel,
                    LocalSettingsViewModel provides settingsViewModel,
                    LocalTemplateListViewModel provides templateListViewModel,
                    LocalTemplateSelectionViewModel provides templateSelectionViewModel,
                    LocalNavigator provides FakeNavigator(),
                    LocalSoundPlayer provides FakeSoundPlayer(),
                ) {
                    AppNavGraph(
                        navController = controller,
                        appContainer = appContainer,
                    )
                }
            }
        }
    }

    @Test
    fun importDestination_recreatesViewModelOnReopen() {
        navigateTo(Routes.Import)
        val firstViewModel = currentImportViewModel()

        returnToMain()
        navigateTo(Routes.Import)
        val secondViewModel = currentImportViewModel()

        assertNotSame(firstViewModel, secondViewModel)
    }

    @Test
    fun exportDestination_recreatesViewModelOnReopen() {
        navigateTo(Routes.Export)
        val firstViewModel = currentExportViewModel()

        returnToMain()
        navigateTo(Routes.Export)
        val secondViewModel = currentExportViewModel()

        assertNotSame(firstViewModel, secondViewModel)
    }

    private fun navigateTo(route: Any) {
        composeRule.runOnIdle {
            navController.navigate(route)
        }
        composeRule.waitForIdle()
    }

    private fun returnToMain() {
        composeRule.runOnIdle {
            check(navController.popBackStack())
        }
        composeRule.waitForIdle()
    }

    private fun currentImportViewModel(): ImportViewModel {
        lateinit var viewModel: ImportViewModel
        composeRule.runOnIdle {
            viewModel =
                ViewModelProvider(
                    checkNotNull(navController.currentBackStackEntry),
                    AppViewModelProvider(appContainer),
                )[ImportViewModel::class.java]
        }
        return viewModel
    }

    private fun currentExportViewModel(): ExportViewModel {
        lateinit var viewModel: ExportViewModel
        composeRule.runOnIdle {
            viewModel =
                ViewModelProvider(
                    checkNotNull(navController.currentBackStackEntry),
                    AppViewModelProvider(appContainer),
                )[ExportViewModel::class.java]
        }
        return viewModel
    }
}
