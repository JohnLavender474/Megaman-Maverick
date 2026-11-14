package com.maverick.game.engine.common.extensions

import com.badlogic.gdx.utils.OrderedSet
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.shouldBe

class ObjectSetExtensionsTest : DescribeSpec({

    describe("OrderedSet.random") {

        it("gives elements of the set and does not change its order") {
            val set = OrderedSet<String>()
            listOf("a", "b", "c", "d").forEach { set.add(it) }
            repeat(50) {
                set.random() shouldBeIn listOf("a", "b", "c", "d")
                set.orderedItems().toList() shouldBe listOf("a", "b", "c", "d")
            }
        }

        it("throws for an empty set") {
            shouldThrow<IllegalStateException> { OrderedSet<String>().random() }
        }
    }
})
