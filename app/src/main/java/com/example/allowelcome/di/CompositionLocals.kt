package com.example.allowelcome.di

import androidx.compose.runtime.staticCompositionLocalOf
import com.example.allowelcome.navigation.Navigator
import com.example.allowelcome.viewmodel.CustomerIntakeViewModel
import com.example.allowelcome.viewmodel.export.ExportViewModel
import com.example.allowelcome.viewmodel.settings.SettingsViewModel

/**
 * CompositionLocal providers for dependency injection in Compose.
 * 
 * These allow ViewModels and other dependencies to be provided at the Activity level
 * and accessed anywhere in the Compose tree without explicit parameter passing.
 * 
 * Benefits:
 * - Easier testing: swap implementations at the CompositionLocalProvider level
 * - Cleaner composable signatures: no need to pass ViewModels as parameters
 * - Consistent dependency graph: all screens share the same ViewModel instances
 * 
 * Usage:
 * ```
 * // In Activity/Root Composable:
 * CompositionLocalProvider(
 *     LocalCustomerIntakeViewModel provides viewModel,
 *     LocalNavigator provides navigator
 * ) {
 *     AppContent()
 * }
 * 
 * // In any child composable:
 * val viewModel = LocalCustomerIntakeViewModel.current
 * ```
 */

/**
 * Provides the CustomerIntakeViewModel throughout the composition.
 * Throws if accessed before being provided (fail-fast for configuration errors).
 */
val LocalCustomerIntakeViewModel = staticCompositionLocalOf<CustomerIntakeViewModel> {
    error("CustomerIntakeViewModel not provided. Wrap your composable with CompositionLocalProvider.")
}

/**
 * Provides the SettingsViewModel throughout the composition.
 * Throws if accessed before being provided (fail-fast for configuration errors).
 */
val LocalSettingsViewModel = staticCompositionLocalOf<SettingsViewModel> {
    error("SettingsViewModel not provided. Wrap your composable with CompositionLocalProvider.")
}

/**
 * Provides the ExportViewModel throughout the composition.
 * Throws if accessed before being provided (fail-fast for configuration errors).
 */
val LocalExportViewModel = staticCompositionLocalOf<ExportViewModel> {
    error("ExportViewModel not provided. Wrap your composable with CompositionLocalProvider.")
}

/**
 * Provides the Navigator interface throughout the composition.
 * Throws if accessed before being provided (fail-fast for configuration errors).
 */
val LocalNavigator = staticCompositionLocalOf<Navigator> {
    error("Navigator not provided. Wrap your composable with CompositionLocalProvider.")
}
