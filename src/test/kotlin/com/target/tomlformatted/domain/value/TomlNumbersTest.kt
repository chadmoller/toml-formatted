package com.target.tomlformatted.domain.value

import com.target.tomlformatted.parse.TomlParseResult
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal
import java.math.BigInteger
import java.util.stream.Stream

class TomlNumbersTest {
    companion object {
        @JvmStatic
        fun provideTomlNumbersParse(): Stream<Arguments> {
            return Stream.of(
                Arguments.of("123_456 Other Stuff", TomlDecimalInteger(pieces = listOf("123", "456"))),
                Arguments.of("0x89af", TomlNonDecimalInteger(base = TomlNonDecimalInteger.Base.HEX, pieces = listOf("89af"))),
                Arguments.of(
                    "123_456.0789 Other Stuff",
                    TomlFloat(
                        intPart = TomlDecimalInteger(pieces = listOf("123", "456")),
                        fractionPart = TomlDecimalInteger(pieces = listOf("0789"), forbidLeadingZero = false),
                    ),
                ),
                Arguments.of("inf Other Stuff", TomlInfinity(Sign.UNSIGNED)),
                Arguments.of("-nan Other Stuff", TomlNan(Sign.NEGATIVE)),
            )
        }
    }

    @ParameterizedTest
    @MethodSource("provideTomlNumbersParse")
    fun `TomlNumber parsing`(
        body: String,
        value: ITomlNumber<*>,
    ) {
        val result = ITomlNumber.parse(body)
        result.shouldBeInstanceOf<TomlParseResult.Success<*>>()
        result.value shouldBe value
    }

    @Test
    fun `TomlDecimalInteger no parts`() {
        shouldThrow<IllegalArgumentException> {
            TomlDecimalInteger(sign = Sign.UNSIGNED, emptyList())
        }
    }

    @Test
    fun `TomlDecimalInteger leading zeros`() {
        shouldThrow<IllegalArgumentException> {
            TomlDecimalInteger(sign = Sign.UNSIGNED, listOf("0123"))
        }

        shouldThrow<IllegalArgumentException> {
            TomlDecimalInteger(sign = Sign.UNSIGNED, listOf("0", "123"))
        }
    }

    @Test
    fun `TomlDecimalInteger non-digit values`() {
        shouldThrow<IllegalArgumentException> {
            TomlDecimalInteger(pieces = listOf("123", "4z6", "789"))
        }
    }

    @Test
    fun `TomlDecimalInteger leading zeros allowed`() {
        val value = TomlDecimalInteger(sign = Sign.UNSIGNED, listOf("0", "123"), forbidLeadingZero = false)

        assertSoftly(value) {
            toString() shouldBe "0123"
            toTomlString() shouldBe "0_123"
            toBigInteger() shouldBe BigInteger("123")
        }
    }

    @Test
    fun `TomlDecimalInteger simple zero`() {
        val value = TomlDecimalInteger(sign = Sign.UNSIGNED, listOf("0"))

        assertSoftly(value) {
            toString() shouldBe "0"
            toTomlString() shouldBe "0"
            toBigInteger() shouldBe BigInteger.ZERO
        }
    }

    @Test
    fun `TomlDecimalInteger unsigned multi-part number`() {
        val value = TomlDecimalInteger(sign = Sign.UNSIGNED, listOf("123", "456"))

        assertSoftly(value) {
            toString() shouldBe "123456"
            toTomlString() shouldBe "123_456"
            toBigInteger() shouldBe BigInteger("123456")
        }
    }

    @Test
    fun `TomlDecimalInteger positive multi-part number`() {
        val value = TomlDecimalInteger(sign = Sign.POSITIVE, listOf("123", "456"))

        assertSoftly(value) {
            toString() shouldBe "+123456"
            toTomlString() shouldBe "+123_456"
            toBigInteger() shouldBe BigInteger("123456")
        }
    }

    @Test
    fun `TomlDecimalInteger negative multi-part number`() {
        val value = TomlDecimalInteger(sign = Sign.NEGATIVE, listOf("123", "456"))

        assertSoftly(value) {
            toString() shouldBe "-123456"
            toTomlString() shouldBe "-123_456"
            toBigInteger() shouldBe BigInteger("-123456")
        }
    }

