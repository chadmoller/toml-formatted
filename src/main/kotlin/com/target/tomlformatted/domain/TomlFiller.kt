package com.target.tomlformatted.domain

import com.target.tomlformatted.parse.TomlParseResult

interface TomlFiller : TomlPiece

data class TomlFillerWhitespace(
    val value: String,
) : TomlFiller {
    init {
        require(value.isBlank()) { "Value must be blank." }
        require(!value.contains('\n')) { "Value must not contain a newline." }
    }

    companion object {
        val EMPTY = TomlFillerWhitespace("")
        private val regex = Regex("^([ \t]+)")

        fun parse(
            currentBody: String,
            allowEmpty: Boolean = false,
        ): TomlParseResult<TomlFillerWhitespace> {
            val matchResult = regex.find(currentBody)
            return if (matchResult != null) {
                val whitespace = matchResult.groupValues[1]
                TomlParseResult.Success(TomlFillerWhitespace(whitespace), currentBody.drop(whitespace.length))
            } else if (allowEmpty) {
                TomlParseResult.Success(TomlFillerWhitespace(""), currentBody)
            } else {
                TomlParseResult.Failure("No Fill")
            }
        }
    }

    override fun toTomlString(): String = value

    override fun toString(): String = ""
}

data class TomlFillerComment(
    val comment: String?,
) : TomlFiller {
    init {
        require(comment?.contains('\n') != true) { "Value must not contain a newline." }
    }

    companion object {
        val EMPTY = TomlFillerComment(null)
        private val regex = Regex("^#([^\n]*)")

        fun parse(
            currentBody: String,
            allowEmpty: Boolean = false,
        ): TomlParseResult<TomlFillerComment> {
            val matchResult = regex.find(currentBody)
            return if (matchResult != null) {
                val comment = matchResult.groupValues[1]
                TomlParseResult.Success(TomlFillerComment(comment), currentBody.drop(comment.length + 1))
            } else if (allowEmpty) {
                TomlParseResult.Success(TomlFillerComment(null), currentBody)
            } else {
                TomlParseResult.Failure("No Fill")
            }
        }
    }

    override fun toTomlString(): String =
        if (comment == null) {
            ""
        } else {
            "#$comment"
        }

    override fun toString(): String = comment ?: ""
}

data class TomlLineFiller(
    val whitespace: TomlFillerWhitespace = TomlFillerWhitespace.EMPTY,
    val comment: TomlFillerComment = TomlFillerComment.EMPTY,
    val terminator: Terminator = Terminator.NEWLINE,
) : TomlFiller {
    enum class Terminator(
        val string: String,
    ) {
        NEWLINE("\n"),
        EOF(""),
    }

    companion object {
        val EMPTY = TomlLineFiller()

        fun parse(
            currentBody: String,
            allowEmpty: Boolean,
        ): TomlParseResult<TomlLineFiller> {
            val whitespaceResult = TomlFillerWhitespace.parse(currentBody, true) as TomlParseResult.Success<TomlFillerWhitespace>
            val commentResult = TomlFillerComment.parse(whitespaceResult.remainingBody, true) as TomlParseResult.Success<TomlFillerComment>
            if (whitespaceResult.value == TomlFillerWhitespace.EMPTY && commentResult.value == TomlFillerComment.EMPTY && !allowEmpty) {
                return TomlParseResult.Failure("Empty isn't allowed")
            }

            var workingBody = commentResult.remainingBody
            return if (workingBody.isEmpty() || workingBody.startsWith('\n')) {
                val terminator =
                    if (workingBody.startsWith('\n')) {
                        workingBody = workingBody.drop(1)
                        Terminator.NEWLINE
                    } else {
                        Terminator.EOF
                    }

                TomlParseResult.Success(
                    value =
                        TomlLineFiller(
                            whitespace = whitespaceResult.value,
                            comment = commentResult.value,
                            terminator = terminator,
                        ),
                    remainingBody = workingBody,
                )
            } else {
                TomlParseResult.Failure("This isn't a fill line")
            }
        }
    }

    override fun toTomlString(): String = whitespace.toTomlString() + comment.toTomlString() + terminator.string

    override fun toString(): String = comment.toString() + terminator.string
}

data class TomlMultilineFiller(
    val lines: List<TomlLineFiller>,
    val suffixWhitespace: TomlFillerWhitespace,
) : TomlFiller {
    companion object {
        val EMPTY = TomlMultilineFiller(emptyList(), TomlFillerWhitespace.EMPTY)

        fun parse(currentBody: String): TomlParseResult<TomlMultilineFiller> {
            var workingBody = currentBody
            var currentResult = TomlLineFiller.parse(workingBody, true)
            val lines = mutableListOf<TomlLineFiller>()
            while (currentResult is TomlParseResult.Success && currentResult.value.terminator == TomlLineFiller.Terminator.NEWLINE) {
                lines.add(currentResult.value)
                workingBody = currentResult.remainingBody

                currentResult = TomlLineFiller.parse(workingBody, true)
            }

            val suffixResult = TomlFillerWhitespace.parse(workingBody, true)

            return if (suffixResult is TomlParseResult.Success) {
                TomlParseResult.Success(
                    value =
                        TomlMultilineFiller(
                            lines = lines,
                            suffixWhitespace = suffixResult.value,
                        ),
                    remainingBody = suffixResult.remainingBody,
                )
            } else {
                TomlParseResult.Failure("Suffix whitespace parse failed")
            }
        }
    }

    override fun toTomlString(): String = lines.joinToString("") { it.toTomlString() } + suffixWhitespace.toTomlString()

    override fun toString(): String = lines.map { it.toString() }.filter { it.isNotEmpty() }.joinToString("") + suffixWhitespace.toString()
}
