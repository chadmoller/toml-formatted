package com.target.tomlformatted.datatree

import arrow.core.Either
import com.target.tomlformatted.datatree.DataTree.BodyNode
import com.target.tomlformatted.datatree.DataTree.LeafNode

class DataTree(
    val children: MutableMap<String, Node> = mutableMapOf(),
) {
    sealed class Node {
        abstract val key: String
        abstract val parentNode: Either<BodyNode, DataTree>

        fun path(): List<String> =
            parentNode.fold(
                ifLeft = { it.path() + key },
                ifRight = { listOf(key) },
            )
    }

    class BodyNode(
        override val key: String,
        override val parentNode: Either<BodyNode, DataTree>,
        val children: MutableMap<String, Node> = mutableMapOf(),
    ) : Node() {
        fun addLeaf(
            keys: List<String>,
            value: Any?,
        ) {
            addLeaf(children, keys, value, path(), Either.Left(this))
        }

        fun toMap() = toMap(children)

        override fun toString(): String {
            return "path: ${path()}, children: ${children.entries.joinToString(",\n") { "${it.key}: ${it.value}" }}"
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true

            if (other !is BodyNode) return false

            return key == other.key && children.deepEquals(other.children)
        }

        override fun hashCode(): Int {
            var result = key.hashCode()
            result = 31 * result + children.deepHashCode()
            return result
        }
    }

    class LeafNode(
        override val key: String,
        override val parentNode: Either<BodyNode, DataTree>,
        val value: Any?,
    ) : Node() {
        override fun toString(): String {
            return "path: ${path()}, value: $value"
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true

            if (other !is LeafNode) return false

            return key == other.key && value == other.value
        }

        override fun hashCode(): Int {
            var result = key.hashCode()
            result = 31 * result + (value?.hashCode() ?: 0)
            return result
        }
    }

    fun toMap() = toMap(children)

    fun addLeaf(
        keys: List<String>,
        value: Any?,
    ) {
        addLeaf(children, keys, value, emptyList(), Either.Right(this))
    }

    fun toBodyNode(
        key: String,
        parentNode: Either<BodyNode, DataTree>,
    ): BodyNode {
        val bodyNode = BodyNode(key, parentNode)
        children.findLeafNodes().forEach { leaf ->
            bodyNode.addLeaf(leaf.path(), leaf.value)
        }
        return bodyNode
    }

    override fun toString(): String {
        return "children: ${children.entries.joinToString(",\n") { "${it.key}: ${it.value}" }}"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true

        if (other !is DataTree) return false

        return children.deepEquals(other.children)
    }

    override fun hashCode(): Int {
        return children.deepHashCode()
    }
}

fun Map<*, *>.deepEquals(other: Map<*, *>?): Boolean {
    if (this === other) return true
    if (other === null) return false
    if (size != other.size) return false

    for ((key, entryValue) in this) {
        val otherValue = other[key]
        if (entryValue is Map<*, *> && otherValue is Map<*, *>) {
            if (!entryValue.deepEquals(otherValue)) {
                return false
            }
        }
        if (otherValue != entryValue) return false
    }
    return true
}

fun Map<*, *>.deepHashCode(): Int {
    var result = 0
    for ((key, value) in this) {
        result = 31 * result + (key?.hashCode() ?: 0)
        if (value is Map<*, *>) {
            result = 31 * result + (value.deepHashCode())
        } else {
            result = 31 * result + (value?.hashCode() ?: 0)
        }
    }
    return result
}

private fun addLeaf(
    children: MutableMap<String, DataTree.Node>,
    keys: List<String>,
    value: Any?,
    path: List<String>,
    parentNode: Either<BodyNode, DataTree>,
) {
    check(keys.isNotEmpty()) { "keys must not be empty" }

    val currentKey = keys.first()
    val pathStr = (path + currentKey).joinToString(".")

    if (keys.size == 1) {
        check(children[currentKey] !is BodyNode) {
            "$pathStr has a value ($value) being set, " +
                "but it already is populated with a table"
        }
        if (value is DataTree) {
            children[currentKey] = value.toBodyNode(currentKey, parentNode)
        } else {
            children[currentKey] = LeafNode(currentKey, parentNode, value)
        }
    } else {
        if (!children.containsKey(currentKey)) {
            children[currentKey] =
                BodyNode(
                    key = currentKey,
                    parentNode = parentNode,
                )
        }

        when (val currentChild = children[currentKey]) {
            null -> throw IllegalArgumentException("Impossible null in theory")
            is LeafNode -> throw IllegalArgumentException(
                "$pathStr has a table being set, " +
                    "but it already is populated with a value (${currentChild.value})",
            )
            is BodyNode -> currentChild.addLeaf(keys.drop(1), value)
        }
    }
}

private fun Map<String, DataTree.Node>.findLeafNodes(): List<LeafNode> {
    return values.fold(emptyList()) { list, node ->
        // If the node is the right type, include it in the list
        val thisList = if (node is LeafNode) list + node else list

        // If the node has children, search them for additional matches. If not, don't modify the list
        return if (node is BodyNode) {
            thisList + node.children.findLeafNodes()
        } else {
            thisList
        }
    }
}

private fun toMap(children: MutableMap<String, DataTree.Node>): Map<String, Any?> {
    return children.mapValues {
        when (val value = it.value) {
            is BodyNode -> value.toMap()
            is LeafNode -> value.value
        }
    }
}
