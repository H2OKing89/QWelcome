package com.kingpaging.qwelcome.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe navigation routes for Jetpack Navigation Compose.
 * Using Kotlin serialization for type-safe argument passing.
 */
object Routes {

    /** Main customer intake form screen */
    @Serializable
    object Main

    /** Settings screen */
    @Serializable
    object Settings

    /** Export templates/backup screen */
    @Serializable
    object Export

    /** Import templates/backup screen */
    @Serializable
    object Import

    /**
     * Template list screen with origin tracking for proper back navigation.
     * @param originRoute The route to navigate back to (serialized route name)
     */
    @Serializable
    data class TemplateList(val originRoute: String = "Main")
}
