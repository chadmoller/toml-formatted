package com.target.tomlformatted.domain.value

import com.target.tomlformatted.domain.TomlFillerWhitespace
import com.target.tomlformatted.parse.TomlParseResult
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test

class TomlBasicStringTest {
    @Test
    fun `TomlBasicString string, escaped, and unicode`() {
        val piCode = 960.toUShort()
        val string =
            TomlBasicString(
                line =
                    TomlBasicString.Line(
                        pieces =
                            listOf(
                                TomlBasicString.StringPiece("Eat More"),
                                TomlBasicString.EscapedCode.LINEFEED,
                                TomlBasicString.Unicode16(piCode),
                            ),
                        terminator = TomlBasicString.EndOfString,
                    ),
            )

        assertSoftly(string) {
            toString() shouldBe "Eat More\nπ"
            toTomlString() shouldBe "\"Eat More\\n\\u03c0\""
        }
    }

    @Test
    fun `TomlBasicString parse string, escaped, and unicode`() {
        val piCode = 960.toUShort()
        val result = TomlBasicString.parse("\"Eat More\\n\\u03c0\" Rest of body")

        assertSoftly(result) {
            shouldBeInstanceOf<TomlParseResult.Success<TomlBasicString>>()
            value shouldBe
                TomlBasicString(
                    line =
                        TomlBasicString.Line(
                            pieces =
                                listOf(
                                    TomlBasicString.StringPiece("Eat More"),
                                    TomlBasicString.EscapedCode.LINEFEED,
                                    TomlBasicString.Unicode16(piCode),
                                ),
                            terminator = TomlBasicString.EndOfString,
                        ),
                )
            remainingBody shouldBe " Rest of body"
        }
    }

    @Test
    fun `TomlMultilineBasicString string, escaped, wrap, and unicode`() {
        val piCode = 960.toUShort()
        val string =
            TomlMultilineBasicString(
                lines =
                    listOf(
                        TomlBasicString.Line(
                            pieces = listOf(TomlBasicString.StringPiece("Eat More")),
                            terminator = TomlBasicString.EndOfString,
                        ),
                        TomlBasicString.Line(
                            pieces = listOf(TomlBasicString.EscapedCode.LINEFEED),
                            terminator = TomlBasicString.EndOfString,
                        ),
                        TomlBasicString.Line(
                            pieces =
                                listOf(
                                    TomlBasicString.Unicode16(piCode),
                                    TomlBasicString.StringPiece(" "),
                                    TomlMultilineBasicString.WrapPiece(lines = listOf(TomlFillerWhitespace("    "))),
                                    TomlBasicString.StringPiece("Now"),
                                ),
                            terminator = TomlBasicString.EndOfString,
                        ),
                    ),
            )

        assertSoftly(string) {
            toString() shouldBe "Eat More\n\n\nπ Now"
            toTomlString() shouldBe "\"\"\"Eat More\n\\n\n\\u03c0 \\\n    Now\"\"\""
        }
    }

    @Test
    fun `TomlMultilineBasicString starts with filler`() {
        val string =
            TomlMultilineBasicString(
                lines =
                    listOf(
                        TomlBasicString.Line(
                            pieces =
                                listOf(
                                    TomlBasicString.StringPiece("Eat More"),
                                ),
                            terminator = TomlBasicString.EndOfString,
                        ),
                    ),
                startsWithNewline = true,
            )

        assertSoftly(string) {
            toString() shouldBe "Eat More"
            toTomlString() shouldBe "\"\"\"\nEat More\"\"\""
        }
    }

    @Test
    fun `TomlMultilineBasicString parse string, escaped, wrap, and unicode`() {
        val piCode = 960.toUShort()
        val result = TomlMultilineBasicString.parse("\"\"\"\nEat More\n\\n\n\\u03c0 \\\n    Now\"\"\" Rest of body")

        assertSoftly(result) {
            shouldBeInstanceOf<TomlParseResult.Success<TomlMultilineBasicString>>()
            value shouldBe
                TomlMultilineBasicString(
                    lines =
                        listOf(
                            TomlBasicString.Line(
                                pieces = listOf(TomlBasicString.StringPiece("Eat More")),
                                terminator = TomlBasicString.Wrap,
                            ),
                            TomlBasicString.Line(
                                pieces = listOf(TomlBasicString.EscapedCode.LINEFEED),
                                terminator = TomlBasicString.Wrap,
                            ),
                            TomlBasicString.Line(
                                pieces =
                                    listOf(
                                        TomlBasicString.Unicode16(piCode),
                                        TomlBasicString.StringPiece(" "),
                                        TomlMultilineBasicString.WrapPiece(lines = listOf(TomlFillerWhitespace("    "))),
                                        TomlBasicString.StringPiece("Now"),
                                    ),
                                terminator = TomlBasicString.EndOfString,
                            ),
                        ),
                    startsWithNewline = true,
                )
            remainingBody shouldBe " Rest of body"
        }
    }

