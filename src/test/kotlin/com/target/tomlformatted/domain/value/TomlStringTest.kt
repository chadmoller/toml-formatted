package com.target.tomlformatted.domain.value

import com.target.tomlformatted.parse.TomlParseResult
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test

class TomlStringTest {
    @Test
    fun `Parse ITomlString - Literal`() {
        assertSoftly {
            ITomlString.parse("'Valid string'") shouldBe
                TomlParseResult.Success(
                    TomlLiteralString("Valid string"),
                    "",
                )
            ITomlString.parse("'''Multiline'''") shouldBe
                TomlParseResult.Success(
                    TomlMultilineLiteralString(listOf("Multiline")),
                    "",
                )
            ITomlString.parse("'''Multiline\nLine2''' Rest of body") shouldBe
                TomlParseResult.Success(
                    TomlMultilineLiteralString(listOf("Multiline", "Line2")),
                    " Rest of body",
                )

            ITomlString.parse("NotAString").shouldBeInstanceOf<TomlParseResult.Failure<ITomlString>>()
            ITomlString.parse("'No closing quote")
                .shouldBeInstanceOf<TomlParseResult.Failure<ITomlString>>()
            ITomlString.parse("'No closing quote\non the line'")
                .shouldBeInstanceOf<TomlParseResult.Failure<ITomlString>>()
            ITomlString.parse("'''No closing quote")
                .shouldBeInstanceOf<TomlParseResult.Failure<ITomlString>>()
        }
    }

    @Test
    fun `Parse ITomlString - Basic`() {
        assertSoftly {
            ITomlString.parse("\"Valid string\"") shouldBe
                TomlParseResult.Success(
                    TomlBasicString(
                        line =
                            TomlBasicString.Line(
                                listOf(
                                    TomlBasicString.StringPiece(
                                        value = "Valid string",
                                    ),
                                ),
                                terminator = TomlBasicString.EndOfString,
                            ),
                    ),
                    "",
                )
            ITomlString.parse("\"\"\"Multiline\"\"\"") shouldBe
                TomlParseResult.Success(
                    TomlMultilineBasicString(
                        lines =
                            listOf(
                                TomlBasicString.Line(
                                    pieces = listOf(TomlBasicString.StringPiece("Multiline")),
                                    terminator = TomlBasicString.EndOfString,
                                ),
                            ),
                    ),
                    "",
                )

            ITomlString.parse("\"\"\"Multiline\nLine2\"\"\" Rest of body") shouldBe
                TomlParseResult.Success(
                    TomlMultilineBasicString(
                        lines =
                            listOf(
                                TomlBasicString.Line(
                                    pieces = listOf(TomlBasicString.StringPiece("Multiline")),
                                    terminator = TomlBasicString.Wrap,
                                ),
                                TomlBasicString.Line(
                                    pieces = listOf(TomlBasicString.StringPiece("Line2")),
                                    terminator = TomlBasicString.EndOfString,
                                ),
                            ),
                    ),
                    " Rest of body",
                )

            ITomlString.parse("NotAString").shouldBeInstanceOf<TomlParseResult.Failure<ITomlString>>()
            ITomlString.parse("\"No closing quote")
                .shouldBeInstanceOf<TomlParseResult.Failure<ITomlString>>()
            ITomlString.parse("\"No closing quote\non the line\"")
                .shouldBeInstanceOf<TomlParseResult.Failure<ITomlString>>()
            ITomlString.parse("\"\"\"No closing quote")
                .shouldBeInstanceOf<TomlParseResult.Failure<ITomlString>>()
        }
    }

    @Test
    fun `Parse ITomlSingleLineString`() {
        assertSoftly {
            ITomlSingleLineString.parse("\"Valid string\"") shouldBe
                TomlParseResult.Success(
                    TomlBasicString(
                        line =
                            TomlBasicString.Line(
                                listOf(
                                    TomlBasicString.StringPiece(
                                        value = "Valid string",
                                    ),
                                ),
                                terminator = TomlBasicString.EndOfString,
                            ),
                    ),
                    "",
                )

            ITomlSingleLineString.parse("'Valid string'") shouldBe
                TomlParseResult.Success(
                    TomlLiteralString("Valid string"),
                    "",
                )
        }
    }
}