    @Test
    fun `Parse TomlDecimalInteger unsigned single piece`() {
        assertSoftly(TomlDecimalInteger.parse("1234 otherText")) {
            this.shouldBeInstanceOf<TomlParseResult.Success<TomlDecimalInteger>>()
            value shouldBe TomlDecimalInteger(sign = Sign.UNSIGNED, pieces = listOf("1234"))
            remainingBody shouldBe " otherText"
        }
    }

    @Test
    fun `Parse TomlDecimalInteger positive single piece`() {
        assertSoftly(TomlDecimalInteger.parse("+1234 otherText")) {
            this.shouldBeInstanceOf<TomlParseResult.Success<TomlDecimalInteger>>()
            value shouldBe TomlDecimalInteger(sign = Sign.POSITIVE, pieces = listOf("1234"))
            remainingBody shouldBe " otherText"
        }
    }

    @Test
    fun `Parse TomlDecimalInteger negative single piece`() {
        assertSoftly(TomlDecimalInteger.parse("-1234 otherText")) {
            this.shouldBeInstanceOf<TomlParseResult.Success<TomlDecimalInteger>>()
            value shouldBe TomlDecimalInteger(sign = Sign.NEGATIVE, pieces = listOf("1234"))
            remainingBody shouldBe " otherText"
        }
    }

    @Test
    fun `Parse TomlDecimalInteger multiple pieces`() {
        assertSoftly(TomlDecimalInteger.parse("12_34_567 otherText")) {
            this.shouldBeInstanceOf<TomlParseResult.Success<TomlDecimalInteger>>()
            value shouldBe TomlDecimalInteger(sign = Sign.UNSIGNED, pieces = listOf("12", "34", "567"))
            remainingBody shouldBe " otherText"
        }
    }

    @Test
    fun `Parse TomlDecimalInteger invalid pieces`() {
        assertSoftly(TomlDecimalInteger.parse("12a34 otherText")) {
            this.shouldBeInstanceOf<TomlParseResult.Failure<TomlDecimalInteger>>()
        }
    }

    @Test
    fun `Parse TomlNonDecimalInteger no valid base`() {
        assertSoftly(TomlNonDecimalInteger.parse("0y100 otherText")) {
            this.shouldBeInstanceOf<TomlParseResult.Failure<TomlNonDecimalInteger>>()
        }
    }

    @Test
    fun `TomlNonDecimalInteger Hexadecimal illegal characters`() {
        shouldThrow<IllegalArgumentException> {
            TomlNonDecimalInteger(listOf("abc", "1z2", "09f"), TomlNonDecimalInteger.Base.HEX)
        }
    }

    @Test
    fun `TomlNonDecimalInteger Hexadecimal number`() {
        val value = TomlNonDecimalInteger(listOf("be", "ef"), TomlNonDecimalInteger.Base.HEX)

        assertSoftly(value) {
            toString() shouldBe "beef"
            toTomlString() shouldBe "0xbe_ef"
            toBigInteger() shouldBe BigInteger("48879")
        }
    }

    @Test
    fun `Parse TomlNonDecimalInteger Hexadecimal single piece`() {
        assertSoftly(TomlNonDecimalInteger.parse("0xab1c otherText")) {
            this.shouldBeInstanceOf<TomlParseResult.Success<TomlDecimalInteger>>()
            value shouldBe TomlNonDecimalInteger(base = TomlNonDecimalInteger.Base.HEX, pieces = listOf("ab1c"))
            remainingBody shouldBe " otherText"
        }
    }

    @Test
    fun `Parse TomlNonDecimalInteger Hexadecimal multiple pieces`() {
        assertSoftly(TomlNonDecimalInteger.parse("0xa1b_def otherText")) {
            this.shouldBeInstanceOf<TomlParseResult.Success<TomlDecimalInteger>>()
            value shouldBe TomlNonDecimalInteger(base = TomlNonDecimalInteger.Base.HEX, pieces = listOf("a1b", "def"))
            remainingBody shouldBe " otherText"
        }
    }

    @Test
    fun `Parse TomlNonDecimalInteger Hexadecimal invalid pieces`() {
        assertSoftly(TomlNonDecimalInteger.parse("0x1ag3 otherText")) {
            this.shouldBeInstanceOf<TomlParseResult.Failure<TomlNonDecimalInteger>>()
        }
    }

