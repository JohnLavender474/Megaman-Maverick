package com.megaman.maverick.game.utils

import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.mega.game.engine.common.objects.Loop
import com.mega.game.engine.common.shapes.GameCircle
import com.mega.game.engine.common.shapes.GameLine
import com.mega.game.engine.common.shapes.GameRectangle

object LoopedSuppliers {

    const val VECTOR2_COUNT = 10
    const val GAME_RECT_COUNT = 10
    const val RECT_COUNT = 5
    const val GAME_CIRCLE_COUNT = 10
    const val GAME_LINES = 5

    private val vector2s: Loop<Vector2>
    private val gameRects: Loop<GameRectangle>
    private val rects: Loop<Rectangle>
    private val gameCircles: Loop<GameCircle>
    private val gameLines: Loop<GameLine>

    init {
        val vector2s = Array<Vector2>()
        (0 until VECTOR2_COUNT).forEach { vector2s.add(Vector2())  }
        this.vector2s = Loop(vector2s)

        val gameRects = Array<GameRectangle>()
        (0 until GAME_RECT_COUNT).forEach { gameRects.add(GameRectangle()) }
        this.gameRects = Loop(gameRects)

        val rects = Array<Rectangle>()
        (0 until RECT_COUNT).forEach { rects.add(Rectangle()) }
        this.rects = Loop(rects)

        val gameCircles = Array<GameCircle>()
        (0 until GAME_CIRCLE_COUNT).forEach { gameCircles.add(GameCircle()) }
        this.gameCircles = Loop(gameCircles)

        val gameLines = Array<GameLine>()
        (0 until GAME_LINES).forEach { gameLines.add(GameLine()) }
        this.gameLines = Loop(gameLines)
    }

    fun getVector2() = vector2s.next()

    fun getGameRectangle() = gameRects.next()

    fun getGdxRectangle() = rects.next()

    fun getGameCircle() = gameCircles.next()

    fun getGameLine() = gameLines.next()
}
