package com.target.tomlformatted.domain.value

import com.target.tomlformatted.parse.TomlParseResult
import com.target.tomlformatted.parse.parseAggregate

interface ITomlString : ITomlValue<String> {
    companion object {
        private val options =
            listOf(
                { body: String -> ITomlLiteralString.parse(body) },
                { body: String -> ITomlBasicString.parse(body) },
            )

        fun parse(currentBody: String): TomlParseResult<out ITomlString> = parseAggregate(options, currentBody)
    }
}

interface ITomlSingleLineString : ITomlString {
    companion object {
        private val options =
            listOf(
                { body: String -> TomlBasicString.parse(body) },
                { body: String -> TomlLiteralString.parse(body) },
            )

        fun parse(currentBody: String): TomlParseResult<out ITomlSingleLineString> = parseAggregate(options, currentBody)
    }
}

interface ITomlMultilineString : ITomlString
