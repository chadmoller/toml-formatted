package com.target.tomlformatted.domain.value

import com.target.tomlformatted.domain.TomlFillerWhitespace
import com.target.tomlformatted.domain.TomlPiece
import com.target.tomlformatted.parse.TomlParseResult
import com.target.tomlformatted.parse.parseAggregate

data class TomlValue<T>(
    private val prefixFiller: TomlFillerWhitespace = TomlFillerWhitespace.EMPTY,
    private val value: ITomlValue<T>,
    private val suffixFiller: TomlFillerWhitespace = TomlFillerWhitespace.EMPTY,
) : ITomlValue<T> {
    companion object {
        fun parse(currentBody: String): TomlParseResult<TomlValue<*>> {
            val prefixFillerResult = TomlFillerWhitespace.parse(currentBody, true)
            if (prefixFillerResult is TomlParseResult.Failure) {
                return TomlParseResult.Failure("Failed to parse prefix: ${prefixFillerResult.message}")
            }
            prefixFillerResult as TomlParseResult.Success

            val valueResult = ITomlValue.parse(prefixFillerResult.remainingBody)
            if (valueResult is TomlParseResult.Failure) {
                return TomlParseResult.Failure("Failed to parse value: ${valueResult.message}")
            }
            valueResult as TomlParseResult.Success

            val suffixFillerResult = TomlFillerWhitespace.parse(valueResult.remainingBody, true)
            if (suffixFillerResult is TomlParseResult.Failure) {
                return TomlParseResult.Failure("Failed to parse prefix: ${suffixFillerResult.message}")
            }
            suffixFillerResult as TomlParseResult.Success

            return TomlParseResult.Success(
                value =
                    TomlValue(
                        prefixFiller = prefixFillerResult.value,
                        value = valueResult.value,
                        suffixFiller = suffixFillerResult.value,
                    ),
                remainingBody = suffixFillerResult.remainingBody,
            )
        }
    }

    override fun toString(): String = "$value"

    override fun toTomlString(): String = prefixFiller.toTomlString() + value.toTomlString() + suffixFiller.toTomlString()

    override fun toValue(): T = value.toValue()
}

interface ITomlValue<T> : TomlPiece {
    fun toValue(): T

    companion object {
        private val options =
            listOf(
                { body: String -> TomlBoolean.parse(body) },
                { body: String -> ITomlDateTime.parse(body) },
                { body: String -> ITomlNumber.parse(body) },
                { body: String -> ITomlString.parse(body) },
                { body: String -> TomlInlineTable.parse(body) },
                { body: String -> TomlArray.parse(body) },
            )

        fun parse(currentBody: String): TomlParseResult<out ITomlValue<*>> = parseAggregate(options, currentBody)
    }
}
