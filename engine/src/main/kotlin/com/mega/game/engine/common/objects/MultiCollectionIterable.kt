package com.mega.game.engine.common.objects

import com.badlogic.gdx.utils.Array


class MultiCollectionIterator<T>(private val multiCollectionIterable: MultiCollectionIterable<T>) : Iterator<T> {

    private var outerIndex = 0
    private var currentInnerIterator: Iterator<T>? = null


    override fun hasNext(): Boolean {
        val iterables = multiCollectionIterable.iterables
        if (outerIndex >= iterables.size) return false

        if (currentInnerIterator == null) {
            // Initialize the inner iterator with the current collection
            currentInnerIterator = iterables[outerIndex].iterator()
        }

        while (!currentInnerIterator!!.hasNext()) {
            // Move to the next collection when the current one is exhausted
            outerIndex++
            if (outerIndex >= iterables.size) return false
            currentInnerIterator = iterables[outerIndex].iterator()
        }

        return true
    }


    override fun next(): T {
        if (!hasNext()) throw NoSuchElementException()
        return currentInnerIterator!!.next()
    }
}


class MultiCollectionIterable<T>(internal val iterables: Array<Iterable<T>>) : Iterable<T> {


    override fun iterator() = MultiCollectionIterator(this)


    fun forEach(action: (outerIndex: Int, innerIndex: Int, value: T) -> Unit) {
        var outerIndex = 0
        iterables.forEach {
            var innerIndex = 0
            it.forEach { value ->
                action(outerIndex, innerIndex, value)
                innerIndex++
                outerIndex++
            }
        }
    }
}