package com.target.tomlformatted.domain.value

import com.target.tomlformatted.datatree.DataTree
import com.target.tomlformatted.domain.TomlFillerWhitespace
import com.target.tomlformatted.domain.TomlKey
import com.target.tomlformatted.domain.TomlKeyPieceLiteral
import com.target.tomlformatted.parse.TomlParseResult
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class TomlInlineTableTest {
    @Test
    fun `empty inline table`() {
        assertSoftly(TomlInlineTable(emptyList())) {
            toValue() shouldBe DataTree()
            toString() shouldBe "{}"
            toTomlString() shouldBe "{}"
        }
    }

    @Test
    fun `pares empty inline table`() {
        assertSoftly {
            TomlInlineTable.parse("{ }") shouldBe
                TomlParseResult.Success(
                    value = TomlInlineTable(entries = emptyList(), prefixFiller = TomlFillerWhitespace(" ")),
                    remainingBody = "",
                )
        }
    }

    @Test
    fun `simple table`() {
        val table =
            TomlInlineTable(
                entries =
                    listOf(
                        TomlInlineTable.Entry(
                            key = TomlKey(pieces = listOf(TomlKeyPieceLiteral("key1"))),
                            value = TomlValue(value = TomlBasicString("Value 1")),
                        ),
                        TomlInlineTable.Entry(
                            key = TomlKey(pieces = listOf(TomlKeyPieceLiteral("key2"))),
                            value = TomlValue(value = TomlBasicString("Value 2")),
                        ),
                    ),
            )

        val tree = DataTree()
        tree.addLeaf(listOf("key1"), "Value 1")
        tree.addLeaf(listOf("key2"), "Value 2")

        assertSoftly(table) {
            toValue() shouldBe tree
            toString() shouldBe "{key1=Value 1,key2=Value 2}"
            toTomlString() shouldBe "{key1=\"Value 1\",key2=\"Value 2\"}"
        }
    }

    @Test
    fun `pares simple table`() {
        assertSoftly {
            TomlInlineTable.parse("{key1=\"Value 1\",key2=\"Value 2\"}") shouldBe
                TomlParseResult.Success(
                    value =
                        TomlInlineTable(
                            entries =
                                listOf(
                                    TomlInlineTable.Entry(
                                        key = TomlKey(pieces = listOf(TomlKeyPieceLiteral("key1"))),
                                        value = TomlValue(value = TomlBasicString("Value 1")),
                                    ),
                                    TomlInlineTable.Entry(
                                        key = TomlKey(pieces = listOf(TomlKeyPieceLiteral("key2"))),
                                        value = TomlValue(value = TomlBasicString("Value 2")),
                                    ),
                                ),
                        ),
                    remainingBody = "",
                )
        }
    }

    @Test
    fun `compound key table`() {
        val table =
            TomlInlineTable(
                entries =
                    listOf(
                        TomlInlineTable.Entry(
                            key = TomlKey(pieces = listOf(TomlKeyPieceLiteral("key1"), TomlKeyPieceLiteral("key2"))),
                            value = TomlValue(value = TomlBasicString("Value 1")),
                        ),
                        TomlInlineTable.Entry(
                            key = TomlKey(pieces = listOf(TomlKeyPieceLiteral("key1"), TomlKeyPieceLiteral("key3"))),
                            value = TomlValue(value = TomlBasicString("Value 2")),
                        ),
                    ),
            )

        val tree = DataTree()
        tree.addLeaf(listOf("key1", "key2"), "Value 1")
        tree.addLeaf(listOf("key1", "key3"), "Value 2")

        assertSoftly(table) {
            toValue() shouldBe tree
            toString() shouldBe "{key1.key2=Value 1,key1.key3=Value 2}"
            toTomlString() shouldBe "{key1.key2=\"Value 1\",key1.key3=\"Value 2\"}"
        }
    }

    @Test
    fun `pares compound key table`() {
        assertSoftly {
            TomlInlineTable.parse("{key1.key2=\"Value 1\",key1.key3=\"Value 2\"}") shouldBe
                TomlParseResult.Success(
                    value =
                        TomlInlineTable(
                            entries =
                                listOf(
                                    TomlInlineTable.Entry(
                                        key = TomlKey(pieces = listOf(TomlKeyPieceLiteral("key1"), TomlKeyPieceLiteral("key2"))),
                                        value = TomlValue(value = TomlBasicString("Value 1")),
                                    ),
                                    TomlInlineTable.Entry(
                                        key = TomlKey(pieces = listOf(TomlKeyPieceLiteral("key1"), TomlKeyPieceLiteral("key3"))),
                                        value = TomlValue(value = TomlBasicString("Value 2")),
                                    ),
                                ),
                        ),
                    remainingBody = "",
                )
        }
    }

    @Test
    fun `nested table`() {
        val nestedTable =
            TomlInlineTable(
                entries =
                    listOf(
                        TomlInlineTable.Entry(
                            key = TomlKey(pieces = listOf(TomlKeyPieceLiteral("key3"))),
                            value = TomlValue(value = TomlBasicString("Nested Value")),
                        ),
                    ),
            )

        val table =
            TomlInlineTable(
                entries =
                    listOf(
                        TomlInlineTable.Entry(
                            key = TomlKey(pieces = listOf(TomlKeyPieceLiteral("key1"))),
                            value = TomlValue(value = TomlBasicString("Value 1")),
                        ),
                        TomlInlineTable.Entry(
                            key = TomlKey(pieces = listOf(TomlKeyPieceLiteral("key2"))),
                            value = TomlValue(value = nestedTable),
                        ),
                    ),
            )

        val tree = DataTree()
        tree.addLeaf(listOf("key1"), "Value 1")
        tree.addLeaf(listOf("key2", "key3"), "Nested Value")

        assertSoftly(table) {
            toValue() shouldBe tree
            toString() shouldBe "{key1=Value 1,key2={key3=Nested Value}}"
            toTomlString() shouldBe "{key1=\"Value 1\",key2={key3=\"Nested Value\"}}"
        }
    }

    @Test
    fun `pares nested table`() {
        assertSoftly {
            TomlInlineTable.parse("{key1=\"Value 1\",key2={key3=\"Nested Value\"}}") shouldBe
                TomlParseResult.Success(
                    value =
                        TomlInlineTable(
                            entries =
                                listOf(
                                    TomlInlineTable.Entry(
                                        key = TomlKey(pieces = listOf(TomlKeyPieceLiteral("key1"))),
                                        value = TomlValue(value = TomlBasicString("Value 1")),
                                    ),
                                    TomlInlineTable.Entry(
                                        key = TomlKey(pieces = listOf(TomlKeyPieceLiteral("key2"))),
                                        value =
                                            TomlValue(
                                                value =
                                                    TomlInlineTable(
                                                        entries =
                                                            listOf(
                                                                TomlInlineTable.Entry(
                                                                    key = TomlKey(pieces = listOf(TomlKeyPieceLiteral("key3"))),
                                                                    value = TomlValue(value = TomlBasicString("Nested Value")),
                                                                ),
                                                            ),
                                                    ),
                                            ),
                                    ),
                                ),
                        ),
                    remainingBody = "",
                )
        }
    }
}
