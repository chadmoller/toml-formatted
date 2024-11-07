package com.target.tomlformatted.domain

import com.target.tomlformatted.datatree.DataTree
import com.target.tomlformatted.domain.value.TomlBasicString
import com.target.tomlformatted.domain.value.TomlValue
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class TomlFileTest {
    @Test
    fun `empty file`() {
        val file =
            TomlFile(
                rootTable =
                    TomlRootTable(
                        lines = emptyList(),
                    ),
                tables = emptyList(),
            )

        assertSoftly(file) {
            toMap() shouldBe emptyMap()
            toTomlString() shouldBe ""
            toString() shouldBe "{\n}\n"
        }
    }

    @Test
    fun `root table only`() {
        val file =
            TomlFile(
                rootTable =
                    TomlRootTable(
                        lines =
                            listOf(
                                TomlTableLine(
                                    key =
                                        TomlKey(
                                            pieces = listOf(TomlKeyPieceLiteral(value = "key1")),
                                        ),
                                    value = TomlValue(value = TomlBasicString("Value 1")),
                                ),
                            ),
                    ),
                tables = emptyList(),
            )

        assertSoftly(file) {
            toMap() shouldBe mapOf("key1" to "Value 1")
            toTomlString() shouldBe "key1=\"Value 1\"\n"
            toString() shouldBe "{\n\tkey1=Value 1\n}\n"
        }
    }

    @Test
    fun `no root table`() {
        val file =
            TomlFile(
                rootTable =
                    TomlRootTable(
                        lines = emptyList(),
                    ),
                tables =
                    listOf(
                        TomlTable(
                            key =
                                TomlTableKey(
                                    key =
                                        TomlKey(
                                            pieces =
                                                listOf(
                                                    TomlKeyPieceLiteral(value = "tableKey1"),
                                                    TomlKeyPieceLiteral(value = "tableKey2"),
                                                ),
                                        ),
                                ),
                            lines =
                                listOf(
                                    TomlTableLine(
                                        key =
                                            TomlKey(
                                                pieces = listOf(TomlKeyPieceLiteral(value = "key1")),
                                            ),
                                        value = TomlValue(value = TomlBasicString("Value 1")),
                                    ),
                                ),
                        ),
                    ),
            )

        val tree = DataTree()
        tree.addLeaf(listOf("tableKey1", "tableKey2", "key1"), "Value 1")

        assertSoftly(file) {
            toTree() shouldBe tree
            toTomlString() shouldBe "[tableKey1.tableKey2]\nkey1=\"Value 1\"\n"
            toString() shouldBe "{\n}\ntableKey1.tableKey2{\n\tkey1=Value 1\n}"
        }
    }
}
