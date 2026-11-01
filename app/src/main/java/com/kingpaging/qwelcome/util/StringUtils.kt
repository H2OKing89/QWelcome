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

        return Regex("""(^|[\s\-'’])(\p{L})""").replace(input.lowercase()) { match ->
            match.groupValues[1] + match.groupValues[2].replaceFirstChar { it.titlecase() }
        }
    }
}
