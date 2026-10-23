package com.maverick.game.engine.world.body

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs

class FixtureTypeSetTest : DescribeSpec({

    class HandWrittenType(override val index: Int) : IFixtureType

    describe("of") {
        it("owns every value of an enum") {
            val set = FixtureTypeSet.of(TestFixtureType.entries)

            set.size shouldBe TestFixtureType.entries.size
            TestFixtureType.entries.forEach { set.owns(it) shouldBe true }
            set[TestFixtureType.KEEP.index] shouldBeSameInstanceAs TestFixtureType.KEEP
        }
    }

    describe("validated") {
        it("accepts types whose indexes are 0 to n-1 in order") {
            val types = listOf(HandWrittenType(0), HandWrittenType(1))

            FixtureTypeSet.validated(types).toList() shouldBe types
        }

        it("rejects a gap") {
            shouldThrow<IllegalArgumentException> {
                FixtureTypeSet.validated(listOf(HandWrittenType(0), HandWrittenType(2)))
            }
        }

        it("rejects a wrong order") {
            shouldThrow<IllegalArgumentException> {
                FixtureTypeSet.validated(listOf(HandWrittenType(1), HandWrittenType(0)))
            }
        }

        it("rejects the same type twice") {
            val type = HandWrittenType(0)

            shouldThrow<IllegalArgumentException> { FixtureTypeSet.validated(listOf(type, type)) }
        }
    }

    describe("named") {
        it("creates types with indexes in order and readable names") {
            val set = FixtureTypeSet.named("FEET", "BLOCK")

            set[0].index shouldBe 0
            set[1].index shouldBe 1
            set[1].toString() shouldBe "BLOCK"
        }
    }

    describe("owns") {
        it("does not own a stray type that uses an index of the set") {
            val set = FixtureTypeSet.validated(listOf(HandWrittenType(0), HandWrittenType(1)))

            set.owns(HandWrittenType(0)) shouldBe false
        }

        it("does not own a type with an index outside the set") {
            FixtureTypeSet.named("ONE").owns(HandWrittenType(5)) shouldBe false
        }
    }
})
