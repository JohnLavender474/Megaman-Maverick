package com.megaman.maverick.game.entities.bosses

import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.OrderedMap
import com.badlogic.gdx.utils.OrderedSet
import com.mega.game.engine.common.extensions.orderedMapOf
import com.mega.game.engine.common.extensions.orderedSetOf
import com.mega.game.engine.common.objects.IntPair
import com.mega.game.engine.common.objects.pairTo

internal object ElecDevilConstants {

    internal const val BODY_WIDTH = 5f
    internal const val BODY_HEIGHT = 4.75f

    internal const val BODY_SPRITE_WIDTH = 8f
    internal const val BODY_SPRITE_HEIGHT = 6f

    internal const val BODY_SPRITE_FACING_LEFT_OFFSET_X = 0.2f
    internal const val BODY_SPRITE_FACING_RIGHT_OFFSET_X = -0.2f

    internal const val PIECE_ROWS = 6
    internal const val PIECE_COLUMNS = 5

    internal const val PIECE_WIDTH = BODY_WIDTH.div(PIECE_COLUMNS)
    internal const val PIECE_HEIGHT = BODY_HEIGHT.div(PIECE_ROWS)

    internal val CELLS = orderedMapOf(
        0 pairTo orderedSetOf(0, 1, 2, 3),
        1 pairTo orderedSetOf(0, 1, 2, 3),
        2 pairTo orderedSetOf(0, 1, 2, 3, 4),
        3 pairTo orderedSetOf(0, 1, 2, 3, 4),
        4 pairTo orderedSetOf(0, 1, 2, 3),
        5 pairTo orderedSetOf(1, 2)
    )

    internal const val LIGHT_SOURCE_SEND_EVENT_DELAY = 0.1f

    internal fun fillMapWithColumnsAsKeys(out: OrderedMap<Int, OrderedSet<Int>>): OrderedMap<Int, OrderedSet<Int>> {
        forEachCell { row, column ->
            val rows = out[column] ?: OrderedSet<Int>()
            rows.add(row)
            out.put(column, rows)
        }
        return out
    }

    internal fun getRowColumnKey(row: Int, column: Int) = "row${row}column${column}"

    internal fun forEachCell(function: (Int, Int) -> Unit) = CELLS.forEach { entry ->
        val row = entry.key
        val columns = entry.value
        columns.forEach { column -> function.invoke(row, column) }
    }

    private val rows = Array<Int>()
    private val pieces = Array<IntPair>()

    internal fun fillPieceQueue(fromLeft: Boolean): Array<IntPair> {
        pieces.clear()

        val columns = if (fromLeft) (PIECE_COLUMNS - 1 downTo 0) else (0 until PIECE_COLUMNS)

        columns.forEach { column ->
            // clear, load, and shuffle rows
            rows.clear()
            (0 until PIECE_ROWS).forEach { rows.add(it) }
            rows.shuffle()

            rows.forEach { row -> if (CELLS[row].contains(column)) pieces.add(row pairTo column) }
        }

        return pieces
    }
}
