package com.kingpaging.qwelcome.util

const val WIFI_MIN_PASSWORD_LENGTH = 8
const val WIFI_MAX_PASSWORD_LENGTH = 63
const val WIFI_MAX_SSID_LENGTH_BYTES = 32

object WifiValidationRules {
    fun isPasswordTooShort(password: String): Boolean =
        password.length < WIFI_MIN_PASSWORD_LENGTH

    fun isPasswordTooLong(password: String): Boolean =
        password.length > WIFI_MAX_PASSWORD_LENGTH

    fun isSsidTooLong(ssid: String): Boolean =
        ssid.toByteArray(Charsets.UTF_8).size > WIFI_MAX_SSID_LENGTH_BYTES
}
