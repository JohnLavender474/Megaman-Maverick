package com.mega.game.engine.common.shapes

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.OrderedSet
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class ShapeUtilsTest : DescribeSpec({

    describe("ShapeUtils") {

        // Test case for intersectRectangleAndLine
        describe("intersectRectangleAndLine method") {

            it("should intersect when the line crosses the rectangle") {
                val rectangle = GameRectangle(0f, 0f, 4f, 4f)
                val line = GameLine(2f, 5f, 2f, -1f) // A vertical line crossing the rectangle
                val intersections = OrderedSet<Vector2>()

                val result = ShapeUtils.intersectRectangleAndLine(rectangle, line, intersections)

                result shouldBe true
                intersections.size shouldBe 2 // The line should intersect at two points
                intersections.contains(Vector2(2f, 0f)) shouldBe true
                intersections.contains(Vector2(2f, 4f)) shouldBe true
            }

            it("should not intersect when the line is outside the rectangle") {
                val rectangle = GameRectangle(0f, 0f, 4f, 4f)
                val line = GameLine(5f, 5f, 6f, 6f) // A line outside and not touching the rectangle
                val intersections = OrderedSet<Vector2>()

                val result = ShapeUtils.intersectRectangleAndLine(rectangle, line, intersections)

                result shouldBe false
                intersections.size shouldBe 0 // No intersection
            }

            it("should intersect at one point when the line is tangent to the rectangle") {
                val rectangle = GameRectangle(0f, 0f, 4f, 4f)
                val line = GameLine(2f, -1f, 2f, 0f) // A vertical line touching the bottom of the rectangle
                val intersections = OrderedSet<Vector2>()

                val result = ShapeUtils.intersectRectangleAndLine(rectangle, line, intersections)

                result shouldBe true
                intersections.size shouldBe 1 // One point of intersection
                intersections.first() shouldBe Vector2(2f, 0f) // Tangent point
            }

            it("should handle diagonal line intersecting the rectangle") {
                val rectangle = GameRectangle(0f, 0f, 4f, 4f)
                val line = GameLine(0f, 0f, 4f, 4f) // A diagonal line crossing from bottom-left to top-right
                val intersections = OrderedSet<Vector2>()

                val result = ShapeUtils.intersectRectangleAndLine(rectangle, line, intersections)

                result shouldBe true
                intersections.size shouldBe 2 // The diagonal line intersects at two points
                intersections.contains(Vector2(0f, 0f)) shouldBe true
                intersections.contains(Vector2(4f, 4f)) shouldBe true
            }

            it("should handle case where line intersects two opposite corners of the rectangle") {
                val rectangle = GameRectangle(0f, 0f, 4f, 4f)
                val line = GameLine(-1f, -1f, 5f, 5f) // A diagonal line from outside touching two opposite corners
                val intersections = OrderedSet<Vector2>()

                val result = ShapeUtils.intersectRectangleAndLine(rectangle, line, intersections)

                result shouldBe true
                intersections.size shouldBe 2 // Two intersections at opposite corners
                intersections.contains(Vector2(0f, 0f)) shouldBe true
                intersections.contains(Vector2(4f, 4f)) shouldBe true
            }

            it("should not intersect when line is completely inside rectangle") {
                val rectangle = GameRectangle(0f, 0f, 4f, 4f)
                val line = GameLine(1f, 1f, 3f, 3f) // A line completely inside the rectangle
                val intersections = OrderedSet<Vector2>()

                val result = ShapeUtils.intersectRectangleAndLine(rectangle, line, intersections)

                result shouldBe false
                intersections.size shouldBe 0 // No intersection as the line is inside the rectangle
            }
        }
    }
})

