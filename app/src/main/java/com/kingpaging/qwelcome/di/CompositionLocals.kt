package com.kingpaging.qwelcome.di

import androidx.compose.runtime.staticCompositionLocalOf
import com.kingpaging.qwelcome.navigation.Navigator
import com.kingpaging.qwelcome.util.SoundPlayer
import com.kingpaging.qwelcome.viewmodel.CustomerIntakeViewModel
import com.kingpaging.qwelcome.viewmodel.settings.SettingsViewModel
import com.kingpaging.qwelcome.viewmodel.templates.TemplateSelectionViewModel

/*
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
val LocalCustomerIntakeViewModel =
    staticCompositionLocalOf<CustomerIntakeViewModel> {
        error("CustomerIntakeViewModel not provided. Wrap your composable with CompositionLocalProvider.")
    }

/**
 * Provides the SettingsViewModel throughout the composition.
 * Throws if accessed before being provided (fail-fast for configuration errors).
 */
val LocalSettingsViewModel =
    staticCompositionLocalOf<SettingsViewModel> {
        error("SettingsViewModel not provided. Wrap your composable with CompositionLocalProvider.")
    }

/**
 * Provides the focused template-selection state used by Customer Intake.
 * Throws if accessed before being provided (fail-fast for configuration errors).
 */
val LocalTemplateSelectionViewModel =
    staticCompositionLocalOf<TemplateSelectionViewModel> {
        error("TemplateSelectionViewModel not provided. Wrap your composable with CompositionLocalProvider.")
    }

/**
 * Provides the [SoundPlayer] throughout the composition.
 * Created via [staticCompositionLocalOf] — throws if accessed before being provided
 * (fail-fast for configuration errors). Callers must supply a [SoundPlayer] implementation
 * (e.g., [SoundManager][com.kingpaging.qwelcome.util.SoundManager]) via
 * [CompositionLocalProvider][androidx.compose.runtime.CompositionLocalProvider].
 */
val LocalSoundPlayer =
    staticCompositionLocalOf<SoundPlayer> {
        error("SoundPlayer not provided. Wrap your composable with CompositionLocalProvider.")
    }

/**
 * Provides the Navigator interface throughout the composition.
 * Throws if accessed before being provided (fail-fast for configuration errors).
 */
val LocalNavigator =
    staticCompositionLocalOf<Navigator> {
        error("Navigator not provided. Wrap your composable with CompositionLocalProvider.")
    }
