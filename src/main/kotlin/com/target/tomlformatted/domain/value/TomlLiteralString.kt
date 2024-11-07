package com.target.tomlformatted.domain.value

import com.target.tomlformatted.parse.TomlParseResult
import com.target.tomlformatted.parse.parseAggregate

interface ITomlLiteralString : ITomlString {
    companion object {
        private val options =
            listOf(
                { body: String -> TomlLiteralString.parse(body) },
                { body: String -> TomlMultilineLiteralString.parse(body) },
            )

        fun parse(currentBody: String): TomlParseResult<out ITomlLiteralString> = parseAggregate(options, currentBody)
    }
}

data class TomlLiteralString(
    val line: String,
) : ITomlSingleLineString, ITomlLiteralString {
    companion object {
        fun parse(currentBody: String): TomlParseResult<TomlLiteralString> {
            if (!currentBody.startsWith("'") ||
                currentBody.startsWith("'''")
            ) {
                return TomlParseResult.Failure("Not a string")
            }
            val workingBody = currentBody.drop(1)
            val splitOnNextQuote = workingBody.split("'", limit = 2)
            if (splitOnNextQuote.size != 2) {
                return TomlParseResult.Failure("No closing quote")
            }
            val stringBody = splitOnNextQuote[0]
            if (stringBody.contains("\n")) {
                return TomlParseResult.Failure("Unexpected newline")
            }
            val remainingBody = splitOnNextQuote[1]
            return TomlParseResult.Success(TomlLiteralString(stringBody), remainingBody)
        }
    }

    override fun toTomlString(): String = "'$line'"

    override fun toString(): String = line

    override fun toValue(): String = toString()
}

data class TomlMultilineLiteralString(
    val lines: List<String>,
    val startsWithNewline: Boolean = false,
) : ITomlMultilineString, ITomlLiteralString {
    companion object {
        fun parse(currentBody: String): TomlParseResult<TomlMultilineLiteralString> {
            if (!currentBody.startsWith("'''")) {
                return TomlParseResult.Failure("Not a multiline string")
            }
            val workingBody = currentBody.drop(3)
            val splitOnNextQuote = workingBody.split("'''", limit = 2)
            if (splitOnNextQuote.size != 2) {
                return TomlParseResult.Failure("No closing quote")
            }

            val startsWithNewline = splitOnNextQuote[0].startsWith("\n")
            val stringBody =
                if (startsWithNewline) {
                    splitOnNextQuote[0].drop(1)
                } else {
                    splitOnNextQuote[0]
                }

            val remainingBody = splitOnNextQuote[1]
            return TomlParseResult.Success(TomlMultilineLiteralString(stringBody.split("\n"), startsWithNewline), remainingBody)
        }
    }

    override fun toTomlString(): String =
        "'''" +
            buildFiller() +
            lines.joinToString("\n") +
            "'''"

    private fun buildFiller(): String = if (startsWithNewline) "\n" else ""

    override fun toString(): String = lines.joinToString("\n")

    override fun toValue(): String = toString()
}
