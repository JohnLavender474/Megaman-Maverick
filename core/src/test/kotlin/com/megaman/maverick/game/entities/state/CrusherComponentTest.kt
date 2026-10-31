package com.megaman.maverick.game.entities.state

import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.shapes.GameRectangle
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class CrusherComponentTest : DescribeSpec({

    fun rect(x: Float, y: Float) = GameRectangle().setSize(10f, 10f).setCenter(x, y)

    describe("CrusherComponent") {
        it("hits the named side only") {
            val player = crusherTestPlayer(100f, 100f)
            val above = rect(100f, 118f)
            CrusherComponent({ above }, { Direction.UP }).crushes(player) shouldBe true
            CrusherComponent({ above }, { Direction.DOWN }).crushes(player) shouldBe false
            CrusherComponent({ rect(88f, 100f) }, { Direction.LEFT }).crushes(player) shouldBe true
            CrusherComponent({ rect(112f, 100f) }, { Direction.RIGHT }).crushes(player) shouldBe true
        }

        it("does nothing when it is not active") {
            val player = crusherTestPlayer(100f, 100f)
            CrusherComponent({ rect(100f, 118f) }, { Direction.UP }, { false }).crushes(player) shouldBe false
        }

        it("needs the opposite side to touch when asked") {
            val player = crusherTestPlayer(100f, 100f)
            var surface = rect(0f, 0f)
            val crusher = CrusherComponent({ rect(88f, 100f) }, { Direction.LEFT }, oppositeTouches = { it.overlaps(surface) })
            crusher.crushes(player) shouldBe false
            surface = rect(112f, 100f)
            crusher.crushes(player) shouldBe true
        }

        it("reads the feet shape for a crush from above, and the head shape from below") {
            val player = crusherTestPlayer(100f, 100f)
            val floor = rect(100f, 82f)
            val ceiling = rect(100f, 118f)
            CrusherComponent({ ceiling }, { Direction.UP }, oppositeTouches = { it.overlaps(floor) })
                .crushes(player) shouldBe true
            CrusherComponent({ ceiling }, { Direction.UP }, oppositeTouches = { it.overlaps(ceiling) })
                .crushes(player) shouldBe false
            CrusherComponent({ floor }, { Direction.DOWN }, oppositeTouches = { it.overlaps(ceiling) })
                .crushes(player) shouldBe true
        }
    }

    describe("worldToBodySide") {
        it("keeps the side when the body is upright") {
            Direction.entries.forEach { worldToBodySide(it, Direction.UP) shouldBe it }
        }

        it("maps world sides for a rotated body") {
            worldToBodySide(Direction.LEFT, Direction.LEFT) shouldBe Direction.UP
            worldToBodySide(Direction.DOWN, Direction.LEFT) shouldBe Direction.LEFT
            worldToBodySide(Direction.UP, Direction.DOWN) shouldBe Direction.DOWN
            worldToBodySide(Direction.LEFT, Direction.DOWN) shouldBe Direction.RIGHT
            worldToBodySide(Direction.RIGHT, Direction.RIGHT) shouldBe Direction.UP
        }
    }
})
