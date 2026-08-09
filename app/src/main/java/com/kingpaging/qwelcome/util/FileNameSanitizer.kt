package com.kingpaging.qwelcome.util

private val UNSAFE_FILENAME_CHARACTERS = Regex("[^A-Za-z0-9._-]")
internal const val FALLBACK_FILE_NAME = "unknown_network"
internal const val UPDATE_FALLBACK_FILE_NAME = "download.apk"
private const val MAX_FILE_NAME_LENGTH = 255

internal fun sanitizeFileName(name: String, fallbackFileName: String = FALLBACK_FILE_NAME): String {
    if (name.isBlank() || name.all { it == '.' }) return fallbackFileName

    val sanitized = name.replace(UNSAFE_FILENAME_CHARACTERS, "_").replace("..", "_")
    return truncateFileName(sanitized)
}

/** Caps [name] at [MAX_FILE_NAME_LENGTH] characters while preserving the file extension. */
private fun truncateFileName(name: String): String {
    if (name.length <= MAX_FILE_NAME_LENGTH) return name

    val dotIndex = name.lastIndexOf('.')
    val rawExtension = if (dotIndex > 0) name.substring(dotIndex) else ""
    val base = if (dotIndex > 0) name.substring(0, dotIndex) else name

    // Cap an oversized extension too, so the combined result never exceeds the limit.
    val extension = if (rawExtension.length >= MAX_FILE_NAME_LENGTH) {
        rawExtension.take(MAX_FILE_NAME_LENGTH)
    } else {
        rawExtension
    }
    val maxBaseLength = (MAX_FILE_NAME_LENGTH - extension.length).coerceAtLeast(0)
    return base.take(maxBaseLength) + extension
}
