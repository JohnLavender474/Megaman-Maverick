package com.mega.game.engine.motion

import com.badlogic.gdx.math.Vector2
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class ArcMotionSpec : DescribeSpec({

    describe("ArcMotion") {

        lateinit var startPosition: Vector2
        lateinit var targetPosition: Vector2

        val speed = 5f
        val arcFactor = 1f
        val delta = 1f // 1 second

        val out = Vector2()

        beforeEach {
            startPosition = Vector2(0f, 0f)
            targetPosition = Vector2(10f, 10f)
        }

        it("should initialize with correct parameters") {
            val arcMotion = ArcMotion(startPosition, targetPosition, speed, arcFactor)

            arcMotion.startPosition shouldBe startPosition.cpy()
            arcMotion.targetPosition shouldBe targetPosition.cpy()
            arcMotion.speed shouldBe speed
            arcMotion.arcFactor shouldBe arcFactor
            arcMotion.getMotionValue(out) shouldBe startPosition.cpy()
        }

        describe("move over time") {

            it("1 - should update position correctly over time") {
                val arcMotion = ArcMotion(startPosition, targetPosition, speed, arcFactor)

                // Simulate 1 second of movement
                arcMotion.update(delta)

                // Compute the expected position at t = 0.5
                val distanceCovered = speed * delta
                val totalDistance = startPosition.dst(targetPosition)
                val t = (distanceCovered / totalDistance).coerceIn(0f, 1f)
                val expectedPosition = ArcMotion.computeBezierPoint(t, arcFactor, startPosition, targetPosition)

                arcMotion.getMotionValue(out) shouldBe expectedPosition
            }

            it("2 - should update position correctly over time") {
                val arcMotion = ArcMotion(startPosition, targetPosition, speed, arcFactor)

                // Simulate 2 seconds of movement
                arcMotion.update(delta)
                arcMotion.update(delta)

                // Compute the expected position at t = 1.0
                val distanceCovered = speed * delta * 2
                val totalDistance = startPosition.dst(targetPosition)
                val t = (distanceCovered / totalDistance).coerceIn(0f, 1f)
                val expectedPosition = ArcMotion.computeBezierPoint(t, arcFactor, startPosition, targetPosition)

                println(
                    "start=$startPosition, target=$targetPosition, expected=$expectedPosition, current=${
                        arcMotion.getMotionValue(
                            out
                        )
                    }"
                )

                arcMotion.getMotionValue(out) shouldBe expectedPosition
            }
        }

        it("should move to the target position when distance is covered") {
            val arcMotion = ArcMotion(startPosition, targetPosition, speed, arcFactor)
            arcMotion.update(5f)
            arcMotion.getMotionValue(out) shouldBe targetPosition
        }

        it("should reset to the start position") {
            val arcMotion = ArcMotion(startPosition, targetPosition, speed, arcFactor)

            arcMotion.update(delta)
            arcMotion.reset()
            arcMotion.getMotionValue(out) shouldBe startPosition
        }

        it("should handle edge cases correctly") {
            val arcMotion = ArcMotion(startPosition, targetPosition, speed, arcFactor)

            // Edge case: delta is zero
            arcMotion.update(0f)
            arcMotion.getMotionValue(out) shouldBe startPosition

            // Edge case: zero speed
            val arcMotionZeroSpeed = ArcMotion(startPosition, targetPosition, 0f, arcFactor)
            arcMotionZeroSpeed.update(delta)
            arcMotionZeroSpeed.getMotionValue(out) shouldBe startPosition
        }
    }
})