    @Test
    fun `TomlBasicString StringPiece newline`() {
        shouldThrow<IllegalArgumentException> {
            TomlBasicString.StringPiece("test\ncase")
        }
    }

    @Test
    fun `TomlBasicString StringPiece`() {
        val value = TomlBasicString.StringPiece("test")

        assertSoftly(value) {
            toString() shouldBe "test"
            toTomlString() shouldBe "test"
        }
    }

    @Test
    fun `TomlBasicString StringPiece Single-line Parse`() {
        assertSoftly {
            TomlBasicString.StringPiece.parse(
                "any text\"",
                false,
            ) shouldBe TomlParseResult.Success(TomlBasicString.StringPiece("any text"), "\"")
            TomlBasicString.StringPiece.parse(
                "any text\" Rest of body",
                false,
            ) shouldBe TomlParseResult.Success(TomlBasicString.StringPiece("any text"), "\" Rest of body")
            TomlBasicString.StringPiece.parse(
                "any text\\n Rest of body",
                false,
            ) shouldBe TomlParseResult.Success(TomlBasicString.StringPiece("any text"), "\\n Rest of body")
            TomlBasicString.StringPiece.parse(
                "any text\n Rest of body",
                false,
            ) shouldBe TomlParseResult.Success(TomlBasicString.StringPiece("any text"), "\n Rest of body")

            TomlBasicString.StringPiece.parse(
                "\"",
                false,
            ) shouldBe TomlParseResult.Failure("Empty String")
            TomlBasicString.StringPiece.parse(
                "just text",
                false,
            ) shouldBe TomlParseResult.Failure("Body ended before piece terminated")
        }
    }

    @Test
    fun `TomlBasicString StringPiece Multiline Parse`() {
        assertSoftly {
            TomlBasicString.StringPiece.parse(
                "any text\"\"\"",
                true,
            ) shouldBe TomlParseResult.Success(TomlBasicString.StringPiece("any text"), "\"\"\"")
            TomlBasicString.StringPiece.parse(
                "any text\"\"\" Rest of body",
                true,
            ) shouldBe TomlParseResult.Success(TomlBasicString.StringPiece("any text"), "\"\"\" Rest of body")
            TomlBasicString.StringPiece.parse(
                "any text\\n Rest of body",
                true,
            ) shouldBe TomlParseResult.Success(TomlBasicString.StringPiece("any text"), "\\n Rest of body")
            TomlBasicString.StringPiece.parse(
                "any text\n Rest of body",
                true,
            ) shouldBe TomlParseResult.Success(TomlBasicString.StringPiece("any text"), "\n Rest of body")

            TomlBasicString.StringPiece.parse(
                "\"\"\"",
                true,
            ) shouldBe TomlParseResult.Failure("Empty String")
            TomlBasicString.StringPiece.parse(
                "just text",
                true,
            ) shouldBe TomlParseResult.Failure("Body ended before piece terminated")
        }
    }

    @Test
    fun `TomlBasicString EscapedCode`() {
        val value = TomlBasicString.EscapedCode.TAB

        assertSoftly(value) {
            toString() shouldBe "\t"
            toTomlString() shouldBe "\\t"
        }
    }

    @Test
    fun `TomlBasicString EscapedCode Parse`() {
        assertSoftly {
            TomlBasicString.EscapedCode.parse(
                "\\t",
            ) shouldBe TomlParseResult.Success(TomlBasicString.EscapedCode.TAB, "")
            TomlBasicString.EscapedCode.parse(
                "\\t Rest of body",
            ) shouldBe TomlParseResult.Success(TomlBasicString.EscapedCode.TAB, " Rest of body")

            TomlBasicString.EscapedCode.parse(
                "\\",
            ) shouldBe TomlParseResult.Failure("Not enough string for and escape code")
            TomlBasicString.EscapedCode.parse(
                "t tab character missing escape",
            ) shouldBe TomlParseResult.Failure("Not escaped")
            TomlBasicString.EscapedCode.parse(
                "\\z",
            ) shouldBe TomlParseResult.Failure("Code z not found")
        }
    }

    @Test
    fun `TomlBasicString Unicode16 ascii character`() {
        val spaceCode = 32.toUShort()
        val value = TomlBasicString.Unicode16(spaceCode)

        assertSoftly(value) {
            toString() shouldBe " "
            toTomlString() shouldBe "\\u0020"
        }
    }

