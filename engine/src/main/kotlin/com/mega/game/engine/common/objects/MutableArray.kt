package com.mega.game.engine.common.objects

import com.badlogic.gdx.utils.Array


fun <T> mutableArrayOf(vararg values: T): MutableArray<T> {
    val array = MutableArray<T>()
    values.forEach { array.add(it) }
    return array
}


class MutableArray<T> : MutableCollection<T> {

    private val array = Array<T>()

    override val size = array.size

    fun sort() = array.sort()

    fun sort(comparator: Comparator<T>) = array.sort(comparator)

    fun reverse() = array.reverse()

    fun shuffle() = array.shuffle()

    fun truncate(newSize: Int) = array.truncate(newSize)

    fun random() = array.random()

    fun removeIndex(index: Int) = array.removeIndex(index)

    fun removeRange(start: Int, end: Int) = array.removeRange(start, end)

    fun removeValue(value: T, identity: Boolean) = array.removeValue(value, identity)

    fun pop() = array.pop()

    fun insert(index: Int, value: T) = array.insert(index, value)

    fun swap(first: Int, second: Int) = array.swap(first, second)

    fun set(index: Int, value: T) = array.set(index, value)

    fun get(index: Int): T = array.get(index)

    fun indexOf(value: T, identity: Boolean) = array.indexOf(value, identity)

    fun lastIndexOf(value: T, identity: Boolean) = array.lastIndexOf(value, identity)

    fun contains(value: T, identity: Boolean) = array.contains(value, identity)

    override fun clear() = array.clear()

    override fun addAll(elements: Collection<T>): Boolean {
        elements.forEach { array.add(it) }
        return true
    }

    override fun add(element: T): Boolean {
        array.add(element)
        return true
    }

    override fun isEmpty() = array.isEmpty

    override fun iterator() = array.iterator()

    override fun retainAll(elements: Collection<T>): Boolean {
        val iterator = array.iterator()
        while (iterator.hasNext()) {
            val next = iterator.next()
            if (!elements.contains(next)) iterator.remove()
        }
        return true
    }

    override fun removeAll(elements: Collection<T>): Boolean {
        val iterator = array.iterator()
        while (iterator.hasNext()) {
            val next = iterator.next()
            if (elements.contains(next)) iterator.remove()
        }
        return true
    }

    override fun remove(element: T) = array.removeValue(element, false)

    override fun containsAll(elements: Collection<T>): Boolean {
        elements.forEach { if (!array.contains(it, false)) return false }
        return true
    }

    override fun contains(element: T) = array.contains(element, false)
}