package com.megaman.maverick.game.entities.megaman.weapons

import com.badlogic.gdx.math.Vector2
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.common.enums.Facing
import com.megaman.maverick.game.ConstVals
import com.megaman.maverick.game.entities.megaman.MegamanMovesRig
import com.megaman.maverick.game.entities.megaman.constants.MegamanValues
import com.megaman.maverick.game.levels.LevelDefinition
import com.megaman.maverick.game.world.body.BodySense
import com.megaman.maverick.game.world.body.setBodySense
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe
import io.mockk.unmockkAll

private const val PPM = ConstVals.PPM.toFloat()
private const val TOLERANCE = 0.01f
private const val MIDPOINT_HOLD = 0.4f

private fun oldImpulse(direction: Direction, facing: Facing): Vector2 {
    val x = 8f
    val y = 20f
    return when (direction) {
        Direction.UP -> Vector2(x * facing.value, y)
        Direction.DOWN -> Vector2(x * -facing.value, -y)
        Direction.LEFT -> Vector2(-y, x * facing.value)
        Direction.RIGHT -> Vector2(y, x * -facing.value)
    }.scl(PPM)
}

private fun oldSpawnOffset(direction: Direction, facing: Facing) = when (direction) {
    Direction.UP -> Vector2(-0.1f * PPM * facing.value, 0.75f * PPM)
    Direction.DOWN -> Vector2(0.1f * PPM * facing.value, -0.75f * PPM)
    Direction.LEFT -> Vector2(-0.75f * PPM, 0.1f * PPM * facing.value)
    Direction.RIGHT -> Vector2(0.75f * PPM, -0.1f * PPM * facing.value)
}

private fun Vector2.shouldBeNear(expected: Vector2) {
    x shouldBe (expected.x plusOrMinus TOLERANCE)
    y shouldBe (expected.y plusOrMinus TOLERANCE)
}

class AxeThrowTest : DescribeSpec({

    afterEach { unmockkAll() }

    describe("the ratio of a hold") {
        it("is 0 up to the windup time") {
            AxeThrow.ratio(0f) shouldBe 0f
            AxeThrow.ratio(MegamanValues.AXE_WINDUP_TIME) shouldBe 0f
        }

        it("is 1 at the hold time and after it") {
            AxeThrow.ratio(MegamanValues.AXE_HOLD_TIME) shouldBe (1f plusOrMinus TOLERANCE)
            AxeThrow.ratio(MegamanValues.AXE_HOLD_TIME + 1f) shouldBe 1f
        }

        it("grows linearly between the windup time and the hold time") {
            AxeThrow.ratio(MIDPOINT_HOLD) shouldBe (0.5f plusOrMinus TOLERANCE)
        }
    }

    describe("the impulse curve") {
        it("is the weakest throw on a tap") {
            AxeThrow.localImpulse(0f, Vector2()).shouldBeNear(
                Vector2(MegamanValues.AXE_IMPULSE_X_MIN, MegamanValues.AXE_IMPULSE_Y_MIN)
            )
        }

        it("is the strongest throw after the full hold") {
            AxeThrow.localImpulse(MegamanValues.AXE_HOLD_TIME, Vector2()).shouldBeNear(
                Vector2(MegamanValues.AXE_IMPULSE_X_MAX, MegamanValues.AXE_IMPULSE_Y_MAX)
            )
        }

        it("is the old throw (8, 20) at the midpoint") {
            AxeThrow.localImpulse(MIDPOINT_HOLD, Vector2()).shouldBeNear(Vector2(8f, 20f))
        }

        it("grows less in y than in x") {
            val weak = AxeThrow.localImpulse(0f, Vector2())
            val strong = AxeThrow.localImpulse(MegamanValues.AXE_HOLD_TIME, Vector2())
            ((strong.y - weak.y) < (strong.x - weak.x)) shouldBe true
        }
    }

    describe("the world impulse") {
        it("maps the old throw to the old impulse for each direction and facing") {
            Direction.entries.forEach { direction ->
                Facing.entries.forEach { facing ->
                    withClue("direction=$direction, facing=$facing") {
                        AxeThrow.worldImpulse(MIDPOINT_HOLD, direction, facing, slowed = false, out = Vector2())
                            .shouldBeNear(oldImpulse(direction, facing))
                    }
                }
            }
        }

        it("is scaled by the slow scalar when slowed") {
            AxeThrow.worldImpulse(MIDPOINT_HOLD, Direction.UP, Facing.RIGHT, slowed = true, out = Vector2())
                .shouldBeNear(oldImpulse(Direction.UP, Facing.RIGHT).scl(MegamanValues.AXE_SLOW_SCALAR))
        }
    }

    describe("the spawn point") {
        it("is the old offset from the center for each direction and facing") {
            val center = Vector2(3f * PPM, 5f * PPM)
            Direction.entries.forEach { direction ->
                Facing.entries.forEach { facing ->
                    withClue("direction=$direction, facing=$facing") {
                        AxeThrow.spawnPoint(center, direction, facing, Vector2())
                            .shouldBeNear(Vector2(center).add(oldSpawnOffset(direction, facing)))
                    }
                }
            }
        }
    }

    describe("the launch of a Mega Man") {
        lateinit var rig: MegamanMovesRig

        beforeEach {
            rig = MegamanMovesRig()
            rig.body.setSize(PPM, 1.5f * PPM)
            rig.body.setCenter(4f * PPM, 2f * PPM)
        }

        it("gives the spawn point and the impulse of his direction and facing") {
            rig.body.direction = Direction.LEFT
            rig.megaman.facing = Facing.LEFT
            val position = Vector2()
            val impulse = Vector2()

            AxeThrow.launch(rig.megaman, MIDPOINT_HOLD, position, impulse)

            position.shouldBeNear(Vector2(4f * PPM, 2f * PPM).add(oldSpawnOffset(Direction.LEFT, Facing.LEFT)))
            impulse.shouldBeNear(oldImpulse(Direction.LEFT, Facing.LEFT))
        }

        it("is slowed in water") {
            rig.body.setBodySense(BodySense.IN_WATER, true)
            val impulse = Vector2()

            AxeThrow.launch(rig.megaman, MIDPOINT_HOLD, Vector2(), impulse)

            impulse.shouldBeNear(oldImpulse(Direction.UP, Facing.RIGHT).scl(MegamanValues.AXE_SLOW_SCALAR))
        }

        it("is slowed in the Moon Man level") {
            rig.services.levelState.currentLevel = LevelDefinition.MOON_MAN
            val impulse = Vector2()

            AxeThrow.launch(rig.megaman, MIDPOINT_HOLD, Vector2(), impulse)

            impulse.shouldBeNear(oldImpulse(Direction.UP, Facing.RIGHT).scl(MegamanValues.AXE_SLOW_SCALAR))
        }

        it("is not slowed out of water in another level") {
            val impulse = Vector2()

            AxeThrow.launch(rig.megaman, MIDPOINT_HOLD, Vector2(), impulse)

            impulse.shouldBeNear(oldImpulse(Direction.UP, Facing.RIGHT))
        }
    }
})
