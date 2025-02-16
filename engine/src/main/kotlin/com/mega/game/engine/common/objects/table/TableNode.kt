package com.mega.game.engine.common.objects.table

class TableNode<T>(
    val element: T,
    val row: Int,
    val column: Int,
    private val table: Table<T>
) {

    fun previousRow(): TableNode<T> {
        val newRow = if (row == table.rowCount() - 1) 0 else row + 1
        val newCol = column.coerceAtMost(table.columnCount(newRow) - 1)
        return table.get(newRow, newCol)
    }

    fun nextRow(): TableNode<T> {
        val newRow = if (row == 0) table.rowCount() - 1 else row - 1
        val newCol = column.coerceAtMost(table.columnCount(newRow) - 1)
        return table.get(newRow, newCol)
    }

    fun previousColumn(): TableNode<T> {
        val newCol = if (column == 0) table.columnCount(row) - 1 else column - 1
        return table.get(row, newCol)
    }

    fun nextColumn(): TableNode<T> {
        val newCol = if (column == table.columnCount(row) - 1) 0 else column + 1
        return table.get(row, newCol)
    }

    override fun toString() = "TableNode{ element=$element, row=$row, column=$column }"
}
