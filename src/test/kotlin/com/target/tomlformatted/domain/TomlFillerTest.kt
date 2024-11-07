package com.target.tomlformatted.domain

import com.target.tomlformatted.parse.TomlParseResult
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test

class TomlFillerTest {
    @Test
    fun `Comment filler - newline`() {
        shouldThrow<IllegalArgumentException> {
            TomlFillerComment("Stuff\nMore Stuff")
        }
    }

    @Test
    fun `No Comment`() {
        TomlFillerComment.EMPTY.toString() shouldBe ""
        TomlFillerComment.EMPTY.toTomlString() shouldBe ""
    }

    @Test
    fun `Comment filler`() {
        assertSoftly(TomlFillerComment("This is a comment")) {
            toString() shouldBe "This is a comment"
            toTomlString() shouldBe "#This is a comment"
        }
    }

    @Test
    fun `Comment parse success`() {
        assertSoftly(TomlFillerComment.parse("# The comment\notherText")) {
            this.shouldBeInstanceOf<TomlParseResult.Success<TomlFillerComment>>()
            value shouldBe TomlFillerComment(" The comment")
            remainingBody shouldBe "\notherText"
        }
    }

    @Test
    fun `Comment parse empty comment`() {
        assertSoftly(TomlFillerComment.parse("#\notherText")) {
            this.shouldBeInstanceOf<TomlParseResult.Success<TomlFillerComment>>()
            value shouldBe TomlFillerComment("")
            remainingBody shouldBe "\notherText"
        }
    }

    @Test
    fun `Comment parse no fill`() {
        assertSoftly(TomlFillerComment.parse("otherText")) {
            this.shouldBeInstanceOf<TomlParseResult.Failure<TomlFillerComment>>()
        }
    }

    @Test
    fun `Comment parse no fill allow empty`() {
        assertSoftly(TomlFillerComment.parse("otherText", allowEmpty = true)) {
            this.shouldBeInstanceOf<TomlParseResult.Success<TomlFillerComment>>()
            value shouldBe TomlFillerComment(null)
            remainingBody shouldBe "otherText"
        }
    }

    @Test
    fun `No Whitespace`() {
        TomlFillerWhitespace.EMPTY.toString() shouldBe ""
        TomlFillerWhitespace.EMPTY.toTomlString() shouldBe ""
    }

    @Test
    fun `Whitespace filler - non-whitespace`() {
        shouldThrow<IllegalArgumentException> {
            TomlFillerWhitespace("a")
        }
    }

    @Test
    fun `Whitespace filler - newline`() {
        shouldThrow<IllegalArgumentException> {
            TomlFillerWhitespace("\n")
        }
    }

    @Test
    fun `Whitespace filler`() {
        assertSoftly(TomlFillerWhitespace("  \t")) {
            toString() shouldBe ""
            toTomlString() shouldBe "  \t"
        }
    }

    @Test
    fun `Whitespace parse success`() {
        assertSoftly(TomlFillerWhitespace.parse(" \t \notherText")) {
            this.shouldBeInstanceOf<TomlParseResult.Success<TomlFillerWhitespace>>()
            value shouldBe TomlFillerWhitespace(" \t ")
            remainingBody shouldBe "\notherText"
        }
    }

    @Test
    fun `Whitespace parse no fill`() {
        assertSoftly(TomlFillerWhitespace.parse("otherText")) {
            this.shouldBeInstanceOf<TomlParseResult.Failure<TomlFillerWhitespace>>()
        }
    }

    @Test
    fun `Whitespace parse no fill allow empty`() {
        assertSoftly(TomlFillerWhitespace.parse("otherText", allowEmpty = true)) {
            this.shouldBeInstanceOf<TomlParseResult.Success<TomlFillerWhitespace>>()
            value shouldBe TomlFillerWhitespace("")
            remainingBody shouldBe "otherText"
        }
    }

    @Test
    fun `No Content Line`() {
        assertSoftly(TomlLineFiller.EMPTY) {
            toString() shouldBe "\n"
            toTomlString() shouldBe "\n"
        }
    }

    @Test
    fun `Line filler EOF`() {
        assertSoftly(
            TomlLineFiller(
                whitespace = TomlFillerWhitespace("\t"),
                comment = TomlFillerComment("This is a comment"),
                terminator = TomlLineFiller.Terminator.EOF,
            ),
        ) {
            toString() shouldBe "This is a comment"
            toTomlString() shouldBe "\t#This is a comment"
        }
    }

    @Test
    fun `Line filler`() {
        assertSoftly(TomlLineFiller(whitespace = TomlFillerWhitespace("\t"), comment = TomlFillerComment("This is a comment"))) {
            toString() shouldBe "This is a comment\n"
            toTomlString() shouldBe "\t#This is a comment\n"
        }
    }

