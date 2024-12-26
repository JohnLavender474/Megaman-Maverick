package com.mega.game.engine.motion

import com.badlogic.gdx.math.Vector2
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class RotatingLineTest :
    DescribeSpec({
        describe("RotatingLine class") {

            val out = Vector2()

            it("should calculate endpoint correctly") {
                // Create a RotatingLine instance
                val origin = Vector2(0f, 0f)
                val radius = 5f
                val speed = 45f
                val rotatingLine = RotatingLine(origin, radius, speed)

                // Calculate the expected endpoint position
                val endpointY = 0f

                // Get the actual endpoint position from the RotatingLine
                val endpoint = rotatingLine.getEndPoint(out)

                // Compare the actual and expected endpoint positions
                endpoint.x shouldBe radius
                endpoint.y shouldBe endpointY
            }

            it("should update rotation angle correctly") {
                // Create a RotatingLine instance
                val origin = Vector2(0f, 0f)
                val radius = 5f
                val speed = 45f
                val rotatingLine = RotatingLine(origin, radius, speed)

                // Set the initial rotation angle
                val initialRotation = 30f
                rotatingLine.degreesOnReset = initialRotation
                rotatingLine.reset()

                // Update the rotation with a time delta
                val delta = 0.1f
                val expectedRotation = initialRotation + speed * delta
                rotatingLine.update(delta)

                // Check if the rotation angle matches the expected value
                rotatingLine.degrees shouldBe expectedRotation
            }

            it("should translate origin correctly") {
                // Create a RotatingLine instance
                val origin = Vector2(0f, 0f)
                val radius = 5f
                val speed = 45f
                val rotatingLine = RotatingLine(origin, radius, speed)

                // Translate the origin
                val translateX = 2.5f
                val translateY = 3.0f
                rotatingLine.translate(translateX, translateY)

                // Get the actual origin position
                val originPosition = rotatingLine.getOrigin(out)

                // Calculate the expected origin position after translation
                val expectedOriginX = origin.x + translateX
                val expectedOriginY = origin.y + translateY

                // Compare the actual and expected origin positions
                originPosition.x shouldBe expectedOriginX
                originPosition.y shouldBe expectedOriginY
            }
        }
    })
