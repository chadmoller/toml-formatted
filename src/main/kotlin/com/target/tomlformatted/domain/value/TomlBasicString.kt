package com.target.tomlformatted.domain.value

import com.target.tomlformatted.domain.TomlFillerWhitespace
import com.target.tomlformatted.domain.TomlPiece
import com.target.tomlformatted.domain.value.TomlBasicString.Piece
import com.target.tomlformatted.parse.TomlParseResult
import com.target.tomlformatted.parse.parseAggregate
import com.target.tomlformatted.parse.valueSetRegexString

interface ITomlBasicString : ITomlString {
    companion object {
        private val options =
            listOf(
                { body: String -> TomlBasicString.parse(body) },
                { body: String -> TomlMultilineBasicString.parse(body) },
            )

        fun parse(currentBody: String): TomlParseResult<out ITomlBasicString> = parseAggregate(options, currentBody)
    }
}

private const val MULTILINE_QUOTES = "\"\"\""
private val MultilinePieceBreak = setOf(MULTILINE_QUOTES, "\n", "\\\\")

private val MultilineBasicEndRegex = valueSetRegexString(MultilinePieceBreak).toRegex()

private val BasicEndRegex = "[\"\n\\\\]".toRegex()

data class TomlBasicString(
    val line: Line,
) : ITomlSingleLineString, ITomlBasicString {
    constructor(string: String) : this(Line(pieces = listOf(StringPiece(string)), terminator = EndOfString))

    init {
        if (line.terminator != EndOfString) {
            throw IllegalArgumentException("Line terminator must be end of string")
        }
    }

    interface LineTerminator : TomlPiece {
        companion object {
            fun parse(
                currentBody: String,
                multiline: Boolean,
            ): TomlParseResult<out LineTerminator> {
                val options =
                    if (multiline) {
                        listOf(
                            { body: String -> EndOfString.parse(body, true) },
                            { body: String -> Wrap.parse(body) },
                            { body: String -> EscapedFiller.parse(body) },
                        )
                    } else {
                        listOf { body: String -> EndOfString.parse(body, false) }
                    }
                return parseAggregate(options, currentBody)
            }
        }
    }

    object EndOfString : LineTerminator {
        fun parse(
            currentBody: String,
            multiline: Boolean,
        ): TomlParseResult<EndOfString> =
            if (multiline && currentBody.startsWith(MULTILINE_QUOTES)) {
                TomlParseResult.Success(EndOfString, currentBody.drop(MULTILINE_QUOTES.length))
            } else if (!multiline && currentBody.startsWith("\"")) {
                TomlParseResult.Success(EndOfString, currentBody.drop(1))
            } else {
                TomlParseResult.Failure("Not the ending of a string")
            }

        override fun toTomlString(): String = ""

        override fun toString(): String = ""
    }

    object Wrap : LineTerminator {
        fun parse(currentBody: String): TomlParseResult<Wrap> =
            if (currentBody.startsWith("\n")) {
                TomlParseResult.Success(Wrap, currentBody.drop(1))
            } else {
                TomlParseResult.Failure("Not a newline terminator")
            }

        override fun toTomlString(): String = "\n"

        override fun toString(): String = "\n"
    }

    object EscapedFiller : LineTerminator {
        fun parse(currentBody: String): TomlParseResult<EscapedFiller> =
            if (currentBody.startsWith("\\\n")) {
                // val filler = TomlFiller.parse(currentBody.drop(2))
                TomlParseResult.Success(EscapedFiller, currentBody.drop(2))
            } else {
                TomlParseResult.Failure("Not a newline terminator")
            }

        override fun toTomlString(): String = "\\\n"

        override fun toString(): String = "\n"
    }

    data class Line(
        val pieces: List<Piece>,
        val terminator: LineTerminator,
    ) : TomlPiece {
        companion object {
            fun parse(
                currentBody: String,
                multiline: Boolean,
            ): TomlParseResult<Line> {
                var workingBody = currentBody
                var currentResult = Piece.parse(currentBody, multiline)
                val pieces = mutableListOf<Piece>()
                while (currentResult is TomlParseResult.Success) {
                    pieces.add(currentResult.value)
                    workingBody = currentResult.remainingBody
                    currentResult = Piece.parse(workingBody, multiline)
                }
                val lineTerminator = LineTerminator.parse(workingBody, multiline)
                return if (lineTerminator is TomlParseResult.Success) {
                    TomlParseResult.Success(Line(pieces = pieces, terminator = lineTerminator.value), lineTerminator.remainingBody)
                } else {
                    TomlParseResult.Failure("Not a successful line parse")
                }
            }
        }

        override fun toTomlString(): String = pieces.joinToString("") { it.toTomlString() } + terminator.toTomlString()

        override fun toString(): String = pieces.joinToString("") + terminator.toString()
    }

    interface Piece : TomlPiece {
        companion object {
            fun parse(
                currentBody: String,
                multiline: Boolean,
            ): TomlParseResult<out Piece> {
                val options =
                    listOfNotNull(
                        { body: String -> TomlMultilineBasicString.WrapPiece.parse(body, multiline) },
                        { body: String -> EscapedCode.parse(body) },
                        { body: String -> Unicode16.parse(body) },
                        { body: String -> Unicode32.parse(body) },
                        { body: String -> StringPiece.parse(body, multiline) },
                    )
                return parseAggregate(options, currentBody)
            }
        }
    }

    data class StringPiece(
        val value: String,
    ) : Piece {
        init {
            require(value.isNotEmpty()) { "Value must not be empty." }
            require(!value.contains('\n')) { "Value must not contain a newline." }
        }

        companion object {
            fun parse(
                currentBody: String,
                multiline: Boolean,
            ): TomlParseResult<StringPiece> {
                val matchResult =
                    if (multiline) {
                        MultilineBasicEndRegex.find(currentBody)
                    } else {
                        BasicEndRegex.find(currentBody)
                    }
                if (matchResult == null) {
                    return TomlParseResult.Failure("Body ended before piece terminated")
                } else if (matchResult.range.first == 0) {
                    return TomlParseResult.Failure("Empty String")
                }
                val string = currentBody.substring(0, matchResult.range.first)
                val remainingBody = currentBody.drop(matchResult.range.first)
                return TomlParseResult.Success(StringPiece(string), remainingBody)
            }
        }

        override fun toTomlString(): String = value

        override fun toString(): String = value
    }

    enum class EscapedCode(
        private val code: Char,
        private val char: Char,
    ) : Piece {
        BACKSPACE('b', '\b'),
        TAB('t', '\t'),
        LINEFEED('n', '\n'),
        FORM_FEED('f', '\u000C'),
        CARRIAGE_RETURN('r', '\r'),
        DOUBLE_QUOTE('"', '"'),
        BACKSLASH('\\', '\\'),
        ;

        companion object {
            fun parse(currentBody: String): TomlParseResult<EscapedCode> {
                if (currentBody.length <= 1) {
                    return TomlParseResult.Failure("Not enough string for and escape code")
                }
                if (currentBody.first() != '\\') {
                    return TomlParseResult.Failure("Not escaped")
                }
                val code = currentBody[1]
                val result =
                    entries.find {
                        it.code == code
                    }
                return if (result == null) {
                    TomlParseResult.Failure("Code $code not found")
                } else {
                    TomlParseResult.Success(result, currentBody.drop(2))
                }
            }
        }

        override fun toString(): String = char.toString()

        override fun toTomlString(): String = "\\$code"
    }

    data class Unicode16(
        val codepoint: UShort,
    ) : Piece {
        companion object {
            fun parse(currentBody: String): TomlParseResult<Unicode16> {
                if (currentBody.length < 6) {
                    return TomlParseResult.Failure("Not enough string for unicode 16")
                }
                if (!currentBody.startsWith("\\u")) {
                    return TomlParseResult.Failure("Not escaped")
                }
                val code = currentBody.substring(2, 6)
                return try {
                    TomlParseResult.Success(Unicode16(code.toUShort(16)), currentBody.drop(6))
                } catch (ex: Exception) {
                    TomlParseResult.Failure(ex.localizedMessage)
                }
            }
        }

        override fun toString(): String = Char(codepoint).toString()

        override fun toTomlString(): String = "\\u${codepoint.toString(16).padStart(4, '0')}"
    }

    data class Unicode32(
        val codepoint: UInt,
    ) : Piece {
        companion object {
            fun parse(currentBody: String): TomlParseResult<Unicode32> {
                if (currentBody.length < 10) {
                    return TomlParseResult.Failure("Not enough string for unicode 32")
                }
                if (!currentBody.startsWith("\\U")) {
                    return TomlParseResult.Failure("Not escaped")
                }
                val code = currentBody.substring(2, 10)
                return try {
                    TomlParseResult.Success(Unicode32(code.toUInt(16)), currentBody.drop(10))
                } catch (ex: Exception) {
                    TomlParseResult.Failure(ex.localizedMessage)
                }
            }
        }

        override fun toString(): String = String(codepoint.toByteArray(), Charsets.UTF_32LE)

        override fun toTomlString(): String = "\\U${codepoint.toString(16).padStart(8, '0')}"
    }

    companion object {
        fun parse(currentBody: String): TomlParseResult<TomlBasicString> {
            if (!currentBody.startsWith("\"") ||
                currentBody.startsWith(MULTILINE_QUOTES)
            ) {
                return TomlParseResult.Failure("Not a string")
            }
            val workingBody = currentBody.drop(1)
            return when (val lineParse = Line.parse(workingBody, false)) {
                is TomlParseResult.Success -> TomlParseResult.Success(TomlBasicString(lineParse.value), lineParse.remainingBody)
                is TomlParseResult.Failure -> TomlParseResult.Failure(lineParse.message)
            }
        }
    }

    override fun toTomlString(): String = "\"${line.toTomlString()}\""

    override fun toString(): String = "$line"

    override fun toValue(): String = toString()
}

