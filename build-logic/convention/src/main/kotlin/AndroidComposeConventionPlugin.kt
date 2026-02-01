package com.kingpaging.qwelcome.buildlogic

import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * Convention plugin for Jetpack Compose configuration.
 * Centralizes Compose-related settings and dependencies.
 * Requires com.android.application plugin to be applied.
 */
class AndroidComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            // Verify com.android.application plugin is applied
            require(pluginManager.hasPlugin("com.android.application")) {
                "AndroidComposeConventionPlugin requires com.android.application plugin to be applied first"
            }

            with(pluginManager) {
                apply("org.jetbrains.kotlin.plugin.compose")
            }

            extensions.configure<ApplicationExtension> {
                buildFeatures {
                    compose = true
                }
            }
        }
    }
}
