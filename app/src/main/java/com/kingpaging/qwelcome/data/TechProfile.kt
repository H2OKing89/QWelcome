package com.kingpaging.qwelcome.data

/**
 * Tech profile information - stored locally per device.
 */
data class TechProfile(
    val name: String = "",
    val title: String = "",
    val dept: String = ""
) {
    companion object
}
