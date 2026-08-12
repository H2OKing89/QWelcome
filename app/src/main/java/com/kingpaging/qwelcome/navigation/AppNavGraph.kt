package com.kingpaging.qwelcome.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.kingpaging.qwelcome.di.AppContainer
import com.kingpaging.qwelcome.ui.CustomerIntakeRoute
import com.kingpaging.qwelcome.ui.export.ExportRoute
import com.kingpaging.qwelcome.ui.import_pkg.ImportRoute
import com.kingpaging.qwelcome.ui.settings.SettingsRoute
import com.kingpaging.qwelcome.ui.templates.TemplateEditorRoute
import com.kingpaging.qwelcome.ui.templates.TemplateListRoute
import com.kingpaging.qwelcome.viewmodel.factory.AppViewModelProvider
import com.kingpaging.qwelcome.viewmodel.templates.TemplateEditorViewModel

/**
 * Main navigation graph for the app using Jetpack Navigation Compose.
 *
 * Navigation flow:
 * - Main -> Settings -> Export/Import/TemplateList
 * - Main -> TemplateList (from template dropdown)
 */
@Composable
fun AppNavGraph(
    navController: NavHostController,
    appContainer: AppContainer,
) {
    NavHost(
        navController = navController,
        startDestination = Routes.Main,
    ) {
        composable<Routes.Main> {
            CustomerIntakeRoute(
                onOpenSettings = { navController.navigate(Routes.Settings) },
                onOpenTemplates = {
                    navController.navigate(Routes.TemplateList)
                },
            )
        }

        composable<Routes.Settings> {
            SettingsRoute(
                onBack = { navController.popBackStack() },
                onOpenExport = { navController.navigate(Routes.Export) },
                onOpenImport = { navController.navigate(Routes.Import) },
                onOpenTemplates = {
                    navController.navigate(Routes.TemplateList)
                },
            )
        }

        composable<Routes.Export> {
            ExportRoute(
                onBack = { navController.popBackStack() },
            )
        }

        composable<Routes.Import> {
            ImportRoute(
                onBack = { navController.popBackStack() },
                onImportComplete = { navController.popBackStack() },
            )
        }

        composable<Routes.TemplateList> {
            TemplateListRoute(
                onBack = {
                    // Navigate back to the origin screen
                    navController.popBackStack()
                },
                onOpenEditor = { templateId ->
                    navController.navigate(Routes.TemplateEditor(templateId))
                },
            )
        }

        composable<Routes.TemplateEditor> {
            // Required because the editor needs the NavBackStackEntry-scoped SavedStateHandle,
            // which the process-wide CompositionLocal cannot provide.
            val factory =
                remember(appContainer) {
                    AppViewModelProvider(appContainer)
                }
            val editorViewModel: TemplateEditorViewModel = viewModel(factory = factory)
            TemplateEditorRoute(
                viewModel = editorViewModel,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
