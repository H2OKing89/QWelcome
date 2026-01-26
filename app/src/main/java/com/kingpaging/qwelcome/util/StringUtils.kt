package com.kingpaging.qwelcome.util

object StringUtils {
    /**
     * Converts a string to Title Case.
     * "QUENTIN KING" -> "Quentin King"
     * "john doe" -> "John Doe"
     * "mary-jane watson" -> "Mary-Jane Watson"
     */
    fun toTitleCase(input: String): String {
        if (input.isBlank()) return input
        
        return input.lowercase()
            .split(" ")
            .joinToString(" ") { word ->
                word.split("-").joinToString("-") { part ->
                    part.replaceFirstChar { it.uppercaseChar() }
                }
            }
    }
}
