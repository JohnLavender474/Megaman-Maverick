package com.megaman.maverick.game.entities.projectiles

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.maverick.game.engine.common.enums.Direction
import com.megaman.maverick.game.ConstVals
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs

private const val PPM = ConstVals.PPM.toFloat()
private const val STEP = 1f / 150f
private const val SPACING = 0.5f * PPM
private const val TOLERANCE = 0.01f
private const val OUT_SIZE = 64
private const val LONG_TIME = 10f

private fun dotArray(size: Int = OUT_SIZE) = Array<Vector2>().apply { repeat(size) { add(Vector2()) } }

private fun upHeight(point: Vector2, start: Vector2, up: Direction) = when (up) {
    Direction.UP -> point.y - start.y
    Direction.DOWN -> start.y - point.y
    Direction.LEFT -> start.x - point.x
    Direction.RIGHT -> point.x - start.x
}

class AxeTrajectoryTest : DescribeSpec({

    describe("the gravity") {
        val g = AxeTrajectory.GRAVITY * PPM
        val cases = listOf(
            Direction.UP to Vector2(0f, -g),
            Direction.DOWN to Vector2(0f, g),
            Direction.LEFT to Vector2(g, 0f),
            Direction.RIGHT to Vector2(-g, 0f)
        )
        cases.forEach { (direction, expected) ->
            listOf(1f, 0.25f).forEach { scalar ->
                it("for $direction with the scalar $scalar is $expected times $scalar") {
                    val out = Vector2(99f, 99f)
                    val result = AxeTrajectory.gravity(direction, scalar, out)

                    result shouldBeSameInstanceAs out
                    out.x shouldBe (expected.x * scalar plusOrMinus TOLERANCE)
                    out.y shouldBe (expected.y * scalar plusOrMinus TOLERANCE)
                }
            }
        }

        it("for RIGHT is the opposite of LEFT") {
            val left = AxeTrajectory.gravity(Direction.LEFT, 1f, Vector2())
            val right = AxeTrajectory.gravity(Direction.RIGHT, 1f, Vector2())

            right.x shouldBe -left.x
            right.y shouldBe 0f
        }
    }

    describe("the dots") {
        it("lie each spacing of arc length on a straight path") {
            val start = Vector2(3f * PPM, 5f * PPM)
            val out = dotArray()
            val count = AxeTrajectory.dots(
                start, Vector2(10f * PPM, 0f), Vector2(), Direction.UP, STEP, SPACING,
                maxDots = 8, maxTime = LONG_TIME, out = out
            )

            count shouldBe 8
            for (i in 0 until count) withClue("dot $i") {
                out[i].x shouldBe (start.x + (i + 1) * SPACING plusOrMinus TOLERANCE)
                out[i].y shouldBe (start.y plusOrMinus TOLERANCE)
            }
        }

        it("are spaced by the spacing along a curved path") {
            val out = dotArray()
            val count = AxeTrajectory.dots(
                Vector2(), Vector2(8f * PPM, 20f * PPM), AxeTrajectory.gravity(Direction.UP, 1f, Vector2()),
                Direction.UP, STEP, SPACING, maxDots = 10, maxTime = LONG_TIME, out = out
            )

            count shouldBe 10
            for (i in 1 until count) withClue("dot $i") {
                out[i].dst(out[i - 1]) shouldBe (SPACING plusOrMinus 0.05f * PPM)
            }
        }

        Direction.entries.forEach { up ->
            it("stop 1 tile below the start along the up axis $up") {
                val start = Vector2(2f * PPM, -4f * PPM)
                val out = dotArray()
                val count = AxeTrajectory.dots(
                    start, Vector2(), AxeTrajectory.gravity(up, 1f, Vector2()), up, STEP, 0.3f * PPM,
                    maxDots = OUT_SIZE, maxTime = LONG_TIME, out = out
                )

                count shouldBe 3
                for (i in 0 until count) withClue("dot $i") {
                    upHeight(out[i], start, up) shouldBe (-(i + 1) * 0.3f * PPM plusOrMinus TOLERANCE)
                }
            }
        }

        it("do not stop by the fall when the fall is across the up axis") {
            val out = dotArray()
            val count = AxeTrajectory.dots(
                Vector2(), Vector2(), AxeTrajectory.gravity(Direction.RIGHT, 1f, Vector2()), Direction.UP, STEP,
                0.3f * PPM, maxDots = 20, maxTime = LONG_TIME, out = out
            )

            count shouldBe 20
        }

        it("stop at the max dots and leave the other vectors as they are") {
            val out = dotArray(10)
            for (i in 0 until out.size) out[i].set(-1f, -1f)
            val count = AxeTrajectory.dots(
                Vector2(), Vector2(10f * PPM, 0f), Vector2(), Direction.UP, STEP, SPACING,
                maxDots = 5, maxTime = LONG_TIME, out = out
            )

            count shouldBe 5
            for (i in 5 until out.size) withClue("vector $i") {
                out[i].x shouldBe -1f
                out[i].y shouldBe -1f
            }
        }

        it("stop at the max time") {
            val speed = 15f * PPM
            val longer = AxeTrajectory.dots(
                Vector2(), Vector2(speed, 0f), Vector2(), Direction.UP, STEP, SPACING,
                maxDots = OUT_SIZE, maxTime = 0.51f, out = dotArray()
            )
            val shorter = AxeTrajectory.dots(
                Vector2(), Vector2(speed, 0f), Vector2(), Direction.UP, STEP, SPACING,
                maxDots = OUT_SIZE, maxTime = 0.49f, out = dotArray()
            )

            longer shouldBe 15
            shorter shouldBe 14
        }

        it("write into the given vectors and change no input") {
            val start = Vector2(1f, 2f)
            val velocity = Vector2(8f * PPM, 20f * PPM)
            val gravity = AxeTrajectory.gravity(Direction.UP, 1f, Vector2())
            val gravityY = gravity.y
            val out = dotArray()
            val vectors = (0 until out.size).map { out[it] }

            val first = AxeTrajectory.dots(
                start, velocity, gravity, Direction.UP, STEP, SPACING,
                maxDots = OUT_SIZE, maxTime = LONG_TIME, out = out
            )
            val firstDots = (0 until first).map { Vector2(out[it]) }
            val second = AxeTrajectory.dots(
                start, velocity, gravity, Direction.UP, STEP, SPACING,
                maxDots = OUT_SIZE, maxTime = LONG_TIME, out = out
            )

            out.size shouldBe OUT_SIZE
            for (i in 0 until out.size) out[i] shouldBeSameInstanceAs vectors[i]
            second shouldBe first
            for (i in 0 until first) out[i] shouldBe firstDots[i]
            start shouldBe Vector2(1f, 2f)
            velocity shouldBe Vector2(8f * PPM, 20f * PPM)
            gravity.y shouldBe gravityY
        }

        it("reject a spacing that is not positive") {
            shouldThrow<IllegalArgumentException> {
                AxeTrajectory.dots(
                    Vector2(), Vector2(), Vector2(), Direction.UP, STEP, 0f,
                    maxDots = 1, maxTime = LONG_TIME, out = dotArray()
                )
            }
        }

        it("reject more max dots than the given vectors") {
            shouldThrow<IllegalArgumentException> {
                AxeTrajectory.dots(
                    Vector2(), Vector2(), Vector2(), Direction.UP, STEP, SPACING,
                    maxDots = 5, maxTime = LONG_TIME, out = dotArray(4)
                )
            }
        }
    }
})
