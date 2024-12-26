package com.mega.game.engine.common.objects

import com.mega.game.engine.common.extensions.gdxArrayOf
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class MultiCollectionIteratorTest :
    DescribeSpec({
        describe("MultiCollectionIterator") {
            it("should iterate over elements from multiple collections") {
                // Create a sequence of collections
                val list1 = listOf(1, 2, 3)
                val list2 = listOf(4, 5, 6)
                val list3 = listOf(7, 8, 9)

                // Create a MultiCollectionIterator
                val iterable = MultiCollectionIterable(gdxArrayOf(list1, list2, list3))

                val iterator = iterable.iterator()

                val result = mutableListOf<Int>()
                while (iterator.hasNext()) {
                    result.add(iterator.next())
                }

                result shouldBe listOf(1, 2, 3, 4, 5, 6, 7, 8, 9)
            }

            it("should handle empty collections gracefully") {
                // Create a sequence of collections with an empty collection
                val list1 = emptyList<Int>()
                val list2 = listOf(4, 5, 6)
                val list3 = emptyList<Int>()

                // Create a MultiCollectionIterator
                val iterable = MultiCollectionIterable(gdxArrayOf(list1, list2, list3))

                val iterator = iterable.iterator()

                val result = mutableListOf<Int>()
                while (iterator.hasNext()) {
                    result.add(iterator.next())
                }

                result shouldBe listOf(4, 5, 6)
            }

            it("should handle multiple empty collections") {
                // Create a sequence of empty collections
                val list1 = emptyList<Int>()
                val list2 = emptyList<Int>()
                val list3 = emptyList<Int>()

                // Create a MultiCollectionIterator
                val iterable = MultiCollectionIterable(gdxArrayOf(list1, list2, list3))

                val iterator = iterable.iterator()

                val result = mutableListOf<Int>()
                while (iterator.hasNext()) {
                    result.add(iterator.next())
                }

                result shouldBe emptyList()
            }

            it("should throw NoSuchElementException when trying to iterate beyond elements") {
                // Create a sequence of collections
                val list1 = listOf(1, 2, 3)
                val list2 = listOf(4, 5, 6)

                // Create a MultiCollectionIterator
                val iterable = MultiCollectionIterable(gdxArrayOf(list1, list2))

                val iterator = iterable.iterator()

                val result = mutableListOf<Int>()
                while (iterator.hasNext()) {
                    result.add(iterator.next())
                }

                result shouldBe listOf(1, 2, 3, 4, 5, 6)

                // Attempting to access another element should throw NoSuchElementException
                shouldThrow<NoSuchElementException> { iterator.next() }
            }
        }
    })
