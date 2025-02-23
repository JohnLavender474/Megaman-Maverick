package com.mega.game.engine.common.shapes

import com.badlogic.gdx.math.Circle
import com.badlogic.gdx.math.Intersector
import com.badlogic.gdx.math.Polygon
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectSet

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

    fun intersectRectangleAndLine(
        rectangle: GameRectangle, line: GameLine, intersections: ObjectSet<Vector2>
    ): Boolean {
        line.calculateWorldPoints(out1, out2)

        outLines.clear()
        val lines = rectangle.getAsLines(outLines, line1, line2, line3, line4)

        var intersects = false

        for (element in lines) {
            element.calculateWorldPoints(out3, out4)

            val intersection = Vector2()
            if (Intersector.intersectSegments(out1, out2, out3, out4, intersection)) {
                intersections.add(intersection)

                intersects = true
            }
        }

        return intersects
    }

    fun overlaps(circle: Circle, polygon: Polygon): Boolean {
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
}
