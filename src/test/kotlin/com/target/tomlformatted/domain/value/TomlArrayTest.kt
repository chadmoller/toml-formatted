package com.target.tomlformatted.domain.value

import com.target.tomlformatted.domain.TomlFillerComment
import com.target.tomlformatted.domain.TomlFillerWhitespace
import com.target.tomlformatted.domain.TomlLineFiller
import com.target.tomlformatted.domain.TomlMultilineFiller
import com.target.tomlformatted.parse.TomlParseResult
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldContainOnly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.math.BigInteger

class TomlArrayTest {
    @Test
    fun `TomlArray trailing comma`() {
        val value =
            TomlArray(
                entries =
                    listOf(
                        TomlArray.Entry(
                            value = TomlDecimalInteger(pieces = listOf("123")),
                            includeComma = true,
                        ),
                        TomlArray.Entry(
                            value = TomlDecimalInteger(pieces = listOf("456")),
                            includeComma = true,
                        ),
                    ),
                suffixFill = TomlMultilineFiller(lines = listOf(), suffixWhitespace = TomlFillerWhitespace(" ")),
            )

        assertSoftly(value) {
            toString() shouldBe "[123, 456]"
            toTomlString() shouldBe "[123,456, ]"
            toValue() shouldContainOnly listOf(BigInteger.valueOf(123), BigInteger.valueOf(456))
        }
    }

    @Test
    fun `TomlArray parse trailing comma`() {
        assertSoftly {
            TomlArray.parse("[123,456, ]") shouldBe
                TomlParseResult.Success(
                    value =
                        TomlArray(
                            entries =
                                listOf(
                                    TomlArray.Entry(
                                        value = TomlDecimalInteger(pieces = listOf("123")),
                                        includeComma = true,
                                    ),
                                    TomlArray.Entry(
                                        value = TomlDecimalInteger(pieces = listOf("456")),
                                        includeComma = true,
                                    ),
                                ),
                            suffixFill = TomlMultilineFiller(lines = listOf(), suffixWhitespace = TomlFillerWhitespace(" ")),
                        ),
                    remainingBody = "",
                )
        }
    }

    @Test
    fun `TomlArray mixed values`() {
        val value =
            TomlArray(
                entries =
                    listOf(
                        TomlArray.Entry(
                            prefixFiller = TomlMultilineFiller(lines = listOf(), suffixWhitespace = TomlFillerWhitespace(" ")),
                            value = TomlDecimalInteger(pieces = listOf("123")),
                            includeComma = true,
                        ),
                        TomlArray.Entry(
                            prefixFiller = TomlMultilineFiller(lines = listOf(), suffixWhitespace = TomlFillerWhitespace("  ")),
                            value =
                                TomlBasicString(
                                    line =
                                        TomlBasicString.Line(
                                            pieces = listOf(TomlBasicString.StringPiece("Test")),
                                            terminator = TomlBasicString.EndOfString,
                                        ),
                                ),
                        ),
                    ),
            )

        assertSoftly(value) {
            toString() shouldBe "[123, Test]"
            toTomlString() shouldBe "[ 123,  \"Test\"]"
            toValue() shouldContainOnly listOf(BigInteger.valueOf(123), "Test")
        }
    }

    @Test
    fun `TomlArray parse mixed values`() {
        val result = TomlArray.parse("[ 123,  \"Test\"]")
        val expected =
            TomlArray(
                entries =
                    listOf(
                        TomlArray.Entry(
                            prefixFiller = TomlMultilineFiller(lines = listOf(), suffixWhitespace = TomlFillerWhitespace(" ")),
                            value = TomlDecimalInteger(pieces = listOf("123")),
                            includeComma = true,
                        ),
                        TomlArray.Entry(
                            prefixFiller = TomlMultilineFiller(lines = listOf(), suffixWhitespace = TomlFillerWhitespace("  ")),
                            value =
                                TomlBasicString(
                                    line =
                                        TomlBasicString.Line(
                                            pieces = listOf(TomlBasicString.StringPiece("Test")),
                                            terminator = TomlBasicString.EndOfString,
                                        ),
                                ),
                        ),
                    ),
            )
        assertSoftly {
            result shouldBe
                TomlParseResult.Success(
                    value = expected,
                    remainingBody = "",
                )
        }
    }

    @Test
    fun `TomlArray wrapped values`() {
        val value =
            TomlArray(
                entries =
                    listOf(
                        TomlArray.Entry(
                            prefixFiller =
                                TomlMultilineFiller(
                                    lines = listOf(),
                                    suffixWhitespace = TomlFillerWhitespace(" "),
                                ),
                            value = TomlDecimalInteger(pieces = listOf("123")),
                            suffixPreCommaFiller =
                                TomlMultilineFiller(
                                    lines =
                                        listOf(
                                            TomlLineFiller(
                                                whitespace = TomlFillerWhitespace(" "),
                                                comment = TomlFillerComment("This is a number"),
                                            ),
                                        ),
                                    suffixWhitespace = TomlFillerWhitespace(" "),
                                ),
                            includeComma = true,
                            suffixPostCommaFiller = TomlLineFiller(comment = TomlFillerComment("This is a comma")),
                        ),
                        TomlArray.Entry(
                            prefixFiller =
                                TomlMultilineFiller(
                                    lines =
                                        listOf(
                                            TomlLineFiller(comment = TomlFillerComment("This is a blank line")),
                                        ),
                                    suffixWhitespace = TomlFillerWhitespace.EMPTY,
                                ),
                            value =
                                TomlBasicString(
                                    line =
                                        TomlBasicString.Line(
                                            pieces = listOf(TomlBasicString.StringPiece("Test")),
                                            terminator = TomlBasicString.EndOfString,
                                        ),
                                ),
                        ),
                    ),
            )

        assertSoftly(value) {
            toString() shouldBe "[123, Test]"
            toTomlString() shouldBe "[ 123 #This is a number\n ,#This is a comma\n#This is a blank line\n\"Test\"]"
            toValue() shouldContainOnly listOf(BigInteger.valueOf(123), "Test")
        }
    }

