package com.megaman.maverick.game.entities.bosses.bigassmaverickrobot

import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.common.shapes.GameRectangle
import com.megaman.maverick.game.ConstVals
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe

private const val PPM = ConstVals.PPM.toFloat()

class ClapAimTest : DescribeSpec({

    val arena = GameRectangle(318f * PPM, 55f * PPM, 14f * PPM, 11.75f * PPM)

    fun tiles(x: Float, y: Float) = Vector2(x * PPM, y * PPM)

    fun plan(target: Vector2, room: GameRectangle = arena) =
        ClapAim.plan(target, room, fistHalfWidth = 1f * PPM, floorReach = 0.5f * PPM, out = ClapPlan())

    fun Vector2.shouldBeAt(x: Float, y: Float) {
        this.x shouldBe (x * PPM plusOrMinus 0.001f * PPM)
        this.y shouldBe (y * PPM plusOrMinus 0.001f * PPM)
    }

    describe("the pincer clap aim") {
        it("meets at the target, with each fist 4 tiles to its side and its face on the meet point at the stop") {
            val plan = plan(tiles(325f, 58f))
            plan.meet.shouldBeAt(325f, 58f)
            plan.leftStart.shouldBeAt(321f, 58f)
            plan.rightStart.shouldBeAt(329f, 58f)
            plan.leftStop.shouldBeAt(324f, 58f)
            plan.rightStop.shouldBeAt(326f, 58f)
        }

        it("holds the meet point 1 tile plus the fist half width from the left wall, and the near fist starts at its stop") {
            val plan = plan(tiles(318.6f, 56f))
            plan.meet.shouldBeAt(320f, 56f)
            plan.leftStart.shouldBeAt(319f, 56f)
            plan.leftStop.shouldBeAt(319f, 56f)
            plan.rightStart.shouldBeAt(324f, 56f)
        }

        it("holds a start near a wall inside the arena margin") {
            val plan = plan(tiles(321f, 56f))
            plan.meet.shouldBeAt(321f, 56f)
            plan.leftStart.shouldBeAt(319.5f, 56f)
            plan.rightStart.shouldBeAt(325f, 56f)
        }

        it("holds the meet point 1 tile plus the fist half width from the right wall") {
            val plan = plan(tiles(331.5f, 56f))
            plan.meet.shouldBeAt(330f, 56f)
            plan.rightStart.shouldBeAt(331f, 56f)
            plan.rightStop.shouldBeAt(331f, 56f)
            plan.leftStart.shouldBeAt(326f, 56f)
        }

        it("holds the meet point at the floor rest height and 1.5 tiles under the ceiling") {
            plan(tiles(325f, 54f)).meet.shouldBeAt(325f, 55.5f)
            plan(tiles(325f, 66.5f)).meet.shouldBeAt(325f, 65.25f)
            plan(tiles(325f, 54f)).leftStart.shouldBeAt(321f, 55.5f)
        }

        it("throws when the arena is too small for the clap") {
            shouldThrow<IllegalStateException> {
                plan(tiles(320f, 56f), GameRectangle(318f * PPM, 55f * PPM, 3f * PPM, 11.75f * PPM))
            }
        }
    }
})
