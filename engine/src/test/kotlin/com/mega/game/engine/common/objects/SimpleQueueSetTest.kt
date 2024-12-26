package com.mega.game.engine.common.objects

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class SimpleQueueSetTest : DescribeSpec({

    describe("SimpleQueueSet") {

        lateinit var queueSet: SimpleQueueSet<String>

        beforeEach {
            queueSet = SimpleQueueSet()
        }

        it("should add elements and maintain queue order") {
            queueSet.add("first") shouldBe true
            queueSet.add("second") shouldBe true
            queueSet.add("third") shouldBe true

            queueSet.queue[0] shouldBe "first"
            queueSet.queue[1] shouldBe "second"
            queueSet.queue[2] shouldBe "third"
        }

        it("should not add duplicate elements") {
            queueSet.add("first") shouldBe true
            queueSet.add("first") shouldBe false

            queueSet.size shouldBe 1
        }

        it("should remove an element from both queue and set") {
            queueSet.add("first")
            queueSet.add("second")
            queueSet.add("third")

            queueSet.remove("second") shouldBe true

            queueSet.size shouldBe 2
            queueSet.set shouldNotContain "second"
            queueSet.queue shouldNotContain "second"
        }

        it("should retain only the specified elements") {
            queueSet.add("first")
            queueSet.add("second")
            queueSet.add("third")

            queueSet.retainAll(listOf("first", "third")) shouldBe true

            queueSet.size shouldBe 2
            queueSet.set shouldContain "first"
            queueSet.set shouldContain "third"
            queueSet.set shouldNotContain "second"
        }

        it("should clear all elements") {
            queueSet.add("first")
            queueSet.add("second")
            queueSet.clear()

            queueSet.size shouldBe 0
            queueSet.isEmpty() shouldBe true
            queueSet.set.isEmpty shouldBe true
            queueSet.queue.isEmpty shouldBe true
        }

        it("should remove all elements from the provided collection") {
            queueSet.add("first")
            queueSet.add("second")
            queueSet.add("third")

            queueSet.removeAll(listOf("second", "third")) shouldBe true

            queueSet.size shouldBe 1
            queueSet.set shouldContain "first"
            queueSet.set shouldNotContain "second"
            queueSet.set shouldNotContain "third"
        }

        it("should confirm if an element exists using contains") {
            queueSet.add("first")
            queueSet.add("second")

            queueSet.contains("first") shouldBe true
            queueSet.contains("second") shouldBe true
            queueSet.contains("third") shouldBe false
        }

        it("should confirm if all elements exist using containsAll") {
            queueSet.add("first")
            queueSet.add("second")
            queueSet.add("third")

            queueSet.containsAll(listOf("first", "second")) shouldBe true
            queueSet.containsAll(listOf("first", "fourth")) shouldBe false
        }

        it("should iterate over all elements in queue order") {
            queueSet.add("first")
            queueSet.add("second")
            queueSet.add("third")

            val iterator = queueSet.iterator()
            iterator.hasNext() shouldBe true
            iterator.next() shouldBe "first"
            iterator.next() shouldBe "second"
            iterator.next() shouldBe "third"
            iterator.hasNext() shouldBe false
        }

        it("should remove elements during iteration") {
            queueSet.add("first")
            queueSet.add("second")
            queueSet.add("third")

            val iterator = queueSet.iterator()
            iterator.next()
            iterator.remove()

            queueSet.size shouldBe 2
            queueSet.queue shouldNotContain "first"
            queueSet.set shouldNotContain "first"
        }

        it("should handle remove when next hasn't been called in iterator") {
            queueSet.add("first")

            val iterator = queueSet.iterator()
            val exception = kotlin.runCatching { iterator.remove() }
            exception.isFailure shouldBe true
            exception.exceptionOrNull() shouldNotBe null
        }

        it("should poll elements in FIFO order and remove them") {
            queueSet.add("first")
            queueSet.add("second")
            queueSet.add("third")

            queueSet.poll() shouldBe "first"
            queueSet.poll() shouldBe "second"
            queueSet.poll() shouldBe "third"
            queueSet.poll() shouldBe null
        }

        it("should peek at the first element without removing it") {
            queueSet.add("first")
            queueSet.add("second")

            queueSet.peek() shouldBe "first"
            queueSet.size shouldBe 2
        }

        it("should return null when peeking an empty queue") {
            queueSet.peek() shouldBe null
        }

        it("should return element and throw NoSuchElementException if empty") {
            queueSet.add("first")
            queueSet.element() shouldBe "first"

            queueSet.clear()
            shouldThrow<NoSuchElementException> {
                queueSet.element()
            }
        }

        it("should remove first element and throw NoSuchElementException if empty") {
            queueSet.add("first")
            queueSet.add("second")

            queueSet.remove() shouldBe "first"
            queueSet.remove() shouldBe "second"

            shouldThrow<NoSuchElementException> {
                queueSet.remove()
            }
        }

        it("should offer elements and return true") {
            queueSet.offer("first") shouldBe true
            queueSet.offer("second") shouldBe true

            queueSet.size shouldBe 2
        }

        it("should handle repeated clear operations") {
            queueSet.add("first")
            queueSet.add("second")

            queueSet.clear()
            queueSet.isEmpty() shouldBe true

            queueSet.clear()
            queueSet.isEmpty() shouldBe true
        }
    }
})
