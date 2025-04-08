package com.megaman.maverick.game.entities.bosses

import com.badlogic.gdx.utils.Array
import com.mega.game.engine.common.extensions.gdxArrayOf
import com.mega.game.engine.common.extensions.orderedMapOf
import com.mega.game.engine.common.objects.IntPair
import com.mega.game.engine.common.objects.pairTo

internal object ElecDevilConstants {

    internal const val PIECE_ROWS = 5
    internal const val PIECE_COLUMNS = 4

    internal val CELLS = orderedMapOf(
        0 pairTo gdxArrayOf(0, 1, 2, 3),
        1 pairTo gdxArrayOf(0, 1, 2, 3),
        2 pairTo gdxArrayOf(0, 1, 2, 3, 4),
        3 pairTo gdxArrayOf(0, 1, 2, 3, 4),
        4 pairTo gdxArrayOf(0, 1, 2, 3),
        5 pairTo gdxArrayOf(1, 2)
    )

    internal fun getRowColumnKey(row: Int, column: Int) = "row${row}column${column}"

    internal fun forEachCell(function: (Int, Int) -> Unit) = CELLS.forEach { entry ->
        val row = entry.key
        val columns = entry.value
        columns.forEach { column -> function.invoke(row, column) }
    }

    private val pieces = Array<IntPair>()

    internal fun fillPieceQueue(fromLeft: Boolean): Array<IntPair> {
        pieces.clear()

        val columns = if (fromLeft) (PIECE_COLUMNS - 1..0 step -1) else (0 until PIECE_COLUMNS)
        columns.forEach { column -> for (row in 0 until PIECE_ROWS) pieces.add(row pairTo column) }

        return pieces
    }
}
