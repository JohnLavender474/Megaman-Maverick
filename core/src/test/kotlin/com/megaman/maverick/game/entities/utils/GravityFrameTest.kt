package com.megaman.maverick.game.entities.utils

import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.shapes.GameRectangle
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class GravityFrameTest : DescribeSpec({

    describe("velocityToLocal") {
        Direction.entries.forEach { direction ->
            it("is the inverse of velocityToWorld for $direction") {
                val world = GravityFrame.velocityToWorld(direction, 3f, -7f, Vector2())
                GravityFrame.velocityToLocal(direction, world, Vector2()) shouldBe Vector2(3f, -7f)
            }

            it("gives back a world velocity through velocityToWorld for $direction") {
                val local = GravityFrame.velocityToLocal(direction, Vector2(5f, 2f), Vector2())
                GravityFrame.velocityToWorld(direction, local.x, local.y, Vector2()) shouldBe Vector2(5f, 2f)
            }
        }

        it("takes local up as the side opposite to the gravity") {
            GravityFrame.velocityToWorld(Direction.UP, 0f, 1f, Vector2()) shouldBe Vector2(0f, 1f)
            GravityFrame.velocityToWorld(Direction.DOWN, 0f, 1f, Vector2()) shouldBe Vector2(0f, -1f)
            GravityFrame.velocityToWorld(Direction.LEFT, 0f, 1f, Vector2()) shouldBe Vector2(-1f, 0f)
            GravityFrame.velocityToWorld(Direction.RIGHT, 0f, 1f, Vector2()) shouldBe Vector2(1f, 0f)
        }
    }

    describe("toLocal") {
        Direction.entries.forEach { direction ->
            it("is the inverse of toWorld for $direction") {
                val local = GameRectangle(2f, 3f, 4f, 5f)
                val world = GravityFrame.toWorld(direction, local, GameRectangle())
                GravityFrame.toLocal(direction, world, GameRectangle()) shouldBe local
            }
        }
    }
})
