package com.maverick.game.engine.common.random

import com.badlogic.gdx.utils.Array
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.floats.shouldBeGreaterThanOrEqual
import io.kotest.matchers.floats.shouldBeLessThan
import io.kotest.matchers.shouldBe
import java.util.Random

class SeededRandomTest : DescribeSpec({

    fun seeded(seed: Long) = SeededRandom(Random(seed))

    fun draws(random: GameRandom) = (0 until 20).map {
        listOf(random.nextInt(0, 100), random.nextFloat(0f, 1f), random.nextBoolean())
    }

    describe("SeededRandom") {

        it("gives the same values for the same seed") {
            draws(seeded(42L)) shouldBe draws(seeded(42L))
        }

        it("takes a seed and draws as a java.util.Random with that seed") {
            val fromSeed = SeededRandom(11L)
            val fromSource = seeded(11L)
            repeat(20) { fromSeed.nextInt(0, 1000) shouldBe fromSource.nextInt(0, 1000) }
        }

        it("draws the same ints as the old UtilMethods.getRandom for the same seed") {
            val old = Random(7L)
            val new = seeded(7L)
            repeat(100) { new.nextInt(-3, 9) shouldBe old.nextInt(9 + 1 - -3) + -3 }
        }

        it("gives ints in the inclusive range and reaches both bounds") {
            val random = seeded(1L)
            val seen = (0 until 1000).map { random.nextInt(2, 5) }.toSet()
            seen shouldBe setOf(2, 3, 4, 5)
        }

        it("gives the min when min equals maxInclusive") {
            seeded(1L).nextInt(4, 4) shouldBe 4
        }

        it("throws when maxInclusive is less than min") {
            shouldThrow<IllegalArgumentException> { seeded(1L).nextInt(5, 4) }
        }

        it("gives floats in [min, max)") {
            val random = seeded(3L)
            repeat(1000) {
                val value = random.nextFloat(-2f, 3f)
                value shouldBeGreaterThanOrEqual -2f
                value shouldBeLessThan 3f
            }
        }

        it("picks only elements of the input and does not change it") {
            val random = seeded(5L)
            val array = Array<String>().apply { addAll("a", "b", "c") }
            val list = listOf("d", "e")
            repeat(50) {
                (random.pick(array) in setOf("a", "b", "c")) shouldBe true
                (random.pick(list) in setOf("d", "e")) shouldBe true
                (random.pickOf("f", "g") in setOf("f", "g")) shouldBe true
            }
            array.toList() shouldBe listOf("a", "b", "c")
        }

        it("throws when it picks from an empty input") {
            shouldThrow<IllegalArgumentException> { seeded(1L).pick(Array<String>()) }
            shouldThrow<IllegalArgumentException> { seeded(1L).pick(emptyList<String>()) }
            shouldThrow<IllegalArgumentException> { seeded(1L).pickOf<String>() }
        }

        it("shuffles to a permutation, the same one for the same seed") {
            fun shuffled(seed: Long) = Array<Int>().apply { (1..8).forEach { add(it) } }
                .also { seeded(seed).shuffle(it) }.toList()

            shuffled(9L) shouldContainExactlyInAnyOrder (1..8).toList()
            shuffled(9L) shouldBe shuffled(9L)
        }
    }
})