    @Test fun `Entry parse simple value`() {
        val result = TomlArray.Entry.parse("123")
        val expected =
            TomlArray.Entry(
                prefixFiller = TomlMultilineFiller.EMPTY,
                value = TomlDecimalInteger(pieces = listOf("123")),
                suffixPreCommaFiller = TomlMultilineFiller.EMPTY,
                includeComma = false,
                suffixPostCommaFiller = null,
            )

        result shouldBe
            TomlParseResult.Success(
                value = expected,
                remainingBody = "",
            )
    }

    @Test fun `Entry parse simple value with comma`() {
        val result = TomlArray.Entry.parse("123,")
        val expected =
            TomlArray.Entry(
                prefixFiller = TomlMultilineFiller.EMPTY,
                value = TomlDecimalInteger(pieces = listOf("123")),
                suffixPreCommaFiller = TomlMultilineFiller.EMPTY,
                includeComma = true,
                suffixPostCommaFiller = null,
            )

        result shouldBe
            TomlParseResult.Success(
                value = expected,
                remainingBody = "",
            )
    }

    @Test fun `Entry parse spaces`() {
        val result = TomlArray.Entry.parse("\t123 , ")
        val expected =
            TomlArray.Entry(
                prefixFiller =
                    TomlMultilineFiller(
                        lines = listOf(),
                        suffixWhitespace = TomlFillerWhitespace("\t"),
                    ),
                value = TomlDecimalInteger(pieces = listOf("123")),
                suffixPreCommaFiller =
                    TomlMultilineFiller(
                        lines = listOf(),
                        suffixWhitespace = TomlFillerWhitespace(" "),
                    ),
                includeComma = true,
                suffixPostCommaFiller = null,
            )

        result shouldBe
            TomlParseResult.Success(
                value = expected,
                remainingBody = " ",
            )
    }

    @Test fun `Entry parse inline comment`() {
        val result = TomlArray.Entry.parse("\t123 , # Comment")
        val expected =
            TomlArray.Entry(
                prefixFiller =
                    TomlMultilineFiller(
                        lines = listOf(),
                        suffixWhitespace = TomlFillerWhitespace("\t"),
                    ),
                value = TomlDecimalInteger(pieces = listOf("123")),
                suffixPreCommaFiller =
                    TomlMultilineFiller(
                        lines = listOf(),
                        suffixWhitespace = TomlFillerWhitespace(" "),
                    ),
                includeComma = true,
                suffixPostCommaFiller =
                    TomlLineFiller(
                        whitespace = TomlFillerWhitespace(" "),
                        comment = TomlFillerComment(" Comment"),
                        terminator = TomlLineFiller.Terminator.EOF,
                    ),
            )

        result shouldBe
            TomlParseResult.Success(
                value = expected,
                remainingBody = "",
            )
    }

    @Test
    fun `TomlArray parse wrapped values`() {
        val result = TomlArray.parse("[ 123 #This is a number\n ,#This is a comma\n#This is a blank line\n\"Test\"]")
        val expected =
            TomlArray(
                entries =
                    listOf(
                        TomlArray.Entry(
                            prefixFiller =
                                TomlMultilineFiller(
                                    lines = listOf(),
                                    suffixWhitespace = TomlFillerWhitespace(" "),
                                ),
                            value = TomlDecimalInteger(pieces = listOf("123")),
                            suffixPreCommaFiller =
                                TomlMultilineFiller(
                                    lines =
                                        listOf(
                                            TomlLineFiller(
                                                whitespace = TomlFillerWhitespace(" "),
                                                comment = TomlFillerComment("This is a number"),
                                            ),
                                        ),
                                    suffixWhitespace = TomlFillerWhitespace(" "),
                                ),
                            includeComma = true,
                            suffixPostCommaFiller = TomlLineFiller(comment = TomlFillerComment("This is a comma")),
                        ),
                        TomlArray.Entry(
                            prefixFiller =
                                TomlMultilineFiller(
                                    lines =
                                        listOf(
                                            TomlLineFiller(comment = TomlFillerComment("This is a blank line")),
                                        ),
                                    suffixWhitespace = TomlFillerWhitespace.EMPTY,
                                ),
                            value =
                                TomlBasicString(
                                    line =
                                        TomlBasicString.Line(
                                            pieces = listOf(TomlBasicString.StringPiece("Test")),
                                            terminator = TomlBasicString.EndOfString,
                                        ),
                                ),
                        ),
                    ),
            )

        assertSoftly {
            result shouldBe
                TomlParseResult.Success(
                    value = expected,
                    remainingBody = "",
                )
        }
    }
}