    @Test
    fun `TomlLineFiller parse whitespace and comment`() {
        assertSoftly(TomlLineFiller.parse("\t#This is a comment\notherText", false)) {
            this.shouldBeInstanceOf<TomlParseResult.Success<TomlLineFiller>>()
            value shouldBe TomlLineFiller(whitespace = TomlFillerWhitespace("\t"), comment = TomlFillerComment("This is a comment"))
            remainingBody shouldBe "otherText"
        }
    }

    @Test
    fun `TomlLineFiller parse no whitespace`() {
        assertSoftly(TomlLineFiller.parse("#This is a comment\notherText", false)) {
            this.shouldBeInstanceOf<TomlParseResult.Success<TomlLineFiller>>()
            value shouldBe TomlLineFiller(whitespace = TomlFillerWhitespace.EMPTY, comment = TomlFillerComment("This is a comment"))
            remainingBody shouldBe "otherText"
        }
    }

    @Test
    fun `TomlLineFiller parse EOF`() {
        assertSoftly(TomlLineFiller.parse("#This is a comment", false)) {
            this.shouldBeInstanceOf<TomlParseResult.Success<TomlLineFiller>>()
            value shouldBe
                TomlLineFiller(
                    whitespace = TomlFillerWhitespace.EMPTY,
                    comment = TomlFillerComment("This is a comment"),
                    terminator = TomlLineFiller.Terminator.EOF,
                )
            remainingBody shouldBe ""
        }
    }

    @Test
    fun `TomlLineFiller parse no comment`() {
        assertSoftly(TomlLineFiller.parse("\t\notherText", false)) {
            this.shouldBeInstanceOf<TomlParseResult.Success<TomlLineFiller>>()
            value shouldBe TomlLineFiller(whitespace = TomlFillerWhitespace("\t"), comment = TomlFillerComment.EMPTY)
            remainingBody shouldBe "otherText"
        }
    }

    @Test
    fun `TomlLineFiller parse no fill not allowed`() {
        assertSoftly(TomlLineFiller.parse("\notherText", false)) {
            this.shouldBeInstanceOf<TomlParseResult.Failure<TomlLineFiller>>()
        }
    }

    @Test
    fun `TomlLineFiller parse no fill`() {
        assertSoftly(TomlLineFiller.parse("\notherText", true)) {
            this.shouldBeInstanceOf<TomlParseResult.Success<TomlLineFiller>>()
            value shouldBe TomlLineFiller(whitespace = TomlFillerWhitespace.EMPTY, comment = TomlFillerComment.EMPTY)
            remainingBody shouldBe "otherText"
        }
    }

    @Test
    fun `TomlLineFiller not a fill line`() {
        assertSoftly(TomlLineFiller.parse("otherText", false)) {
            this.shouldBeInstanceOf<TomlParseResult.Failure<TomlLineFiller>>()
        }
    }

    @Test
    fun `Empty multiline`() {
        assertSoftly(TomlMultilineFiller.EMPTY) {
            toString() shouldBe ""
            toTomlString() shouldBe ""
        }
    }

    @Test
    fun `Multiline filler - One Line`() {
        assertSoftly(
            TomlMultilineFiller(
                lines =
                    listOf(
                        TomlLineFiller(
                            whitespace = TomlFillerWhitespace("\t"),
                            comment = TomlFillerComment("This is a comment"),
                        ),
                    ),
                suffixWhitespace = TomlFillerWhitespace.EMPTY,
            ),
        ) {
            toString() shouldBe "This is a comment\n"
            toTomlString() shouldBe "\t#This is a comment\n"
        }
    }

    @Test
    fun `Multiline filler - multiple Lines`() {
        assertSoftly(
            TomlMultilineFiller(
                lines =
                    listOf(
                        TomlLineFiller(
                            whitespace = TomlFillerWhitespace("\t"),
                            comment = TomlFillerComment("This is a comment"),
                        ),
                        TomlLineFiller(
                            comment = TomlFillerComment("This is a comment without whitespace"),
                        ),
                    ),
                suffixWhitespace = TomlFillerWhitespace("    "),
            ),
        ) {
            toString() shouldBe "This is a comment\nThis is a comment without whitespace\n"
            toTomlString() shouldBe "\t#This is a comment\n#This is a comment without whitespace\n    "
        }
    }

    @Test
    fun `TomlMultilineFiller parse no lines`() {
        assertSoftly(TomlMultilineFiller.parse("otherText")) {
            this.shouldBeInstanceOf<TomlParseResult.Success<TomlMultilineFiller>>()
            value shouldBe TomlMultilineFiller.EMPTY
            remainingBody shouldBe "otherText"
        }
    }
}
