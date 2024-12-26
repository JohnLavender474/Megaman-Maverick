package com.mega.game.engine.common.shapes

import com.badlogic.gdx.math.Polygon
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class GamePolygonTest : DescribeSpec({
    describe("GamePolygon class") {

        lateinit var gamePolygon: GamePolygon

        val outFloatArr = Array<Float>()
        val outRect = GameRectangle()

        beforeEach {
            gamePolygon = GamePolygon(floatArrayOf(0f, 0f, 1f, 0f, 1f, 1f, 0f, 1f))
            outFloatArr.clear()
        }

        it("should contain the point") {
            gamePolygon.contains(Vector2(0.5f, 0.5f)) shouldBe true
        }

        it("should not contain the point") {
            gamePolygon.contains(Vector2(2f, 2f)) shouldBe false
        }

        it("should get the bounding rectangle") {
            val boundingRectangle = gamePolygon.getBoundingRectangle(outRect)
            boundingRectangle.getX() shouldBe 0f
            boundingRectangle.getY() shouldBe 0f
            boundingRectangle.getWidth() shouldBe 1f
            boundingRectangle.getHeight() shouldBe 1f
        }

        it("should overlap with another polygon") {
            val otherPolygon = GamePolygon(floatArrayOf(0.5f, 0.5f, 1.5f, 0.5f, 1.5f, 1.5f, 0.5f, 1.5f))
            gamePolygon.overlaps(otherPolygon) shouldBe true
        }

        it("should not overlap with another polygon") {
            val otherPolygon = GamePolygon(floatArrayOf(2f, 2f, 3f, 2f, 3f, 3f, 2f, 3f))
            gamePolygon.overlaps(otherPolygon) shouldBe false
        }

        it("should set and get origin") {
            gamePolygon.originX = 2.0f
            gamePolygon.originY = 2.0f
            gamePolygon.originX shouldBe 2.0f
            gamePolygon.originY shouldBe 2.0f
        }

        it("should set and get rotation") {
            gamePolygon.rotation = 45f
            gamePolygon.rotation shouldBe 45f
        }

        it("should set and get scale") {
            gamePolygon.scaleX = 2.0f
            gamePolygon.scaleY = 3.0f
            gamePolygon.scaleX shouldBe 2.0f
            gamePolygon.scaleY shouldBe 3.0f
        }

        it("should set and get vertices") {
            val vertices = floatArrayOf(0f, 0f, 1f, 0f, 1f, 1f, 0f, 1f)
            gamePolygon.setLocalVertices(vertices)

            outFloatArr.clear()
            gamePolygon.getLocalVertices(outFloatArr)

            outFloatArr.size shouldBe vertices.size
            for (i in 0 until vertices.size) outFloatArr[i] shouldBe vertices[i]
        }

        it("should rotate the polygon") {
            gamePolygon.rotate(90f)
            val polygon = Polygon(floatArrayOf(0f, 0f, 1f, 0f, 1f, 1f, 0f, 1f))
            polygon.rotate(90f)

            val rotatedVertices = polygon.transformedVertices
            outFloatArr.clear()
            gamePolygon.getLocalVertices(outFloatArr)
            outFloatArr.size shouldBe rotatedVertices.size

            outFloatArr.clear()
            gamePolygon.getTransformedVertices(outFloatArr)
            for (i in rotatedVertices.indices) outFloatArr[i] shouldBe rotatedVertices[i]
        }

        it("should scale the polygon") {
            gamePolygon.scale(2.0f)
            val polygon = Polygon(floatArrayOf(0f, 0f, 1f, 0f, 1f, 1f, 0f, 1f))
            polygon.scale(2.0f)
            val scaledVertices = polygon.transformedVertices

            gamePolygon.getLocalVertices(outFloatArr)
            outFloatArr.size shouldBe scaledVertices.size

            outFloatArr.clear()
            gamePolygon.getTransformedVertices(outFloatArr)

            for (i in scaledVertices.indices) outFloatArr[i] shouldBe scaledVertices[i]
        }

        it("should calculate the area of the polygon") {
            val area = gamePolygon.area()
            val polygon = Polygon(floatArrayOf(0f, 0f, 1f, 0f, 1f, 1f, 0f, 1f))
            val expectedArea = polygon.area()
            area shouldBe expectedArea
        }

        it("should get the centroid of the polygon") {
            val centroid = Vector2()
            gamePolygon.getCentroid(centroid)
            val polygon = Polygon(floatArrayOf(0f, 0f, 1f, 0f, 1f, 1f, 0f, 1f))
            val expectedCentroid = Vector2()
            polygon.getCentroid(expectedCentroid)
            centroid.x shouldBe expectedCentroid.x
            centroid.y shouldBe expectedCentroid.y
        }
    }
})
