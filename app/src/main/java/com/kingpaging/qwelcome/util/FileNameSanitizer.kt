package com.kingpaging.qwelcome.util

private val UNSAFE_FILENAME_CHARACTERS = Regex("[^A-Za-z0-9._-]")
private const val FALLBACK_FILE_NAME = "download.apk"

internal fun sanitizeFileName(name: String): String {
    val sanitized = name.replace(UNSAFE_FILENAME_CHARACTERS, "_").replace("..", "_")
    return sanitized.takeUnless { it.isBlank() || it.all { character -> character == '.' } }
        ?: FALLBACK_FILE_NAME
}
