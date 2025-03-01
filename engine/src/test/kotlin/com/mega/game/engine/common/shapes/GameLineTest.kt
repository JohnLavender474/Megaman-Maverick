package com.mega.game.engine.common.shapes

import com.badlogic.gdx.math.Polyline
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.common.UtilMethods
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class GameLineTest : DescribeSpec({

    describe("GameLine class") {

        val out1 = Vector2()
        val out2 = Vector2()
        val out3 = Vector2()
        val out4 = Vector2()
        val outRect = GameRectangle()

        it("should construct a line with specified points") {
            val line = GameLine(0f, 0f, 3f, 4f)
            line.getLength() shouldBe 5f
            line.getMaxX() shouldBe 3f
            line.getMaxY() shouldBe 4f
        }

        it("should calculate line length correctly") {
            val line = GameLine(1f, 2f, 4f, 6f)
            line.getLength() shouldBe 5f
        }

        it("should correctly check containment of a point") {
            val line = GameLine(1f, 1f, 4f, 4f)
            val pointInside = Vector2(2f, 2f)
            val pointOutside = Vector2(5f, 5f)
            line.contains(pointInside) shouldBe true
            line.contains(pointOutside) shouldBe false
        }

        it("should correctly check containment using coordinates") {
            val line = GameLine(1f, 1f, 4f, 4f)
            val xInside = 2f
            val yInside = 2f
            val xOutside = 5f
            val yOutside = 5f
            line.contains(xInside, yInside) shouldBe true
            line.contains(xOutside, yOutside) shouldBe false
        }

        it("should calculate the center") {
            val line = GameLine(1f, 1f, 4f, 4f)
            val expectedCenter = Vector2(2.5f, 2.5f)
            line.getCenter(out1) shouldBe expectedCenter
        }

        it("should handle translation correctly") {
            val line = GameLine(1f, 1f, 4f, 4f)
            val translationX = 2f
            val translationY = 2f
            val expectedCenter = Vector2(4.5f, 4.5f)
            line.translate(translationX, translationY)
            line.getCenter(out1) shouldBe expectedCenter
        }

        it("should correctly check overlaps with another line") {
            val line1 = GameLine(1f, 1f, 4f, 4f)
            val line2 = GameLine(2f, 1f, 4f, 6f)
            val line3 = GameLine(5f, 1f, 8f, 4f)

            line1.overlaps(line2) shouldBe true
            line1.overlaps(line3) shouldBe false
        }

        it("local points and world points should be the same") {
            val line = GameLine(2f, 3f, 4f, 5f)
            val localPoint1 = line.getFirstLocalPoint(Vector2())
            val localPoint2 = line.getSecondLocalPoint(Vector2())

            val worldPoint1 = Vector2()
            val worldPoint2 = Vector2()
            line.calculateWorldPoints(worldPoint1, worldPoint2)

            (localPoint1 == worldPoint1) shouldBe true
            (localPoint2 == worldPoint2) shouldBe true
        }

        it("should have equal local and world points when local point 1 and origin are the same") {
            val testsToConduct = 100
            (0 until testsToConduct).forEach {
                val x1 = UtilMethods.getRandom(0f, 10f)
                val y1 = UtilMethods.getRandom(0f, 10f)
                val x2 = UtilMethods.getRandom(0f, 10f)
                val y2 = UtilMethods.getRandom(0f, 10f)

                val line = GameLine()
                line.setOrigin(x1, y1)
                line.setFirstLocalPoint(x1, y1)
                line.setSecondLocalPoint(x2, y2)

                line.getFirstLocalPoint(out1)
                line.getSecondLocalPoint(out2)
                line.calculateWorldPoints(out3, out4)

                out1.epsilonEquals(out3, 0.0001f) shouldBe true
                out2.epsilonEquals(out4, 0.0001f) shouldBe true
            }
        }

        it("should provide correct local and world points") {
            val testsToConduct = 25
            (0 until testsToConduct).forEach {
                val randomVarCount = 11

                val random = Array<Float>()
                (0 until randomVarCount).forEach { random.add(UtilMethods.getRandom(0, 359).toFloat()) }
                println("Vars: $random")

                // control line
                val controlLine = Polyline()
                controlLine.setVertices(floatArrayOf(random[0], random[1], random[2], random[3]))
                controlLine.setPosition(random[4], random[5])
                controlLine.setOrigin(random[6], random[7])
                controlLine.rotation = random[8]
                controlLine.setScale(random[9], random[10])

                // test line
                val testLine = GameLine(random[0], random[1], random[2], random[3])
                testLine.setPosition(random[4], random[5])
                testLine.setOrigin(random[6], random[7])
                testLine.rotation = random[8]
                testLine.scaleX = random[9]
                testLine.scaleY = random[10]

                // test local points
                val controlLocalPoints = controlLine.vertices
                println("Control local points: ${controlLocalPoints.contentToString()}")
                val testLocalPoint1 = testLine.getFirstLocalPoint(out1)
                val testLocalPoint2 = testLine.getSecondLocalPoint(out2)
                println("Test local points: $testLocalPoint1, $testLocalPoint2")
                testLocalPoint1 shouldBe Vector2(controlLocalPoints[0], controlLocalPoints[1])
                testLocalPoint2 shouldBe Vector2(controlLocalPoints[2], controlLocalPoints[3])

                // test world points
                val controlWorldPoints = controlLine.transformedVertices
                println("Control world points: ${controlWorldPoints.contentToString()}")
                testLine.calculateWorldPoints(out1, out2)
                println("Test world points: $out1, $out2")
                out1 shouldBe Vector2(controlWorldPoints[0], controlWorldPoints[1])
                out2 shouldBe Vector2(controlWorldPoints[2], controlWorldPoints[3])
            }
        }

        it("should set the center correctly") {
            val line = GameLine(0f, 0f, 1f, 1f)
            line.setCenter(Vector2(0.5f, 0.5f))
            val center = line.getCenter(out1)
            center shouldBe Vector2(0.5f, 0.5f)
        }

        it("should return correct local center") {
            val line = GameLine(0f, 0f, 1f, 1f)
            val localCenter = line.getLocalCenter(out1)
            localCenter shouldBe Vector2(0.5f, 0.5f)
        }

        it("should set X coordinate correctly") {
            val line = GameLine(0f, 0f, 1f, 1f)
            line.setX(2f)
            line.getX() shouldBe 2f
        }

        it("should set Y coordinate correctly") {
            val line = GameLine(0f, 0f, 1f, 1f)
            line.setY(2f)
            line.getY() shouldBe 2f
        }

        it("should provide correct maxX") {
            val line = GameLine(0f, 0f, 1f, 1f)
            val maxX = line.getMaxX()
            maxX shouldBe 1f
        }

        it("should provide correct maxY") {
            val line = GameLine(0f, 0f, 1f, 1f)
            val maxY = line.getMaxY()
            maxY shouldBe 1f
        }

        it("should translate correctly") {
            val line = GameLine(0f, 0f, 1f, 1f)
            line.translate(1f, 1f)
            val center = line.getCenter(out1)
            center shouldBe Vector2(1.5f, 1.5f)
        }

        it("should overlap rectangle") {
            val line = GameLine(0f, 0f, 1f, 1f)
            val rectangle = GameRectangle(0.5f, 0.5f, 1f, 1f)
            line.overlaps(rectangle) shouldBe true
        }

        it("should not overlap rectangle") {
            val line = GameLine(0f, 0f, 1f, 1f)
            val rectangle = GameRectangle(2f, 2f, 1f, 1f)
            line.overlaps(rectangle) shouldBe false
        }

        it("should overlap circle") {
            val line = GameLine(0f, 0f, 1f, 1f)
            val circle = GameCircle(0.5f, 0.5f, 1f)
            line.overlaps(circle) shouldBe true
        }

        it("should not overlap circle") {
            val line = GameLine(0f, 0f, 1f, 1f)
            val circle = GameCircle(3f, 3f, 1f)
            line.overlaps(circle) shouldBe false
        }

        describe("getBoundingRectangle()") {

            it("test 1") {
                val line = GameLine(1f, 1f, 3f, 3f)
                println(line)
                val bounds = line.getBoundingRectangle(outRect)
                bounds shouldBe GameRectangle(1f, 1f, 2f, 2f)
            }

            it("test 2") {
                val line = GameLine(-2f, -3f, 1f, 5f)
                println(line)
                val bounds = line.getBoundingRectangle(outRect)
                bounds shouldBe GameRectangle(-2f, -3f, 3f, 8f)
            }

            it("test 3") {
                val line = GameLine(1f, 2f, 3f, 4f)
                line.rotation = 90f
                println(line)
                val bounds = line.getBoundingRectangle(outRect)
                bounds shouldBe GameRectangle(-4f, 1f, 2f, 2f)
            }
        }
    }
})
