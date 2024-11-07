package com.target.tomlformatted.domain.value

import com.target.tomlformatted.parse.TomlParseResult
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test

class TomlLiteralStringTest {
    @Test
    fun `TomlLiteralString test`() {
        val string = TomlLiteralString(line = "Eat More")

        assertSoftly(string) {
            toString() shouldBe "Eat More"
            toTomlString() shouldBe "'Eat More'"
        }
    }

    @Test
    fun `Parse TomlLiteralString`() {
        assertSoftly {
            TomlLiteralString.parse("NotAString").shouldBeInstanceOf<TomlParseResult.Failure<TomlLiteralString>>()
            TomlLiteralString.parse("'Not closing quote").shouldBeInstanceOf<TomlParseResult.Failure<TomlLiteralString>>()
            TomlLiteralString.parse("'Not closing quote\non the line'").shouldBeInstanceOf<TomlParseResult.Failure<TomlLiteralString>>()
            TomlLiteralString.parse("'''Multiline'''").shouldBeInstanceOf<TomlParseResult.Failure<TomlLiteralString>>()
            TomlLiteralString.parse("'Valid string'") shouldBe TomlParseResult.Success(TomlLiteralString("Valid string"), "")
            TomlLiteralString.parse(
                "'Valid string' extra stuff",
            ) shouldBe TomlParseResult.Success(TomlLiteralString("Valid string"), " extra stuff")
        }
    }

    @Test
    fun `TomlMultilineLiteralString test`() {
        val string =
            TomlMultilineLiteralString(
                lines =
                    listOf(
                        "Eat More",
                        "π",
                        "Now",
                    ),
            )

        assertSoftly(string) {
            toString() shouldBe "Eat More\nπ\nNow"
            toTomlString() shouldBe "'''Eat More\nπ\nNow'''"
        }
    }

    @Test
    fun `TomlMultilineLiteralString starts with filler`() {
        val string =
            TomlMultilineLiteralString(
                lines =
                    listOf(
                        "Eat More",
                        "π",
                    ),
                startsWithNewline = true,
            )

        assertSoftly(string) {
            toString() shouldBe "Eat More\nπ"
            toTomlString() shouldBe "'''\nEat More\nπ'''"
        }
    }

    @Test
    fun `Parse TomlMultilineLiteralString`() {
        assertSoftly {
            TomlMultilineLiteralString.parse("NotAString").shouldBeInstanceOf<TomlParseResult.Failure<TomlMultilineLiteralString>>()
            TomlMultilineLiteralString.parse("'Single line'").shouldBeInstanceOf<TomlParseResult.Failure<TomlMultilineLiteralString>>()
            TomlMultilineLiteralString.parse(
                "'''Not closing quote'\non any line''",
            ).shouldBeInstanceOf<TomlParseResult.Failure<TomlMultilineLiteralString>>()
            TomlMultilineLiteralString.parse(
                "'''Valid string'''",
            ) shouldBe TomlParseResult.Success(TomlMultilineLiteralString(listOf("Valid string"), false), "")
            TomlMultilineLiteralString.parse(
                "'''\n  Valid string\nOn 2 lines''' extra stuff",
            ) shouldBe TomlParseResult.Success(TomlMultilineLiteralString(listOf("  Valid string", "On 2 lines"), true), " extra stuff")
        }
    }
}
