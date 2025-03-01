package com.mega.game.engine.motion

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.common.UtilMethods
import com.mega.game.engine.common.UtilMethods.getRandom
import com.mega.game.engine.common.shapes.GameLine
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class RotatingLineTest : DescribeSpec({

    describe("RotatingLine class") {

        val out1 = Vector2()
        val out2 = Vector2()
        val out3 = Vector2()
        val out4 = Vector2()
        val outFloatArr = Array<Float>()

        it("should calculate endpoint correctly") {
            val origin = Vector2(0f, 0f)
            val radius = 5f
            val speed = 45f
            val rotatingLine = RotatingLine(origin, radius, speed)

            val endpointY = 0f

            val endpoint = rotatingLine.getEndPoint(out1)

            endpoint.x shouldBe radius
            endpoint.y shouldBe endpointY
        }

        it("should update rotation angle correctly") {
            val origin = Vector2(0f, 0f)
            val radius = 5f
            val speed = 45f
            val rotatingLine = RotatingLine(origin, radius, speed)

            val initialRotation = 30f
            rotatingLine.degreesOnReset = initialRotation
            rotatingLine.reset()

            val delta = 0.1f
            val expectedRotation = initialRotation + speed * delta
            rotatingLine.update(delta)

            rotatingLine.degrees shouldBe expectedRotation
        }

        it("should translate origin correctly") {
            val origin = Vector2(0f, 0f)
            val radius = 5f
            val speed = 45f
            val rotatingLine = RotatingLine(origin, radius, speed)

            val translateX = 2.5f
            val translateY = 3.0f
            rotatingLine.translate(translateX, translateY)

            val originPosition = rotatingLine.getOrigin(out1)

            val expectedOriginX = origin.x + translateX
            val expectedOriginY = origin.y + translateY

            originPosition.x shouldBe expectedOriginX
            originPosition.y shouldBe expectedOriginY
        }

        it("should calculate scaled position correctly") {
            val origin = Vector2(0f, 0f)
            val radius = 5f
            val speed = 45f
            val rotatingLine = RotatingLine(origin, radius, speed)

            val scalar = 0.5f

            val scaledPosition = rotatingLine.getScaledPosition(scalar, out1)

            val expectedX = origin.x + (radius * scalar)
            val expectedY = origin.y

            scaledPosition.x shouldBe expectedX
            scaledPosition.y shouldBe expectedY
        }

        it("should reset rotation to the initial value") {
            val origin = Vector2(0f, 0f)
            val radius = 5f
            val speed = 45f
            val rotatingLine = RotatingLine(origin, radius, speed)

            val initialRotation = 30f
            rotatingLine.degreesOnReset = initialRotation
            rotatingLine.reset()

            rotatingLine.update(0.1f)

            rotatingLine.reset()
            rotatingLine.degrees shouldBe initialRotation
        }

        it("should correctly set and get start and end points") {
            val origin = Vector2(0f, 0f)
            val radius = 5f
            val speed = 45f
            val rotatingLine = RotatingLine(origin, radius, speed)

            val startPoint = Vector2(0f, 0f)
            val endPoint = Vector2(radius, 0f)

            rotatingLine.set(startPoint, endPoint)

            val retrievedStartPoint = rotatingLine.getStartPoint(out1)
            val retrievedEndPoint = rotatingLine.getEndPoint(out2)

            retrievedStartPoint.x shouldBe startPoint.x
            retrievedStartPoint.y shouldBe startPoint.y
            retrievedEndPoint.x shouldBe endPoint.x
            retrievedEndPoint.y shouldBe endPoint.y
        }

        it("should calculate local points and transformed points correctly") {
            val testCount = 50
            val randomVarCount = 4
            val updatesPerTest = 25
            val deltaSupplier: () -> Float = { getRandom(0f, 0.5f, 1f) }
            val scaleSupplier: (Int) -> Float = { if (it % 2 == 0) 1f else getRandom(0.5f, 2f) }
            val updateTeardown: () -> Unit = { outFloatArr.clear() }

            (1..testCount).forEach { testNum ->
                println("Test$testNum: begin ---")

                val randomVars = Array<Float>()
                (0 until randomVarCount).forEach { randomVars.add(UtilMethods.getRandom(0, 359).toFloat()) }
                println("Test$testNum: set up randomVars: $randomVars")

                val origin = Vector2(randomVars[0], randomVars[1])
                val speed = randomVars[2]
                val radius = randomVars[3]
                val scaleX = scaleSupplier.invoke(testCount)
                val scaleY = scaleSupplier.invoke(testCount)

                // Test line (RotatingLine)
                val rotatingLine = RotatingLine(origin, radius, speed, 0f)
                val endPoint = rotatingLine.getEndPoint(out1)
                rotatingLine.scaleX = scaleX
                rotatingLine.scaleY = scaleY
                println("Test$testNum: set up rotatingLine: $rotatingLine")

                // Control line (GameLine: use vars from test line to begin with)
                val controlLine = GameLine(origin, endPoint)
                controlLine.setOrigin(origin)
                controlLine.rotation = 0f
                controlLine.scaleX = scaleX
                controlLine.scaleY = scaleY
                println("Test$testNum: set up controlLine: $controlLine")

                (1..updatesPerTest).forEach { updateNum ->
                    val delta = deltaSupplier.invoke()
                    val controlRotationDelta = speed * delta
                    println("Test$testNum: update$updateNum: delta=$delta, controlRotationDelta=$controlRotationDelta")

                    controlLine.rotation += controlRotationDelta
                    println("Test$testNum: update$updateNum: conttrolLine after update: $controlLine")

                    rotatingLine.update(delta)
                    println("Test$testNum: update$updateNum: rotatingLine after update: $rotatingLine")

                    val controlLocalPoint1 = controlLine.getFirstLocalPoint(out1)
                    val controlLocalPoint2 = controlLine.getSecondLocalPoint(out2)
                    println(
                        "Test$testNum: update$updateNum: " +
                            "controlLocalPoint1=$controlLocalPoint1, " +
                            "controlLocalPoint2=$controlLocalPoint2"
                    )
                    val testLocalPoint1 = rotatingLine.line.getFirstLocalPoint(out3)
                    val testLocalPoint2 = rotatingLine.line.getSecondLocalPoint(out4)
                    println(
                        "Test$testNum: update$updateNum: " +
                            "testLocalPoint1=$testLocalPoint1, " +
                            "testLocalPoint2=$testLocalPoint2"
                    )
                    testLocalPoint1.epsilonEquals(controlLocalPoint1, 0.01f) shouldBe true
                    testLocalPoint2.epsilonEquals(controlLocalPoint2, 0.01f) shouldBe true

                    val controlTransformedVerts = controlLine.getTransformedVertices(outFloatArr)
                    println("Test$testNum: update$updateNum: controlTransformedVerts=$controlTransformedVerts")
                    val testTransformedPoint1 = rotatingLine.getOrigin(out1)
                    val testTransformedPoint2 = rotatingLine.getEndPoint(out2)
                    println(
                        "Test$testNum: update$updateNum: " +
                            "testTransformedPoint1=$testTransformedPoint1 " +
                            "testTransformedPoint2=$testTransformedPoint2"
                    )
                    testTransformedPoint1.epsilonEquals(
                        controlTransformedVerts[0],
                        controlTransformedVerts[1],
                        0.01f
                    ) shouldBe true
                    testTransformedPoint2.epsilonEquals(
                        controlTransformedVerts[2],
                        controlTransformedVerts[3],
                        0.01f
                    ) shouldBe true

                    println("Test$testNum: update$updateNum: invoking updateTeardown")
                    updateTeardown.invoke()
                }

                println("Test$testNum: end ---\n")
            }
        }
    }
})
