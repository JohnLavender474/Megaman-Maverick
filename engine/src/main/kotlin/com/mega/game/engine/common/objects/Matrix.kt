package com.mega.game.engine.common.objects

import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.ObjectSet
import com.badlogic.gdx.utils.OrderedMap

class MatrixIterator<T>(private val matrix: Matrix<T>) : MutableIterator<T> {

    var rowIndex = 0
        private set
    var columnIndex = -1
        private set

    override fun hasNext(): Boolean {
        if (rowIndex >= matrix.rows) return false

        if (columnIndex + 1 >= matrix.columns) {
            rowIndex++
            columnIndex = 0
            return hasNext()
        }

        return try {
            matrix[columnIndex + 1, rowIndex] != null
        } catch (_: Exception) {
            false
        }
    }

    override fun next(): T =
        try {
            columnIndex++
            val element = matrix[columnIndex, rowIndex]
            element!!
        } catch (e: Exception) {
            throw Exception("Could not get next element at row $rowIndex and column $columnIndex", e)
        }

    override fun remove() {
        matrix[columnIndex, rowIndex] = null
    }
}

open class Matrix<T>(var rows: Int = 0, var columns: Int = 0) : MutableCollection<T> {

    override val size: Int
        get() = matrixMap.size

    internal val matrixMap = OrderedMap<IntPair, T>()
    internal val elementToIndexMap = ObjectMap<T, ObjectSet<IntPair>>()

    constructor(array: Array<Array<T>>) : this(array.size, array[0].size) {
        for (x in 0 until columns) for (y in 0 until rows) {
            val row = rows - 1 - y
            set(x, y, array[row][x])
        }
    }

    constructor(rows: Int, columns: Int, initializer: (Int, Int) -> T) : this(rows, columns) {
        for (x in 0 until columns) for (y in 0 until rows) set(x, y, initializer(x, y))
    }

    operator fun get(column: Int, row: Int): T? {
        // Indexes must be within bounds
        if (isColumnOutOfBounds(column)) throw IndexOutOfBoundsException("Column index $column is out of bounds")
        if (isRowOutOfBounds(row)) throw IndexOutOfBoundsException("Row index $row is out of bounds")

        return matrixMap[column pairTo row]
    }

    operator fun set(column: Int, row: Int, element: T?): T? {
        // Indexes must be within bounds
        if (isColumnOutOfBounds(column))
            throw IndexOutOfBoundsException("Column index $column is out of bounds")
        if (isRowOutOfBounds(row)) throw IndexOutOfBoundsException("Row index $row is out of bounds")

        // Convert row and column index to index pair
        val indexPair = column pairTo row

        // Remove the old value from the elementToIndexMap if it exists
        // This is done so that the elementToIndexMap does not contain any stale values
        val oldValue = matrixMap[indexPair]
        if (oldValue != null) {
            val oldValueSet = elementToIndexMap[oldValue]
            oldValueSet?.remove(indexPair)

            if (oldValueSet?.isEmpty == true) elementToIndexMap.remove(oldValue)
        } else matrixMap.remove(indexPair)

        // If the new value is null, then simply remove the index pair from the array 2D map,
        // otherwise add the index pair and element to the array 2D map and element to the element to
        // index map respectively
        if (element != null) {
            matrixMap.put(indexPair, element)
            if (!elementToIndexMap.containsKey(element)) elementToIndexMap.put(element, ObjectSet())
            val set = elementToIndexMap.get(element)
            set.add(indexPair)
        } else matrixMap.remove(indexPair)

        // Return the old value
        return oldValue
    }

    fun isRowOutOfBounds(rowIndex: Int) = rowIndex < 0 || rowIndex >= rows

    fun isColumnOutOfBounds(columnIndex: Int) = columnIndex < 0 || columnIndex >= columns

    fun isOutOfBounds(columnIndex: Int, rowIndex: Int) =
        isRowOutOfBounds(rowIndex) || isColumnOutOfBounds(columnIndex)

    fun getIndexes(element: T?) =
        if (element == null) {
            val nullIndexes = HashSet<IntPair>()
            for (x in 0 until columns) for (y in 0 until rows) if (this[x, y] == null)
                nullIndexes.add(x pairTo y)
            nullIndexes
        } else elementToIndexMap[element] ?: emptySet()


    fun forEach(action: ((Int, Int, T?) -> Unit)) {
        for (x in 0 until columns) for (y in 0 until rows) action(x, y, this[x, y])
    }

    override fun contains(element: T) = elementToIndexMap.containsKey(element)

    override fun containsAll(elements: Collection<T>) = elements.all { contains(it) }

    override fun clear() {
        matrixMap.clear()
        elementToIndexMap.clear()
    }

    override fun addAll(elements: Collection<T>) = elements.all { add(it) }

    override fun add(element: T): Boolean {
        for (x in 0 until columns) for (y in 0 until rows) if (matrixMap[x pairTo y] == null) {
            set(x, y, element)
            return true
        }

        return false
    }

    override fun isEmpty() = size == 0

    override fun iterator() = MatrixIterator(this)

    override fun retainAll(elements: Collection<T>): Boolean {
        var removed = false
        val toRemove = HashSet<T>()

        matrixMap.forEach { entry ->
            val e = entry.value
            e?.let {
                if (!elements.contains(it)) {
                    toRemove.add(it)
                    removed = true
                }
            }
        }

        toRemove.forEach { remove(it) }

        return removed
    }

    override fun removeAll(elements: Collection<T>): Boolean {
        elements.forEach { remove(it) }
        return true
    }

    override fun remove(element: T): Boolean {
        val indexPairs = elementToIndexMap.remove(element) ?: return false
        indexPairs.forEach { matrixMap.remove(it) }

        return true
    }

    override fun hashCode(): Int {
        var result = rows
        result = 31 * result + columns
        result = 31 * result + matrixMap.hashCode()
        result = 31 * result + elementToIndexMap.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Matrix<*>) {
            return false
        }
        for (x in 0 until columns) for (y in 0 until rows) if (this[x, y] != other[x, y]) return false
        return true
    }

    override fun toString(): String {
        val sb = StringBuilder()

        sb.append("[")
        for (y in rows - 1 downTo 0) {
            sb.append("[")
            for (x in 0 until columns) {
                sb.append(this[x, y])

                if (x < columns - 1) sb.append(", ")
            }
            sb.append("]")
            if (y > 0) sb.append(", ")
        }
        sb.append("]")

        return sb.toString()
    }
}