    @Test
    fun `TomlNonDecimalInteger Octal illegal characters`() {
        shouldThrow<IllegalArgumentException> {
            TomlNonDecimalInteger(listOf("012", "3x4", "567"), TomlNonDecimalInteger.Base.OCTAL)
        }
    }

    @Test
    fun `TomlNonDecimalInteger Octal number`() {
        val value = TomlNonDecimalInteger(listOf("012", "345"), TomlNonDecimalInteger.Base.OCTAL)

        assertSoftly(value) {
            toString() shouldBe "012345"
            toTomlString() shouldBe "0o012_345"
            toBigInteger() shouldBe BigInteger("5349")
        }
    }

    @Test
    fun `Parse TomlNonDecimalInteger Octal single piece`() {
        assertSoftly(TomlNonDecimalInteger.parse("0o123 otherText")) {
            this.shouldBeInstanceOf<TomlParseResult.Success<TomlDecimalInteger>>()
            value shouldBe TomlNonDecimalInteger(base = TomlNonDecimalInteger.Base.OCTAL, pieces = listOf("123"))
            remainingBody shouldBe " otherText"
        }
    }

    @Test
    fun `Parse TomlNonDecimalInteger Octal multiple pieces`() {
        assertSoftly(TomlNonDecimalInteger.parse("0o123_456 otherText")) {
            this.shouldBeInstanceOf<TomlParseResult.Success<TomlDecimalInteger>>()
            value shouldBe TomlNonDecimalInteger(base = TomlNonDecimalInteger.Base.OCTAL, pieces = listOf("123", "456"))
            remainingBody shouldBe " otherText"
        }
    }

    @Test
    fun `Parse TomlNonDecimalInteger Octal invalid pieces`() {
        assertSoftly(TomlNonDecimalInteger.parse("0o192 otherText")) {
            this.shouldBeInstanceOf<TomlParseResult.Failure<TomlNonDecimalInteger>>()
        }
    }

    @Test
    fun `TomlNonDecimalInteger Binary illegal characters`() {
        shouldThrow<IllegalArgumentException> {
            TomlNonDecimalInteger(listOf("010", "0x0", "101"), TomlNonDecimalInteger.Base.BINARY)
        }
    }

    @Test
    fun `TomlNonDecimalInteger Binary number`() {
        val value = TomlNonDecimalInteger(listOf("010", "101"), TomlNonDecimalInteger.Base.BINARY)

        assertSoftly(value) {
            toString() shouldBe "010101"
            toTomlString() shouldBe "0b010_101"
            toBigInteger() shouldBe BigInteger("21")
        }
    }

    @Test
    fun `Parse TomlNonDecimalInteger Binary invalid pieces`() {
        assertSoftly(TomlNonDecimalInteger.parse("0b121 otherText")) {
            this.shouldBeInstanceOf<TomlParseResult.Failure<TomlNonDecimalInteger>>()
        }
    }

    @Test
    fun `Parse TomlNonDecimalInteger Binary single piece`() {
        assertSoftly(TomlNonDecimalInteger.parse("0b101 otherText")) {
            this.shouldBeInstanceOf<TomlParseResult.Success<TomlDecimalInteger>>()
            value shouldBe TomlNonDecimalInteger(base = TomlNonDecimalInteger.Base.BINARY, pieces = listOf("101"))
            remainingBody shouldBe " otherText"
        }
    }

    @Test
    fun `Parse TomlNonDecimalInteger Binary multiple pieces`() {
        assertSoftly(TomlNonDecimalInteger.parse("0b101_010 otherText")) {
            this.shouldBeInstanceOf<TomlParseResult.Success<TomlDecimalInteger>>()
            value shouldBe TomlNonDecimalInteger(base = TomlNonDecimalInteger.Base.BINARY, pieces = listOf("101", "010"))
            remainingBody shouldBe " otherText"
        }
    }

    @Test
    fun `TomlFloat Fraction part is signed`() {
        shouldThrow<IllegalArgumentException> {
            TomlFloat(
                intPart = TomlDecimalInteger(pieces = listOf("123")),
                fractionPart = TomlDecimalInteger(sign = Sign.NEGATIVE, pieces = listOf("456"), forbidLeadingZero = false),
            )
        }
    }

