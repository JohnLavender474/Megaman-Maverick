package com.mega.game.engine.common.shapes

import com.badlogic.gdx.math.Vector2
import com.mega.game.engine.common.extensions.round
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class GameCircleTest :
    DescribeSpec({
        lateinit var gameCircle: GameCircle

        beforeTest { gameCircle = GameCircle(10f, 10f, 5f) }

        describe("GameCircle tests") {
            it("should return the correct radius") { gameCircle.getRadius() shouldBe 5f }

            it("should calculate the area correctly") {
                gameCircle.getArea().round(5) shouldBe (Math.PI * 5 * 5).toFloat().round(5)
            }

            it("should calculate the circumference correctly") {
                gameCircle.getCircumference().round(5) shouldBe (2 * Math.PI * 5).toFloat().round(5)
            }

            it("should create a copy with the same properties") {
                val copiedCircle = gameCircle.copy()
                copiedCircle.getRadius() shouldBe gameCircle.getRadius()
                copiedCircle.getX() shouldBe gameCircle.getX()
                copiedCircle.getY() shouldBe gameCircle.getY()
            }

            it("should overlap with another circle") {
                val otherCircle = GameCircle(12f, 12f, 5f)
                gameCircle.overlaps(otherCircle) shouldBe true
            }

            it("should not overlap with another circle") {
                val otherCircle = GameCircle(20f, 20f, 5f)
                gameCircle.overlaps(otherCircle) shouldBe false
            }

            it("should overlap with a rectangle") {
                val rectangle = GameRectangle(8f, 8f, 4f, 4f)
                gameCircle.overlaps(rectangle) shouldBe true
            }

            it("should not overlap with a rectangle") {
                val rectangle = GameRectangle(20f, 20f, 4f, 4f)
                gameCircle.overlaps(rectangle) shouldBe false
            }

            it("should overlap with line") {
                val line = GameLine(8f, 8f, 12f, 12f)
                gameCircle.overlaps(line) shouldBe true
            }

            it("should not overlap with line") {
                val line = GameLine(20f, 20f, 24f, 24f)
                gameCircle.overlaps(line) shouldBe false
            }

            it("should contain a point") { gameCircle.contains(Vector2(10f, 10f)) shouldBe true }

            it("should not contain a point") { gameCircle.contains(Vector2(20f, 20f)) shouldBe false }
        }
    })
