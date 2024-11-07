package com.target.tomlformatted.domain

import com.target.tomlformatted.domain.value.TomlBasicString
import com.target.tomlformatted.domain.value.TomlLiteralString
import com.target.tomlformatted.parse.TomlParseResult
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class TomlKeyTest {
    @Test
    fun `TomlKey single key no filler`() {
        val result = TomlKey(listOf(TomlKeyPieceLiteral("key")))

        result.toKeys() shouldBe listOf("key")
        result.toTomlString() shouldBe "key"
        result.toString() shouldBe "key"
    }

    @Test
    fun `TomlKey multi key with filler`() {
        val result =
            TomlKey(
                listOf(
                    TomlKeyPieceLiteral("key", prefixFiller = TomlFillerWhitespace("\t"), suffixFiller = TomlFillerWhitespace(" ")),
                    TomlKeyPieceString(
                        value =
                            TomlBasicString(
                                TomlBasicString.Line(
                                    pieces = listOf(TomlBasicString.StringPiece(" key2 ")),
                                    terminator = TomlBasicString.EndOfString,
                                ),
                            ),
                        prefixFiller = TomlFillerWhitespace(" "),
                        suffixFiller = TomlFillerWhitespace("  "),
                    ),
                    TomlKeyPieceString(
                        value = TomlLiteralString("key3"),
                        prefixFiller = TomlFillerWhitespace(" "),
                        suffixFiller = TomlFillerWhitespace(" "),
                    ),
                ),
            )

        result.toKeys() shouldBe listOf("key", " key2 ", "key3")
        result.toTomlString() shouldBe "\tkey . \" key2 \"  . 'key3' "
        result.toString() shouldBe "key. key2 .key3"
    }

    @Test
    fun `TomlKey Parse`() {
        assertSoftly {
            TomlKey.parse("key") shouldBe TomlParseResult.Success(value = TomlKey(listOf(TomlKeyPieceLiteral("key"))), remainingBody = "")
            TomlKey.parse(
                "key.key2",
            ) shouldBe
                TomlParseResult.Success(
                    value = TomlKey(listOf(TomlKeyPieceLiteral("key"), TomlKeyPieceLiteral("key2"))),
                    remainingBody = "",
                )
            TomlKey.parse(
                "key.'key 2'",
            ) shouldBe
                TomlParseResult.Success(
                    value = TomlKey(listOf(TomlKeyPieceLiteral("key"), TomlKeyPieceString(TomlLiteralString("key 2")))),
                    remainingBody = "",
                )
            TomlKey.parse(
                "\tkey . 'key 2' Rest of Body",
            ) shouldBe
                TomlParseResult.Success(
                    value =
                        TomlKey(
                            listOf(
                                TomlKeyPieceLiteral(
                                    prefixFiller = TomlFillerWhitespace("\t"),
                                    value = "key",
                                    suffixFiller = TomlFillerWhitespace(" "),
                                ),
                                TomlKeyPieceString(
                                    prefixFiller = TomlFillerWhitespace(" "),
                                    value = TomlLiteralString("key 2"),
                                    suffixFiller = TomlFillerWhitespace(" "),
                                ),
                            ),
                        ),
                    remainingBody = "Rest of Body",
                )
        }
    }
}
