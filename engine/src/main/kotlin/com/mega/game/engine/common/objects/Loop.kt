package com.mega.game.engine.common.objects

import com.badlogic.gdx.utils.Array
import com.mega.game.engine.common.interfaces.Resettable

class Loop<T> : Iterable<T>, Resettable {

    private var array = Array<T>()
    private var index = 0

    val size: Int
        get() = array.size

    constructor(vararg elements: T) {
        array = Array(elements)
        index = 0
    }

    constructor(elements: Array<T>, startBeforeFirst: Boolean = false) {
        array = Array(elements)
        index = if (startBeforeFirst) -1 else 0
    }

    constructor(loop: Loop<T>) {
        array = Array(loop.array)
        index = loop.index
    }

    fun setIndex(index: Int) {
        this.index = index
    }

    fun next(): T {
        if (size == 0) throw NoSuchElementException("The loop is empty.")
        if (index >= size - 1) index = 0 else index++
        val value = array[index]
        return value
    }

    fun isBeforeFirst() = index == -1

    fun isFirst() = index == 0

    fun isLast() = index == size - 1

    fun getCurrent(): T {
        if (size == 0) throw NoSuchElementException("The loop is empty.")
        if (isBeforeFirst()) throw NoSuchElementException(
            "The loop is set to before the first element. Must call 'next()' first."
        )
        return array[index]
    }

    override fun iterator() = array.iterator()

    override fun reset() {
        index = 0
    }

    override fun toString() = "Loop(array=$array, index=$index)"

    override fun hashCode() = array.hashCode()

    override fun equals(other: Any?) = other is Loop<*> && array == other.array
}
