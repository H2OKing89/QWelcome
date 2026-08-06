package com.kingpaging.qwelcome.util

private val UNSAFE_FILENAME_CHARACTERS = Regex("[^A-Za-z0-9._-]")
internal const val FALLBACK_FILE_NAME = "unknown_network"
internal const val UPDATE_FALLBACK_FILE_NAME = "download.apk"

internal fun sanitizeFileName(name: String, fallbackFileName: String = FALLBACK_FILE_NAME): String {
    if (name.isBlank() || name.all { it == '.' }) return fallbackFileName

    return name.replace(UNSAFE_FILENAME_CHARACTERS, "_").replace("..", "_")
}
