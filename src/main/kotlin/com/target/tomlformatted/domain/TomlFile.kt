package com.target.tomlformatted.domain

import com.target.tomlformatted.datatree.DataTree

data class TomlFile(
    val rootTable: TomlRootTable,
    val tables: List<TomlTable>,
    val suffixFiller: TomlFiller = TomlFillerWhitespace.EMPTY,
) : TomlPiece {
    override fun toTomlString(): String = "${rootTable.toTomlString()}${tables.joinToString("\n") { it.toTomlString() }}"

    override fun toString(): String = "$rootTable\n${tables.joinToString("\n")}"

    fun toTree(): DataTree {
        val tree = rootTable.toTree()
        tables.forEach { table ->
            tree.addLeaf(table.tableKeys(), table.toTree())
        }
        return tree
    }

    fun toMap(): Map<String, Any?> = toTree().toMap()
}
