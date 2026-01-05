package com.mega.game.engine.common.shapes

import com.badlogic.gdx.math.Circle
import com.badlogic.gdx.math.Intersector
import com.badlogic.gdx.math.Polygon
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectSet
import kotlin.math.max
import kotlin.math.min

object ShapeUtils {

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

    fun intersectRectangleAndLine(
        rectangle: GameRectangle, line: GameLine, overlaps: ObjectSet<Vector2>
    ): Boolean {
        line.calculateWorldPoints(out1, out2)

        outLines.clear()
        val lines = rectangle.getAsLines(outLines, line1, line2, line3, line4)

        var intersects = false

        for (element in lines) {
            element.calculateWorldPoints(out3, out4)

            val overlap = Vector2()
            if (Intersector.intersectSegments(out1, out2, out3, out4, overlap)) {
                overlaps.add(overlap)

                intersects = true
            }
        }

        return intersects
    }

    fun overlapCircleAndPolygon(circle: Circle, polygon: Polygon): Boolean {
        val vertices = polygon.transformedVertices

        val center = Vector2(circle.x, circle.y)

        val squareRadius = circle.radius * circle.radius

        for (i in vertices.indices step 2) {
            if (i == 0) {
                if (Intersector.intersectSegmentCircle(
                        Vector2(vertices[vertices.size - 2], vertices[vertices.size - 1]),
                        Vector2(vertices[0], vertices[1]),
                        center,
                        squareRadius
                    )
                ) return true
            } else if (Intersector.intersectSegmentCircle(
                    Vector2(vertices[i - 2], vertices[i - 1]),
                    Vector2(vertices[i], vertices[i + 1]),
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
