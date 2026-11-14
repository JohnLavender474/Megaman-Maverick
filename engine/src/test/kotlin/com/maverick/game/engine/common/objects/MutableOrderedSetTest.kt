package com.maverick.game.engine.common.objects

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class MutableOrderedSetTest : DescribeSpec({

    describe("MutableOrderedSet.retainAll") {

        lateinit var set: MutableOrderedSet<String>

        beforeEach {
            set = MutableOrderedSet()
            set.addAll(listOf("a", "b", "c", "d"))
        }

        it("keeps only the elements that are in the argument, in their order") {
            set.retainAll(listOf("d", "b", "x")) shouldBe true
            set.toList() shouldBe listOf("b", "d")
        }

        it("returns false and removes nothing when every element is in the argument") {
            set.retainAll(listOf("a", "b", "c", "d", "x")) shouldBe false
            set.toList() shouldBe listOf("a", "b", "c", "d")
        }

        it("removes every element for an empty argument") {
            set.retainAll(emptyList()) shouldBe true
            set.size shouldBe 0
        }
    }
})