private const val WRAP_ESCAPE = "\\\n"

data class TomlMultilineBasicString(
    val lines: List<TomlBasicString.Line>,
    val startsWithNewline: Boolean = false,
) : ITomlMultilineString, ITomlBasicString {
    data class WrapPiece(
        val lines: List<TomlFillerWhitespace>,
    ) : Piece {
        companion object {
            fun parse(
                currentBody: String,
                multiline: Boolean,
            ): TomlParseResult<WrapPiece> {
                if (!multiline) {
                    return TomlParseResult.Failure("Non-multiline strings don't support Wrap escapes")
                }
                if (!currentBody.startsWith(WRAP_ESCAPE)) {
                    return TomlParseResult.Failure("Not escaped")
                }
                var remainingBody = currentBody.drop(WRAP_ESCAPE.length)
                val lines = mutableListOf<TomlFillerWhitespace>()
                var fillerParseResult: TomlParseResult<TomlFillerWhitespace>? = null
                while (fillerParseResult !is TomlParseResult.Failure) {
                    if (remainingBody.startsWith("\n")) {
                        lines.add(TomlFillerWhitespace.EMPTY)
                    } else {
                        fillerParseResult = TomlFillerWhitespace.parse(remainingBody)
                        if (fillerParseResult is TomlParseResult.Success) {
                            remainingBody = fillerParseResult.remainingBody
                            lines.add(fillerParseResult.value)
                        }
                    }

                    if (remainingBody.startsWith("\n")) {
                        remainingBody = remainingBody.drop(1)
                    }
                }
                return TomlParseResult.Success(WrapPiece(lines.toList()), remainingBody)
            }
        }

        override fun toTomlString(): String = "\\\n" + lines.joinToString("\n") { it.toTomlString() }

        override fun toString(): String = ""
    }

    companion object {
        fun parse(currentBody: String): TomlParseResult<TomlMultilineBasicString> {
            if (!currentBody.startsWith(MULTILINE_QUOTES)) {
                return TomlParseResult.Failure("Not a multiline string")
            }
            val workingBody = currentBody.drop(MULTILINE_QUOTES.length)
            val splitOnNextQuote = workingBody.split(MULTILINE_QUOTES, limit = 2)
            if (splitOnNextQuote.size != 2) {
                return TomlParseResult.Failure("No closing quote")
            }

            val startsWithNewline = splitOnNextQuote[0].startsWith("\n")
            var remainingBody =
                if (startsWithNewline) {
                    workingBody.drop(1)
                } else {
                    workingBody
                }

            val lines = mutableListOf<TomlBasicString.Line>()

            while (
                lines.isEmpty() || lines.last().terminator !is TomlBasicString.EndOfString
            ) {
                val lineResult = TomlBasicString.Line.parse(remainingBody, true)
                if (lineResult is TomlParseResult.Success) {
                    remainingBody = lineResult.remainingBody
                    lines.add(lineResult.value)
                } else if (lineResult is TomlParseResult.Failure) {
                    return TomlParseResult.Failure(lineResult.message)
                }
            }

            return TomlParseResult.Success(
                TomlMultilineBasicString(lines.toList(), startsWithNewline),
                remainingBody,
            )
        }
    }

    override fun toTomlString(): String =
        MULTILINE_QUOTES +
            buildFiller() +
            lines.joinToString("\n") { it.toTomlString() } +
            MULTILINE_QUOTES

    private fun buildFiller(): String = if (startsWithNewline) "\n" else ""

    override fun toString(): String = lines.joinToString("\n")

    override fun toValue(): String = toString()
}

fun UInt.toByteArray(): ByteArray =
    byteArrayOf(
        (this shr 0).toByte(),
        (this shr 8).toByte(),
        (this shr 16).toByte(),
        (this shr 24).toByte(),
    )
