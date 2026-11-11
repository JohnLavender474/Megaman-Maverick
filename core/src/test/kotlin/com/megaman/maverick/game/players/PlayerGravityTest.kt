package com.megaman.maverick.game.players

import com.maverick.game.engine.common.enums.Direction
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class PlayerGravityTest : DescribeSpec({

    val values = PlayerGravityValues(ground = -1f, jump = -2f, fall = -3f, ice = -4f, water = -5f, waterIce = -6f)

    fun PlayerGravityComponent.all() = listOf(ground, jump, fall, ice, water, waterIce)

    describe("the gravity table") {
        it("is zero before a direction is applied") {
            PlayerGravityComponent(values).all() shouldBe List(6) { 0f }
        }

        listOf(Direction.UP, Direction.RIGHT).forEach { direction ->
            it("keeps the signs of the values for $direction") {
                val gravity = PlayerGravityComponent(values).also { it.applyDirection(direction) }

                gravity.all() shouldBe listOf(-1f, -2f, -3f, -4f, -5f, -6f)
            }
        }

        listOf(Direction.DOWN, Direction.LEFT).forEach { direction ->
            it("flips the signs of the values for $direction") {
                val gravity = PlayerGravityComponent(values).also { it.applyDirection(direction) }

                gravity.all() shouldBe listOf(1f, 2f, 3f, 4f, 5f, 6f)
            }
        }

        it("does not stack a flip when the direction is applied twice") {
            val gravity = PlayerGravityComponent(values)
            gravity.applyDirection(Direction.DOWN)
            gravity.applyDirection(Direction.DOWN)

            gravity.all() shouldBe listOf(1f, 2f, 3f, 4f, 5f, 6f)
        }
    }
})
