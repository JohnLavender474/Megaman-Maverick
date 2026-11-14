package com.maverick.game.engine.common.objects

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class InsertionOrderPriorityQueueTest : DescribeSpec({

    describe("InsertionOrderPriorityQueue") {

        it("gives the live size after adds and polls") {
            val queue = InsertionOrderPriorityQueue<Int>()
            queue.size shouldBe 0
            queue.add(3)
            queue.add(1)
            queue.offer(2)
            queue.size shouldBe 3
            queue.poll() shouldBe 1
            queue.size shouldBe 2
            queue.clear()
            queue.size shouldBe 0
        }

        it("polls equal elements in insertion order") {
            val queue = InsertionOrderPriorityQueue<Item>()
            val first = Item(1, "first")
            val second = Item(1, "second")
            val low = Item(0, "low")
            queue.add(first)
            queue.add(second)
            queue.add(low)
            queue.poll() shouldBe low
            queue.poll() shouldBe first
            queue.poll() shouldBe second
        }
    }
})

private data class Item(val priority: Int, val name: String) : Comparable<Item> {

    override fun compareTo(other: Item) = priority.compareTo(other.priority)
}
