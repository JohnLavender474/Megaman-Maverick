package com.maverick.game.engine.common.shapes

import com.badlogic.gdx.math.Circle
import com.badlogic.gdx.math.Intersector
import com.badlogic.gdx.math.Polygon
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import kotlin.math.max
import kotlin.math.min

object ShapeUtils {

    private const val HIT_EPSILON = 0.001f

    private val out1 = Vector2()
    private val out2 = Vector2()
    private val out3 = Vector2()
    private val out4 = Vector2()

    private val line1 = GameLine()
    private val line2 = GameLine()
    private val line3 = GameLine()
    private val line4 = GameLine()
    private val outLines = Array<GameLine>()

    fun intersectLines(line1: GameLine, line2: GameLine, out: Vector2): Boolean {
        line1.calculateWorldPoints(out1, out2)
        line2.calculateWorldPoints(out3, out4)
        val intersect = Intersector.intersectLines(out1, out2, out3, out4, out)
        return intersect
    }

    fun intersectRectangleAndLine(rectangle: GameRectangle, line: GameLine, out: Array<Vector2>): Int {
        if (out.size < 4) throw IllegalArgumentException("Out must hold at least 4 vectors: size=${out.size}")

        line.calculateWorldPoints(out1, out2)

        outLines.clear()
        val lines = rectangle.getAsLines(outLines, line1, line2, line3, line4)

        var count = 0

        for (element in lines) {
            element.calculateWorldPoints(out3, out4)

            val hit = out[count]
            if (Intersector.intersectSegments(out1, out2, out3, out4, hit)) {
                var duplicate = false
                for (i in 0 until count) if (out[i] == hit) duplicate = true
                if (!duplicate) count++
            }
        }

        return count
    }

    fun intersectPolygonAndLine(polygon: GamePolygon, line: GameLine, out: Array<Vector2>): Int {
        if (out.size < 2) throw IllegalArgumentException("Out must hold at least 2 vectors: size=${out.size}")

        line.calculateWorldPoints(out1, out2)
        val vertices = polygon.libgdxPolygon.transformedVertices

        var count = 0
        var i = 0
        while (i < vertices.size && count < out.size) {
            val j = (i + 2) % vertices.size
            out3.set(vertices[i], vertices[i + 1])
            out4.set(vertices[j], vertices[j + 1])

            val hit = out[count]
            if (Intersector.intersectSegments(out1, out2, out3, out4, hit)) {
                var duplicate = false
                for (k in 0 until count) if (out[k].epsilonEquals(hit, HIT_EPSILON)) duplicate = true
                if (!duplicate) count++
            }
            i += 2
        }

        return count
    }

    fun overlapCircleAndPolygon(circle: Circle, polygon: Polygon): Boolean {
        val vertices = polygon.transformedVertices

        val center = out3.set(circle.x, circle.y)

        val squareRadius = circle.radius * circle.radius

        for (i in vertices.indices step 2) {
            if (i == 0) {
                if (Intersector.intersectSegmentCircle(
                        out1.set(vertices[vertices.size - 2], vertices[vertices.size - 1]),
                        out2.set(vertices[0], vertices[1]),
                        center,
                        squareRadius
                    )
                ) return true
            } else if (Intersector.intersectSegmentCircle(
                    out1.set(vertices[i - 2], vertices[i - 1]),
                    out2.set(vertices[i], vertices[i + 1]),
                    center,
                    squareRadius
                )
            ) return true
        }

        return polygon.contains(circle.x, circle.y)
    }

    fun intersectRectangles(rect1: GameRectangle, rect2: GameRectangle, overlap: GameRectangle): Boolean {
        if (rect1.overlaps(rect2)) {
            overlap.setX(max(rect1.getX(), rect2.getX()))
            overlap.setWidth(min(rect1.getX() + rect1.getWidth(), rect2.getX() + rect2.getWidth()) - overlap.getX())
            overlap.setY(max(rect1.getY(), rect2.getY()))
            overlap.setHeight(min(rect1.getY() + rect1.getHeight(), rect2.getY() + rect2.getHeight()) - overlap.getY())
            return true
        }

        return false
    }
}