    @Test
    fun `TomlFloat Fraction and Exponent`() {
        val value =
            TomlFloat(
                intPart = TomlDecimalInteger(pieces = listOf("123", "456")),
                fractionPart = TomlDecimalInteger(pieces = listOf("789"), forbidLeadingZero = false),
                exponent =
                    TomlFloat.Exponent(
                        power = TomlDecimalInteger(sign = Sign.NEGATIVE, pieces = listOf("02"), forbidLeadingZero = false),
                        marker = TomlFloat.ExponentMarker.LOWER_E,
                    ),
            )

        assertSoftly(value) {
            toString() shouldBe "123456.789E-02"
            toTomlString() shouldBe "123_456.789e-02"
            toBigDecimal() shouldBe BigDecimal("1234.56789")
        }
    }

    @Test
    fun `TomlFloat Fraction`() {
        val value =
            TomlFloat(
                intPart = TomlDecimalInteger(pieces = listOf("123", "456")),
                fractionPart = TomlDecimalInteger(pieces = listOf("789"), forbidLeadingZero = false),
            )

        assertSoftly(value) {
            toString() shouldBe "123456.789"
            toTomlString() shouldBe "123_456.789"
            toBigDecimal() shouldBe BigDecimal("123456.789")
        }
    }

    @Test
    fun `TomlFloat Exponent`() {
        val value =
            TomlFloat(
                intPart = TomlDecimalInteger(pieces = listOf("123", "456")),
                exponent =
                    TomlFloat.Exponent(
                        power = TomlDecimalInteger(sign = Sign.NEGATIVE, pieces = listOf("02"), forbidLeadingZero = false),
                        marker = TomlFloat.ExponentMarker.LOWER_E,
                    ),
            )

        assertSoftly(value) {
            toString() shouldBe "123456E-02"
            toTomlString() shouldBe "123_456e-02"
            toBigDecimal() shouldBe BigDecimal("1234.56")
        }
    }

    @Test
    fun `TomlFloat Int`() {
        val value =
            TomlFloat(
                intPart = TomlDecimalInteger(pieces = listOf("123", "456")),
            )

        assertSoftly(value) {
            toString() shouldBe "123456"
            toTomlString() shouldBe "123_456"
            toBigDecimal() shouldBe BigDecimal("123456")
        }
    }

    @Test
    fun `Parse TomlFloat Fraction and Exponent`() {
        val expectedValue =
            TomlFloat(
                intPart = TomlDecimalInteger(pieces = listOf("123", "456")),
                fractionPart = TomlDecimalInteger(pieces = listOf("789"), forbidLeadingZero = false),
                exponent =
                    TomlFloat.Exponent(
                        power = TomlDecimalInteger(sign = Sign.NEGATIVE, pieces = listOf("02"), forbidLeadingZero = false),
                        marker = TomlFloat.ExponentMarker.LOWER_E,
                    ),
            )

        assertSoftly(TomlFloat.parse("123_456.789e-02 Other text")) {
            shouldBeInstanceOf<TomlParseResult.Success<TomlFloat>>()
            value shouldBe expectedValue
            remainingBody shouldBe " Other text"
        }
    }

    @Test
    fun `Parse TomlFloat Fraction`() {
        val expectedValue =
            TomlFloat(
                intPart = TomlDecimalInteger(pieces = listOf("123", "456"), sign = Sign.NEGATIVE),
                fractionPart = TomlDecimalInteger(pieces = listOf("789"), forbidLeadingZero = false),
            )

        assertSoftly(TomlFloat.parse("-123_456.789 Other text")) {
            shouldBeInstanceOf<TomlParseResult.Success<TomlFloat>>()
            value shouldBe expectedValue
            remainingBody shouldBe " Other text"
        }
    }

    @Test
    fun `Parse TomlFloat Exponent`() {
        val expectedValue =
            TomlFloat(
                intPart = TomlDecimalInteger(pieces = listOf("123", "456")),
                exponent =
                    TomlFloat.Exponent(
                        power = TomlDecimalInteger(pieces = listOf("2"), forbidLeadingZero = false),
                        marker = TomlFloat.ExponentMarker.E,
                    ),
            )

        assertSoftly(TomlFloat.parse("123_456E2 Other text")) {
            shouldBeInstanceOf<TomlParseResult.Success<TomlFloat>>()
            value shouldBe expectedValue
            remainingBody shouldBe " Other text"
        }
    }

