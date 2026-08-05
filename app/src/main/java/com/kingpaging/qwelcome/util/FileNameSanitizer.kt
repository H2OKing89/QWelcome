package com.kingpaging.qwelcome.util

private val UNSAFE_FILENAME_CHARACTERS = Regex("[^A-Za-z0-9._-]")

internal fun sanitizeFileName(name: String): String =
    name.replace(UNSAFE_FILENAME_CHARACTERS, "_").replace("..", "_")
