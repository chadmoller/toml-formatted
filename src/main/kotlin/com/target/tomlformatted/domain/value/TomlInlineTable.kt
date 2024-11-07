package com.target.tomlformatted.domain.value

import com.target.tomlformatted.datatree.DataTree
import com.target.tomlformatted.domain.TomlFillerWhitespace
import com.target.tomlformatted.domain.TomlKey
import com.target.tomlformatted.domain.TomlPiece
import com.target.tomlformatted.parse.TomlParseResult

// TODO Parse

data class TomlInlineTable(
    val entries: List<Entry<*>>,
    val prefixFiller: TomlFillerWhitespace = TomlFillerWhitespace.EMPTY,
) : ITomlValue<DataTree> {
    data class Entry<T>(
        val key: TomlKey,
        val value: TomlValue<T>,
    ) : TomlPiece {
        companion object {
            fun parse(currentBody: String): TomlParseResult<Entry<*>> {
                val keyResult = TomlKey.parse(currentBody)
                if (keyResult is TomlParseResult.Failure) {
                    return TomlParseResult.Failure("Key not found: ${keyResult.message}")
                }
                keyResult as TomlParseResult.Success
                if (!keyResult.remainingBody.startsWith("=")) {
                    return TomlParseResult.Failure("No equals sign")
                }
                val workingBody = keyResult.remainingBody.drop(1)

                val valueResult = TomlValue.parse(workingBody)
                if (valueResult is TomlParseResult.Failure) {
                    return TomlParseResult.Failure("Value not found: ${valueResult.message}")
                }
                valueResult as TomlParseResult.Success

                return TomlParseResult.Success(
                    value = Entry(keyResult.value, valueResult.value),
                    remainingBody = valueResult.remainingBody,
                )
            }
        }

        override fun toString(): String = "$key=$value"

        override fun toTomlString(): String = "${key.toTomlString()}=${value.toTomlString()}"

        fun toKeys(): List<String> = key.toKeys()

        fun toValue(): T = value.toValue()
    }

    companion object {
        fun parse(currentBody: String): TomlParseResult<TomlInlineTable> {
            if (!currentBody.startsWith("{")) {
                return TomlParseResult.Failure("No opening bracket")
            }
            var workingBody = currentBody.drop(1)

            val whitespaceFiller = TomlFillerWhitespace.parse(workingBody, true)
            if (whitespaceFiller is TomlParseResult.Failure) {
                return TomlParseResult.Failure("Whitespace failed: ${whitespaceFiller.message}")
            }
            whitespaceFiller as TomlParseResult.Success
            workingBody = whitespaceFiller.remainingBody

            var currentResult = Entry.parse(workingBody)
            var trailingComma = false
            val pieces = mutableListOf<Entry<*>>()
            while (currentResult is TomlParseResult.Success) {
                pieces.add(currentResult.value)
                workingBody = currentResult.remainingBody
                currentResult =
                    if (workingBody.startsWith(',')) {
                        workingBody = workingBody.drop(1)
                        trailingComma = true
                        Entry.parse(workingBody)
                    } else {
                        trailingComma = false
                        TomlParseResult.Failure("End of key")
                    }
            }
            if (trailingComma) {
                return TomlParseResult.Failure("Trailing comma's aren't allowed in inline tables")
            }
            if (!workingBody.startsWith("}")) {
                return TomlParseResult.Failure("No closing bracket")
            }
            workingBody = workingBody.drop(1)
            return TomlParseResult.Success(
                TomlInlineTable(
                    entries = pieces.toList(),
                    prefixFiller = whitespaceFiller.value,
                ),
                remainingBody = workingBody,
            )
        }
    }

    override fun toString(): String = entries.joinToString(separator = ",", prefix = "{", postfix = "}")

    override fun toTomlString(): String = entries.joinToString(separator = ",", prefix = "{", postfix = "}") { it.toTomlString() }

    override fun toValue(): DataTree {
        val tree = DataTree()
        entries.forEach { entry ->
            tree.addLeaf(entry.toKeys(), entry.toValue())
        }
        return tree
    }
}
