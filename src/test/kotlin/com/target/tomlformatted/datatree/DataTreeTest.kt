package com.target.tomlformatted.datatree

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.maps.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test

class DataTreeTest {
    @Test
    fun `add root leaf to empty tree`() {
        // SETUP
        val dataTree = DataTree()

        // TEST
        dataTree.addLeaf(listOf("key"), 1234)

        // VERIFY
        assertSoftly(dataTree) {
            children shouldHaveSize 1
            children shouldContainKey "key"
            val child = children["key"]
            child.shouldBeInstanceOf<DataTree.LeafNode>()
            child.value shouldBe 1234
        }
    }

    @Test
    fun `add nested leaf to empty tree`() {
        // SETUP
        val dataTree = DataTree()

        // TEST
        dataTree.addLeaf(listOf("key1", "key2", "key3"), 1234)

        // VERIFY
        assertSoftly(dataTree) {
            children shouldHaveSize 1
            children shouldContainKey "key1"
            val child1 = children["key1"]
            child1.shouldBeInstanceOf<DataTree.BodyNode>()

            assertSoftly(child1) {
                children shouldHaveSize 1
                children shouldContainKey "key2"
                val child2 = children["key2"]
                child2.shouldBeInstanceOf<DataTree.BodyNode>()

                assertSoftly(child2) {
                    children shouldHaveSize 1
                    children shouldContainKey "key3"
                    val leaf = children["key3"]
                    leaf.shouldBeInstanceOf<DataTree.LeafNode>()
                    leaf.value shouldBe 1234
                }
            }
        }
    }

    @Test
    fun `add nested datatree`() {
        // SETUP
        val dataTree = DataTree()
        val nestedDataTree = DataTree()

        // TEST
        nestedDataTree.addLeaf(listOf("key3", "key4"), 1234)
        dataTree.addLeaf(listOf("key1", "key2"), nestedDataTree)

        // VERIFY
        assertSoftly(dataTree) {
            children.keys shouldContainExactly listOf("key1")
            assertSoftly(children["key1"]) {
                shouldBeInstanceOf<DataTree.BodyNode>()
                path() shouldBe listOf("key1")
                children.keys shouldContainExactly listOf("key2")
                assertSoftly(children["key2"]) {
                    shouldBeInstanceOf<DataTree.BodyNode>()
                    path() shouldBe listOf("key1", "key2")
                    children.keys shouldContainExactly listOf("key3")
                    assertSoftly(children["key3"]) {
                        shouldBeInstanceOf<DataTree.BodyNode>()
                        path() shouldBe listOf("key1", "key2", "key3")
                        children.keys shouldContainExactly listOf("key4")
                        assertSoftly(children["key4"]) {
                            shouldBeInstanceOf<DataTree.LeafNode>()
                            path() shouldBe listOf("key1", "key2", "key3", "key4")
                            value shouldBe 1234
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `add two different leaves`() {
        // SETUP
        val dataTree = DataTree()

        // TEST
        dataTree.addLeaf(listOf("key1"), 1234)
        dataTree.addLeaf(listOf("key2"), 5678)

        // VERIFY
        assertSoftly(dataTree) {
            children shouldHaveSize 2
            children.keys shouldContainAll listOf("key1", "key2")
            val child1 = children["key1"]
            child1.shouldBeInstanceOf<DataTree.LeafNode>()
            child1.value shouldBe 1234

            val child2 = children["key2"]
            child2.shouldBeInstanceOf<DataTree.LeafNode>()
            child2.value shouldBe 5678
        }
    }

    @Test
    fun `add body node branch`() {
        // SETUP
        val dataTree = DataTree()

        // TEST
        dataTree.addLeaf(listOf("key1", "key2-1", "key3"), 1234)
        dataTree.addLeaf(listOf("key1", "key2-2", "key3"), 5678)

        // VERIFY
        assertSoftly(dataTree) {
            children shouldHaveSize 1
            children shouldContainKey "key1"
            assertSoftly(children["key1"]) {
                shouldBeInstanceOf<DataTree.BodyNode>()
                children shouldHaveSize 2
                children.keys shouldContainAll listOf("key2-1", "key2-2")
                assertSoftly(children["key2-1"]) {
                    shouldBeInstanceOf<DataTree.BodyNode>()
                    children shouldHaveSize 1
                    children shouldContainKey "key3"
                    assertSoftly(children["key3"]) {
                        shouldBeInstanceOf<DataTree.LeafNode>()
                        value shouldBe 1234
                    }
                }
                assertSoftly(children["key2-2"]) {
                    shouldBeInstanceOf<DataTree.BodyNode>()
                    children shouldHaveSize 1
                    children shouldContainKey "key3"
                    assertSoftly(children["key3"]) {
                        shouldBeInstanceOf<DataTree.LeafNode>()
                        value shouldBe 5678
                    }
                }
            }
        }
    }

    @Test
    fun `toMap mixed`() {
        // SETUP
        val dataTree = DataTree()

        dataTree.addLeaf(listOf("key1-1", "key2-1", "key3"), 123)
        dataTree.addLeaf(listOf("key1-1", "key2-2", "key3"), "456")
        dataTree.addLeaf(listOf("key1-2"), 789)

        // VERIFY
        dataTree.toMap() shouldBe
            mapOf(
                "key1-1" to
                    mapOf(
                        "key2-1" to
                            mapOf(
                                "key3" to 123,
                            ),
                        "key2-2" to
                            mapOf(
                                "key3" to "456",
                            ),
                    ),
                "key1-2" to 789,
            )
    }

    @Test
    fun `test Equals & Hash`() {
        // SETUP
        val dataTree = DataTree()

        dataTree.addLeaf(listOf("key1-1", "key2-1", "key3"), 123)
        dataTree.addLeaf(listOf("key1-1", "key2-2", "key3"), "456")
        dataTree.addLeaf(listOf("key1-2"), 789)

        val sameDataTree = DataTree()

        sameDataTree.addLeaf(listOf("key1-1", "key2-1", "key3"), 123)
        sameDataTree.addLeaf(listOf("key1-1", "key2-2", "key3"), "456")
        sameDataTree.addLeaf(listOf("key1-2"), 789)

        val differentDataTree = DataTree()

        differentDataTree.addLeaf(listOf("key1-1", "key2-1", "key3"), 123)
        differentDataTree.addLeaf(listOf("key1-1", "key2-2", "key3"), "DIFFERENT")
        differentDataTree.addLeaf(listOf("key1-2"), 789)

        // VERIFY
        assertSoftly(dataTree) {
            (this == sameDataTree).shouldBeTrue()
            this.hashCode() shouldBe sameDataTree.hashCode()
            (this == differentDataTree).shouldBeFalse()
            this.hashCode() shouldNotBe differentDataTree.hashCode()
        }
    }
}
