package com.kingpaging.qwelcome.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.kingpaging.qwelcome.ui.CustomerIntakeScreen
import com.kingpaging.qwelcome.ui.export.ExportScreen
import com.kingpaging.qwelcome.ui.import_pkg.ImportScreen
import com.kingpaging.qwelcome.ui.settings.SettingsScreen
import com.kingpaging.qwelcome.ui.templates.TemplateEditorScreen
import com.kingpaging.qwelcome.ui.templates.TemplateListScreen
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
fun AppNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Routes.Main
    ) {
        composable<Routes.Main> {
            CustomerIntakeScreen(
                onOpenSettings = { navController.navigate(Routes.Settings) },
                onOpenTemplates = {
                    navController.navigate(Routes.TemplateList)
                }
            )
        }

        composable<Routes.Settings> {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onOpenExport = { navController.navigate(Routes.Export) },
                onOpenImport = { navController.navigate(Routes.Import) },
                onOpenTemplates = {
                    navController.navigate(Routes.TemplateList)
                }
            )
        }

        composable<Routes.Export> {
            ExportScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable<Routes.Import> {
            ImportScreen(
                onBack = { navController.popBackStack() },
                onImportComplete = { navController.popBackStack() }
            )
        }

        composable<Routes.TemplateList> {
            TemplateListScreen(
                onBack = {
                    // Navigate back to the origin screen
                    navController.popBackStack()
                },
                onOpenEditor = { templateId ->
                    navController.navigate(Routes.TemplateEditor(templateId))
                }
            )
        }

        composable<Routes.TemplateEditor> {
            val context = LocalContext.current
            // Required because the editor needs the NavBackStackEntry-scoped SavedStateHandle,
            // which the process-wide CompositionLocal cannot provide.
            val factory = remember(context) {
                AppViewModelProvider(context.applicationContext)
            }
            val editorViewModel: TemplateEditorViewModel = viewModel(factory = factory)
            TemplateEditorScreen(
                viewModel = editorViewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
