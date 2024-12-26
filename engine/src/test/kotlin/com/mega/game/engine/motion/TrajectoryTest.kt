package com.mega.game.engine.motion

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.common.extensions.gdxArrayOf
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class TrajectoryTest :
    DescribeSpec({
        describe("Trajectory class") {
            val ppm = 32
            val trajectoryDefinitionsString = "1.0, 2.0, 2.0; 2.0, 3.0, 1.0; 3.0, 4.0, 3.0"
            val trajectoryDefinitions =
                gdxArrayOf(
                    TrajectoryDefinition(1.0f, 2.0f, 2.0f),
                    TrajectoryDefinition(2.0f, 3.0f, 1.0f),
                    TrajectoryDefinition(3.0f, 4.0f, 3.0f)
                )

            val defaultTrajectory = Trajectory(Array())
            val parsedTrajectory = Trajectory(trajectoryDefinitionsString, ppm)
            val customTrajectory = Trajectory(trajectoryDefinitions, ppm)

            val out = Vector2()

            it("should initialize correctly with empty definitions") {
                val motionValue = defaultTrajectory.getMotionValue(out)
                motionValue shouldBe null
            }

            it("should parse and initialize correctly from a string") {
                val motionValue = parsedTrajectory.getMotionValue(out)
                motionValue shouldBe Vector2(1.0f * ppm, 2.0f * ppm)
            }

            it("should initialize correctly with custom definitions") {
                val motionValue = customTrajectory.getMotionValue(out)
                motionValue shouldBe Vector2(1.0f * ppm, 2.0f * ppm)
            }

            it("should update trajectory motion correctly") {
                // First definition (1.0, 2.0, 2.0)
                customTrajectory.update(1.0f)
                val motionValue1 = customTrajectory.getMotionValue(out)
                motionValue1 shouldBe Vector2(1.0f * ppm, 2.0f * ppm)

                // Second definition (2.0, 3.0, 1.0)
                customTrajectory.update(1.0f)
                val motionValue2 = customTrajectory.getMotionValue(out)
                motionValue2 shouldBe Vector2(2.0f * ppm, 3.0f * ppm)
            }

            it("should reset trajectory to its initial state") {
                customTrajectory.update(1.0f)
                customTrajectory.reset()
                val motionValue = customTrajectory.getMotionValue(out)
                motionValue shouldBe Vector2(3f * ppm, 4f * ppm)
            }
        }
    })
