package com.megaman.maverick.game.entities.hazards

import com.badlogic.gdx.graphics.g2d.Batch
import com.maverick.game.engine.common.enums.Direction
import com.maverick.game.engine.drawables.sorting.DrawingPriority
import com.maverick.game.engine.drawables.sprites.GameSprite
import kotlin.math.max
import kotlin.math.min

class LaserBeamSprite(priority: DrawingPriority) : GameSprite(priority) {

    companion object {
        private const val X = 0
        private const val Y = 1
        private const val C = 2
        private const val U = 3
        private const val V = 4
        private const val VERTEX_STRIDE = 5
        private const val QUAD_SIZE = 4 * VERTEX_STRIDE
    }

    private var quadVertices = FloatArray(0)
    private var shiftedVertices = FloatArray(0)
    private var quadCount = 0
    private var minX = 0f
    private var minY = 0f
    private var maxX = 0f
    private var maxY = 0f

    fun ensureCapacity(quads: Int) {
        if (quadVertices.size >= quads * QUAD_SIZE) return
        quadVertices = quadVertices.copyOf(quads * QUAD_SIZE)
        shiftedVertices = FloatArray(quads * QUAD_SIZE)
    }

    fun clearQuads() {
        quadCount = 0
    }

    fun addQuad(
        x0: Float,
        y0: Float,
        x1: Float,
        y1: Float,
        nearU: Float,
        farU: Float,
        topV: Float,
        bottomV: Float,
        direction: Direction
    ) {
        ensureCapacity(quadCount + 1)
        if (quadCount == 0) {
            minX = x0
            minY = y0
            maxX = x1
            maxY = y1
        } else {
            minX = min(minX, x0)
            minY = min(minY, y0)
            maxX = max(maxX, x1)
            maxY = max(maxY, y1)
        }

        val base = quadCount * QUAD_SIZE
        val color = packedColor
        when (direction) {
            Direction.RIGHT -> {
                putVertex(base, 0, x0, y0, color, nearU, bottomV)
                putVertex(base, 1, x0, y1, color, nearU, topV)
                putVertex(base, 2, x1, y1, color, farU, topV)
                putVertex(base, 3, x1, y0, color, farU, bottomV)
            }
            Direction.UP -> {
                putVertex(base, 0, x0, y0, color, nearU, topV)
                putVertex(base, 1, x0, y1, color, farU, topV)
                putVertex(base, 2, x1, y1, color, farU, bottomV)
                putVertex(base, 3, x1, y0, color, nearU, bottomV)
            }
            Direction.LEFT -> {
                putVertex(base, 0, x0, y0, color, farU, topV)
                putVertex(base, 1, x0, y1, color, farU, bottomV)
                putVertex(base, 2, x1, y1, color, nearU, bottomV)
                putVertex(base, 3, x1, y0, color, nearU, topV)
            }
            Direction.DOWN -> {
                putVertex(base, 0, x0, y0, color, farU, bottomV)
                putVertex(base, 1, x0, y1, color, nearU, bottomV)
                putVertex(base, 2, x1, y1, color, nearU, topV)
                putVertex(base, 3, x1, y0, color, farU, topV)
            }
        }
        quadCount++
    }

    fun applyQuadBounds() {
        if (quadCount == 0) return
        setBounds(minX, minY, maxX - minX, maxY - minY)
    }

    private fun putVertex(base: Int, corner: Int, x: Float, y: Float, color: Float, u: Float, v: Float) {
        val i = base + corner * VERTEX_STRIDE
        quadVertices[i + X] = x
        quadVertices[i + Y] = y
        quadVertices[i + C] = color
        quadVertices[i + U] = u
        quadVertices[i + V] = v
    }

    override fun draw(drawer: Batch) {
        if (hidden || texture == null || quadCount == 0) return
        drawShifted(drawer, drawOffsetX, drawOffsetY)
        if (wrapOffsetX != 0f) drawShifted(drawer, drawOffsetX + wrapOffsetX, drawOffsetY)
    }

    private fun drawShifted(drawer: Batch, offsetX: Float, offsetY: Float) {
        val count = quadCount * QUAD_SIZE
        if (offsetX == 0f && offsetY == 0f) {
            drawer.draw(texture, quadVertices, 0, count)
            return
        }
        quadVertices.copyInto(shiftedVertices, endIndex = count)
        for (i in 0 until count step VERTEX_STRIDE) {
            shiftedVertices[i + X] += offsetX
            shiftedVertices[i + Y] += offsetY
        }
        drawer.draw(texture, shiftedVertices, 0, count)
    }
}
