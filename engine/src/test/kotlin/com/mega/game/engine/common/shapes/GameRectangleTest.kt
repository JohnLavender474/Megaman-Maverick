package com.mega.game.engine.common.shapes

import com.badlogic.gdx.math.Vector2
import com.mega.game.engine.common.enums.Direction
import com.mega.game.engine.common.enums.Position
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class GameRectangleTest :
    DescribeSpec({
        describe("GameRectangle class") {

            lateinit var gameRectangle: GameRectangle

            val outVec1 = Vector2()
            val outGRect1 = GameRectangle()

            beforeEach { gameRectangle = GameRectangle(1f, 2f, 3f, 4f) }

            it("should contain the point") {
                gameRectangle.contains(Vector2(2f, 3f)) shouldBe true
            }

            it("should not contain the point") {
                gameRectangle.contains(Vector2(10f, 10f)) shouldBe false
            }

            it("should get the position values") {
                gameRectangle.getX() shouldBe 1f
                gameRectangle.getY() shouldBe 2f
                gameRectangle.getMaxX() shouldBe 4f
                gameRectangle.getMaxY() shouldBe 6f
            }

            it("should get the center") { gameRectangle.getCenter(outVec1) shouldBe Vector2(2.5f, 4f) }

            it("should set the center") {
                gameRectangle.setCenter(5f, 6f)
                gameRectangle.getX() shouldBe 3.5f
                gameRectangle.getY() shouldBe 4f
            }

            it("should get the maximum X value") { gameRectangle.getMaxX() shouldBe 4f }

            it("should get the maximum Y value") { gameRectangle.getMaxY() shouldBe 6f }

            it("should position on a point") {
                Position.values().forEach {
                    gameRectangle.positionOnPoint(Vector2(7f, 8f), it)
                    gameRectangle.getPositionPoint(it, outVec1) shouldBe Vector2(7f, 8f)
                }
            }

            it("should get position point") {
                gameRectangle.getPositionPoint(Position.CENTER, outVec1) shouldBe Vector2(2.5f, 4f)
            }

            it("should get the bounding rectangle") {
                val boundingRectangle = gameRectangle.getBoundingRectangle(outGRect1)
                boundingRectangle.getX() shouldBe 1f
                boundingRectangle.getY() shouldBe 2f
                boundingRectangle.getWidth() shouldBe 3f
                boundingRectangle.getHeight() shouldBe 4f
            }

            it("should overlap circle") {
                val circle = GameCircle(2f, 3f, 1f)
                gameRectangle.overlaps(circle) shouldBe true
            }

            it("should not overlap circle") {
                val circle = GameCircle(10f, 10f, 1f)
                gameRectangle.overlaps(circle) shouldBe false
            }

            it("should overlap line") {
                val line = GameLine(2f, 3f, 4f, 5f)
                gameRectangle.overlaps(line) shouldBe true
            }

            it("should not overlap line") {
                val line = GameLine(10f, 10f, 12f, 12f)
                gameRectangle.overlaps(line) shouldBe false
            }

            describe("should rotate correctly") {
                it("rotation test 1") {
                    val actualRectangle = GameRectangle(1f, 2f, 3f, 4f)
                    actualRectangle.rotate(Direction.LEFT.rotation, 0f, 0f)

                    val expectedRectangle = GameRectangle(-6f, 1f, 4f, 3f)
                    actualRectangle shouldBe expectedRectangle
                }

                it("rotation test 2") {
                    val actualRectangle = GameRectangle(1f, 2f, 3f, 4f)
                    actualRectangle.rotate(Direction.DOWN.rotation, 0f, 0f)

                    val expectedRectangle = GameRectangle(-4f, -6f, 3f, 4f)
                    actualRectangle shouldBe expectedRectangle
                }

                it("rotation test 3") {
                    val actualRectangle = GameRectangle(1f, 2f, 3f, 4f)
                    actualRectangle.rotate(Direction.RIGHT.rotation, 0f, 0f)

                    val expectedRectangle = GameRectangle(2f, -4f, 4f, 3f)
                    actualRectangle shouldBe expectedRectangle
                }

                it("rotation test 4") {
                    val actualRectangle = GameRectangle(1f, 2f, 3f, 4f)
                    actualRectangle.rotate(Direction.UP.rotation, 0f, 0f)

                    val expectedRectangle = GameRectangle(1f, 2f, 3f, 4f)
                    actualRectangle shouldBe expectedRectangle
                }

                it("rotation test 5") {
                    var actualRectangle = GameRectangle(1f, 2f, 3f, 4f)
                    actualRectangle.rotate(Direction.LEFT.rotation, 1.5f, 0f)

                    val expectedRectangle = GameRectangle(-4.5f, -0.5f, 4f, 3f)
                    actualRectangle shouldBe expectedRectangle
                }

                it("rotation test 6") {
                    var actualRectangle = GameRectangle(1f, 2f, 3f, 4f)
                    actualRectangle.rotate(Direction.LEFT.rotation, 0f, 3.5f)

                    val expectedRectangle = GameRectangle(-2.5f, 4.5f, 4f, 3f)
                    actualRectangle shouldBe expectedRectangle
                }

                it("rotation test 7") {
                    var actualRectangle = GameRectangle(1f, 2f, 3f, 4f)
                    actualRectangle.rotate(Direction.LEFT.rotation, -1.5f, 3.5f)

                    val expectedRectangle = GameRectangle(-4f, 6f, 4f, 3f)
                    actualRectangle shouldBe expectedRectangle
                }
            }
        }
    })
