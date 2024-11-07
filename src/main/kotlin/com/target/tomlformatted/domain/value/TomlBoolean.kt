package com.target.tomlformatted.domain.value

import com.target.tomlformatted.parse.TomlParseResult
import com.target.tomlformatted.parse.valueSetRegexString

private val TRUE_REGEX = "^true${valueSetRegexString()}".toRegex()
private val FALSE_REGEX = "^false${valueSetRegexString()}".toRegex()

enum class TomlBoolean : ITomlValue<Boolean> {
    TRUE,
    FALSE,
    ;

    companion object {
        fun parse(currentBody: String): TomlParseResult<TomlBoolean> =
            when {
                currentBody.contains(TRUE_REGEX) ->
                    TomlParseResult.Success(value = TRUE, remainingBody = currentBody.substring(4))
                currentBody.contains(FALSE_REGEX) ->
                    TomlParseResult.Success(value = FALSE, remainingBody = currentBody.substring(5))
                else ->
                    TomlParseResult.Failure("Booleans must be either true or false")
            }
    }

    override fun toString(): String = this.name.lowercase().replaceFirstChar { it.uppercase() }

    override fun toTomlString(): String = this.name.lowercase()

    fun toBoolean(): Boolean = this == TRUE

    override fun toValue(): Boolean = toBoolean()
}
