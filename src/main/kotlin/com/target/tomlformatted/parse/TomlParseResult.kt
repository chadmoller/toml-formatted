package com.target.tomlformatted.parse

import com.target.tomlformatted.domain.TomlPiece

val VALUE_TERMINATOR_SET = setOf(",", "}", "]", "\\s", "$")

fun valueSetRegexString(terminators: Set<String> = VALUE_TERMINATOR_SET): String =
    terminators.joinToString(separator = "|", prefix = "(", postfix = ")")

fun <T : TomlPiece> parseAggregate(
    options: List<(String) -> TomlParseResult<out T>>,
    currentBody: String,
): TomlParseResult<out T> {
    for (option in options) {
        val result = option(currentBody)
        if (result is TomlParseResult.Success) {
            return result
        }
    }

    return TomlParseResult.Failure("No integers found")
}

sealed interface TomlParseResult<T> {
    data class Success<T>(
        val value: T,
        val remainingBody: String,
    ) : TomlParseResult<T>

    data class Failure<T>(
        val message: String,
    ) : TomlParseResult<T>
}
