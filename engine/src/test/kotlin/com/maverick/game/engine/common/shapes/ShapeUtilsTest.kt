package com.maverick.game.engine.common.shapes

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

class ShapeUtilsTest : DescribeSpec({

    val out1 = Vector2()
    val out2 = Vector2()
    val hitPoints = Array<Vector2>().apply { repeat(4) { add(Vector2()) } }

    fun hits(rectangle: GameRectangle, line: GameLine): List<Vector2> {
        val count = ShapeUtils.intersectRectangleAndLine(rectangle, line, hitPoints)
        return (0 until count).map { hitPoints[it] }
    }

    describe("ShapeUtils") {

        describe("intersectPolygonAndLine method") {

            fun polygonHits(polygon: GamePolygon, line: GameLine): Set<Vector2> {
                val count = ShapeUtils.intersectPolygonAndLine(polygon, line, hitPoints)
                return (0 until count).map { Vector2(hitPoints[it]) }.toSet()
            }

            it("hits the two faces of a V that a vertical line crosses") {
                val polygon = GamePolygon(floatArrayOf(0f, 32f, 32f, 0f, 64f, 32f))
                polygonHits(polygon, GameLine(16f, -10f, 16f, 50f)) shouldBe setOf(Vector2(16f, 16f), Vector2(16f, 32f))
            }

            it("uses the polygon position") {
                val polygon = GamePolygon(floatArrayOf(0f, 32f, 32f, 0f, 64f, 32f))
                polygon.setPosition(100f, 200f)
                polygonHits(polygon, GameLine(116f, 190f, 116f, 250f)) shouldBe
                    setOf(Vector2(116f, 216f), Vector2(116f, 232f))
            }

            it("counts a hit on a vertex once") {
                val polygon = GamePolygon(floatArrayOf(0f, 32f, 32f, 0f, 64f, 32f))
                polygonHits(polygon, GameLine(32f, -10f, 32f, 50f)) shouldBe setOf(Vector2(32f, 0f), Vector2(32f, 32f))
            }

            it("gives no hit for a line that misses") {
                val polygon = GamePolygon(floatArrayOf(0f, 32f, 32f, 0f, 64f, 32f))
                polygonHits(polygon, GameLine(4f, -10f, 4f, 20f)) shouldBe emptySet()
            }
        }

        // Test case for intersectRectangleAndLine
        describe("intersectRectangleAndLine method") {

            it("should intersect when the line crosses the rectangle") {
                val rectangle = GameRectangle(0f, 0f, 4f, 4f)
                val line = GameLine(2f, 5f, 2f, -1f) // A vertical line crossing the rectangle

                val intersections = hits(rectangle, line)
                val result = intersections.isNotEmpty()

                result shouldBe true
                intersections.size shouldBe 2 // The line should intersect at two points
                intersections.contains(Vector2(2f, 0f)) shouldBe true
                intersections.contains(Vector2(2f, 4f)) shouldBe true
            }

            it("should not intersect when the line is outside the rectangle") {
                val rectangle = GameRectangle(0f, 0f, 4f, 4f)
                val line = GameLine(5f, 5f, 6f, 6f) // A line outside and not touching the rectangle

                val intersections = hits(rectangle, line)
                val result = intersections.isNotEmpty()

                result shouldBe false
                intersections.size shouldBe 0 // No intersection
            }

            it("should intersect at one point when the line is tangent to the rectangle") {
                val rectangle = GameRectangle(0f, 0f, 4f, 4f)
                val line = GameLine(2f, -1f, 2f, 0f) // A vertical line touching the bottom of the rectangle

                val intersections = hits(rectangle, line)
                val result = intersections.isNotEmpty()

                result shouldBe true
                intersections.size shouldBe 1 // One point of intersection
                intersections.first() shouldBe Vector2(2f, 0f) // Tangent point
            }

            it("should handle diagonal line intersecting the rectangle") {
                val rectangle = GameRectangle(0f, 0f, 4f, 4f)
                val line = GameLine(0f, 0f, 4f, 4f) // A diagonal line crossing from bottom-left to top-right

                val intersections = hits(rectangle, line)
                val result = intersections.isNotEmpty()

                result shouldBe true
                intersections.size shouldBe 2 // The diagonal line intersects at two points
                intersections.contains(Vector2(0f, 0f)) shouldBe true
                intersections.contains(Vector2(4f, 4f)) shouldBe true
            }

            it("should handle case where line intersects two opposite corners of the rectangle") {
                val rectangle = GameRectangle(0f, 0f, 4f, 4f)
                val line = GameLine(-1f, -1f, 5f, 5f) // A diagonal line from outside touching two opposite corners

                val intersections = hits(rectangle, line)
                val result = intersections.isNotEmpty()

                result shouldBe true
                intersections.size shouldBe 2 // Two intersections at opposite corners
                intersections.contains(Vector2(0f, 0f)) shouldBe true
                intersections.contains(Vector2(4f, 4f)) shouldBe true
            }

            it("should not intersect when line is completely inside rectangle") {
                val rectangle = GameRectangle(0f, 0f, 4f, 4f)
                val line = GameLine(1f, 1f, 3f, 3f) // A line completely inside the rectangle

                val intersections = hits(rectangle, line)
                val result = intersections.isNotEmpty()

                result shouldBe false
                intersections.size shouldBe 0 // No intersection as the line is inside the rectangle
            }

            it("writes the hits into the given vectors and counts a corner hit once") {
                val rectangle = GameRectangle(0f, 0f, 4f, 4f)
                val line = GameLine(-1f, -1f, 5f, 5f)

                val count = ShapeUtils.intersectRectangleAndLine(rectangle, line, hitPoints)

                count shouldBe 2
                (0 until count).map { hitPoints[it] }.toSet() shouldBe setOf(Vector2(0f, 0f), Vector2(4f, 4f))
                hitPoints.size shouldBe 4
            }

            it("throws when the out array holds fewer than 4 vectors") {
                val out = Array<Vector2>().apply { repeat(3) { add(Vector2()) } }
                shouldThrow<IllegalArgumentException> {
                    ShapeUtils.intersectRectangleAndLine(GameRectangle(0f, 0f, 4f, 4f), GameLine(2f, 5f, 2f, -1f), out)
                }
            }

            describe("should intersect line world points") {

                it("test 1") {
                    val line = GameLine(1f, 1f, 3f, 3f)
                    line.rotation = 90f
                    line.calculateWorldPoints(out1, out2)
                    println("out1=$out1, out2=$out2")
                    out1 shouldBe Vector2(-1f, 1f)
                    out2 shouldBe Vector2(-3f, 3f)

                    val rectangle = GameRectangle(-3f, 2f, 2f, 2f)
                    println("rectangle=$rectangle")
                    val intersections = hits(rectangle, line)
                    println("intersections=$intersections")

                    intersections.size shouldBe 2
                    intersections shouldContain Vector2(-3f, 3f)
                    intersections shouldContain Vector2(-2f, 2f)
                }

                it("test 2") {
                    val line = GameLine(1f, 1f, 3f, 3f)
                    line.rotation = 180f
                    line.calculateWorldPoints(out1, out2)
                    println("out1=$out1, out2=$out2")
                    out1 shouldBe Vector2(-1f, -1f)
                    out2 shouldBe Vector2(-3f, -3f)

                    val rectangle = GameRectangle(-3f, -3f, 2f, 2f)
                    println("rectangle=$rectangle")
                    val intersections = hits(rectangle, line)
                    println("intersections=$intersections")

                    intersections.size shouldBe 2
                    intersections shouldContain Vector2(-3f, -3f)
                    intersections shouldContain Vector2(-1f, -1f)
                }

                it("test 3") {
                    val line = GameLine(1f, 1f, 3f, 3f)
                    line.setOrigin(3f, 3f)
                    line.rotation = 270f
                    line.calculateWorldPoints(out1, out2)
                    println("out1=$out1, out2=$out2")
                    out1 shouldBe Vector2(1f, 5f)
                    out2 shouldBe Vector2(3f, 3f)

                    val rectangle = GameRectangle(1f, 3f, 2f, 1f)
                    println("rectangle=$rectangle")
                    val intersections = hits(rectangle, line)
                    println("intersections=$intersections")

                    intersections.size shouldBe 2
                    intersections shouldContain Vector2(2f, 4f)
                    intersections shouldContain Vector2(3f, 3f)
                }
            }
        }
    }
})

