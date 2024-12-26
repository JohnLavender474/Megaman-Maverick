package com.mega.game.engine.motion

import com.badlogic.gdx.math.Vector2
import com.mega.game.engine.common.extensions.round
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class PendulumTest :
    DescribeSpec({
        describe("Pendulum class") {
            val length = 10f
            val gravity = -9.81f * 32 // multiply by 32 to mock PPM conversion
            val anchor = Vector2(0f, 0f)
            val targetFPS = 60f

            val out = Vector2()

            lateinit var pendulum: Pendulum

            beforeEach { pendulum = Pendulum(length, gravity, anchor, targetFPS) }

            it("should update pendulum motion correctly") {
                pendulum.update(targetFPS)

                val expectedX = -2.665f
                val expectedY = 9.638f

                val endpoint = pendulum.getMotionValue(out)

                endpoint.x.round(3) shouldBe expectedX
                endpoint.y.round(3) shouldBe expectedY
            }

            it("should reset the pendulum motion correctly") {
                pendulum.update(targetFPS)

                pendulum.reset()

                val endpoint = pendulum.getMotionValue(out)
                val expectedX = 0f
                val expectedY = 0f

                endpoint.x shouldBe expectedX
                endpoint.y shouldBe expectedY
            }

            it("should calculate point from anchor correctly") {
                val distance = 5f
                val point = pendulum.getPointFromAnchor(distance)

                val expectedX = 5f
                val expectedY = 0f

                point.x shouldBe expectedX
                point.y shouldBe expectedY
            }
        }
    })
