package com.target.tomlformatted.domain.value

import com.target.tomlformatted.domain.TomlKey
import com.target.tomlformatted.domain.TomlKeyPieceLiteral
import com.target.tomlformatted.parse.TomlParseResult
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class TomlValueTest {
    @Test
    fun `Parse ITomlValue`() {
        assertSoftly {
            ITomlValue.parse("true Rest of body") shouldBe
                TomlParseResult.Success(
                    TomlBoolean.TRUE,
                    " Rest of body",
                )

            ITomlValue.parse("1979-05-27T14:32:00.123+07:00 Rest of body") shouldBe
                TomlParseResult.Success(
                    value = TomlOffsetDateTime("1979-05-27T14:32:00.123+07:00"),
                    remainingBody = " Rest of body",
                )

            ITomlValue.parse("1234 Rest of body") shouldBe
                TomlParseResult.Success(
                    value = TomlDecimalInteger(sign = Sign.UNSIGNED, listOf("1234"), forbidLeadingZero = true),
                    remainingBody = " Rest of body",
                )

            ITomlValue.parse("'Any kind of string' Rest of body") shouldBe
                TomlParseResult.Success(
                    TomlLiteralString("Any kind of string"),
                    " Rest of body",
                )

            ITomlValue.parse("{key='value'} Rest of body") shouldBe
                TomlParseResult.Success(
                    TomlInlineTable(
                        entries =
                            listOf(
                                TomlInlineTable.Entry(
                                    TomlKey(listOf(TomlKeyPieceLiteral("key"))),
                                    TomlValue(value = TomlLiteralString("value")),
                                ),
                            ),
                    ),
                    " Rest of body",
                )

            ITomlValue.parse("['value'] Rest of body") shouldBe
                TomlParseResult.Success(
                    TomlArray(entries = listOf(TomlArray.Entry(value = TomlLiteralString("value")))),
                    " Rest of body",
                )
        }
    }
}
