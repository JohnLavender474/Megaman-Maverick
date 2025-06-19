package com.mega.game.engine.common.objects.table

import com.badlogic.gdx.utils.Array
import com.mega.game.engine.common.objects.IntPair

class Table<T>(elements: Array<Array<T>>) {

    private val table = Array<Array<TableNode<T>>>()

    init {
        for (row in 0 until elements.size) {
            val rowArray = Array<TableNode<T>>()

            for (column in 0 until elements[row].size) {
                val element = elements[row][column]
                rowArray.add(TableNode(element, row, column, this))
            }

            table.add(rowArray)
        }
    }

    fun get(row: Int, column: Int): TableNode<T> {
        if (row >= table.size || row < 0) throw IllegalArgumentException("Row out of bounds: $row")
        if (column >= table[row].size || column < 0) throw IllegalArgumentException("Column out of bounds: $column")
        return table[row][column]
    }

    fun rowCount() = table.size

    fun columnCount(row: Int): Int {
        if (row >= table.size || row < 0) return 0
        return table[row].size
    }

    fun findPositionOf(element: T): IntPair? {
        for (row in 0 until rowCount())
            for (column in 0 until columnCount(row))
                if (get(row, column).element == element) return IntPair(row, column)
        return null
    }

    override fun toString() = StringBuilder()
        .append("Table{\n")
        .also { builder -> table.forEach { row -> builder.append("\t$row\n") } }
        .append("}")
        .toString()
}

