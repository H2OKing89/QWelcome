package com.kingpaging.qwelcome.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.kingpaging.qwelcome.ui.CustomerIntakeScreen
import com.kingpaging.qwelcome.ui.export.ExportScreen
import com.kingpaging.qwelcome.ui.import_pkg.ImportScreen
import com.kingpaging.qwelcome.ui.settings.SettingsScreen
import com.kingpaging.qwelcome.ui.templates.TemplateListScreen

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
                }
            )
        }
    }
}