    @Test
    fun `TomlBasicString Unicode16 unicode character`() {
        val piCode = 960.toUShort()
        val value = TomlBasicString.Unicode16(piCode)

        assertSoftly(value) {
            toString() shouldBe "π"
            toTomlString() shouldBe "\\u03c0"
        }
    }

    @Test
    fun `TomlBasicString Unicode16 Parse`() {
        val spaceCode = 32.toUShort()

        assertSoftly {
            TomlBasicString.Unicode16.parse(
                "\\u0020",
            ) shouldBe TomlParseResult.Success(TomlBasicString.Unicode16(spaceCode), "")
            TomlBasicString.Unicode16.parse(
                "\\u0020 Rest of body",
            ) shouldBe TomlParseResult.Success(TomlBasicString.Unicode16(spaceCode), " Rest of body")

            TomlBasicString.Unicode16.parse(
                "\\u12",
            ) shouldBe TomlParseResult.Failure("Not enough string for unicode 16")
            TomlBasicString.Unicode16.parse(
                "\\t1234",
            ) shouldBe TomlParseResult.Failure("Not escaped")
            TomlBasicString.Unicode16.parse(
                "\\unohx",
            ) shouldBe TomlParseResult.Failure("Invalid number format: 'nohx'")
        }
    }

    @Test
    fun `TomlBasicString Unicode32 ascii character`() {
        val spaceCode = 32.toUInt()
        val value = TomlBasicString.Unicode32(spaceCode)

        assertSoftly(value) {
            toString() shouldBe " "
            toTomlString() shouldBe "\\U00000020"
        }
    }

    @Test
    fun `TomlBasicString Unicode32 unicode character`() {
        val piCode = 960.toUInt()
        val value = TomlBasicString.Unicode32(piCode)

        assertSoftly(value) {
            toString() shouldBe "π"
            toTomlString() shouldBe "\\U000003c0"
        }
    }

    @Test
    fun `TomlBasicString Unicode32 extended unicode character`() {
        val aceOfSpadesCode = 127137.toUInt()
        val value = TomlBasicString.Unicode32(aceOfSpadesCode)

        assertSoftly(value) {
            toString() shouldBe "\uD83C\uDCA1"
            toTomlString() shouldBe "\\U0001f0a1"
        }
    }

    @Test
    fun `TomlBasicString Unicode32 Parse`() {
        val spaceCode = 32.toUInt()

        assertSoftly {
            TomlBasicString.Unicode32.parse(
                "\\U00000020",
            ) shouldBe TomlParseResult.Success(TomlBasicString.Unicode32(spaceCode), "")
            TomlBasicString.Unicode32.parse(
                "\\U00000020 Rest of body",
            ) shouldBe TomlParseResult.Success(TomlBasicString.Unicode32(spaceCode), " Rest of body")

            TomlBasicString.Unicode32.parse(
                "\\U12",
            ) shouldBe TomlParseResult.Failure("Not enough string for unicode 32")
            TomlBasicString.Unicode32.parse(
                "\\t12345678",
            ) shouldBe TomlParseResult.Failure("Not escaped")
            TomlBasicString.Unicode32.parse(
                "\\Unohexstr",
            ) shouldBe TomlParseResult.Failure("Invalid number format: 'nohexstr'")
        }
    }

    @Test
    fun `TomlMultilineBasicString WrapPiece`() {
        val value =
            TomlMultilineBasicString.WrapPiece(
                lines =
                    listOf(
                        TomlFillerWhitespace("  "),
                        TomlFillerWhitespace("\t"),
                    ),
            )

        assertSoftly(value) {
            toString() shouldBe ""
            toTomlString() shouldBe "\\\n  \n\t"
        }
    }

    @Test
    fun `TomlMultilineBasicString WrapPiece Parse`() {
        assertSoftly {
            TomlMultilineBasicString.WrapPiece.parse(
                "\\\n  \n\tRest of body",
                multiline = true,
            ) shouldBe
                TomlParseResult.Success(
                    TomlMultilineBasicString.WrapPiece(
                        lines =
                            listOf(
                                TomlFillerWhitespace("  "),
                                TomlFillerWhitespace("\t"),
                            ),
                    ),
                    "Rest of body",
                )

            TomlMultilineBasicString.WrapPiece.parse(
                "\\\n  \n\t",
                multiline = false,
            ) shouldBe TomlParseResult.Failure("Non-multiline strings don't support Wrap escapes")
            TomlMultilineBasicString.WrapPiece.parse(
                "\n",
                multiline = true,
            ) shouldBe TomlParseResult.Failure("Not escaped")
        }
    }
}
