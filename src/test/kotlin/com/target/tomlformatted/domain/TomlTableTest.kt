package com.target.tomlformatted.domain

import com.target.tomlformatted.datatree.DataTree
import com.target.tomlformatted.domain.value.TomlBasicString
import com.target.tomlformatted.domain.value.TomlValue
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class TomlTableTest {
    private val table1 =
        TomlTable(
            key =
                TomlTableKey(
                    TomlKey(
                        listOf(
                            TomlKeyPieceLiteral("key1"),
                            TomlKeyPieceLiteral("key2", prefixFiller = TomlFillerWhitespace(" ")),
                        ),
                    ),
                ),
            lines =
                listOf(
                    TomlTableLine(
                        key =
                            TomlKey(
                                listOf(
                                    TomlKeyPieceLiteral(
                                        prefixFiller = TomlFillerWhitespace("\t"),
                                        value = "key3",
                                        suffixFiller = TomlFillerWhitespace(" "),
                                    ),
                                ),
                            ),
                        value = TomlValue(prefixFiller = TomlFillerWhitespace(" "), value = TomlBasicString("Hello")),
                    ),
                    TomlTableLine(
                        prefixFiller =
                            TomlMultilineFiller(
                                lines =
                                    listOf(
                                        TomlLineFiller(
                                            whitespace = TomlFillerWhitespace("\t"),
                                            comment = TomlFillerComment(comment = "Comment"),
                                        ),
                                    ),
                                suffixWhitespace = TomlFillerWhitespace.EMPTY,
                            ),
                        key =
                            TomlKey(
                                listOf(
                                    TomlKeyPieceLiteral(
                                        prefixFiller = TomlFillerWhitespace("\t"),
                                        value = "key4",
                                        suffixFiller = TomlFillerWhitespace(" "),
                                    ),
                                ),
                            ),
                        value = TomlValue(prefixFiller = TomlFillerWhitespace(" "), value = TomlBasicString("World")),
                    ),
                ),
        )

    private val rootTable =
        TomlRootTable(
            lines =
                listOf(
                    TomlTableLine(
                        key =
                            TomlKey(
                                listOf(
                                    TomlKeyPieceLiteral(
                                        value = "key3",
                                        suffixFiller = TomlFillerWhitespace(" "),
                                    ),
                                ),
                            ),
                        value = TomlValue(prefixFiller = TomlFillerWhitespace(" "), value = TomlBasicString("Hello")),
                    ),
                    TomlTableLine(
                        prefixFiller =
                            TomlMultilineFiller(
                                lines =
                                    listOf(
                                        TomlLineFiller(
                                            comment = TomlFillerComment(comment = "Comment"),
                                        ),
                                    ),
                                suffixWhitespace = TomlFillerWhitespace.EMPTY,
                            ),
                        key =
                            TomlKey(
                                listOf(
                                    TomlKeyPieceLiteral(
                                        value = "key4",
                                        suffixFiller = TomlFillerWhitespace(" "),
                                    ),
                                ),
                            ),
                        value = TomlValue(prefixFiller = TomlFillerWhitespace(" "), value = TomlBasicString("World")),
                    ),
                ),
        )

    @Test
    fun `TomlTable Key strings`() {
        val key =
            TomlTableKey(
                TomlKey(
                    listOf(
                        TomlKeyPieceLiteral("key"),
                        TomlKeyPieceLiteral("key2", prefixFiller = TomlFillerWhitespace(" ")),
                    ),
                ),
            )

        assertSoftly(key) {
            toTomlString() shouldBe "[key. key2]"
            toString() shouldBe "key.key2"
        }
    }

    @Test
    fun `empty TomlTable strings`() {
        val tableKey =
            TomlTableKey(
                TomlKey(
                    listOf(
                        TomlKeyPieceLiteral("key1"),
                        TomlKeyPieceLiteral("key2", prefixFiller = TomlFillerWhitespace(" ")),
                    ),
                ),
            )

        assertSoftly(
            TomlTable(
                key = tableKey,
                lines = emptyList(),
            ),
        ) {
            toTomlString() shouldBe "[key1. key2]\n"
            toString() shouldBe "key1.key2{\n}"
        }
    }

    @Test
    fun `TomlRootTable strings`() {
        assertSoftly(rootTable) {
            toTomlString() shouldBe "key3 = \"Hello\"\n#Comment\nkey4 = \"World\"\n"
            toString() shouldBe "{\n" +
                "\tkey3=Hello\n" +
                "\tkey4=World\n" +
                "}"
        }
    }

    @Test
    fun `TomlRootTable tree & map`() {
        val tree = DataTree()
        tree.addLeaf(listOf("key3"), "Hello")
        tree.addLeaf(listOf("key4"), "World")

        assertSoftly(rootTable) {
            toTree() shouldBe tree
            toMap() shouldBe
                mapOf(
                    "key3" to "Hello",
                    "key4" to "World",
                )
        }
    }

    @Test
    fun `TomlTable strings`() {
        assertSoftly(table1) {
            toTomlString() shouldBe "[key1. key2]\n\tkey3 = \"Hello\"\n\t#Comment\n\tkey4 = \"World\"\n"
            toString() shouldBe "key1.key2{\n" +
                "\tkey3=Hello\n" +
                "\tkey4=World\n" +
                "}"
        }
    }

    @Test
    fun `TomlTable tree & map`() {
        val tree = DataTree()
        tree.addLeaf(listOf("key3"), "Hello")
        tree.addLeaf(listOf("key4"), "World")

        assertSoftly(table1) {
            toTree() shouldBe tree
            toMap() shouldBe
                mapOf(
                    "key3" to "Hello",
                    "key4" to "World",
                )
        }
    }
}