    @Test
    fun `Parse TomlFloat Decimal without number after`() {
        assertSoftly(TomlFloat.parse("123_456. Other text")) {
            shouldBeInstanceOf<TomlParseResult.Failure<TomlFloat>>()
        }
    }

    @Test
    fun `Parse TomlFloat exponent without number after`() {
        assertSoftly(TomlFloat.parse("123_456ea Other text")) {
            shouldBeInstanceOf<TomlParseResult.Failure<TomlFloat>>()
        }
    }

    @Test
    fun `TomlInfinity Unsigned`() {
        val value =
            TomlInfinity(
                sign = Sign.UNSIGNED,
            )

        assertSoftly(value) {
            toString() shouldBe "∞"
            toTomlString() shouldBe "inf"
            toDouble() shouldBe Double.POSITIVE_INFINITY
        }
    }

    @Test
    fun `TomlInfinity Positive`() {
        val value =
            TomlInfinity(
                sign = Sign.POSITIVE,
            )

        assertSoftly(value) {
            toString() shouldBe "+∞"
            toTomlString() shouldBe "+inf"
            toDouble() shouldBe Double.POSITIVE_INFINITY
        }
    }

    @Test
    fun `TomlInfinity Negative`() {
        val value =
            TomlInfinity(
                sign = Sign.NEGATIVE,
            )

        assertSoftly(value) {
            toString() shouldBe "-∞"
            toTomlString() shouldBe "-inf"
            toDouble() shouldBe Double.NEGATIVE_INFINITY
        }
    }

    @Test
    fun `Parse TomlInfinity Unsigned`() {
        assertSoftly(TomlInfinity.parse("inf Other text")) {
            shouldBeInstanceOf<TomlParseResult.Success<TomlInfinity>>()
            value shouldBe TomlInfinity()
            remainingBody shouldBe " Other text"
        }
    }

    @Test
    fun `Parse TomlInfinity Signed`() {
        assertSoftly(TomlInfinity.parse("-inf Other text")) {
            shouldBeInstanceOf<TomlParseResult.Success<TomlInfinity>>()
            value shouldBe TomlInfinity(Sign.NEGATIVE)
            remainingBody shouldBe " Other text"
        }
    }

    @Test
    fun `Parse TomlInfinity Failed`() {
        assertSoftly(TomlInfinity.parse("ing Other text")) {
            shouldBeInstanceOf<TomlParseResult.Failure<TomlInfinity>>()
        }
    }

    @Test
    fun `TomlNan Unsigned`() {
        val value =
            TomlNan(
                sign = Sign.UNSIGNED,
            )

        assertSoftly(value) {
            toString() shouldBe "NaN"
            toTomlString() shouldBe "nan"
            toDouble() shouldBe Double.NaN
        }
    }

    @Test
    fun `TomlNan Positive`() {
        val value =
            TomlNan(
                sign = Sign.POSITIVE,
            )

        assertSoftly(value) {
            toString() shouldBe "NaN"
            toTomlString() shouldBe "+nan"
            toDouble() shouldBe Double.NaN
        }
    }

    @Test
    fun `TomlNan Negative`() {
        val value =
            TomlNan(
                sign = Sign.NEGATIVE,
            )

        assertSoftly(value) {
            toString() shouldBe "NaN"
            toTomlString() shouldBe "-nan"
            toDouble() shouldBe Double.NaN
        }
    }

    @Test
    fun `Parse TomlNan Unsigned`() {
        assertSoftly(TomlNan.parse("nan Other text")) {
            shouldBeInstanceOf<TomlParseResult.Success<TomlNan>>()
            value shouldBe TomlNan()
            remainingBody shouldBe " Other text"
        }
    }

    @Test
    fun `Parse TomlNan Signed`() {
        assertSoftly(TomlNan.parse("-nan Other text")) {
            shouldBeInstanceOf<TomlParseResult.Success<TomlNan>>()
            value shouldBe TomlNan(Sign.NEGATIVE)
            remainingBody shouldBe " Other text"
        }
    }

    @Test
    fun `Parse TomlNan Failed`() {
        assertSoftly(TomlNan.parse("pan Other text")) {
            shouldBeInstanceOf<TomlParseResult.Failure<TomlNan>>()
        }
    }
}
