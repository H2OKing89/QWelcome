package com.kingpaging.qwelcome.util

import androidx.annotation.StringRes

/**
 * An interface to abstract away the Android Context dependency for getting resources.
 * This makes ViewModels more easily testable.
 */
interface ResourceProvider {
    fun getString(@StringRes resId: Int): String
    fun getString(@StringRes resId: Int, vararg formatArgs: Any): String
}

/**
 * Implementation of [ResourceProvider] that uses the Android Context.
 */
class AndroidResourceProvider(private val context: android.content.Context) : ResourceProvider {
    override fun getString(@StringRes resId: Int): String {
        return context.getString(resId)
    }

    override fun getString(@StringRes resId: Int, vararg formatArgs: Any): String {
        return context.getString(resId, *formatArgs)
    }
}
