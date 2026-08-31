package com.mega.game.engine.common.objects

import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.IntSet
import com.badlogic.gdx.utils.ObjectMap

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

/**
 * A 2D grid backed by a flat array indexed `row * columns + column`.
 *
 * Cells are sparse in the sense that an unset cell reads back as null; setting a cell to null clears it. The backing
 * array is allocated for the full `rows * columns` extent and only grows, so re-dimensioning a reused instance is
 * allocation-free as long as the new extent fits within the current capacity.
 *
 * [elementToIndexMap] is a reverse index used by the element-based [MutableCollection] operations ([contains],
 * [remove], [getIndexes]) so that they stay O(1)/O(k). It stores packed `row * columns + column` indexes rather than
 * [IntPair]s so that [set] does not allocate.
 */
open class Matrix<T>(rows: Int = 0, columns: Int = 0) : MutableCollection<T> {

    // declared before `rows` and `columns` so that it is initialized before their setters can run
    private var elements: kotlin.Array<Any?> = arrayOfNulls(rows * columns)
    private var count = 0

    internal val elementToIndexMap = ObjectMap<T, IntSet>()

    /**
     * Re-dimensioning discards the contents: the flat index of a cell depends on [columns], so existing entries would
     * not survive a change meaningfully. Every caller in this codebase calls [clear] before re-dimensioning anyway.
     */
    var rows: Int = rows
        set(value) {
            if (field == value) return
            field = value
            resize()
        }

    var columns: Int = columns
        set(value) {
            if (field == value) return
            field = value
            resize()
        }

    override val size: Int
        get() = count

    constructor(array: Array<Array<T>>) : this(array.size, array[0].size) {
        for (x in 0 until columns) for (y in 0 until rows) {
            val row = rows - 1 - y
            set(x, y, array[row][x])
        }
    }

    constructor(rows: Int, columns: Int, initializer: (Int, Int) -> T) : this(rows, columns) {
        for (x in 0 until columns) for (y in 0 until rows) set(x, y, initializer(x, y))
    }

    private fun resize() {
        val needed = rows * columns
        if (needed > elements.size) elements = arrayOfNulls(needed) else elements.fill(null)

        count = 0
        elementToIndexMap.clear()
    }

    operator fun get(column: Int, row: Int): T? {
        // Indexes must be within bounds
        if (isColumnOutOfBounds(column)) throw IndexOutOfBoundsException("Column index $column is out of bounds")
        if (isRowOutOfBounds(row)) throw IndexOutOfBoundsException("Row index $row is out of bounds")

        @Suppress("UNCHECKED_CAST")
        return elements[row * columns + column] as T?
    }

    operator fun set(column: Int, row: Int, element: T?): T? {
        // Indexes must be within bounds
        if (isColumnOutOfBounds(column))
            throw IndexOutOfBoundsException("Column index $column is out of bounds")
        if (isRowOutOfBounds(row)) throw IndexOutOfBoundsException("Row index $row is out of bounds")

        val index = row * columns + column

        @Suppress("UNCHECKED_CAST")
        val oldValue = elements[index] as T?

        // Remove the old value from the elementToIndexMap if it exists
        // This is done so that the elementToIndexMap does not contain any stale values
        if (oldValue != null) {
            val oldIndexes = elementToIndexMap[oldValue]
            oldIndexes?.remove(index)

            if (oldIndexes != null && oldIndexes.size == 0) elementToIndexMap.remove(oldValue)

            count--
        }

        elements[index] = element

        if (element != null) {
            var indexes = elementToIndexMap[element]
            if (indexes == null) {
                indexes = IntSet()
                elementToIndexMap.put(element, indexes)
            }
            indexes.add(index)

            count++
        }

        // Return the old value
        return oldValue
    }

    fun isRowOutOfBounds(rowIndex: Int) = rowIndex < 0 || rowIndex >= rows

    fun isColumnOutOfBounds(columnIndex: Int) = columnIndex < 0 || columnIndex >= columns

    fun isOutOfBounds(columnIndex: Int, rowIndex: Int) =
        isRowOutOfBounds(rowIndex) || isColumnOutOfBounds(columnIndex)

    fun getIndexes(element: T?): Set<IntPair> {
        val out = HashSet<IntPair>()

        if (element == null) {
            for (x in 0 until columns) for (y in 0 until rows) if (this[x, y] == null) out.add(x pairTo y)
            return out
        }

        val indexes = elementToIndexMap[element] ?: return out

        val iter = indexes.iterator()
        while (iter.hasNext) {
            val index = iter.next()
            out.add((index % columns) pairTo (index / columns))
        }

        return out
    }

    fun forEach(action: ((Int, Int, T?) -> Unit)) {
        for (x in 0 until columns) for (y in 0 until rows) {
            @Suppress("UNCHECKED_CAST")
            action(x, y, elements[y * columns + x] as T?)
        }
    }

    fun flatten(out: Array<T>): Array<T> {
        forEach { _, _, element -> out.add(element) }
        return out
    }

    override fun contains(element: T) = elementToIndexMap.containsKey(element)

    override fun containsAll(elements: Collection<T>) = elements.all { contains(it) }

    override fun clear() {
        elements.fill(null)
        count = 0
        elementToIndexMap.clear()
    }

    override fun addAll(elements: Collection<T>) = elements.all { add(it) }

    override fun add(element: T): Boolean {
        for (x in 0 until columns) for (y in 0 until rows) if (elements[y * columns + x] == null) {
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

        for (index in 0 until rows * columns) {
            @Suppress("UNCHECKED_CAST")
            val e = this.elements[index] as T? ?: continue

            if (!elements.contains(e)) {
                toRemove.add(e)
                removed = true
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
        val indexes = elementToIndexMap.remove(element) ?: return false

        val iter = indexes.iterator()
        while (iter.hasNext) {
            elements[iter.next()] = null
            count--
        }

        return true
    }

    override fun hashCode(): Int {
        var result = rows
        result = 31 * result + columns
        for (index in 0 until rows * columns) result = 31 * result + (elements[index]?.hashCode() ?: 0)
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
