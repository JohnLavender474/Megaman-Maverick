package com.megaman.maverick.game.utils

import com.badlogic.gdx.math.Vector2
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs

class OwnedArrayTest : DescribeSpec({

    describe("an owned array") {

        it("reuses its own instances after a clear") {
            var made = 0
            val array = OwnedArray { made++; Vector2() }
            val first = array.add().set(1f, 1f)
            val second = array.add().set(2f, 2f)

            array.clear()
            array.isEmpty.shouldBeTrue()

            array.add() shouldBeSameInstanceAs first
            array.add() shouldBeSameInstanceAs second
            array.add()
            made shouldBe 3
            array.size shouldBe 3
        }

        it("reads, visits and tests only the live entries") {
            val array = OwnedArray { Vector2() }
            array.add().set(1f, 0f)
            array.add().set(2f, 0f)
            array.clear()
            array.add().set(3f, 0f)

            array[0].x shouldBe 3f
            shouldThrow<IndexOutOfBoundsException> { array[1] }

            var sum = 0f
            array.forEach { sum += it.x }
            sum shouldBe 3f
            array.any { it.x == 2f }.shouldBeFalse()
            array.any { it.x == 3f }.shouldBeTrue()
        }
    }
})
