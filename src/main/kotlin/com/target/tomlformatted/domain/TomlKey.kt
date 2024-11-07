package com.target.tomlformatted.domain

import com.target.tomlformatted.domain.value.ITomlSingleLineString
import com.target.tomlformatted.parse.TomlParseResult
import com.target.tomlformatted.parse.parseAggregate

data class TomlKey(
    private val pieces: List<TomlKeyPiece>,
) : TomlPiece {
    companion object {
        fun parse(currentBody: String): TomlParseResult<TomlKey> {
            var workingBody = currentBody
            var currentResult = TomlKeyPiece.parse(currentBody)
            val pieces = mutableListOf<TomlKeyPiece>()
            while (currentResult is TomlParseResult.Success) {
                pieces.add(currentResult.value)
                workingBody = currentResult.remainingBody
                currentResult =
                    if (currentResult.remainingBody.startsWith('.')) {
                        workingBody = workingBody.drop(1)
                        TomlKeyPiece.parse(workingBody)
                    } else {
                        TomlParseResult.Failure("End of key")
                    }
            }
            return if (pieces.isNotEmpty()) {
                TomlParseResult.Success(
                    value =
                        TomlKey(
                            pieces = pieces,
                        ),
                    remainingBody = workingBody,
                )
            } else {
                TomlParseResult.Failure("No key piece found")
            }
        }
    }

    override fun toTomlString(): String =
        pieces.joinToString(".") {
            it.toTomlString()
        }

    override fun toString(): String = pieces.joinToString(".")

    fun toKeys(): List<String> = pieces.map { it.toKey() }
}

interface TomlKeyPiece : TomlPiece {
    companion object {
        private val options =
            listOf(
                { body: String -> TomlKeyPieceLiteral.parse(body) },
                { body: String -> TomlKeyPieceString.parse(body) },
            )

        fun parse(currentBody: String): TomlParseResult<out TomlKeyPiece> = parseAggregate(options, currentBody)
    }

    fun toKey(): String
}

// Literal & Basic Strings, and normal pieces

data class TomlKeyPieceLiteral(
    private val value: String,
    private val prefixFiller: TomlFillerWhitespace = TomlFillerWhitespace.EMPTY,
    private val suffixFiller: TomlFillerWhitespace = TomlFillerWhitespace.EMPTY,
) : TomlKeyPiece {
    companion object {
        val regex = Regex("^[\\w-]+")

        fun parse(currentBody: String): TomlParseResult<TomlKeyPieceLiteral> {
            val prefixFillerResult = TomlFillerWhitespace.parse(currentBody, true)
            if (prefixFillerResult is TomlParseResult.Failure) {
                return TomlParseResult.Failure("Failed to parse prefix: ${prefixFillerResult.message}")
            }
            prefixFillerResult as TomlParseResult.Success

            val matchResult = regex.find(prefixFillerResult.remainingBody) ?: return TomlParseResult.Failure("No Key piece found")
            val keyValue = matchResult.value
            val remainingBody = prefixFillerResult.remainingBody.drop(keyValue.length)

            val suffixFillerResult = TomlFillerWhitespace.parse(remainingBody, true)
            if (suffixFillerResult is TomlParseResult.Failure) {
                return TomlParseResult.Failure("Failed to parse prefix: ${suffixFillerResult.message}")
            }
            suffixFillerResult as TomlParseResult.Success

            return TomlParseResult.Success(
                value =
                    TomlKeyPieceLiteral(
                        prefixFiller = prefixFillerResult.value,
                        value = keyValue,
                        suffixFiller = suffixFillerResult.value,
                    ),
                remainingBody = suffixFillerResult.remainingBody,
            )
        }
    }

    override fun toString(): String = value

    override fun toTomlString(): String = prefixFiller.toTomlString() + value + suffixFiller.toTomlString()

    override fun toKey(): String = value
}

data class TomlKeyPieceString(
    private val value: ITomlSingleLineString,
    private val prefixFiller: TomlFillerWhitespace = TomlFillerWhitespace.EMPTY,
    private val suffixFiller: TomlFillerWhitespace = TomlFillerWhitespace.EMPTY,
) : TomlKeyPiece {
    companion object {
        fun parse(currentBody: String): TomlParseResult<TomlKeyPieceString> {
            val prefixFillerResult = TomlFillerWhitespace.parse(currentBody, true)
            if (prefixFillerResult is TomlParseResult.Failure) {
                return TomlParseResult.Failure("Failed to parse prefix: ${prefixFillerResult.message}")
            }
            prefixFillerResult as TomlParseResult.Success

            val valueResult = ITomlSingleLineString.parse(prefixFillerResult.remainingBody)
            if (valueResult is TomlParseResult.Failure) {
                return TomlParseResult.Failure("Failed to parse string: ${valueResult.message}")
            }
            valueResult as TomlParseResult.Success

            val suffixFillerResult = TomlFillerWhitespace.parse(valueResult.remainingBody, true)
            if (suffixFillerResult is TomlParseResult.Failure) {
                return TomlParseResult.Failure("Failed to parse prefix: ${suffixFillerResult.message}")
            }
            suffixFillerResult as TomlParseResult.Success

            return TomlParseResult.Success(
                value =
                    TomlKeyPieceString(
                        prefixFiller = prefixFillerResult.value,
                        value = valueResult.value,
                        suffixFiller = suffixFillerResult.value,
                    ),
                remainingBody = suffixFillerResult.remainingBody,
            )
        }
    }

    override fun toString(): String = value.toString()

    override fun toTomlString(): String = prefixFiller.toTomlString() + value.toTomlString() + suffixFiller.toTomlString()

    override fun toKey(): String = value.toString()
}
