package com.target.tomlformatted.domain

import com.target.tomlformatted.datatree.DataTree
import com.target.tomlformatted.domain.value.TomlValue
import com.target.tomlformatted.parse.TomlParseResult

open class TomlRootTable(
    val lines: List<TomlTableLine<*>>,
) : TomlPiece {
    override fun toTomlString(): String = lines.joinToString(separator = "") { it.toTomlString() }

    override fun toString(): String = "{\n" + lines.joinToString(separator = "") { "\t$it" } + "}"

    fun toTree(): DataTree {
        val tree = DataTree()
        lines.forEach { line ->
            val fullKey = line.toKeys()
            tree.addLeaf(fullKey, line.toValue())
        }
        return tree
    }

    fun toMap(): Map<String, Any?> {
        return toTree().toMap()
    }
}

class TomlTable(
    val key: TomlTableKey,
    lines: List<TomlTableLine<*>>,
) : TomlRootTable(lines) {
    override fun toTomlString(): String {
        return key.toTomlString() + super.toTomlString()
    }

    override fun toString(): String {
        return "$key${super.toString()}"
    }

    fun tableKeys(): List<String> = key.toKeys()
}

data class TomlTableKey(
    val key: TomlKey,
    val suffixFiller: TomlLineFiller,
) : TomlPiece {
    companion object {
        fun parse(currentBody: String): TomlParseResult<TomlTableKey> {
            if (!currentBody.startsWith("[")) {
                return TomlParseResult.Failure("No opening bracket sign")
            }
            var workingBody = currentBody.drop(1)

            val keyResult = TomlKey.parse(workingBody)
            if (keyResult is TomlParseResult.Failure) {
                return TomlParseResult.Failure("Key failed: ${keyResult.message}")
            }
            keyResult as TomlParseResult.Success
            workingBody = keyResult.remainingBody

            if (!workingBody.startsWith("]")) {
                return TomlParseResult.Failure("No closing bracket sign")
            }
            workingBody = workingBody.drop(1)

            val suffixFillerResult = TomlLineFiller.parse(workingBody, true)
            if (suffixFillerResult is TomlParseResult.Failure) {
                return TomlParseResult.Failure("Suffix filler failed: ${suffixFillerResult.message}")
            }
            suffixFillerResult as TomlParseResult.Success
            workingBody = keyResult.remainingBody

            return TomlParseResult.Success(
                value =
                    TomlTableKey(
                        key = keyResult.value,
                        suffixFiller = suffixFillerResult.value,
                    ),
                remainingBody = workingBody,
            )
        }
    }

    override fun toTomlString(): String = "[${key.toTomlString()}]${suffixFiller.toTomlString()}"

    override fun toString(): String = "$key"

    fun toKeys(): List<String> = key.toKeys()
}

data class TomlTableLine<T>(
    val prefixFiller: TomlMultilineFiller = TomlMultilineFiller.EMPTY,
    val key: TomlKey,
    val value: TomlValue<T>,
    val suffixComment: TomlLineFiller = TomlLineFiller.EMPTY,
) : TomlPiece {
    companion object {
        fun parse(currentBody: String): TomlParseResult<TomlTableLine<*>> {
            val prefixFillerResult = TomlMultilineFiller.parse(currentBody)
            if (prefixFillerResult is TomlParseResult.Failure) {
                return TomlParseResult.Failure("Prefix filler failed: ${prefixFillerResult.message}")
            }
            prefixFillerResult as TomlParseResult.Success
            var workingBody = prefixFillerResult.remainingBody

            val keyResult = TomlKey.parse(workingBody)
            if (keyResult is TomlParseResult.Failure) {
                return TomlParseResult.Failure("Key not found: ${keyResult.message}")
            }
            keyResult as TomlParseResult.Success
            workingBody = keyResult.remainingBody

            if (!workingBody.startsWith("=")) {
                return TomlParseResult.Failure("No equals sign")
            }
            workingBody = workingBody.drop(1)

            val valueResult = TomlValue.parse(workingBody)
            if (valueResult is TomlParseResult.Failure) {
                return TomlParseResult.Failure("Value not found: ${valueResult.message}")
            }
            valueResult as TomlParseResult.Success
            workingBody = valueResult.remainingBody

            val suffixCommentResult = TomlLineFiller.parse(workingBody, true)
            if (suffixCommentResult is TomlParseResult.Failure) {
                return TomlParseResult.Failure("Key not found: ${suffixCommentResult.message}")
            }
            suffixCommentResult as TomlParseResult.Success
            workingBody = suffixCommentResult.remainingBody

            return TomlParseResult.Success(
                value =
                    TomlTableLine(
                        prefixFiller = prefixFillerResult.value,
                        key = keyResult.value,
                        value = valueResult.value,
                        suffixComment = suffixCommentResult.value,
                    ),
                remainingBody = workingBody,
            )
        }
    }

    override fun toString(): String = "$key=$value\n"

    override fun toTomlString(): String =
        "${prefixFiller.toTomlString()}${key.toTomlString()}=${value.toTomlString()}${suffixComment.toTomlString()}"

    fun toKeys(): List<String> = key.toKeys()

    fun toValue(): T = value.toValue()
}
