package com.target.tomlformatted.domain.value

import com.target.tomlformatted.domain.TomlFillerComment
import com.target.tomlformatted.domain.TomlLineFiller
import com.target.tomlformatted.domain.TomlMultilineFiller
import com.target.tomlformatted.domain.TomlPiece
import com.target.tomlformatted.parse.TomlParseResult

// TODO Validate that all non-last entries have commas
data class TomlArray(
    val entries: List<Entry<*>>,
    val suffixFill: TomlMultilineFiller = TomlMultilineFiller.EMPTY,
) : ITomlValue<List<Any>> {
    data class Entry<T>(
        val prefixFiller: TomlMultilineFiller = TomlMultilineFiller.EMPTY,
        val value: ITomlValue<T>,
        val suffixPreCommaFiller: TomlMultilineFiller = TomlMultilineFiller.EMPTY,
        val includeComma: Boolean = false,
        val suffixPostCommaFiller: TomlLineFiller? = null,
    ) : TomlPiece {
        companion object {
            fun parse(currentBody: String): TomlParseResult<Entry<*>> {
                val prefixFillerResult = TomlMultilineFiller.parse(currentBody)
                if (prefixFillerResult is TomlParseResult.Failure) {
                    return TomlParseResult.Failure("Failed to parse prefix filler: ${prefixFillerResult.message}")
                }
                prefixFillerResult as TomlParseResult.Success
                var remainingBody = prefixFillerResult.remainingBody

                val valueResult = ITomlValue.parse(remainingBody)
                if (valueResult is TomlParseResult.Failure) {
                    return TomlParseResult.Failure("Failed to parse value: ${valueResult.message}")
                }
                valueResult as TomlParseResult.Success
                remainingBody = valueResult.remainingBody

                val suffixPreCommaFillerResult = TomlMultilineFiller.parse(remainingBody)
                if (suffixPreCommaFillerResult is TomlParseResult.Failure) {
                    return TomlParseResult.Failure("Failed to parse suffix pre-comma filler: ${suffixPreCommaFillerResult.message}")
                }
                suffixPreCommaFillerResult as TomlParseResult.Success
                remainingBody = suffixPreCommaFillerResult.remainingBody

                var suffixPostCommaFiller: TomlLineFiller? = null
                var includeComma = false
                if (remainingBody.startsWith(",")) {
                    includeComma = true
                    remainingBody = remainingBody.drop(1)
                    val postCommaFillerResult = TomlLineFiller.parse(remainingBody, false)
                    if (postCommaFillerResult is TomlParseResult.Success) {
                        if (postCommaFillerResult.value.comment != TomlFillerComment.EMPTY) {
                            suffixPostCommaFiller = postCommaFillerResult.value
                            remainingBody = postCommaFillerResult.remainingBody
                        }
                    }
                }

                return TomlParseResult.Success(
                    value =
                        Entry(
                            prefixFiller = prefixFillerResult.value,
                            value = valueResult.value,
                            suffixPreCommaFiller = suffixPreCommaFillerResult.value,
                            includeComma = includeComma,
                            suffixPostCommaFiller = suffixPostCommaFiller,
                        ),
                    remainingBody = remainingBody,
                )
            }
        }

        override fun toString(): String = "$value"

        override fun toTomlString(): String =
            prefixFiller.toTomlString() +
                value.toTomlString() +
                suffixPreCommaFiller.toTomlString() +
                (if (includeComma) "," else "") +
                (suffixPostCommaFiller?.toTomlString() ?: "")

        fun toValue(): T = value.toValue()
    }

    companion object {
        fun parse(currentBody: String): TomlParseResult<TomlArray> {
            if (!currentBody.startsWith("[")) {
                return TomlParseResult.Failure("No opening bracket")
            }
            var workingBody = currentBody.drop(1)

            var currentResult = Entry.parse(workingBody)

            val pieces = mutableListOf<Entry<*>>()
            while (currentResult is TomlParseResult.Success) {
                pieces.add(currentResult.value)
                workingBody = currentResult.remainingBody
                currentResult = Entry.parse(workingBody)
            }
            val suffixFillResult = TomlMultilineFiller.parse(workingBody)
            if (suffixFillResult is TomlParseResult.Failure) {
                return TomlParseResult.Failure("Failed to parse post-filler: ${suffixFillResult.message}")
            }
            suffixFillResult as TomlParseResult.Success
            workingBody = suffixFillResult.remainingBody

            if (!workingBody.startsWith("]")) {
                return TomlParseResult.Failure("No closing bracket")
            }
            workingBody = workingBody.drop(1)
            return TomlParseResult.Success(
                TomlArray(
                    entries = pieces.toList(),
                    suffixFill = suffixFillResult.value,
                ),
                remainingBody = workingBody,
            )
        }
    }

    override fun toString(): String = entries.map { "$it" }.joinToString(separator = ", ", prefix = "[", postfix = "]")

    override fun toTomlString(): String = tomlStrings().joinToString(separator = "", prefix = "[", postfix = "]")

    private fun tomlStrings(): List<String> {
        return entries.map { it.toTomlString() } + suffixFill.toTomlString()
    }

    override fun toValue(): List<Any> = entries.mapNotNull { it.toValue() }
}
